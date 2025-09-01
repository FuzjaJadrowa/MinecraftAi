plugins {
    java
    application
}

repositories {
    mavenCentral()
}

val lwjglVersion = "3.3.3"
val jomlVersion = "1.10.5"

dependencies {
    implementation("org.lwjgl:lwjgl:$lwjglVersion")
    implementation("org.lwjgl:lwjgl-opengl:$lwjglVersion")
    implementation("org.lwjgl:lwjgl-glfw:$lwjglVersion")
    implementation("org.joml:joml:$jomlVersion")

    runtimeOnly("org.lwjgl:lwjgl:$lwjglVersion:natives-windows")
    runtimeOnly("org.lwjgl:lwjgl-opengl:$lwjglVersion:natives-windows")
    runtimeOnly("org.lwjgl:lwjgl-glfw:$lwjglVersion:natives-windows")

}

application {
    mainClass.set("com.minecraftai.Main")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}