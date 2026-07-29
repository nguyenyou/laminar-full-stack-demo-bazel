package tools.scalajs

import java.nio.file.Files
import java.nio.file.Paths

import scala.concurrent.Await
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.Duration

import io.bazel.rulesscala.worker.Worker
import org.scalajs.linker.PathIRContainer
import org.scalajs.linker.PathOutputDirectory
import org.scalajs.linker.StandardImpl
import org.scalajs.linker.interface.ClearableLinker
import org.scalajs.linker.interface.ESVersion
import org.scalajs.linker.interface.IRFileCache
import org.scalajs.linker.interface.ModuleInitializer
import org.scalajs.linker.interface.ModuleKind
import org.scalajs.linker.interface.StandardConfig
import org.scalajs.logging.NullLogger

object ScalaJSLinkerMain extends Worker.Interface {
  private given ExecutionContext = ExecutionContext.global

  private val irFileCache = StandardImpl.irFileCache()
  private val irCache: IRFileCache.Cache = irFileCache.newCache

  private var linkerOpt = Option.empty[ClearableLinker]
  private var developmentOpt = Option.empty[Boolean]

  def main(args: Array[String]): Unit = Worker.workerMain(args, ScalaJSLinkerMain)

  override def work(args: Array[String]): Unit = {
    require(
      args.length >= 4,
      "Usage: ScalaJSLinkerMain <development|production> <main-class> <output-directory> <classpath>...",
    )

    val development = args(0) match {
      case "development" => true
      case "production" => false
      case mode => throw IllegalArgumentException(s"Unknown Scala.js link mode: $mode")
    }
    val mainClass = args(1)
    val outputDirectoryPath = Paths.get(args(2))
    val classpath = args.drop(3).map(Paths.get(_)).toSeq

    Files.createDirectories(outputDirectoryPath)

    val linker = linkerFor(development)

    val result = for {
      (containers, _) <- PathIRContainer.fromClasspath(classpath)
      irFiles <- irCache.cached(containers)
      report <- linker.link(
        irFiles,
        Seq(ModuleInitializer.mainMethodWithArgs(mainClass, "main")),
        PathOutputDirectory(outputDirectoryPath),
        NullLogger,
      )
    } yield report

    Await.result(result, Duration.Inf)

    if (development) {
      println(s"Scala.js IR cache: ${irFileCache.stats.logLine}")
      irFileCache.clearStats()
    }
  }

  private def linkerFor(development: Boolean): ClearableLinker = {
    developmentOpt.foreach { existingDevelopment =>
      require(
        existingDevelopment == development,
        "A Scala.js linker worker cannot change between development and production mode.",
      )
    }

    linkerOpt.getOrElse {
      val config = StandardConfig()
        .withBatchMode(!development)
        .withOptimizer(!development)
        .withModuleKind(ModuleKind.ESModule)
        .withESFeatures(_.withESVersion(ESVersion.ES2020))
        .withSourceMap(true)

      val linker = StandardImpl.clearableLinker(config)
      developmentOpt = Some(development)
      linkerOpt = Some(linker)
      linker
    }
  }
}
