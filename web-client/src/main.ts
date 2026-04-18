/**
 * This UI only calls the Micronaut BFF over HTTP (no protobuf in the browser).
 * Shared service definitions live under `proto/schemas/` (Gradle `proto` project) for JVM/grpc targets.
 */
const API_BASE = import.meta.env.VITE_API_BASE ?? "";

const app = document.querySelector<HTMLDivElement>("#app");
if (!app) {
  throw new Error("#app missing");
}

app.innerHTML = `
  <main style="font-family: system-ui, sans-serif; max-width: 48rem; margin: 2rem auto; padding: 0 1rem;">
    <h1>gRPC demo</h1>
    <p>REST BFF (Micronaut) at <code>/api/*</code>; gRPC on port 9090 for the CLI.</p>

    <section style="margin-bottom: 2rem;">
      <h2>Ping / peek (global counter)</h2>
      <p>
        <button type="button" id="btn-ping">Ping</button>
        <button type="button" id="btn-peek">Peek</button>
      </p>
      <pre id="out" style="background: #f4f4f5; padding: 1rem; border-radius: 6px;"></pre>
    </section>

    <section>
      <h2>Named jobs (master + worker actors)</h2>
      <p>Worker increments once per second until <code>count</code> is reached; master tracks state.</p>
      <form id="job-form" style="display: flex; flex-wrap: wrap; gap: 0.5rem; align-items: flex-end; margin-bottom: 1rem;">
        <label>Name <input type="text" id="job-name" required maxlength="64" pattern="[a-zA-Z][a-zA-Z0-9_-]{0,63}" title="Letter first, then letters, digits, underscore, hyphen (max 64 chars)" style="width: 12rem;" /></label>
        <label>Count <input type="number" id="job-count" min="1" value="5" required style="width: 6rem;" /></label>
        <button type="submit">Start job</button>
        <button type="button" id="btn-jobs-refresh">Refresh list</button>
      </form>
      <div id="job-msg" style="min-height: 1.25rem; color: #b45309;"></div>
      <table style="width: 100%; border-collapse: collapse; font-size: 0.9rem;">
        <thead>
          <tr style="text-align: left; border-bottom: 1px solid #e4e4e7;">
            <th style="padding: 0.35rem 0;">Name</th>
            <th>Status</th>
            <th>Current</th>
            <th>Target</th>
            <th></th>
          </tr>
        </thead>
        <tbody id="job-rows"></tbody>
      </table>
      <pre id="job-peek" style="background: #f4f4f5; padding: 1rem; border-radius: 6px; margin-top: 1rem; min-height: 2rem;"></pre>
    </section>
  </main>
`;

const out = document.querySelector<HTMLPreElement>("#out")!;
const jobRows = document.querySelector<HTMLTableSectionElement>("#job-rows")!;
const jobPeek = document.querySelector<HTMLPreElement>("#job-peek")!;
const jobMsg = document.querySelector<HTMLDivElement>("#job-msg")!;

async function call(path: string): Promise<void> {
  out.textContent = "…";
  const res = await fetch(`${API_BASE}${path}`);
  const text = await res.text();
  out.textContent = res.ok ? text : `HTTP ${res.status}: ${text}`;
}

document.querySelector("#btn-ping")!.addEventListener("click", () => call("/api/ping"));
document.querySelector("#btn-peek")!.addEventListener("click", () => call("/api/peek"));

function esc(s: string): string {
  const d = document.createElement("div");
  d.textContent = s;
  return d.innerHTML;
}

async function refreshJobs(): Promise<void> {
  jobMsg.textContent = "";
  const res = await fetch(`${API_BASE}/api/jobs`);
  const text = await res.text();
  if (!res.ok) {
    jobMsg.textContent = `List failed: HTTP ${res.status}`;
    jobRows.innerHTML = "";
    return;
  }
  const data = JSON.parse(text) as { jobs: Array<{ name: string; status: string; currentCount: number; targetCount: number }> };
  jobRows.innerHTML = data.jobs
    .map((j) => {
      const nameEnc = encodeURIComponent(j.name);
      const canClean = j.status === "DONE" || j.status === "TERMINATED";
      const canTerminate = j.status === "RUNNING";
      return `<tr style="border-bottom: 1px solid #f4f4f5;">
        <td style="padding: 0.35rem 0;"><code>${esc(j.name)}</code></td>
        <td>${esc(j.status)}</td>
        <td>${j.currentCount}</td>
        <td>${j.targetCount}</td>
        <td style="white-space: nowrap;">
          <button type="button" class="btn-peek" data-name="${j.name.replace(/"/g, "")}">Peek</button>
          ${canTerminate ? `<button type="button" class="btn-term" data-name="${j.name.replace(/"/g, "")}">Terminate</button>` : ""}
          ${canClean ? `<button type="button" class="btn-clean" data-name="${j.name.replace(/"/g, "")}">Clean</button>` : ""}
        </td>
      </tr>`;
    })
    .join("");

  for (const btn of jobRows.querySelectorAll<HTMLButtonElement>(".btn-peek")) {
    const name = btn.dataset.name!;
    btn.addEventListener("click", () => doPeek(name));
  }
  for (const btn of jobRows.querySelectorAll<HTMLButtonElement>(".btn-term")) {
    const name = btn.dataset.name!;
    btn.addEventListener("click", () => doTerminate(name));
  }
  for (const btn of jobRows.querySelectorAll<HTMLButtonElement>(".btn-clean")) {
    const name = btn.dataset.name!;
    btn.addEventListener("click", () => doClean(name));
  }
}

async function doPeek(name: string): Promise<void> {
  jobPeek.textContent = "…";
  const res = await fetch(`${API_BASE}/api/jobs/${encodeURIComponent(name)}/peek`);
  const text = await res.text();
  jobPeek.textContent = res.ok ? text : `HTTP ${res.status}: ${text}`;
}

async function doTerminate(name: string): Promise<void> {
  if (!confirm(`Terminate job "${name}"?`)) return;
  const res = await fetch(`${API_BASE}/api/jobs/${encodeURIComponent(name)}/terminate`, { method: "POST" });
  const text = await res.text();
  jobMsg.textContent = res.ok ? `Terminated: ${text}` : `Terminate failed: HTTP ${res.status} ${text}`;
  await refreshJobs();
}

async function doClean(name: string): Promise<void> {
  if (!confirm(`Remove finished job "${name}" from the registry?`)) return;
  const res = await fetch(`${API_BASE}/api/jobs/${encodeURIComponent(name)}`, { method: "DELETE" });
  const text = await res.text();
  jobMsg.textContent = res.ok ? `Cleaned: ${text}` : `Clean failed: HTTP ${res.status} ${text}`;
  await refreshJobs();
}

document.querySelector("#job-form")!.addEventListener("submit", async (e) => {
  e.preventDefault();
  jobMsg.textContent = "";
  const name = (document.querySelector("#job-name") as HTMLInputElement).value.trim();
  const count = Number((document.querySelector("#job-count") as HTMLInputElement).value);
  const res = await fetch(`${API_BASE}/api/jobs`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ name, count }),
  });
  const text = await res.text();
  jobMsg.textContent = res.ok ? `Start: ${text}` : `Start failed: HTTP ${res.status} ${text}`;
  await refreshJobs();
});

document.querySelector("#btn-jobs-refresh")!.addEventListener("click", () => void refreshJobs());

void refreshJobs();
