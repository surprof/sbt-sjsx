//     Project: sbt-sjs-annots
//      Module: annots
// Description: Scala annotation to write JavaScript content into an annotations file during fastOptJS / fullOptJS

// Copyright (c) 2016. Distributed under the MIT License (see included LICENSE file).
package de.surfice.sjsannots

import scala.annotation.StaticAnnotation

/**
 * Annotates a Scala class with some JavaScript code that will be written into a separate annotation
 * file by the sbt plugin when the `writeJSAnnots` task is run.
 *
 * @param js JavScript code to be written to the annotations file
// * @param prio priority of this annotation; in the output annotations file smaller numbers occur before higher numbers
 */
class SJSAnnotation(js: String) extends StaticAnnotation
