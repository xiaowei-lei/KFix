package com.kfix.patch.generator.tools.source;
public class PublicClass {
    int packagePrivateField = 1;
    public int publicAccessField = 2;
    protected int protectedField = 3;

    void packagePrivateMethod() {
        System.out.println("defaultAccessMethod");
    }

    public void publicAccessMethod() {
        System.out.println("publicAccessMethod");
    }

    protected void protectedHello() {
        System.out.println("protectedHello");
    }
}
