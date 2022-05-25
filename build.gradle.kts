import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.6.21"
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

group = "dev.moru3"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()

    maven { url = uri("https://nexus.velocitypowered.com/repository/maven-public/") }
}

dependencies {
    testImplementation(kotlin("test"))
    implementation("com.velocitypowered:velocity-api:3.0.1")
    implementation("com.github.alexdlaird:java-ngrok:1.5.6")
    implementation("com.squareup.okhttp3:okhttp:4.9.3")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

tasks.named("build") {
    dependsOn("shadowJar")
}