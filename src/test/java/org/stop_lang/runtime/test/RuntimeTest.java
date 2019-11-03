package org.stop_lang.runtime.test;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
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
}
