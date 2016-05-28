//     Project: sbt-sjs-annots
//      Module: annots
// Description: Annotation

// Copyright (c) 2016. Distributed under the MIT License (see included LICENSE file).
package de.surfice.sjsannots

import scala.annotation.StaticAnnotation

class SJSRequire(globalName: String, dependency: String) extends StaticAnnotation
