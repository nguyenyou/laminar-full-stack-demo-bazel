package tools.snippets

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

import scala.collection.mutable
import scala.jdk.CollectionConverters.*
import scala.util.matching.Regex

// BEGIN[codesnippets/generator]
object SnippetGenerator {
  private final case class Begin(key: String, lineNumber: Int, whitespacePrefix: String)

  private final case class End(key: String, lineNumber: Int)

  private final case class Snippet(
    filePath: String,
    fileName: String,
    language: String,
    startLineNumber: Int,
    endLineNumber: Int,
    key: String,
    lines: List[String],
  )

  private val BeginPattern: Regex = """^([ \t]*)(?://|#)\s*BEGIN\[([^]]+)]""".r
  private val EndPattern: Regex = """^[ \t]*(?://|#)\s*END\[([^]]+)]""".r

  def main(args: Array[String]): Unit = {
    require(args.length >= 2, "Usage: SnippetGenerator <output-file> <source-files>...")

    val outputPath = Path.of(args.head)
    val snippets = args.tail.toList.sorted.flatMap(sourcePath => extract(Path.of(sourcePath)))
    val snippetsByKey = snippets.groupBy(_.key)

    Files.createDirectories(outputPath.getParent)
    Files.writeString(outputPath, render(snippetsByKey), StandardCharsets.UTF_8)
  }

  private def extract(path: Path): List[Snippet] = {
    val lines = Files.readAllLines(path, StandardCharsets.UTF_8).asScala.toList
    val begins = mutable.ArrayBuffer.empty[Begin]
    val ends = mutable.ArrayBuffer.empty[End]

    lines.zipWithIndex.foreach { case (line, index) =>
      line match {
        case BeginPattern(whitespacePrefix, key) =>
          begins += Begin(key, index + 1, whitespacePrefix)
        case EndPattern(key) =>
          ends += End(key, index + 1)
        case _ =>
      }
    }

    begins.flatMap { begin =>
      val endIndex = ends.indexWhere(end => end.key == begin.key && end.lineNumber > begin.lineNumber)
      if (endIndex >= 0) {
        val end = ends.remove(endIndex)
        val startLineNumber = begin.lineNumber + 1
        val endLineNumber = end.lineNumber - 1
        val snippetLines = lines
          .slice(startLineNumber - 1, endLineNumber)
          .map { line =>
            if (line.startsWith(begin.whitespacePrefix)) {
              line.drop(begin.whitespacePrefix.length)
            } else {
              line
            }
          }

        Some(
          Snippet(
            filePath = workspacePath(path),
            fileName = path.getFileName.toString,
            language = language(path),
            startLineNumber = startLineNumber,
            endLineNumber = endLineNumber,
            key = begin.key,
            lines = snippetLines,
          ),
        )
      } else {
        System.err.println(
          s"Missing // END[${begin.key}] for ${workspacePath(path)}:${begin.lineNumber}",
        )
        None
      }
    }.toList
  }

  private def render(snippetsByKey: Map[String, List[Snippet]]): String = {
    val output = StringBuilder()
    output.append("package com.raquo.app.codesnippets.generated\n\n")
    output.append("import com.raquo.app.codesnippets.CodeSnippet\n")
    output.append("import vendor.highlightjs.hljs.LanguageName\n\n")
    output.append("/** This file is generated at compile time by Bazel. */\n")
    output.append("object GeneratedSnippets {\n")

    snippetsByKey.toList.sortBy(_._1).foreach { case (key, snippets) =>
      output.append(s"\n  val `${key}` = List(\n")
      snippets.sortBy(snippet => (snippet.filePath, snippet.startLineNumber)).foreach { snippet =>
        val fields = List(
          repr(snippet.filePath),
          repr(snippet.fileName),
          s"${repr(snippet.language)}.asInstanceOf[LanguageName]",
          snippet.startLineNumber.toString,
          snippet.endLineNumber.toString,
          repr(snippet.key),
          snippet.lines.map(repr).mkString("List(", ", ", ")"),
        )
        output.append(s"    CodeSnippet(${fields.mkString(", ")}),\n")
      }
      output.append("  )\n")
    }

    output.append("}\n")
    output.toString
  }

  private def language(path: Path): String = {
    val fileName = path.getFileName.toString
    if (fileName.endsWith(".less")) {
      "less"
    } else if (fileName.endsWith(".css")) {
      "css"
    } else if (fileName.endsWith(".js") || fileName.endsWith(".ts")) {
      "javascript"
    } else {
      "scala"
    }
  }

  private def workspacePath(path: Path): String = {
    val rawPath = path.toString.replace('\\', '/')
    val execRootMarker = "/execroot/_main/"
    val markerIndex = rawPath.indexOf(execRootMarker)
    if (markerIndex >= 0) {
      rawPath.drop(markerIndex + execRootMarker.length)
    } else {
      rawPath
    }
  }

  private def repr(value: String): String = {
    val escaped = value.flatMap {
      case '\\' => "\\\\"
      case '"' => "\\\""
      case '\n' => "\\n"
      case '\r' => "\\r"
      case '\t' => "\\t"
      case character => character.toString
    }
    s"\"${escaped}\""
  }
}
// END[codesnippets/generator]
