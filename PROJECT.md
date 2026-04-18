# grpc-demo

## Goal

This repository is a small demo of **Akka 2.6.20**, **gRPC**, **Protocol Buffers**, and **Akka Actors** (counter state), together with a **Micronaut BFF** (REST) and a **TypeScript** web UI. The **gRPC server** (Akka) and the **REST API** (Micronaut) run as **separate processes**: the browser and the BFF talk HTTP; the BFF calls the backend over gRPC. Shared API contracts live in the **`proto`** Gradle project (`proto/schemas/`).

---

## Subprojects and how to run them

All Gradle commands below assume the repo root and use **`./gradlew`** (Unix). Build everything once with:

```bash
./gradlew build
```

### `proto`

Holds shared `.proto` definitions (`proto/schemas/pingpong.proto`) and a minimal `java-library` so other modules can depend on `:proto`. There is no separate runnable app.

**Useful tasks**

```bash
./gradlew :proto:build          # compile + jar
./gradlew :proto:dependencies # inspect classpath (optional)
```

---

### `grpc-server`

Standalone **Akka gRPC** process: actor-backed counter, **no REST**.

**Run**

```bash
./gradlew :grpc-server:run
```

Listens on **gRPC** (default **127.0.0.1:9090**). Stop the process with **Ctrl+C** in that terminal (or send SIGINT/SIGTERM to the JVM).

**Config** (optional): `grpc-server/src/main/resources/application.conf` — `grpc.host`, `grpc.port`.

---

### `bff`

**Micronaut** REST API for the web UI; forwards to the gRPC server.

**Run** (in a **second** terminal, with `grpc-server` already running)

```bash
./gradlew :bff:run
```

Serves HTTP on **8080** by default (`bff/src/main/resources/application.yml` — `grpc.client.*` points at the gRPC backend).

Stop with **Ctrl+C**.

---

### `cli-client`

Command-line **gRPC** client (grpc-java), talks to `grpc-server` on **9090**.

**Run**

```bash
# Ping (increment counter) — default when you pass no args or any arg other than "peek"
./gradlew :cli-client:run

# Peek (read counter without incrementing)
./gradlew :cli-client:run --args=peek
```

Requires **`grpc-server`** to be running. Optional JVM overrides:

```bash
./gradlew :cli-client:run -Dgrpc.host=127.0.0.1 -Dgrpc.port=9090
```

---

### `web-client`

**Vite + TypeScript** SPA. It is a **Gradle subproject** (`web-client/build.gradle.kts`) so the same `./gradlew` workflow can install deps and run/build the UI; **npm** remains the real toolchain. The app calls the **BFF** over HTTP; **no protobuf in the browser**.

**Run** (third terminal; needs **`bff`** and **`grpc-server`** for full behavior)

```bash
./gradlew start_bff_client
# same as: ./gradlew :web-client:start_bff_client  (runs npm install when needed, then Vite)
```

Open **http://127.0.0.1:5173** (or the URL Vite prints). The dev server proxies **`/api`** to **http://127.0.0.1:8080** (see `web-client/vite.config.ts`), so the Micronaut BFF must be up.

Stop with **Ctrl+C**.

**Production build** (static files under `web-client/dist/`) — also runs as part of **`./gradlew build`**:

```bash
./gradlew :web-client:build
# or: cd web-client && npm run build
```

---

## Typical demo order

1. `./gradlew start_grpc_server` (or `./gradlew :grpc-server:run`)
2. `./gradlew start_bff_server` (or `./gradlew :bff:run`)
3. `./gradlew start_bff_client` → browser at **http://127.0.0.1:5173** when supported
4. Optional: `./gradlew :cli-client:run` or `./gradlew :cli-client:run --args=peek`
