//     Project: sbt-sjs-annots
//      Module: plugin
// Description: Plugin for generating an JS file with contents from Scala annotations

// Copyright (c) 2016. Distributed under the MIT License (see included LICENSE file).
package de.surfice.sjsannots.sbtplugin

import org.scalajs.sbtplugin.ScalaJSPlugin
import org.scalajs.sbtplugin.impl.DependencyBuilders
import sbt.Keys._
import sbt._

object SJSAnnotsPlugin extends sbt.AutoPlugin {
  import SJSAnnotsPluginInternal._
  import ScalaJSPlugin.AutoImport.{fastOptJS,fullOptJS}

  override def requires = ScalaJSPlugin

  lazy val jsAnnotsFile = settingKey[File]("Target file for JS annotations")

  lazy val jsAnnotsList = taskKey[Iterable[String]]("List with all defined JS annotations")

  lazy val writeJSAnnots = taskKey[Unit]("Writes the JS annotations file")

  override def projectSettings = Seq(
    jsAnnotsFile := (crossTarget in compile).value / s"${(moduleName in compile).value}-annots.js",
    jsAnnotsList := discoverAnnotations((compile in Compile).value),
    writeJSAnnots <<= (jsAnnotsFile, jsAnnotsList, streams) map writeAnnotations,
    libraryDependencies += DepBuilder.toScalaJSGroupID("de.surfice") %%% "sjs-annots" % Version.sjsannotsVersion,
    (fastOptJS in Compile) <<= (fastOptJS in Compile).dependsOn(writeJSAnnots),
    (fullOptJS in Compile) <<= (fullOptJS in Compile).dependsOn(writeJSAnnots)
  )

  private object DepBuilder extends DependencyBuilders
}
