
lazy val commonSettings = Seq(
  organization := "de.surfice",
  version := "0.3.1",
  scalaVersion := "2.11.11",
  scalacOptions ++= Seq("-deprecation","-unchecked","-feature","-Xlint"),
  libraryDependencies ++= Seq(
    "com.lihaoyi" %% "utest" % "0.4.4" % "test"
  ),
  testFrameworks += new TestFramework("utest.runner.Framework"),
  resolvers += Resolver.sonatypeRepo("snapshots")
)

lazy val root = project.in(file(".")).
  aggregate(sjsx,plugin).
  settings(commonSettings:_*).
  settings(publishingSettings: _*).
  settings(
    name := "SJSX",
    publish := {},
    publishLocal := {}
  )

lazy val sjsx = project.
  enablePlugins(ScalaJSPlugin).
  settings(commonSettings: _*).
  settings(publishingSettings: _*).
  settings(
    name := "sjsx",
    //autoScalaLibrary := false,
    //crossPaths := false,
    libraryDependencies ++= Seq(
    ),
    crossScalaVersions := Seq("2.11.11","2.12.2")
  )

//lazy val tests = project.
//  enablePlugins(ScalaJSPlugin).
//  dependsOn(sjsx).
//  settings(commonSettings:_*).
//  settings(
//    name := "tests",
//    publish := {},
//    publishLocal := {}
//  )
//

lazy val plugin = project.
  //dependsOn(sjsx).
  settings(commonSettings: _*).
  settings(publishingSettings: _*).
  settings( 
    name := "sbt-sjsx",
    description := "sbt plugin for generating JS annotation files from annotations defined on Scala classes",
    sbtPlugin := true,
    scalaVersion := "2.10.6",
    addSbtPlugin("org.scala-js" % "sbt-scalajs" % scalaJSVersion),
    sourceGenerators in Compile += Def.task {
      val file = (sourceManaged in Compile).value / "Version.scala"
      IO.write(file,
        s"""package sjsx.sbtplugin
        |object Version { val sjsxVersion = "${version.value}" }
        """.stripMargin)
      Seq(file)
    }.taskValue,
    libraryDependencies ++= Seq()
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
    <url>https://github.com/jokade/sjsx</url>
    <licenses>
      <license>
        <name>MIT License</name>
        <url>http://www.opensource.org/licenses/mit-license.php</url>
      </license>
    </licenses>
    <scm>
      <url>git@github.com:jokade/sjsx</url>
      <connection>scm:git:git@github.com:jokade/sjsx.git</connection>
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
 
