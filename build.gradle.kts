// Root aggregator — JVM modules own their builds; web-client owns npm (see web-client/build.gradle.kts).

tasks.register("start_grpc_server") {
    group = "demo"
    description = "Runs the Akka gRPC server (:grpc-server:run; see grpc-server application.conf for host/port)."
    dependsOn(":grpc-server:run")
}

tasks.register("start_bff_server") {
    group = "demo"
    description = "Runs the Micronaut BFF (:bff:run; HTTP port from bff application.yml)."
    dependsOn(":bff:run")
}

tasks.register("start_bff_client") {
    group = "demo"
    description =
            "Runs the Vite dev server (:web-client:start_bff_client). Same as ./gradlew :web-client:start_bff_client"
    dependsOn(":web-client:start_bff_client")
}

tasks.register("npmInstallWebClient") {
    group = "demo"
    description = "Runs npm install in web-client (:web-client:npmInstall)."
    dependsOn(":web-client:npmInstall")
}
