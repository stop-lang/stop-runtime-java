package org.stop_lang.runtime.test.dynamic;

import java.util.HashMap;

public class DynamicRuntimeBase extends HashMap<String, Object> {
    private String name;

    public DynamicRuntimeBase(String name){
        super();
        this.name = name;
    }

    public String getName(){
        return this.name;
    }
}
