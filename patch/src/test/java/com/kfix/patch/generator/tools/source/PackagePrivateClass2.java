package com.kfix.patch.generator.tools.source;
class PackagePrivateClass2 {
    int packagePrivateAccessField = 1;

    void packagePrivateMethod() {
        new PublicClass().publicAccessMethod();
    }
}
