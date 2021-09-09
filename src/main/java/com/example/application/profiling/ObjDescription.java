package com.example.application.profiling;

public class ObjDescription {
    private ObjDescription(){}

    public static String getDescription(Object o) {
        return o.getClass().getName() + "@" + System.identityHashCode(o);
    }
}
