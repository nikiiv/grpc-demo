# grpc-demo

Small end-to-end sample: **Akka** actors and **Akka gRPC** in a standalone JVM, **Micronaut** as a REST BFF, **TypeScript** web UI, and shared **Protocol Buffers** in `proto/schemas/`. The browser talks HTTP only; the BFF calls the backend over gRPC.

## Requirements

- **JDK 17** (Gradle toolchain)
- **Node.js** and **npm** (for `web-client`; Gradle runs `npm install` / `npm run build` when needed)

## Clone and build

```bash
git clone git@github.com:nikiiv/grpc-demo.git
cd grpc-demo
./gradlew build
```

## Run the full stack (three terminals)

Default ports: **gRPC 9090**, **BFF 8080**, **Vite 5173**.

```bash
# Terminal 1
./gradlew start_grpc_server

# Terminal 2
./gradlew start_bff_server

# Terminal 3
./gradlew start_bff_client
```

Then open **http://127.0.0.1:5173** (the dev task may open a browser on macOS/Linux/Windows). The UI proxies `/api` to the BFF.

Other useful tasks: `./gradlew tasks --group demo`, `./gradlew :cli-client:run` (gRPC CLI to the counter service).

## Modules

| Module | Role |
|--------|------|
| `proto` | Shared `.proto` sources and thin JAR for dependents |
| `grpc-server` | Akka gRPC server: global ping/peek counter + named job actors |
| `bff` | Micronaut REST → gRPC |
| `cli-client` | Command-line gRPC client |
| `web-client` | Vite + TypeScript SPA (Gradle subproject wrapping npm) |

## Documentation

- **[PROJECT.md](PROJECT.md)** — subprojects, ports, config paths, CLI usage
- **[JOB_DEMO_README.md](JOB_DEMO_README.md)** — named jobs (master/worker actors, REST `/api/jobs`, lifecycle)
