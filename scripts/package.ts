#!/usr/bin/env bun

import {cp, mkdir, rm, copyFile} from "node:fs/promises"
import {fileURLToPath} from "node:url"
import path from "node:path"

const repositoryRoot = fileURLToPath(new URL("..", import.meta.url))
const clientDirectory = path.join(repositoryRoot, "client")
const distributionDirectory = path.join(repositoryRoot, "dist")
const stagingDirectory = path.join(distributionDirectory, ".staging")
const applicationJar = path.join(distributionDirectory, "app.jar")
const bazel = process.env.BAZEL ?? "bazel"
const bazelStartupArgs = (process.env.BAZEL_STARTUP_ARGS ?? "")
  .split(/\s+/)
  .filter(argument => argument.length > 0)

function run(
  command: string,
  args: string[],
  cwd: string,
  env: Record<string, string | undefined> = process.env
): void {
  const process = Bun.spawnSync([command, ...args], {
    cwd,
    env,
    stdout: "inherit",
    stderr: "inherit"
  })

  if (!process.success) {
    throw new Error(`${command} ${args.join(" ")} failed with exit code ${process.exitCode}`)
  }
}

run(
  bazel,
  [...bazelStartupArgs, "build", "//client:client_js_prod", "//server:server_deploy.jar"],
  repositoryRoot
)
run("bun", ["install", "--frozen-lockfile"], clientDirectory)
run(
  "bun",
  ["run", "build"],
  clientDirectory,
  {
    ...process.env,
    SCALA_JS_MODULE: path.join(
      repositoryRoot,
      "bazel-bin/client/client_js_prod.js/main.js"
    )
  }
)

await rm(distributionDirectory, {recursive: true, force: true})
await mkdir(path.join(stagingDirectory, "static"), {recursive: true})
await copyFile(
  path.join(repositoryRoot, "bazel-bin/server/server_deploy.jar"),
  applicationJar
)
await cp(
  path.join(clientDirectory, "dist"),
  path.join(stagingDirectory, "static"),
  {recursive: true}
)

run(
  "jar",
  ["--update", "--file", applicationJar, "-C", stagingDirectory, "static"],
  repositoryRoot
)

await rm(stagingDirectory, {recursive: true, force: true})
console.log(`Packaged ${applicationJar}`)
