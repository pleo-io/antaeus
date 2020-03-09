import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    base
    kotlin("jvm") version "1.3.70" apply false
    id("org.jmailen.kotlinter") version "2.3.2"
}

allprojects {
    group = "io.pleo"
    version = "1.0"

    // Automatic linting plugin
    // run ./gradlew formatKotlin to autoformat
    // Here it is commented out because it can be a pain to setup the
    // IDE to autoformat
    // apply(plugin = "org.jmailen.kotlinter")

    repositories {
        mavenCentral()
        jcenter()
    }

    tasks.withType<KotlinCompile>().configureEach {
        kotlinOptions.jvmTarget = "11"
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }
}