buildscript {
    repositories {
        mavenCentral()
        maven { url = uri("https://maven.fabricmc.net/") }
    }
    dependencies {
        classpath("net.fabricmc:fabric-loom:1.8.1")
    }
}

apply(plugin = "fabric-loom")
apply(plugin = "java")
apply(plugin = "maven-publish")

group = "com.example"
version = "0.1.0-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
    maven { url = uri("https://maven.fabricmc.net/") }
}

dependencies {
    minecraft("com.mojang:minecraft:1.21.11")
    mappings("net.fabricmc:yarn:1.21.11+build.1:v2")
    modImplementation("net.fabricmc:fabric-loader:0.16.10")
    modApi("net.fabricmc.fabric-api:fabric-api:0.112.0+1.21.11")
    include("net.fabricmc.fabric-api:fabric-api:0.112.0+1.21.11")
}

loom {
    mappingsLayer.add("net.fabricmc:intermediary")
    accessWidener = file("src/main/resources/rlpvp.accesswidener")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(listOf("-Xlint:unchecked", "-Xlint:deprecation", "-parameters"))
}

tasks.named<Jar>("jar") {
    manifest {
        attributes(
            "Mod-Language" to "java",
            "Mod-Language-Version" to "21"
        )
    }
}