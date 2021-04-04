package org.stop_lang.runtime.test;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.stop_lang.runtime.test.annotations.AnnotationsRuntime;
import org.stop_lang.runtime.test.annotations.AnnotationsRuntimeBase;
import org.stop_lang.stop.models.*;
import org.stop_lang.runtime.StopRuntimeErrorException;
import org.stop_lang.runtime.StopRuntimeException;
import org.stop_lang.runtime.StopRuntimeImplementation;
import org.stop_lang.runtime.StopRuntimeImplementationExecution;
import org.stop_lang.runtime.test.dynamic.DynamicRuntime;
import org.stop_lang.runtime.test.dynamic.DynamicRuntimeBase;
import org.stop_lang.runtime.test.enums.EnumRuntime;
import org.stop_lang.runtime.test.enums.EnumRuntimeBase;
import org.stop_lang.runtime.test.helloworld.HelloRuntime;
import org.stop_lang.runtime.test.helloworld.HelloRuntimeBase;
import org.stop_lang.stop.models.Enumeration;
import org.stop_lang.stop.validation.StopValidationException;

import java.io.IOException;
import java.util.*;

public class RuntimeTest {

    @Test
    public void smoke() throws Exception {
        HelloRuntime runtime = new HelloRuntime();
        HelloRuntimeBase startInstance = new HelloRuntimeBase("A");
        startInstance.put("test1", "hey now");
        HelloRuntimeBase stop = runtime.getRuntime().start(startInstance);
        Assertions.assertNotNull(stop);
        Assertions.assertNotNull(runtime.getRuntime().getStop());

        Assertions.assertNotNull(stop.get("h"));
        StateInstance h = (StateInstance)stop.get("h");
        Assertions.assertEquals("F IT", h.getProperty("i"));

        Assertions.assertNotNull(stop.get("j"));
        StateInstance j = (StateInstance)stop.get("j");
        Assertions.assertEquals("F IT", j.getProperty("k"));

        Assertions.assertNotNull(stop.get("n"));
        Assertions.assertEquals("F IT", stop.get("n"));

        List<StateInstance> orderedStates = runtime.getRuntime().getOrderedStates();
        Assertions.assertTrue(orderedStates.size() == 3);
    }

    @Test
    public void included() throws Exception {
        HelloRuntime runtime = new HelloRuntime();
        HelloRuntimeBase startInstance = new HelloRuntimeBase("IncludeTest");
        runtime.getRuntime().addPackageImplementation("test.models", new StopRuntimeImplementation<StateInstance>() {
            @Override
            public StateInstance buildStateInstance(StateInstance implementationInstance) throws StopRuntimeException {
                return implementationInstance;
            }

            @Override
            public StateInstance buildImplementationInstance(StateInstance stateInstance) throws StopRuntimeException {
                return stateInstance;
            }

            @Override
            public StateInstance execute(StateInstance implementationInstance, StopRuntimeImplementationExecution<StateInstance> execution) throws StopRuntimeErrorException, StopRuntimeException {
                if (implementationInstance.getState().getName().equalsIgnoreCase("test.models.D")){
                    Map<String, Object> props = new HashMap<String, Object>();
                    props.put("wow", "now");
                    StateInstance c = new StateInstance(runtime.getRuntime().getStop().getStates().get("test.models.C"), props);
                    return c;
                }
                Assertions.assertEquals("now", implementationInstance.getProperties().get("wow"));
                Assertions.assertEquals("cvalue", implementationInstance.getProperties().get("cvalue"));
                Assertions.assertEquals(3, ((Collection<String>)implementationInstance.getProperties().get("dcollection")).size());
                return null;
            }

            @Override
            public Object executeAndReturnValue(StateInstance implementationInstance, StopRuntimeImplementationExecution<StateInstance> execution) throws StopRuntimeErrorException, StopRuntimeException {
                if ( implementationInstance.getState().getName().equalsIgnoreCase("test.models.GetCValue")){
                    return "cvalue";
                }
                return null;
            }

            @Override
            public Collection executeAndReturnCollection(StateInstance implementationInstance, StopRuntimeImplementationExecution<StateInstance> execution) throws StopRuntimeErrorException, StopRuntimeException {
                if ( implementationInstance.getState().getName().equalsIgnoreCase("test.models.GetDCollection")){
                    List<String> collection = new ArrayList<>();
                    collection.add("one");
                    collection.add("two");
                    collection.add("three");
                    return collection;
                }
                return null;
            }

            @Override
            public void enqueue(StateInstance implementationInstance) {

            }

            @Override
            public void enqueue(StateInstance implementationInstance, Integer delayInSeconds) {

            }

            @Override
            public void log(String message) {

            }
        });
        HelloRuntimeBase stop = runtime.getRuntime().start(startInstance);
        Assertions.assertNotNull(stop);
        Assertions.assertNotNull(runtime.getRuntime().getStop());
        Assertions.assertEquals(stop.getName(), "test.models.C");
        Assertions.assertEquals(stop.get("wow"), "now");
    }

    @Test
    public void badStart() throws Exception {
        HelloRuntime runtime = new HelloRuntime();
        HelloRuntimeBase startInstance = new HelloRuntimeBase("Y");
        try {
            HelloRuntimeBase stop = runtime.getRuntime().start(startInstance);
            Assertions.fail();
        }catch(StopRuntimeException e){
            // GOod
        }

        HelloRuntime runtime2 = new HelloRuntime();
        HelloRuntimeBase startInstance2 = new HelloRuntimeBase("Iso");
        try {
            HelloRuntimeBase stop2 = runtime2.getRuntime().start(startInstance2);
            Assertions.fail();
        }catch(StopRuntimeException e){
            // GOod
        }
    }

    @Test
    public void dynamic() throws Exception {
        DynamicRuntime runtime = new DynamicRuntime();
        DynamicRuntimeBase startInstance = new DynamicRuntimeBase("Begin");
        startInstance.put("v", "test v");
        DynamicRuntimeBase stop = runtime.getRuntime().start(startInstance);
        Assertions.assertNotNull(stop);
        Assertions.assertNotNull(runtime.getRuntime().getStop());
        Assertions.assertEquals("End", stop.getName());
        Assertions.assertNotNull(stop.get("v"));
        Assertions.assertNull(stop.get("w"));
        Assertions.assertNotNull(stop.get("a"));
        Assertions.assertEquals("GetA", stop.get("a"));
        Assertions.assertNotNull(stop.get("b"));
        Assertions.assertEquals("GetB", stop.get("b"));
        Assertions.assertNotNull(stop.get("c"));
        Assertions.assertEquals("GetC", stop.get("c"));
        Assertions.assertNotNull(stop.get("d"));
        Assertions.assertEquals("GetD", stop.get("d"));
        Assertions.assertNotNull(stop.get("e"));
        StateInstance e = (StateInstance)stop.get("e");
        Assertions.assertNotNull(e.getProperties().get("f"));
        for (StateInstance f : (List<StateInstance>)e.getProperties().get("f")){
            Assertions.assertEquals(f.getProperty("g"), "g");
            Assertions.assertEquals(f.getProperty("h"), "GetH");
        }
        String optionalTest = (String)stop.get("optional_test");
        Assertions.assertNotNull(optionalTest);
        Assertions.assertEquals("OptionalGetTestAlphaOne_null_OptionalGetTestAlphaTwo_null_null", optionalTest);
    }

    @Test
    public void dynamicOptionals() throws Exception {
        DynamicRuntime runtime = new DynamicRuntime();
        DynamicRuntimeBase startInstance = new DynamicRuntimeBase("Begin");
        startInstance.put("v", "test v");
        startInstance.put("w", "optional w");
        DynamicRuntimeBase stop = runtime.getRuntime().start(startInstance);
        Assertions.assertNotNull(stop);
        Assertions.assertNotNull(runtime.getRuntime().getStop());
        Assertions.assertEquals("End", stop.getName());
        Assertions.assertNotNull(stop.get("v"));
        Assertions.assertNotNull(stop.get("w"));
        Assertions.assertNotNull(stop.get("a"));
        Assertions.assertEquals("GetA", stop.get("a"));
        Assertions.assertNotNull(stop.get("b"));
        Assertions.assertEquals("GetB", stop.get("b"));
        Assertions.assertNotNull(stop.get("c"));
        Assertions.assertEquals("GetC", stop.get("c"));
        Assertions.assertNotNull(stop.get("d"));
        Assertions.assertEquals("GetD", stop.get("d"));
        Assertions.assertNotNull(stop.get("e"));
        StateInstance e = (StateInstance)stop.get("e");
        Assertions.assertNotNull(e.getProperties().get("f"));
        for (StateInstance f : (List<StateInstance>)e.getProperties().get("f")){
            Assertions.assertEquals(f.getProperty("g"), "g");
            Assertions.assertEquals(f.getProperty("h"), "GetH");
        }
        String optionalTest = (String)stop.get("optional_test");
        Assertions.assertNotNull(optionalTest);
        Assertions.assertEquals("OptionalGetTestAlphaOne_optional w_OptionalGetTestAlphaTwo_optional w_OptionalGetTestAlphaThree", optionalTest);
    }

    @Test
    public void enums() throws Exception {
        EnumRuntime runtime = new EnumRuntime();
        EnumRuntimeBase startInstance = new EnumRuntimeBase("sandbox.A");
        Enumeration e = runtime.getStop().getStates().get("sandbox.A").getEnumerations().get("Y");
        EnumerationInstance ei = new EnumerationInstance(e, "TWO");
        startInstance.put("f", ei);
        EnumRuntimeBase stop = runtime.getRuntime().start(startInstance);
        Assertions.assertNotNull(stop);
    }

    @Test
    public void getOrderedPropertiesForState() throws IOException, StopValidationException {
        HelloRuntime runtime = new HelloRuntime();
        State b = runtime.getRuntime().getStop().getStates().get("B");
        Collection<Property> properties = b.getOrderedProperties();
        Assertions.assertEquals(properties.size(), 13);
        int aj = -1;
        int n = -1;
        int h = -1;
        int i = 0;
        for (Property p : properties){
            if (p.getName().equalsIgnoreCase("aj")){
                aj = i;
            }
            if (p.getName().equalsIgnoreCase("n")){
                n = i;
            }
            if (p.getName().equalsIgnoreCase("h")){
                h = i;
            }
            i++;
        }
        Assertions.assertTrue(aj < n);
        Assertions.assertTrue(h < aj);
    }

    @Test
    public void annotations() throws Exception {
        AnnotationsRuntime runtime = new AnnotationsRuntime();

        Map<String, Object> params = new HashMap<>();
        params.put("name", "Hammer");
        params.put("weight", 1.5);
        StateInstance tool = new StateInstance(runtime.getRuntime().getStop().getStates().get("Hammer"), params);

        Map<String, Object> params1 = new HashMap<>();
        params1.put("name", "Gala");
        StateInstance apple1 = new StateInstance(runtime.getRuntime().getStop().getStates().get("Gala"), params1);

        Map<String, Object> params2 = new HashMap<>();
        params2.put("name", "Honeycrisp");
        StateInstance apple2 = new StateInstance(runtime.getRuntime().getStop().getStates().get("Honeycrisp"), params2);

        Collection<StateInstance> apples = new ArrayList<>();
        apples.add(apple1);
        apples.add(apple2);

        AnnotationsRuntimeBase startInstance = new AnnotationsRuntimeBase("Begin");
        startInstance.put("tool", tool);
        startInstance.put("index", 1);
        startInstance.put("apples", apples);
        startInstance.put("apple_type", "Honeycrisp");
        startInstance.put("digits", "123");
        startInstance.put("email", "kyle.shank@email.com");
        startInstance.put("number", -33.333);
        startInstance.put("minonly", 1337L);
        AnnotationsRuntimeBase stop = runtime.getRuntime().start(startInstance);
        Assertions.assertNotNull(stop);
        Assertions.assertEquals(stop.getName(), "End");

        AnnotationsRuntimeBase startInstance2 = new AnnotationsRuntimeBase("Begin");
        startInstance2.put("tool", tool);
        startInstance2.put("index", 2);
        AnnotationsRuntimeBase stop2 = runtime.getRuntime().start(startInstance2);
        Assertions.assertNotNull(stop2);
        Assertions.assertEquals(stop2.getName(), "AlternateEnding");

        AnnotationsRuntimeBase startInstance3 = new AnnotationsRuntimeBase("Begin");
        startInstance3.put("tool", tool);
        startInstance3.put("index", 3);
        Assertions.assertThrows(StopRuntimeException.class, new Executable() {
            @Override
            public void execute() throws Throwable {
                runtime.getRuntime().start(startInstance3);
            }
        });

        AnnotationsRuntimeBase startInstance4 = new AnnotationsRuntimeBase("Begin");
        startInstance4.put("tool", tool);
        startInstance4.put("index", 4);
        AnnotationsRuntimeBase stop4 = runtime.getRuntime().start(startInstance4);
        Assertions.assertNotNull(stop4);
        Assertions.assertEquals(stop4.getName(), "End");

        AnnotationsRuntimeBase startInstance5 = new AnnotationsRuntimeBase("Begin");
        startInstance5.put("tool", tool);
        startInstance5.put("index", 5);
        AnnotationsRuntimeBase stop5 = runtime.getRuntime().start(startInstance5);
        Assertions.assertNotNull(stop5);
        Assertions.assertEquals(stop5.getName(), "AnotherError");

        Iterator<Annotation> it = runtime.getRuntime().getStop().getStates().get("StateWithTemplateParameters").getAnnotations().iterator();
        StateAnnotation stateAnnotation = (StateAnnotation)it.next();
        Assertions.assertEquals(stateAnnotation.getState().getName(), "Error");
        Assertions.assertEquals(stateAnnotation.getParameters().get("scream"), "yes");
        Annotation templateAnnotation = it.next();
        Assertions.assertEquals(templateAnnotation.getParameters().get("testing"), "123");

        AnnotationsRuntimeBase startInstance6 = new AnnotationsRuntimeBase("Begin");
        startInstance6.put("tool", tool);
        startInstance6.put("index", 3);
        Collection<StateInstance> apples2 = new ArrayList<>();
        apples2.add(tool);
        startInstance6.put("apples", apples2);

        Assertions.assertThrows(StopValidationException.class, new Executable() {
            @Override
            public void execute() throws Throwable {
                runtime.getRuntime().start(startInstance6);
            }
        });
    }
}
