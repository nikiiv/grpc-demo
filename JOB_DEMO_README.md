# Named jobs demo

This document describes the **named background job** feature: user-defined jobs that run inside `grpc-server` as **Akka actor pairs** (master + worker), exposed through **gRPC** and a **Micronaut BFF** REST API, with a small **web UI**.

---

## What it does

- **Start a job** with a **name** and a **target count** (`count`).
- For each job, the **worker** actor increments a local counter **once per second**, starting from 1, until it reaches `count`. After each increment it notifies the **master** actor, which forwards state to a central **job manager**.
- **List jobs** to see name, status, current value, and target.
- **Peek** a job to read the latest counter and metadata without side effects.
- When the worker reaches the target, it tells the master the job is **done**; the master updates the registry. Finished jobs stay listed until **cleaned**.
- **Terminate** stops a **running** job early: the master stops the worker and marks the job **terminated**.
- **Clean** removes a **done** or **terminated** job from the registry and stops the master actor.

Job names must match server validation: **one letter first**, then letters, digits, `_`, or `-`, up to **64** characters total (`[a-zA-Z][a-zA-Z0-9_-]{0,63}`). Count must be between **1** and **1_000_000**.

---

## Architecture (brief)

| Layer | Role |
|--------|------|
| **`proto/schemas/job.proto`** | Contract for `JobService` RPCs and messages (`StartJob`, `ListJobs`, `PeekJob`, `TerminateJob`, `CleanJob`, `JobStatus`, etc.). |
| **`grpc-server`** | Akka: **`JobManagerActor`** holds a registry (name → state + master ref). **`JobMasterActor`** is spawned per job; it creates a child **`JobWorkerActor`**. **`JobGrpcEndpoint`** implements the generated Akka gRPC `JobService` and uses `PatternsCS.ask` to talk to the manager. |
| **`bff`** | Micronaut: **`GrpcBackendClient`** uses **grpc-java** blocking stubs on the same channel as Ping/Pong. **`JobController`** maps REST to gRPC. |
| **`web-client`** | Vite/TypeScript: forms and buttons calling **`/api/jobs`** (proxied to the BFF in dev). |

The browser **never** uses gRPC or protobuf directly; only the BFF speaks gRPC to `grpc-server`.

---

## REST API (BFF)

Base path: **`/api/jobs`** (Micronaut default port **8080**).

| Method | Path | Body / notes |
|--------|------|----------------|
| `POST` | `/api/jobs` | JSON `{"name": "...", "count": N}` |
| `GET` | `/api/jobs` | List all jobs |
| `GET` | `/api/jobs/{name}/peek` | Peek one job |
| `POST` | `/api/jobs/{name}/terminate` | Terminate if running |
| `DELETE` | `/api/jobs/{name}` | Clean if done or terminated |

Responses are JSON-friendly maps (e.g. `ok`, `errorMessage`, `jobs`, `info` with `status` as `RUNNING` / `DONE` / `TERMINATED`).

---

## gRPC API (`grpc-server`)

Service: **`job.JobService`** (Java package `com.example.grpcdemo.job`), same port as the Ping/Pong service (default **127.0.0.1:9090**). Use **gRPC reflection** or the shared `.proto` file for tooling.

---

## Actor behavior (implementation sketch)

1. **`JobManagerActor`** (`job-manager`): Validates requests, creates **`JobMasterActor`** under a path derived from the job name (`JobNaming`), stores **`JobState`**, and applies **`StateUpdate`** messages from masters.
2. **`JobMasterActor`**: On start, spawns **`JobWorkerActor`** as child `worker`. Relays progress to the manager; on normal completion or **early terminate**, updates status (`DONE` / `TERMINATED`).
3. **`JobWorkerActor`**: `AbstractActorWithTimers` — fixed **1 second** delay between ticks; sends **Progress** to the master each tick; when `current >= targetCount`, cancels timers, sends **WorkerDone**, and stops itself.

Logging uses **SLF4J** across the BFF, gRPC entrypoints, and these actors (see codebase for levels).

---

## How to run the full path

1. Start **`grpc-server`**: `./gradlew :grpc-server:run`
2. Start **`bff`**: `./gradlew :bff:run`
3. Start the **web client** (optional): `./gradlew start_bff_client` — open the printed URL; `/api` is proxied to the BFF.

See **`PROJECT.md`** for ports, config files, and the rest of the repo layout.
