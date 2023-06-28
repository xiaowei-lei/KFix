package com.kfix.sdk;

public class Patch {
    public final String path;
    public final String oDexPath;
    public final String libraryPath;
    public Patch(String path, String oDexPath, String libraryPath) {
        this.path = path;
        this.oDexPath = oDexPath;
        this.libraryPath = libraryPath;
    }
}
