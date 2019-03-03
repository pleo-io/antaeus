plugins {
    kotlin("jvm")
}

kotlinProject()

dependencies {
    implementation(project(":pleo-antaeus-data"))
    compile(project(":pleo-antaeus-models"))
    implementation("io.projectreactor:reactor-core:3.2.6.RELEASE")
    testImplementation("io.projectreactor:reactor-test:3.2.6.RELEASE")
}