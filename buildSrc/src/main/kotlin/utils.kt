
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.kotlin

const val junitVersion = "5.7.0"
const val kotlinCoroutinesVersion = "1.3.5"
const val h2Version = "1.4.199"

/**
 * Configures the current project as a Kotlin project by adding the Kotlin `stdlib` as a dependency.
 */
fun Project.kotlinProject() {
    dependencies {
        // Kotlin libs
        "implementation"(kotlin("stdlib"))
        "implementation"("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinCoroutinesVersion")

        // Logging
        "implementation"("org.slf4j:slf4j-simple:1.7.30")
        "implementation"("io.github.microutils:kotlin-logging:1.7.8")

        // Mockk
        "testImplementation"("io.mockk:mockk:1.9.3")

        // Kotlin Coroutines
        "testImplementation"("org.jetbrains.kotlinx:kotlinx-coroutines-test:$kotlinCoroutinesVersion")

        // JUnit 5
        "testImplementation"("org.junit.jupiter:junit-jupiter-api:$junitVersion")
        "testImplementation"("org.junit.jupiter:junit-jupiter-params:$junitVersion")
        "runtime"("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
    }
}

/**
 * Configures data layer libs needed for interacting with the DB
 */
fun Project.dataLibs() {
    dependencies {
        "implementation"("org.jetbrains.exposed:exposed:0.17.7")
        "implementation"("org.xerial:sqlite-jdbc:3.30.1")
        "testImplementation"("com.h2database", "h2", h2Version)
    }
}