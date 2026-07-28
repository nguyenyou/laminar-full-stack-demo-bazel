package tools.scalajs

import java.nio.file.Files
import java.nio.file.Paths

import scala.concurrent.Await
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.Duration

import org.scalajs.linker.PathIRContainer
import org.scalajs.linker.PathOutputDirectory
import org.scalajs.linker.StandardImpl
import org.scalajs.linker.interface.ESVersion
import org.scalajs.linker.interface.ModuleInitializer
import org.scalajs.linker.interface.ModuleKind
import org.scalajs.linker.interface.StandardConfig
import org.scalajs.logging.NullLogger

object ScalaJSLinkerMain {
  def main(args: Array[String]): Unit = {
    require(
      args.length >= 3,
      "Usage: ScalaJSLinkerMain <main-class> <output-directory> <classpath>...",
    )

    val mainClass = args(0)
    val outputDirectoryPath = Paths.get(args(1))
    val classpath = args.drop(2).map(Paths.get(_)).toSeq

    Files.createDirectories(outputDirectoryPath)

    given ExecutionContext = ExecutionContext.global

    val cache = StandardImpl.irFileCache().newCache
    val linker = StandardImpl.linker(
      StandardConfig()
        .withBatchMode(true)
        .withModuleKind(ModuleKind.ESModule)
        .withESFeatures(_.withESVersion(ESVersion.ES2020))
        .withSourceMap(true),
    )

    val result = for {
      (containers, _) <- PathIRContainer.fromClasspath(classpath)
      irFiles <- cache.cached(containers)
      report <- linker.link(
        irFiles,
        Seq(ModuleInitializer.mainMethodWithArgs(mainClass, "main")),
        PathOutputDirectory(outputDirectoryPath),
        NullLogger,
      )
    } yield report

    try {
      Await.result(result, Duration.Inf)
    } finally {
      cache.free()
    }
  }
}
