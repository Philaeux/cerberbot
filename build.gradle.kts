import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.6.10"
    application
}

group = "live.admiralbulldog"
version = "1.0"

repositories {
    google()
    mavenCentral()
}

dependencies {
    implementation("io.ktor:ktor-client-core:1.6.7")
    implementation("io.ktor:ktor-client-cio:1.6.7")
    implementation("io.ktor:ktor-client-gson:1.6.7")
    implementation("org.slf4j:slf4j-simple:1.7.36")
    implementation("io.github.microutils:kotlin-logging-jvm:2.1.21")
    implementation("io.github.resilience4j:resilience4j-kotlin:1.7.1")
    implementation("io.github.resilience4j:resilience4j-ratelimiter:1.7.1")


    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "16"
}

application {
    mainClass.set("MainKt")
}
