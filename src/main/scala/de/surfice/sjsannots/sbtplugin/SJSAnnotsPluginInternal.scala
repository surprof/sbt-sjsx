//     Project: sbt-sjs-annots
//      Module:
// Description:

// Copyright (c) 2016. Distributed under the MIT License (see included LICENSE file).
package de.surfice.sjsannots.sbtplugin

import sbt.Keys.TaskStreams
import sbt._
import sbt.inc.Analysis
import xsbti.api.{Definition, Projection}

object SJSAnnotsPluginInternal {

  def discoverAnnotations(analysis: Analysis) : Iterable[String] = {
    val acs = analysis.apis.external.values.flatMap(_.api.definitions()).toVector ++
      analysis.apis.internal.values.flatMap(_.api.definitions).toVector
    acs collect {
      case SJSAnnotation(annot) => annot
//      case SJSRequire(annot) => annot
    }
  }

  def writeAnnotations(file: File, annotations: Iterable[String],streams: TaskStreams): Unit = {
    streams.log.info(s"Writing Angular annotations to $file")
    // TODO: currently we need to replace \' quotes; find out why/where quotation of ' occurs...
    val annots = annotations.map( _.replaceAll("\\\\'","'")).mkString("\n")
    IO.write(file,annots)
  }

  object SJSAnnotation {
    val annotated = "SJSAnnotation"
    def unapply(t: Definition) : Option[String] = t.annotations().
      find( _.base().asInstanceOf[Projection].id == annotated ).
      map { l =>
        val s = l.arguments.apply(0).value
        if(s.startsWith("("))
          s.substring(2,s.length-2)
        else
          s.substring(1,s.length-1)
      }
  }

//  object SJSRequire {
//    val annotated = "SJSRequire"
//    def unapply(t: Definition) : Option[String] = t.annotations().
//      find( _.base().asInstanceOf[Projection].id == annotated ).
//      map { l =>
//        val s = l.arguments.apply(0).value
//        val args = s.substring(2,s.length-2).split("\",\"")
//        val js = s"""if("""
//        println(s"SJSRequire: $s")
//        ""
//      }
//  }
}
