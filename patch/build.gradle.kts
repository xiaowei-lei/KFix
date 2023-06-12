plugins {
    `kotlin-dsl`
}

repositories {
    google()
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.7.20")

    // apkanalyzer implementations baksmali/dexlib2/android.tools:common, manually add these dependencies
    implementation("com.android.tools.apkparser:apkanalyzer:31.2.0-alpha05")
    implementation("com.android.tools:common:31.2.0-alpha05")

    implementation("com.android.tools.smali:smali:3.0.3")
    implementation("com.android.tools.smali:smali-baksmali:3.0.3")
    implementation("com.android.tools.smali:smali-util:3.0.3")
    implementation("com.android.tools.smali:smali-dexlib2:3.0.3")

    testImplementation ("junit:junit:4.13.2")
    testImplementation ("io.kotest:kotest-assertions-core:5.5.5")
    testImplementation("io.mockk:mockk:1.12.0")
}