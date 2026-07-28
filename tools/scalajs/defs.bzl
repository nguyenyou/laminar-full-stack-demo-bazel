load("@rules_java//java/common:java_info.bzl", "JavaInfo")
load("@rules_scala//scala:scala.bzl", _scala_library = "scala_library")

_SCALA_JS_LIBRARY = "@maven//:org_scala_js_scalajs_library_2_13"

def scala_js_library(name, srcs, deps = [], visibility = None):
    _scala_library(
        name = name,
        srcs = srcs,
        deps = deps + [_SCALA_JS_LIBRARY],
        scalacopts = [
            "-deprecation",
            "-language:implicitConversions",
            "-scalajs",
        ],
        visibility = visibility,
    )

def scala_js_binary(name, srcs, main_class, deps = [], visibility = None):
    ir_name = name + "_ir"

    scala_js_library(
        name = ir_name,
        srcs = srcs,
        deps = deps,
    )

    _scala_js_link(
        name = name,
        deps = [":" + ir_name],
        main_class = main_class,
        visibility = visibility,
    )

def _scala_js_link_impl(ctx):
    output_directory = ctx.actions.declare_directory(ctx.label.name + ".js")
    runner = ctx.actions.declare_file(ctx.label.name)
    jars = depset(
        transitive = [
            dep[JavaInfo].transitive_runtime_jars
            for dep in ctx.attr.deps
        ],
    )

    args = ctx.actions.args()
    args.add(ctx.attr.main_class)
    args.add(output_directory.path)
    args.add_all(jars)

    ctx.actions.run(
        executable = ctx.attr._linker[DefaultInfo].files_to_run,
        arguments = [args],
        inputs = jars,
        outputs = [output_directory],
        mnemonic = "ScalaJSLink",
        progress_message = "Linking Scala.js %{label}",
    )

    ctx.actions.write(
        output = runner,
        content = """#!/usr/bin/env bash
set -euo pipefail

runner_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
exec bun "${runner_dir}/%s/main.js" "$@"
""" % output_directory.basename,
        is_executable = True,
    )

    return DefaultInfo(
        executable = runner,
        files = depset([output_directory]),
        runfiles = ctx.runfiles(files = [output_directory]),
    )

_scala_js_link = rule(
    implementation = _scala_js_link_impl,
    attrs = {
        "deps": attr.label_list(
            mandatory = True,
            providers = [JavaInfo],
        ),
        "main_class": attr.string(mandatory = True),
        "_linker": attr.label(
            default = "//tools/scalajs:linker",
            cfg = "exec",
            executable = True,
        ),
    },
    executable = True,
)
