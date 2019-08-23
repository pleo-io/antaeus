plugins {
    application
    kotlin("jvm")
}

kotlinProject()

dataLibs()

application {
    mainClassName = "io.pleo.antaeus.app.AntaeusApp"
}

dependencies {
    implementation(project(":pleo-antaeus-data"))
    implementation(project(":pleo-antaeus-rest"))
    implementation(project(":pleo-antaeus-core"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.0-RC2")
    compile(project(":pleo-antaeus-models"))
}