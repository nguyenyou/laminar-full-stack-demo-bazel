import fs from "node:fs"
import path from "node:path"

// If calling this script from `bun run sourcemap-report`, pass extra arguments
// after `--`, e.g. `bun run sourcemap-report -- --html report.html`.

// -- PARAMS --

const directory = path.join("dist", "assets")
const filePattern = /^index-[a-z0-9_]+\.js$/i

// -- HELPERS --

function getJsBundleFilePath() {
  if (!fs.existsSync(directory)) {
    console.error(`Error: Directory ${directory} does not exist! See sourcemap-report.js`)
    process.exit(1)
  }

  const files = fs.readdirSync(directory)
  const matchingFiles = files.filter(file => filePattern.test(file))

  if (matchingFiles.length === 0) {
    console.error(`Error: No matching files found in directory ${directory}`)
    process.exit(1)
  } else if (matchingFiles.length > 1) {
    console.error(`Error: Multiple matching files found: ${matchingFiles.join(", ")}`)
    process.exit(1)
  }

  return path.join(directory, matchingFiles[0])
}

// -- SCRIPT --

const args = process.argv.slice(2)

// If JS file name is not provided, find and inject the bundle JS file path automatically
const indexOfJsFilename = args.findIndex(arg => arg.toLowerCase().endsWith(".js"))
if (indexOfJsFilename === -1) {
  args.unshift(getJsBundleFilePath())
} else if (!fs.existsSync(args[indexOfJsFilename])) {
  console.error(`Error: File ${args[indexOfJsFilename]} does not exist.`)
  process.exit(1)
}

console.log(`> bunx source-map-explorer ${args.join(" ")}\n`)

const sourceMapExplorer = Bun.spawn(["bunx", "source-map-explorer", ...args], {
  stdout: "inherit",
  stderr: "inherit"
})

process.exit(await sourceMapExplorer.exited)
