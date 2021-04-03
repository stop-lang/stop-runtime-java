package org.stop_lang.runtime.test.annotations;

import org.junit.jupiter.api.Assertions;
import org.stop_lang.runtime.*;
import org.stop_lang.runtime.test.dynamic.DynamicRuntimeBase;
import org.stop_lang.stop.Stop;
import org.stop_lang.stop.models.Annotation;
import org.stop_lang.stop.models.StateInstance;
import org.stop_lang.stop.validation.StopValidationException;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class AnnotationsRuntime  implements StopRuntimeImplementation<AnnotationsRuntimeBase> {
    private Stop stop;
    private StopRuntime<AnnotationsRuntimeBase> runtime;

    public AnnotationsRuntime() throws IOException, StopValidationException {
        stop = new Stop("./examples/annotations.stop");
        runtime = new StopRuntime<>(stop, this);
    }

    public StopRuntime<AnnotationsRuntimeBase> getRuntime(){
        return runtime;
    }

    @Override
    public StateInstance buildStateInstance(AnnotationsRuntimeBase implementationInstance) throws StopRuntimeException {
        return new StateInstance(this.stop.getStates().get(implementationInstance.getName()), implementationInstance);
    }

    @Override
    public AnnotationsRuntimeBase buildImplementationInstance(StateInstance stateInstance)  throws StopRuntimeException {
        AnnotationsRuntimeBase base = new AnnotationsRuntimeBase(stateInstance.getState().getName(), stateInstance.getState().getAnnotations());
        base.putAll(stateInstance.getProperties());
        return base;
    }

    @Override
    public AnnotationsRuntimeBase execute(AnnotationsRuntimeBase implementationInstance, StopRuntimeImplementationExecution<AnnotationsRuntimeBase> execution) throws StopRuntimeErrorException {
        if (implementationInstance.getName().equalsIgnoreCase("Begin")){
            Integer index = (Integer) implementationInstance.get("index");
            if (index==1){
                try {
                    execution.queue(new AnnotationsRuntimeBase("Notifications"));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return new AnnotationsRuntimeBase("End");
            }
            if (index==2){
                return new AnnotationsRuntimeBase("AlternateEnding");
            }
            if (index==3){
                return new AnnotationsRuntimeBase("BadEnding");
            }
            if (index==4){
                try {
                    execution.queue(new AnnotationsRuntimeBase("AppNotifications"));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return new AnnotationsRuntimeBase("End");
            }
            if (index==5){
                Map<String, Object> params = new HashMap<>();
                params.put("message", "error");
                StateInstance errorInstance = new StateInstance(stop.getStates().get("AnotherError"), params );
                throw new StopRuntimeErrorException(errorInstance);
            }
        }

        if (implementationInstance.getName().equalsIgnoreCase("Notifications")){
            return new AnnotationsRuntimeBase("Send");
        }

        if (implementationInstance.getName().equalsIgnoreCase("AppNotifications")){
            return new AnnotationsRuntimeBase("Send");
        }

        if (implementationInstance.getName().contains("End")){
            boolean found = false;
            for (Annotation annotation : implementationInstance.getAnnotations()){
                if (annotation.getName().equalsIgnoreCase("template")){
                    found = true;
                }
            }
            Assertions.assertTrue(found);
        }

        return null;
    }

    @Override
    public Object executeAndReturnValue(AnnotationsRuntimeBase implementationInstance, StopRuntimeImplementationExecution<AnnotationsRuntimeBase> execution) throws StopRuntimeErrorException {
        if (implementationInstance.getName().equalsIgnoreCase("GetTool")){
            AnnotationsRuntimeBase tool2 = new AnnotationsRuntimeBase("Hammer");
            tool2.put("name", "Hammer");
            tool2.put("weight", 1.7);
            return tool2;
        }

        return null;
    }

    @Override
    public Collection executeAndReturnCollection(AnnotationsRuntimeBase implementationInstance, StopRuntimeImplementationExecution<AnnotationsRuntimeBase> execution) throws StopRuntimeErrorException {

        return null;
    }

    @Override
    public void enqueue(AnnotationsRuntimeBase implementationInstance) {

    }

    @Override
    public void enqueue(AnnotationsRuntimeBase implementationInstance, Integer delayInSeconds) {

    }

    @Override
    public void log(String message){

    }
}
