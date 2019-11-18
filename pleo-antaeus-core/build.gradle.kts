plugins {
    kotlin("jvm")
}

val activeMQVersion = "5.7.0"
val quartzSchedulerVersion = "2.3.1"
val jacksonVersion = "2.10.1"

kotlinProject()

dependencies {
    implementation(project(":pleo-antaeus-data"))
    compile(project(":pleo-antaeus-models"))
    compile("org.quartz-scheduler:quartz:$quartzSchedulerVersion")
    // https://mvnrepository.com/artifact/org.apache.activemq/activemq-core
    compile("org.apache.activemq:activemq-core:$activeMQVersion")

    // https://mvnrepository.com/artifact/com.fasterxml.jackson.core/jackson-core
    compile("com.fasterxml.jackson.core:jackson-core:$jacksonVersion")
    compile("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
    compile("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersion")
}
