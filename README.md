![](https://laminar.dev/img/brand/laminar-logo-100px-rounded.png)

# Laminar Full-Stack Demo with Bazel

This is a Bazel port of [raquo/laminar-full-stack-demo](https://github.com/raquo/laminar-full-stack-demo). It preserves the original Laminar examples, Scala.js and JVM shared models, Vite frontend, http4s backend, and production jar workflow while replacing SBT with Bazel.

## Architecture

```text
shared Scala sources
       |
       +--> //shared:shared_js  --> //client:client_js
       |                                  |
       |                                  v
       |                         ESM main.js --> Vite
       |
       +--> //shared:shared_jvm --> //server:server
                                          |
                                          v
                                     http4s API
```

The client build emits:

```text
bazel-bin/client/client_js.js/main.js
```

`client/index.js` imports that ESM file directly. Vite then resolves JavaScript packages, LESS and CSS imports, serves the development frontend, and proxies `/api` to http4s.

## Requirements

- JDK 17 or newer
- Bazel or Bazelisk; `.bazelversion` selects Bazel 9.2.0
- [Bun](https://bun.sh/)

Bazel downloads Scala, Scala.js, and JVM dependencies. Bun manages only the Vite-side JavaScript dependencies.

## Development

Install JavaScript dependencies once:

```bash
cd client
bun install --frozen-lockfile
cd ..
```

Build the Scala.js ESM module:

```bash
bazel build //client:client_js
```

Start the backend:

```bash
bazel run //server:server
```

In another terminal, start Vite:

```bash
cd client
bun run dev
```

Open [http://localhost:3000](http://localhost:3000). The backend listens on [http://localhost:9000](http://localhost:9000).

After changing Scala client or shared sources, rerun:

```bash
bazel build //client:client_js
```

Vite notices the updated `main.js`. LESS, CSS, and JavaScript files remain under Vite's normal hot-reload loop.

## Useful Bazel targets

```bash
# Build the Scala.js client
bazel build //client:client_js

# Build the backend launcher
bazel build //server:server

# Build the backend deploy jar
bazel build //server:server_deploy.jar

# Build everything
bazel build //...
```

The shared sources are intentionally compiled twice:

- `//shared:shared_js` produces Scala.js IR for the browser.
- `//shared:shared_jvm` produces JVM bytecode for the server.

## Compile-time code snippets

The application's code-snippet page is still generated at compile time. `//client:generated_snippets` gives a declared set of source files to `//tools/snippets:generator`, which emits `GeneratedSnippets.scala` under `bazel-bin`. The Scala.js client compiles that generated source, so snippet names remain statically checked.

## Production

Build the Scala.js client, run Vite, build the deploy jar, and embed the frontend under `/static`:

```bash
./scripts/package.ts
```

Run the resulting application:

```bash
java -jar dist/app.jar
```

Then open [http://localhost:9000](http://localhost:9000).

The existing Dockerfile consumes the same jar:

```bash
docker build --tag laminar-demo .
docker run --rm -p 9000:8080 laminar-demo
```

## Dependency updates

JVM and Scala.js artifacts are pinned in `maven_install.json`. After changing Maven coordinates in `MODULE.bazel`, refresh the lock:

```bash
REPIN=1 bazel run @maven//:pin
```

JavaScript artifacts are pinned in `client/bun.lock`:

```bash
cd client
bun install
```

## Included examples

- TodoMVC
- Laminar signals, events, forms, and controlled inputs
- Waypoint routing
- JSON calls to an http4s backend
- Shared Scala 3 models compiled for Scala.js and JVM
- Chart.js
- Shoelace and UI5 web components
- Local storage and session storage
- LESS and component-local styles
- Compile-time generated, syntax-highlighted source snippets

## Upstream

The application code and teaching examples originate from [raquo/laminar-full-stack-demo](https://github.com/raquo/laminar-full-stack-demo), authored by [Nikita Gazarov](https://github.com/raquo). The original project also credits [Antoine](https://github.com/sherpal) for deployment and web-component examples. This repository changes the build and packaging implementation to Bazel and Bun.

## License

The source is provided under the MIT license; see [LICENSE.md](LICENSE.md). The author, Laminar, and sponsor logos and avatars are not covered by that license. The rocket favicon was derived from [Twitter Twemoji](https://github.com/twitter/twemoji) under CC-BY 4.0.
