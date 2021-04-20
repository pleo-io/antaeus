plugins {
    kotlin("jvm")
}

kotlinProject()

dependencies {
    implementation(project(":pleo-antaeus-data"))
    api(project(":pleo-antaeus-models"))
    //implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.0-M1") // Needed to use coroutines
    implementation("org.quartz-scheduler:quartz:2.3.2")
}