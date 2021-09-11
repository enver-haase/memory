package com.example.application.profiling;

import javax.annotation.Nullable;

public class ObjDescription {
    private ObjDescription(){}

    public static String getDescription(@Nullable Object o) {
        if (o == null){
            return "(null)";
        }
        else {
            return o.getClass().getName() + "@" + System.identityHashCode(o);
        }
    }
}
