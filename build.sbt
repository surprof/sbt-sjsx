
lazy val commonSettings = Seq(
  organization := "de.surfice",
  version := "0.1-SNAPSHOT",
  scalaVersion := "2.11.8",
  scalacOptions ++= Seq("-deprecation","-unchecked","-feature","-Xlint"),
  libraryDependencies ++= Seq(
  )
)

lazy val annots = project.
  enablePlugins(ScalaJSPlugin).
  settings(commonSettings: _*).
  settings(
    name := "sjs-annots"
  )

lazy val plugin = project.in(file(".")).
  aggregate(annots).
  settings(commonSettings: _*).
  //settings(publishingSettings: _*).
  settings( 
    name := "sbt-sjs-annots",
    description := "sbt plugin for generating of JS annotation files from annotations defined on Scala classes",
    sbtPlugin := true,
    scalaVersion := "2.10.6",
    addSbtPlugin("org.scala-js" % "sbt-scalajs" % "0.6.9"),
    sourceGenerators in Compile += Def.task {
      val file = (sourceManaged in Compile).value / "Version.scala"
      IO.write(file,
        s"""package de.surfice.sjsannots.sbtplugin
        |object Version { val sjsannotsVersion = "${version.value}" }
        """.stripMargin)
      Seq(file)
    }.taskValue,
    libraryDependencies ++= Seq(
    )
  )


lazy val publishingSettings = Seq(
  publishMavenStyle := true,
  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value)
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("releases"  at nexus + "service/local/staging/deploy/maven2")
  },
  pomExtra := (
    <url>https://github.com/jokade/REPO</url>
    <licenses>
      <license>
        <name>MIT License</name>
        <url>http://www.opensource.org/licenses/mit-license.php</url>
      </license>
    </licenses>
    <scm>
      <url>git@github.com:jokade/REPO</url>
      <connection>scm:git:git@github.com:jokade/REPO.git</connection>
    </scm>
    <developers>
      <developer>
        <id>jokade</id>
        <name>Johannes Kastner</name>
        <email>jokade@karchedon.de</email>
      </developer>
    </developers>
  )
)
 
