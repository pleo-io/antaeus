plugins {
    kotlin("jvm")
}

kotlinProject()

coreLibs()

dependencies {
    implementation(project(":pleo-antaeus-data"))
    api(project(":pleo-antaeus-models"))
}