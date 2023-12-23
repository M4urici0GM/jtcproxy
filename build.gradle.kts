/*
 * This file was generated by the Gradle 'init' task.
 */

plugins {
    java
    application
    `java-library`
    `maven-publish`
}

group = "dev.mgbarbosa"
version = "1.0-SNAPSHOT"
description = "jtcproxy"
java.sourceCompatibility = JavaVersion.VERSION_21
java.targetCompatibility = JavaVersion.VERSION_21

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    // Logging
    implementation("org.slf4j:slf4j-api:2.+")
    implementation("org.slf4j:slf4j-simple:2.+")
    implementation("org.apache.logging.log4j:log4j-api:2.7")
    implementation("org.apache.logging.log4j:log4j-core:2.7")
    implementation("org.apache.logging.log4j:log4j-slf4j-impl:2.7")

    // Utils
    implementation("org.apache.commons:commons-lang3:3.14.+")
    implementation("org.apache.commons:commons-collections4:4.+")
    compileOnly("org.projectlombok:lombok:1.18.30")
    annotationProcessor("org.projectlombok:lombok:1.18.30")

    // Test Utils
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:3.+")
    testCompileOnly("org.projectlombok:lombok:1.18.30")
    testAnnotationProcessor("org.projectlombok:lombok:1.18.30")
}


publishing {
    publications.create<MavenPublication>("maven") {
        from(components["java"])
    }
}

tasks.withType<JavaCompile>() {
    options.encoding = "UTF-8"
}

tasks.withType<Javadoc>() {
    options.encoding = "UTF-8"
}
