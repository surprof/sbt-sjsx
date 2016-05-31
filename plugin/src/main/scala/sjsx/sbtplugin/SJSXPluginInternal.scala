//     Project: sbt-sjs-annots
//      Module:
// Description:

// Copyright (c) 2016. Distributed under the MIT License (see included LICENSE file).
package sjsx.sbtplugin

import sbt.Keys.TaskStreams
import sbt._
import sbt.inc.Analysis
import sjsx.sbtplugin.SJSXPlugin.{SJSXDependency, SJSXLoader, SJSXSnippet}
import xsbti.api.{Definition, Projection}

object SJSXPluginInternal {


  def getDefinitions(analysis: Analysis): Iterable[Definition] =
    analysis.apis.external.values.flatMap(_.api.definitions).toVector ++
    analysis.apis.internal.values.flatMap(_.api.definitions).toVector

  def discoverSJSXStatic(defs: Iterable[Definition]) : Iterable[SJSXSnippet] = defs.collect {
      case SJSXStatic(annot) => annot
    }


  def discoverSJSXRequire(defs: Iterable[Definition]) : Set[SJSXDependency] =
    defs.collect {
      case SJSXRequire(dep) => dep
    }.toSet


  def makeRequireJS(reqs: Seq[SJSXDependency], require: String): String = {
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
    val prefix = "window"
    reqs.foldLeft(Acc(Set.empty[Seq[String]],"")){ (acc,d) =>
      val a = createPath(Seq(prefix),d.global.split("\\.").init,acc)
      val req = s"$prefix.${d.global} = $require('${d.id}');\n"
      a.copy(script = a.script+req)
    }.script
  }

  private def preamble(loader: SJSXLoader.Value, preamble: String) =
    (loader match {
      case SJSXLoader.None => ""
      case SJSXLoader.SystemJS => "window.require = function(id){System.amdRequire(document.baseURI+'/'+id)};\n"
    }) + preamble


  def writeAnnotations(file: File, analysis: Analysis, streams: TaskStreams, sjsxLoader: SJSXLoader.Value,
                       sjsxSnippets: Seq[SJSXSnippet], sjsxDeps: Seq[SJSXDependency], sjsxAnnotsPreamble: String,
                       sjsxDebug: Boolean): Unit = {
    streams.log.info(s"Writing JS annotations to $file")

    val defs = getDefinitions(analysis)
    defs.foreach(d => println(d.name()))

    // TODO: currently we need to replace \' quotes; find out why/where quotation of ' occurs...
    val annots = (discoverSJSXStatic(defs).toSeq++sjsxSnippets).sortBy(_.prio).
      map( _.arg.replaceAll("\\\\'","'")).mkString("\n")

    sjsxLoader match {
      case SJSXLoader.None =>
        IO.write(file,sjsxAnnotsPreamble+annots)
      case SJSXLoader.SystemJS =>
        val reqs = (discoverSJSXRequire(defs) ++ sjsxDeps).toSeq.sortBy(_.global)
        val reqsJS = makeRequireJS(reqs,"require")
        val deps = reqs.map(_.id).mkString("['","','","']")
        val script =
          s"""$sjsxAnnotsPreamble
             |System.registerDynamic("__main",$deps,true,function(require) {
             |$reqsJS
             |$annots
             |});
             |System.import("__main");
           """.stripMargin
        IO.write(file,script)
    }
  }


  object SJSXStatic {
    val annotated = "SJSXStatic"
    def unapply(t: Definition) : Option[SJSXSnippet] = t.annotations().
      find( _.base().asInstanceOf[Projection].id == annotated ).
      map { l =>
        val s = l.arguments.apply(0).value
        val args = s.substring(1,s.length-1).split(",",2)
        SJSXSnippet(args(0).toInt,extractString(args(1)))
      }
  }


  object SJSXRequire {
    val annotated = "SJSXRequire"
    def unapply(t: Definition) : Option[SJSXDependency] = t.annotations().
      find( _.base().asInstanceOf[Projection].id == annotated ).
      map { l =>
        val s = l.arguments.apply(0).value
        val args = s.substring(1,s.length-1).split(",",2)
        SJSXDependency(extractString(args(1)),extractString(args(0)))
      }
  }

  @inline
  private def extractString(s: String) = s.substring(1,s.length-1)

}
