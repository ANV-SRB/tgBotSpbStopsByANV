

plugins {
    kotlin("jvm") version "1.8.0"
    application
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation ("com.github.pengrad:java-telegram-bot-api:6.5.0")
    //implementation("com.github.pengrad:java-telegram-bot-api:6.5.0")
   // implementation("com.github.pengrad:java-telegram-bot-api:6.5.0")

    implementation("org.apache.commons:commons-csv:1.10.0")
    implementation("org.apache.commons:commons-csv:1.10.0")

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(8)
}

application {
    mainClass.set("MainKt")
}