package com.kfix.patch.generator.tools.source;

import com.kfix.patch.generator.tools.source.sub.SubPublicClass;

public class CaseAccessPublicClassProtectedFieldOrMethod extends SubPublicClass {
    public CaseAccessPublicClassProtectedFieldOrMethod() {
        System.out.println(protectedField);
        protectedHello();
    }
}
