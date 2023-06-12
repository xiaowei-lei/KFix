package com.kfix.patch.generator.tools.source;

public class CaseAccessPublicClassPublicFieldOrMethod {
    public CaseAccessPublicClassPublicFieldOrMethod() {
        PublicClass publicAccessClass = new PublicClass();
        System.out.println(publicAccessClass.publicAccessField);
        publicAccessClass.publicAccessMethod();
    }
}
