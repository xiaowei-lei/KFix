# KFix
Android Hotfix solution by insert parent ClassLoader on runtime.
```
Bootstrap ClassLoader
       ^
       |
Patch DexClassLoader
       ^
       |
PathClassLoader -> New PathClassLoader
```

## Usage
### Settings
1. AGP version >= `7.3.1` 
```text
# project build.gradle.kts
buildscript {
    dependencies {
        classpath("com.android.tools.build:gradle:${version}")
        ...
    }
}
```

2. proguard-rules.pro config
```text
-keep class com.kfix.sdk.** { *; }
-dontoptimize
```

3. add `sdk` dependency and apply patch when the application launch, see the `app` for more.

### Patch Generating
1. commit your fixed code
2. `proguard-rules.pro` applyMapping
```text
-dontoptimize
-applymapping "Replace with old apk's mapping file path"
```
3. build apk
4. generate the patch using command
```shell
./gradlew :patch:run --args "$WORKSPACE $OLD_APK $OLD_MAPPING $NEW_APK $NEW_MAPPING"
```
`WORKSPACE`: A folder that will contain temporary output files and patch file during the patch generating.