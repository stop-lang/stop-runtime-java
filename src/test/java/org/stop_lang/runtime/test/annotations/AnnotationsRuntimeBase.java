package org.stop_lang.runtime.test.annotations;

import org.stop_lang.stop.models.Annotation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

public class AnnotationsRuntimeBase extends HashMap<String, Object>{
    private String name;
    private Collection<Annotation> annotations;

    public AnnotationsRuntimeBase(String name){
        super();
        this.name = name;
        this.annotations = new ArrayList<>();
    }
    public AnnotationsRuntimeBase(String name, Collection<Annotation> annotationCollection){
        super();
        this.name = name;
        this.annotations = annotationCollection;
    }

    public String getName(){
        return this.name;
    }

    public Collection<Annotation> getAnnotations(){
        return annotations;
    }
}
