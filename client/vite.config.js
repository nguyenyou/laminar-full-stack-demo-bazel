import {defineConfig} from "vite";
import injectHtmlVarsPlugin from "./vite-plugins/inject-html-vars.js";
import rollupPluginSourcemaps from "rollup-plugin-sourcemaps";
import globResolverPlugin from "@raquo/vite-plugin-glob-resolver";
import importSideEffectPlugin from "@raquo/vite-plugin-import-side-effect";
import rollupCopyPlugin from 'rollup-plugin-copy'
import path from "path";

const scalaJsModule = path.resolve(
  __dirname,
  process.env.SCALA_JS_MODULE ?? "../bazel-bin/client/client_js_dev.js/main.js"
);

export default defineConfig({
  base: "/",
  publicDir: "public",
  resolve: {
    alias: [
      {
        find: "scalajs:main.js",
        replacement: scalaJsModule
      },
      {
        find: "@shoelace-style",
        replacement: path.resolve(__dirname, "node_modules/@shoelace-style")
      },
      {
        find: "@ui5",
        replacement: path.resolve(__dirname, "node_modules/@ui5")
      },
      {
        find: "chart.js",
        replacement: path.resolve(__dirname, "node_modules/chart.js")
      },
      {
        find: "chartjs-plugin-datalabels",
        replacement: path.resolve(__dirname, "node_modules/chartjs-plugin-datalabels")
      },
      {
        find: "highlight.js",
        replacement: path.resolve(__dirname, "node_modules/highlight.js")
      }
    ]
  },
  plugins: [
    globResolverPlugin({
      // See https://github.com/raquo/vite-plugin-glob-resolver
      cwd: __dirname,
      ignore: [
        'node_modules/**',
        'bazel-*/**',
        'target/**'
      ]
    }),
    importSideEffectPlugin({
      // See https://github.com/raquo/vite-plugin-import-side-effect
      defNames: ['importStyle'],
      rewriteModuleIds: ['**/*.less', '**/*.css'],
      // verbose: true
    }),
    injectHtmlVarsPlugin({
      SCRIPT_URL: './index.js'
    }),
    rollupCopyPlugin({
      copyOnce: true,
      targets: [
        {
          // If changing `dest`, you must also change the call to `Shoelace.setBasePath` in your Scala.js code.
          // Required by Shoelace: https://shoelace.style/getting-started/installation
          src: path.resolve(__dirname, 'node_modules/@shoelace-style/shoelace/dist/assets/icons/*.svg'),
          dest: path.resolve(__dirname, 'public/assets/shoelace/assets/icons')
        }
      ]
    })
  ],
  build: {
    outDir: "dist",
    assetsDir: "assets", // path relative to outDir
    // outDir: "../server/src/main/resources/static", // #TODO can we do this directly?
    cssCodeSplit: false,  // false = Load all CSS upfront
    rollupOptions: {
      plugins: [rollupPluginSourcemaps()],
    },
    minify: "terser",
    sourcemap: true
  },
  server: {
    port: 3000,
    strictPort: true,
    fs: {
      allow: [".."]
    },
    // host: "0.0.0.0",
    proxy: {
      // Note: we only proxy /api URLs to the server, if you need more,
      // you need to set that up.
      // #TODO Can't we just proxy everything except index.html and /app?
      // #TODO hide away the proxy config (do we even need it?)
      "/api": {
        target: "http://127.0.0.1:9000",
        secure: false,
        configure: (proxy, _options) => {
          proxy.on("error", (err, _req, _res) => {
            console.log("proxy error", err);
          });
          proxy.on("proxyReq", (proxyReq, req, _res) => {
            console.log("Sending Request to the Target:", req.method, req.url);
          });
          proxy.on("proxyRes", (proxyRes, req, _res) => {
            console.log(
              "Received Response from the Target:",
              proxyRes.statusCode,
              req.url
            );
          });
        },
      }
    },
    logLevel: "debug"
  }
})
