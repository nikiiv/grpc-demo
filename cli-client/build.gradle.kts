plugins {
    java
    application
    id("com.google.protobuf") version "0.9.6"
}

group = "com.example"
version = "0.1.0-SNAPSHOT"

val grpcJavaVersion: String by project
val protobufVersion: String by project

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":proto"))

    implementation("io.grpc:grpc-netty-shaded:$grpcJavaVersion")
    implementation("io.grpc:grpc-protobuf:$grpcJavaVersion")
    implementation("io.grpc:grpc-stub:$grpcJavaVersion")
    implementation("com.google.protobuf:protobuf-java:$protobufVersion")
    implementation("javax.annotation:javax.annotation-api:1.3.2")
}

application {
    mainClass.set("com.example.grpcdemo.cli.PingPongCli")
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:$protobufVersion"
    }
    plugins {
        create("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:$grpcJavaVersion"
        }
    }
    generateProtoTasks {
        all().forEach { task ->
            task.plugins {
                create("grpc") {
                }
            }
        }
    }
}

sourceSets {
    main {
        proto {
            srcDir(project(":proto").projectDir.resolve("schemas"))
        }
    }
}

tasks.named<JavaCompile>("compileJava") {
    options.encoding = "UTF-8"
}

tasks.named("extractProto") {
    dependsOn(":proto:compileJava")
}
