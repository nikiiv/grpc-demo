plugins {
    java
    application
    id("com.lightbend.akka.grpc.gradle") version "2.1.6"
}

group = "com.example"
version = "0.1.0-SNAPSHOT"

val akkaVersion: String by project
val akkaGrpcVersion: String by project

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

repositories {
    mavenCentral()
}

configurations.all {
    resolutionStrategy.eachDependency {
        if (requested.group == "com.typesafe.akka") {
            val n = requested.name
            if (n.startsWith("akka-http") || n.startsWith("akka-parsing")) {
                return@eachDependency
            }
            useVersion(akkaVersion)
            because("Pinned Akka core version for demo")
        }
    }
}

dependencies {
    implementation(project(":proto"))

    implementation("com.typesafe.akka:akka-actor_2.13:$akkaVersion")
    implementation("com.typesafe.akka:akka-stream_2.13:$akkaVersion")
    implementation("com.typesafe.akka:akka-http_2.13:10.2.10")
    implementation("com.typesafe.akka:akka-http2-support_2.13:10.2.10")
    implementation("com.lightbend.akka.grpc:akka-grpc-runtime_2.13:$akkaGrpcVersion")

    implementation("org.slf4j:slf4j-api:2.0.9")
    runtimeOnly("ch.qos.logback:logback-classic:1.4.14")
}

application {
    mainClass.set("com.example.grpcdemo.GrpcServerApp")
}

tasks.named<JavaCompile>("compileJava") {
    options.encoding = "UTF-8"
}

tasks.processResources {
    duplicatesStrategy = org.gradle.api.file.DuplicatesStrategy.EXCLUDE
}

sourceSets {
    main {
        proto {
            // Shared definitions: `proto/schemas/` in the `proto` Gradle project
            srcDir(project(":proto").projectDir.resolve("schemas"))
        }
    }
}
