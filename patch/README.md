# Patch Generate Tool
Library to generate patch.

## Usage
```shell
./gradlew :patch:run --args "$WORKSPACE $OLD_APK $OLD_MAPPING $NEW_APK $NEW_MAPPING"
```
`WORKSPACE`: A folder that will contain temporary output files and patch file during the patch generating.
