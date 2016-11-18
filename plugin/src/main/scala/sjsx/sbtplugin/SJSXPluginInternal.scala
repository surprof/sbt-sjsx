//     Project: sbt-sjsx
//      Module:
// Description:

// Copyright (c) 2016. Distributed under the MIT License (see included LICENSE file).
package sjsx.sbtplugin

import org.scalajs.core.tools.linker.analyzer.Analysis.ClassInfo
import org.scalajs.core.tools.linker.analyzer.Analyzer
import org.scalajs.core.tools.linker.backend.{BasicLinkerBackend, LinkerBackend}
import org.scalajs.core.tools.logging.NullLogger
import sbt.Keys.TaskStreams
import sbt._
import sjsx.sbtplugin.SJSXPlugin.autoImport.{SJSXDependency, SJSXLoader, SJSXSnippet}
import sjsx.sbtplugin.SJSXPlugin.{SJSXConfig, ScalaJSTools}

import scala.collection.mutable
import scala.util.{Failure, Success, Try}

object SJSXPluginInternal {
  import scala.reflect.runtime.{universe => ru}


  // TODO: more efficient way to generate sjsx file?
  def writeAnnotations(sjsxConfig: SJSXConfig, scalaJSTools: ScalaJSTools, streams: TaskStreams): Unit = {
    import sjsxConfig._
    import streams.log

    val classes = analyse(scalaJSTools,log)
    val (snippets,requires) = findAnnots(classes,scalaJSTools)

    val js = (snippets ++ sjsxSnippets).sortBy (_.prio).map( _.arg.replaceAll("\\\\'","'")).mkString("\n")

    sjsxLoader match {
      case SJSXLoader.None =>
        IO.write(sjsxFile, sjsxPreamble + js)
//      case SJSXLoader.SystemJS | SJSXLoader.CommonJS =>
//        val reqs = (requires ++ sjsxDeps) sortBy(_.global)
//        val reqsJS = makeRequireJS(reqs,"require")
//        val deps = reqs.map(_.id).mkString("['","','","']")
//        val script =
//          s"""$sjsxPreamble
//             |System.registerDynamic("__main",$deps,true,function(require) {
//             |$reqsJS
//             |$js
//             |});
//             |System.import("__main");
//           """.stripMargin
//        IO.write(sjsxFile,script)
      case SJSXLoader.CommonJS =>
        val reqs = (requires ++ sjsxDeps) sortBy(_.global)
        val reqsJS = makeRequireJS(reqs,"require",sjsxLoader)
        val deps = reqs.map(_.id).mkString("['","','","']")
        val script =
          s"""$sjsxPreamble
             |$reqsJS
             |$js
           """.stripMargin
        IO.write(sjsxFile,script)
    }

  }

  private def analyse(scalaJSTools: ScalaJSTools, log: sbt.Logger) = {
    import scalaJSTools._

    // code taken from scala-js-call-graph/sbt-scalajs-callgraph/src/main/scala/ch/epfl/sbtplugin/CallGraphPlugin.scala
    val semantics = linker.semantics
    val symbolRequirements = new BasicLinkerBackend(semantics,outputMode,withSourceMaps,LinkerBackend.Config())
      .symbolRequirements

    val infos = Try {
      linker.linkUnit(ir, symbolRequirements, NullLogger)
    } match {
      case Success(linkUnit) =>
        linkUnit.infos.values.toSeq
      case Failure(e) =>
        log.warn(e.getMessage)
        log.warn("Non linking program, falling back to all the *.sjsir files on the classpath...")
        ir map (_.info)
    }

    Analyzer.computeReachability(semantics, symbolRequirements, infos, false).classInfos.values.filter(_.isNeededAtAll)
  }

  @inline
  private def filterClass(cname: String): Boolean = cname.split("\\.",2).head match {
    case "scala" | "java" | "utest" => false
    case _ => true
  }

  private def findAnnots(classInfos: Iterable[ClassInfo], scalaJSTools: ScalaJSTools): (Seq[SJSXSnippet],Seq[SJSXDependency]) = {
    import scalaJSTools._

    val mirror = ru.runtimeMirror(classLoader)

    val snippets = mutable.Buffer.empty[SJSXSnippet]
    val deps = mutable.Buffer.empty[SJSXDependency]

    def extractAnnots(cls: Class[_]) = try {
      mirror.classSymbol(cls).annotations foreach {
        case SJSXStatic(snippet) => snippets += snippet
        case SJSXRequire(dep) => deps += dep
        case _ =>
      }
    } catch {
      case _:Throwable =>
    }

    // TODO: avoid filtering (scala.runtime classes and java classes cannot be found by the used class loader)
    classInfos.view map (_.displayName) filter filterClass map classLoader.loadClass foreach extractAnnots

    (snippets,deps)
  }

  private def makeRequireJS(reqs: Seq[SJSXDependency], require: String, loader: SJSXLoader.Value): String = {
    case class Acc(existentPaths: Set[Seq[String]], script: String)
    @annotation.tailrec
    def createPath(prefix: Seq[String], path: Seq[String], acc: Acc): Acc =
      if(path.isEmpty) acc
      else {
        import acc._
        val next = path.head
        val newPrefix = prefix :+ next
        val newPrefixString = newPrefix.mkString(".")
        if(!existentPaths.contains(newPrefix)) {
          val newScript = script + s"if( typeof($newPrefixString) == 'undefined' ) $newPrefixString = {};\n"
          createPath(newPrefix,path.tail,Acc(existentPaths+newPrefix,newScript))
        }
        else
          createPath(newPrefix,path.tail,acc)
      }
    val prefix = loader match {
      case SJSXLoader.None => Seq("window")
      case SJSXLoader.CommonJS => Nil
    }
    reqs.foldLeft(Acc(Set.empty[Seq[String]],"")){ (acc,d) =>
      val a = createPath(prefix,d.global.split("\\.").init,acc)
      val req = s"${prefix.headOption.map( _ + ".").getOrElse("")}${d.global} = $require('${d.id}');\n"
      a.copy(script = a.script+req)
    }.script
  }

  private def preamble(loader: SJSXLoader.Value, preamble: String) =
    (loader match {
      case SJSXLoader.None => ""
//      case SJSXLoader.SystemJS => "window.require = function(id){System.amdRequire(document.baseURI+'/'+id)};\n"
      case SJSXLoader.CommonJS => ""
    }) + preamble


  object SJSXStatic {
    val annotated = "SJSXStatic"
    def unapply(annot: reflect.runtime.universe.Annotation) : Option[SJSXSnippet] =
      if(annot.tpe.toString=="sjsx.SJSXStatic") Some( SJSXSnippet(
        annot.scalaArgs(0).productElement(0).asInstanceOf[ru.Constant].value.asInstanceOf[Int],
        annot.scalaArgs(1).productElement(0).asInstanceOf[ru.Constant].value.asInstanceOf[String]
      ))
      else None
  }


  object SJSXRequire {
    val annotated = "SJSXRequire"
    def unapply(annot: reflect.runtime.universe.Annotation) : Option[SJSXDependency] =
      if(annot.tpe.toString=="sjsx.SJSXRequire") Some( SJSXDependency(
        annot.scalaArgs(0).productElement(0).asInstanceOf[ru.Constant].value.asInstanceOf[String],
        annot.scalaArgs(1).productElement(0).asInstanceOf[ru.Constant].value.asInstanceOf[String]
      ))
      else None

  }


}
