package com.kfix.patch.generator.tools.source;


public class CaseAccessPackagePrivateClass {
    public CaseAccessPackagePrivateClass() {
        PackagePrivateClass packagedAccessClass = new PackagePrivateClass();
        System.out.println(packagedAccessClass.packagePrivateAccessField);
        packagedAccessClass.packagePrivateMethod();
    }
}
