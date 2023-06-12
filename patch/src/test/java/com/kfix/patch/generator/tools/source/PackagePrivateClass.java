package com.kfix.patch.generator.tools.source;
class PackagePrivateClass {
    int packagePrivateAccessField = 1;

    void packagePrivateMethod() {
        new PackagePrivateClass2().packagePrivateMethod();
    }
}
