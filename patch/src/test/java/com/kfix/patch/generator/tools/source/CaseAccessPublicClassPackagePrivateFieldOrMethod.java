package com.kfix.patch.generator.tools.source;

public class CaseAccessPublicClassPackagePrivateFieldOrMethod {
    public CaseAccessPublicClassPackagePrivateFieldOrMethod() {
        PublicClass publicAccessClass = new PublicClass();
        System.out.println(publicAccessClass.packagePrivateField);
        publicAccessClass.packagePrivateMethod();
    }
}
