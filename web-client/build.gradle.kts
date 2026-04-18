// TypeScript/Vite UI — npm is the toolchain; Gradle orchestrates install + dev server for repo-wide consistency.

plugins {
    base
}

tasks.register<Exec>("npmInstall") {
    group = "demo"
    description = "Runs npm install (skipped when node_modules is up to date for package.json / package-lock.json)."
    workingDir = layout.projectDirectory.asFile
    commandLine("npm", "install")
    inputs.file("package.json")
    inputs.file("package-lock.json")
    outputs.dir("node_modules")
}

tasks.register<Exec>("start_bff_client") {
    group = "demo"
    description =
            "Runs the Vite dev server. Depends on npmInstall; opens http://127.0.0.1:5173 in a browser when possible."
    dependsOn("npmInstall")
    workingDir = layout.projectDirectory.asFile
    val os = System.getProperty("os.name").lowercase()
    when {
        os.contains("mac") ->
                commandLine(
                        "sh",
                        "-c",
                        "(sleep 1.5 && open 'http://127.0.0.1:5173') & exec npm run dev",
                )
        os.contains("linux") ->
                commandLine(
                        "sh",
                        "-c",
                        "(sleep 1.5 && xdg-open 'http://127.0.0.1:5173') & exec npm run dev",
                )
        os.contains("win") ->
                commandLine(
                        "cmd",
                        "/c",
                        "start /B cmd /c \"timeout /t 2 /nobreak >nul && start http://127.0.0.1:5173\" && npm run dev",
                )
        else -> commandLine("npm", "run", "dev")
    }
}

tasks.register<Exec>("buildWeb") {
    group = "build"
    description = "Production build: npm run build (writes dist/)."
    dependsOn("npmInstall")
    workingDir = layout.projectDirectory.asFile
    commandLine("npm", "run", "build")
    inputs.dir("src")
    inputs.file("package.json")
    inputs.file("vite.config.ts")
    inputs.file("index.html")
    outputs.dir("dist")
}

tasks.named("build") {
    dependsOn("buildWeb")
}
