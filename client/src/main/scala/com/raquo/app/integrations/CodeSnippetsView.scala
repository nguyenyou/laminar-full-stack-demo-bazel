package com.raquo.app.integrations

import com.raquo.app.codesnippets.CodeSnippets
import com.raquo.laminar.api.L.{*, given}

object CodeSnippetsView {

  def apply(): HtmlElement = {
    div(
      h1("Code Snippets"),
      p("Across this demo app you see code snippets like this:"),
      // BEGIN[codesnippets/usage]
      CodeSnippets(_.`hello world`, asParagraph = true),
      // END[codesnippets/usage]
      p("They show the actual pieces of the relevant code, extracted from the codebase at compile time. So, we're getting compile-time data from Bazel all the way into Scala.js. Simple values such as the Laminar version live in a shared BuildInfo object, while the generated snippet object gives every snippet a statically checked Scala field. Here is how we do this."),
      p("First, we define a Bazel genrule and make the Scala.js client depend on its output. Whenever an input source changes, Bazel reruns the generator before compiling the client."),
      CodeSnippets(_.`codesnippets/precompile`, asParagraph = true),
      p("The generator reads only the source files declared to Bazel and finds matching ", code("// BEGIN[hello world]"), " and ", code("// END[hello world]"), " comments, grabbing the code snippet in between and removing extra whitespace. The important part is that we get a list of snippets for every key like ", code("hello world"), "."),
      CodeSnippets(_.`codesnippets/generator`, asParagraph = true),
      p("The generator is a small Scala binary declared in ", code("tools/snippets/BUILD.bazel"), ":"),
      CodeSnippets(_.`compile-time-build.bazel`, asParagraph = true),
      p("The result is a regular GeneratedSnippets.scala source file under bazel-bin, with an object that has many val-s, one for each key, containing a list of snippets for that key."),
      p("Finally, we can render those snippets on the frontend like so:"),
      CodeSnippets(_.`codesnippets/usage`, asParagraph = true),
      p("The implementation is pretty straightforward, and uses the ", a(href("https://highlightjs.org/"), "Highlight.js"), " library for syntax highlighting."),
      CodeSnippets(_.`codesnippets/object`.sortBy(_.fileName != "CodeSnippets.scala"), asParagraph = true, startExpanded = _.fileName == "CodeSnippets.scala"),
      p("Finally, here are the interfaces I made for the Highlight.js library. The interfaces themselves take about 30 lines, and integration with Laminar is another 10 lines."),
      CodeSnippets(_.`codesnippets/highlight.js`.sortBy(_.fileName != "hljs.scala"), asParagraph = true)
    )
  }
}
