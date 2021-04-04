package org.stop_lang.runtime;

import org.stop_lang.stop.Stop;
import org.stop_lang.stop.models.*;
import org.stop_lang.stop.validation.StopValidationException;

import java.util.*;
import java.util.regex.Pattern;

public class StopRuntime<T> implements StopRuntimeImplementationExecution<T> {
    private final static String REFERENCE_DELIMETER = ".";

    private Stop stop;
    private StopRuntimeImplementation<T> implementation;
    private StateInstance currentStateInstance = null;
    private List<StateInstance> orderedStates = new ArrayList<StateInstance>();
    private Map<String, StopRuntimeImplementation<StateInstance>> packageImplementations;
    private StopRuntimeImplementationExecution<StateInstance> packageImplementationRuntimeImplementationExecution;

    public StopRuntime(Stop stop, StopRuntimeImplementation<T> implementation){
        this.stop = stop;
        this.implementation = implementation;
        this.packageImplementations = new HashMap<>();
        this.packageImplementationRuntimeImplementationExecution = new StopRuntimeImplementationExecution<StateInstance>() {
            @Override
            public void queue(StateInstance implementationInstance) throws StopRuntimeException, StopValidationException {
                packageImplementationRuntimeImplementationExecutionQueue(implementationInstance);
            }

            @Override
            public void log(String message) {
                packageImplementationRuntimeImplementationExecutionLog(message);
            }
        };
    }

    public Stop getStop(){
        return this.stop;
    }

    public T start(T toImplementationInstance) throws StopRuntimeException, StopValidationException {
        StateInstance to = implementation.buildStateInstance(toImplementationInstance);
        return start(to);
    }

    @Override
    public void queue(T implementationInstance) throws StopRuntimeException, StopValidationException {
        if (currentStateInstance == null){
            throw new StopRuntimeException("No current state instance");
        }

        StateInstance queue = implementation.buildStateInstance(implementationInstance);

        if (queue == null){
            throw new StopRuntimeException("queue state instance must be defined");
        }

        StateTransition foundStateTransition = null;

        for (StateTransition transition : currentStateInstance.getState().getEnqueues().values()){
            if (transition.getState().equals(queue.getState())){
                foundStateTransition = transition;
                break;
            }

            if (transition.isAnnotation()){
                for (Annotation annotation : queue.getState().getAnnotations()){
                    if (annotation instanceof StateAnnotation){
                        StateAnnotation stateAnnotation = (StateAnnotation) annotation;
                        if (transition.getState().equals(stateAnnotation.getState())){
                            foundStateTransition = transition;
                            break;
                        }
                    }
                }
                if (foundStateTransition!=null){
                    break;
                }
            }
        }

        if (foundStateTransition == null){
            throw new StopRuntimeException("Could not find queue " + queue.getState().getName());
        }

        if(!foundStateTransition.getState().isQueue()){
            throw new StopRuntimeException("Invalid queue state");
        }

        validateStateInstance(queue, false);

        implementation.enqueue(implementationInstance);
    }

    @Override
    public void log(String message){
        implementation.log(message);
    }

    public List<StateInstance> getOrderedStates(){
        return this.orderedStates;
    }

    public void addPackageImplementation(String packageName, StopRuntimeImplementation<StateInstance> packageImplementation ){
        this.packageImplementations.put(packageName, packageImplementation);
    }

    public void removePackageImplementation(String packageName){
        this.packageImplementations.remove(packageName);
    }

    private T start(StateInstance to) throws StopRuntimeException, StopValidationException {
        orderedStates.clear();

        if (to == null){
            throw new StopRuntimeException("To state instances must be defined");
        }

        if (!to.getState().isStart() && !to.getState().isQueue()){
            throw new StopRuntimeException("Invalid start state");
        }

        StateInstance resultInstance = execute(to);

        if (resultInstance!=null){
            if (!resultInstance.getState().isStop()){
                throw new StopRuntimeException(resultInstance.getState().getName()  + " is not a stopping state!");
            }
            return implementation.buildImplementationInstance(resultInstance);
        }

        throw new StopRuntimeException("No ending state!");
    }

    private StateInstance execute(StateInstance stateInstance) throws StopRuntimeException, StopValidationException{
        try {
            gatherDynamicProperties(stateInstance);
        }catch(StopRuntimeErrorException errorException){
            StateInstance errorState = errorException.getErrorStateInstance();
            StateInstance contextState = errorException.getContextStateInstance();

            if (errorState == null){
                throw new StopRuntimeException("Error state was undefined in StopRuntimeErrorException during dynamic property gathering");
            }

            if (contextState == null){
                throw new StopRuntimeException("Context state was undefined in StopRuntimeErrorException during dynamic property gathering");
            }

            return transition(contextState, errorState);
        }

        validateStateInstance(stateInstance, true);

        currentStateInstance = stateInstance;

        orderedStates.add(stateInstance);

        T implementationInstance = implementation.buildImplementationInstance(stateInstance);

        try {
            T nextImplementationInstance = executeWithPackageImplementations(implementationInstance);

            if (nextImplementationInstance != null) {
                StateInstance nextStateInstance = implementation.buildStateInstance(nextImplementationInstance);
                return transition(stateInstance, nextStateInstance);
            } else {
                return stateInstance;
            }
        } catch (StopRuntimeErrorException errorException) {
            StateInstance errorStateInstance = errorException.getErrorStateInstance();
            return transition(stateInstance, errorStateInstance);
        }
    }

    private StateInstance transition(StateInstance from, StateInstance to) throws StopRuntimeException, StopValidationException {
        if (from == null || to == null){
            throw new StopRuntimeException("From and to state instances must be defined");
        }

        validateStateInstance(from, true);

        State toState = to.getState();

        if (toState==null){
            throw new StopRuntimeException("Could not find state " + toState.getName());
        }

        StateTransition errorStateTransition = null;
        for (StateTransition transition : from.getState().getErrors().values()){
            if (transition.getState().equals(toState)){
                errorStateTransition = transition;
                break;
            }

            if (transition.isAnnotation()){
                for (Annotation annotation : toState.getAnnotations()){
                    if (annotation instanceof StateAnnotation){
                        StateAnnotation stateAnnotation = (StateAnnotation) annotation;
                        if (transition.getState().equals(stateAnnotation.getState())){
                            errorStateTransition = transition;
                            break;
                        }
                    }
                }
                if (errorStateTransition!=null){
                    break;
                }
            }
        }

        StateTransition stateTransition = null;
        for (StateTransition transition : from.getState().getTransitions().values()){
            if (transition.getState().equals(toState)){
                stateTransition = transition;
                break;
            }

            if (transition.isAnnotation()){
                for (Annotation annotation : toState.getAnnotations()){
                    if (annotation instanceof StateAnnotation){
                        StateAnnotation stateAnnotation = (StateAnnotation) annotation;
                        if (transition.getState().equals(stateAnnotation.getState())){
                            stateTransition = transition;
                            break;
                        }
                    }
                }
                if (stateTransition!=null){
                    break;
                }
            }
        }

        if ((errorStateTransition == null) && (stateTransition == null)){
            throw new StopRuntimeException("Could not find state to transition to called " + to.getState().getName());
        }

        return execute(to);
    }

    private void gatherDynamicProperties(StateInstance to) throws StopRuntimeException, StopValidationException, StopRuntimeErrorException {
        Collection<Property> orderedDynamicProperties = to.getState().getOrderedProperties();

        for (Property property : orderedDynamicProperties){
            if (property != null){
                if (property.getProvider() != null){
                    State providerState = property.getProvider();
                    if (property.isOptional() && !shouldMapProvider(to, property, providerState)){
                        continue;
                    }
                    StateInstance providerStateInstance = mapStateInstancePropertiesToProvider(to, providerState, property.getProviderMapping());
                    gatherDynamicProperties(providerStateInstance);
                    validateStateInstance(providerStateInstance, true);
                    T providerImplementationInstance = implementation.buildImplementationInstance(providerStateInstance);

                    try {
                        Object value = null;

                        if (providerState.isReturnCollection()) {
                            Collection collection = executeAndReturnCollectionWithPackageImplementations(providerImplementationInstance);

                            if (providerState.getReturnState() != null) {
                                List<StateInstance> stateInstances = new ArrayList<StateInstance>();
                                if(collection!=null) {
                                    for (Object collectionElement : collection) {
                                        StateInstance si = implementation.buildStateInstance((T) collectionElement);
                                        stateInstances.add(si);
                                    }
                                }
                                value = stateInstances;
                            } else {
                                value = collection;
                            }
                        } else {
                            Object returnValue = executeAndReturnValueWithPackageImplementations(providerImplementationInstance);

                            if (returnValue!=null) {
                                if (providerState.getReturnState() != null) {
                                    value = implementation.buildStateInstance((T) returnValue);
                                } else {
                                    value = returnValue;
                                }
                            }
                        }

                        if (value != null) {
                            if (value instanceof Collection){
                                Collection instances = (Collection)value;
                                for (Object instance : instances){
                                    if (instance instanceof StateInstance){
                                        gatherDynamicProperties((StateInstance)instance);
                                    }
                                }
                            }else if(value instanceof StateInstance){
                                gatherDynamicProperties((StateInstance)value);
                            }
                            to.getProperties().put(property.getName(), value);
                        }
                    }catch(StopRuntimeErrorException errorException){
                        throw new StopRuntimeErrorException(errorException.getErrorStateInstance(), providerStateInstance);
                    }
                }
            }
        }

        for ( Map.Entry<String, Object> entry : to.getProperties().entrySet() ){
            Object value = entry.getValue();
            if (value != null){
                if (value instanceof Collection){
                    Collection instances = (Collection)value;
                    for (Object instance : instances){
                        if (instance instanceof StateInstance){
                            StateInstance collectionStateInstance = (StateInstance)instance;
                            gatherDynamicProperties(collectionStateInstance);
                        }
                    }
                } else if (value instanceof StateInstance){
                    StateInstance propertyStateInstance = (StateInstance)value;
                    gatherDynamicProperties(propertyStateInstance);
                }
            }
        }
    }

    private StateInstance mapStateInstancePropertiesToProvider(StateInstance stateInstance, State providerState, Map<String, String> providerMapping){
        Map<String, Object> providerProperties = new HashMap<>();

        for (Map.Entry<String, Property> providerPropertyEntry : providerState.getProperties().entrySet()){
            String field = providerPropertyEntry.getKey();

            if (providerMapping!=null){
                if (providerMapping.containsKey(field)){
                    field = providerMapping.get(field);
                }
            }

            if (field.contains(REFERENCE_DELIMETER)){
                // Reference
                Object value = getValueForReference(stateInstance, field);
                if (value!=null){
                    providerProperties.put(providerPropertyEntry.getKey(), value);
                }
            }else {
                // Value
                if (stateInstance.getProperties().containsKey(field)) {
                    providerProperties.put(providerPropertyEntry.getKey(), stateInstance.getProperties().get(field));
                }
            }
        }

        return new StateInstance(providerState, providerProperties);
    }

    private Object getValueForReference(StateInstance stateInstance, String reference){
        String[] parts = reference.split("\\.");
        String valueName = parts[0];

        if (valueName != null){
            Object value = stateInstance.getProperties().get(valueName);
            if (value != null){
                if (parts.length > 1){
                    if ( value instanceof StateInstance){
                        StateInstance valueStateInstance = (StateInstance)value;
                        List<String> newParts = new ArrayList<>();
                        for (int i = 1; i < parts.length; i++) {
                            newParts.add(parts[i]);
                        }
                        String newReference = String.join(REFERENCE_DELIMETER, newParts);
                        return getValueForReference(valueStateInstance, newReference);
                    }
                }else {
                    return value;
                }
            }
        }

        return null;
    }

    private boolean shouldMapProvider(StateInstance stateInstance, Property stateInstanceProperty, State providerState){
        for (Map.Entry<String, Property> providerStatePropertyEntry : providerState.getProperties().entrySet()){
            Property providerStatePropertyEntryProperty = providerStatePropertyEntry.getValue();
            if (providerStatePropertyEntryProperty!= null){
                if (providerStatePropertyEntryProperty.getProvider()!=null){
                    continue;
                }
            }
            String propertyName = providerStatePropertyEntry.getKey();
            if (stateInstanceProperty.getProviderMapping() != null){
                if (stateInstanceProperty.getProviderMapping().containsKey(propertyName)){
                    propertyName = stateInstanceProperty.getProviderMapping().get(propertyName);
                    propertyName = getRootFromPropertyName(propertyName);
                }
            }
            Property stateProperty = stateInstance.getState().getProperties().get(propertyName);
            if (stateProperty != null){
                if ((stateProperty.getProvider()==null)
                        && !providerStatePropertyEntryProperty.isOptional()
                        && (stateInstance.getProperties().get(propertyName)==null)){
                    return false;
                }
            }else{
                return false;
            }
        }
        return true;
    }

    private T executeWithPackageImplementations(T implementationInstance) throws StopRuntimeErrorException, StopRuntimeException {
        if (!packageImplementations.isEmpty()){
            StateInstance stateInstance = implementation.buildStateInstance(implementationInstance);
            String stateName = stateInstance.getState().getName();
            for (Map.Entry<String, StopRuntimeImplementation<StateInstance>> packageImplementation : packageImplementations.entrySet()){
                if (stateName.startsWith(packageImplementation.getKey()+REFERENCE_DELIMETER)){
                    StateInstance returnStateInstance = packageImplementation.getValue().execute(stateInstance, packageImplementationRuntimeImplementationExecution);
                    if (returnStateInstance!=null) {
                        return implementation.buildImplementationInstance(returnStateInstance);
                    }
                    return null;
                }
            }
        }

        return implementation.execute(implementationInstance, this);
    }

    private Object executeAndReturnValueWithPackageImplementations(T implementationInstance) throws StopRuntimeErrorException, StopRuntimeException {
        if (!packageImplementations.isEmpty()){
            StateInstance stateInstance = implementation.buildStateInstance(implementationInstance);
            String stateName = stateInstance.getState().getName();
            for (Map.Entry<String, StopRuntimeImplementation<StateInstance>> packageImplementation : packageImplementations.entrySet()){
                if (stateName.startsWith(packageImplementation.getKey()+REFERENCE_DELIMETER)){
                    Object returnObject = packageImplementation.getValue().executeAndReturnValue(stateInstance, packageImplementationRuntimeImplementationExecution);
                    return returnObject;
                }
            }
        }

        return implementation.executeAndReturnValue(implementationInstance, this);
    }

    private Collection executeAndReturnCollectionWithPackageImplementations(T implementationInstance) throws StopRuntimeErrorException, StopRuntimeException {
        if (!packageImplementations.isEmpty()){
            StateInstance stateInstance = implementation.buildStateInstance(implementationInstance);
            String stateName = stateInstance.getState().getName();
            for (Map.Entry<String, StopRuntimeImplementation<StateInstance>> packageImplementation : packageImplementations.entrySet()){
                if (stateName.startsWith(packageImplementation.getKey()+REFERENCE_DELIMETER)){
                    Collection returnCollection = packageImplementation.getValue().executeAndReturnCollection(stateInstance, packageImplementationRuntimeImplementationExecution);
                    return returnCollection;
                }
            }
        }

        return implementation.executeAndReturnCollection(implementationInstance, this);
    }

    private void packageImplementationRuntimeImplementationExecutionQueue(StateInstance stateInstance) throws StopRuntimeException, StopValidationException {
        queue(implementation.buildImplementationInstance(stateInstance));
    }

    private void packageImplementationRuntimeImplementationExecutionLog(String message) {
        log(message);
    }

    private String getRootFromPropertyName(String propertyName){
        String rootPropertyName = propertyName;

        if (propertyName.contains(REFERENCE_DELIMETER)) {
            String[] parts = propertyName.split("\\"+REFERENCE_DELIMETER);
            if (parts.length > 1) {
                rootPropertyName = parts[0];
            }
        }
        return rootPropertyName;
    }

    private void validateStateInstance(StateInstance stateInstance, boolean validateDynamicProperties) throws StopValidationException {
        stateInstance.validateProperties(validateDynamicProperties);
        runValidations(stateInstance);
    }

    private void runValidations(StateInstance stateInstance) throws StopValidationException{
        for (Property property : stateInstance.getState().getOrderedProperties()){
            Collection<PropertyValidation> validations = property.getValidations();
            if (validations!=null && !validations.isEmpty()){
                Object value = stateInstance.getProperty(property.getName());
                if (value!=null){
                    for (PropertyValidation validation : validations){
                        if (property.getType() == Property.PropertyType.STRING) {
                            String valueString = (String)value;
                            if (validation instanceof StatePropertyValidation) {
                                StatePropertyValidation statePropertyValidation = (StatePropertyValidation) validation;
                                State valueState = stop.getStates().get(valueString);
                                State propertyState = statePropertyValidation.getState();
                                if (statePropertyValidation.isInheritable()) {
                                    if ((valueState == null) || (!valueState.equals(propertyState) && !valueState.getInheritedStates().contains(propertyState))) {
                                        throw new StopValidationException("State instance " + stateInstance.getState().getName() + " property " + property.getName() + " doesn't validate with value " + valueString);
                                    }
                                }else{
                                    if ((valueState == null) || !valueState.equals(propertyState)) {
                                        throw new StopValidationException("State instance " + stateInstance.getState().getName() + " property " + property.getName() + " doesn't validate with value " + valueString);
                                    }
                                }
                            } else {
                                if (validation.getName().equalsIgnoreCase("regex")) {
                                    String matches = (String) validation.getParameters().get("matches");
                                    if (matches != null) {
                                        if (!Pattern.matches(matches, valueString)) {
                                            throw new StopValidationException(valueString + " doesn't match regex " + matches);
                                        }
                                    }
                                }else if (validation.getName().equalsIgnoreCase("length")){
                                    Integer min = 0;
                                    Integer max = Integer.MAX_VALUE;
                                    Integer exact = null;
                                    Integer stringLength = valueString.length();
                                    if (validation.getParameters().containsKey("min")){
                                        Object paramValue = (Object)validation.getParameters().get("min");
                                        if (paramValue instanceof Double){
                                            min = ((Double)paramValue).intValue();
                                        }
                                    }
                                    if (validation.getParameters().containsKey("max")){
                                        Object paramValue = (Object)validation.getParameters().get("max");
                                        if (paramValue instanceof Double){
                                            max = ((Double)paramValue).intValue();
                                        }
                                    }
                                    if (validation.getParameters().containsKey("exact")){
                                        Object paramValue = (Object)validation.getParameters().get("exact");
                                        if (paramValue instanceof Double){
                                            exact = ((Double)paramValue).intValue();
                                        }
                                    }
                                    if (exact!=null){
                                        if (valueString.length()!=exact){
                                            throw new StopValidationException(valueString + " is not "+ exact + "characters long");
                                        }
                                    }else {
                                        if (!((stringLength>=min) && (stringLength<=max))){
                                            throw new StopValidationException(valueString + " is not within "+ min + "..."+max+" characters");
                                        }
                                    }
                                }
                                // more

                            }
                        }else if (value instanceof Double || value instanceof Integer || value instanceof Long || value instanceof Float){
                            if (validation.getName().equalsIgnoreCase("range")){
                                Double valueDouble;
                                if (value instanceof Double){
                                    valueDouble = (Double) value;
                                }else if (value instanceof Float){
                                    valueDouble = ((Float)value).doubleValue();
                                }else if (value instanceof Integer){
                                    valueDouble = ((Integer)value).doubleValue();
                                }else{
                                    valueDouble = ((Long)value).doubleValue();
                                }

                                Double min = Double.MIN_VALUE;
                                Double max = Double.MAX_VALUE;
                                if (validation.getParameters().containsKey("min")){
                                    Object paramValue = (Object)validation.getParameters().get("min");
                                    if (paramValue instanceof Double){
                                        min = ((Double)paramValue);
                                    }
                                }
                                if (validation.getParameters().containsKey("max")){
                                    Object paramValue = (Object)validation.getParameters().get("max");
                                    if (paramValue instanceof Double){
                                        max = ((Double)paramValue);
                                    }
                                }
                                if (!((valueDouble>=min) && (valueDouble<=max))){
                                    throw new StopValidationException(valueDouble + " is not within range "+ min + "..."+max);
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
