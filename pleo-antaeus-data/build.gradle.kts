plugins {
    kotlin("jvm")
}

kotlinProject()

dataLibs()

dependencies {
    api(project(":pleo-antaeus-models"))
}
