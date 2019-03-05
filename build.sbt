import org.scalajs.sbtplugin.ScalaJSPlugin.AutoImport.scalaJSModuleKind
import sbt.Keys.version

val Http4sVersion = "0.20.0-M6"
val Specs2Version = "4.1.0"
val LogbackVersion = "1.2.3"
val circeVersion = "0.10.0"

lazy val root = (project in file("."))
  .aggregate(appJS, appJVM)

lazy val app = (crossProject in file("app"))
  .jsConfigure(_.enablePlugins(ScalaJSBundlerPlugin))
  .settings(
    unmanagedSourceDirectories in Compile +=
      baseDirectory.value  / "shared" / "main" / "scala",
    organization := "org.tksfz",
    name := "homely",
    version := "0.0.1-SNAPSHOT",
    scalaVersion := "2.12.6",
    libraryDependencies ++= Seq(
      "io.circe" %%% "circe-core",
      "io.circe" %%% "circe-generic",
      "io.circe" %%% "circe-parser",
      "io.circe" %%% "circe-generic-extras",
    ).map(_ % circeVersion),
  ).jsSettings(
    npmDependencies in Compile += "@shopify/draggable" -> "v1.0.0-beta.7",
    resolvers += "jitpack" at "https://jitpack.io",
    libraryDependencies += "com.github.outwatch" % "outwatch" % "master-SNAPSHOT",
    scalaJSUseMainModuleInitializer := true,
    scalaJSModuleKind := ModuleKind.CommonJSModule,
    webpackConfigFile in fastOptJS := Some(baseDirectory.value / "webpack.config.dev.js"),
    webpackBundlingMode in fastOptJS := BundlingMode.LibraryOnly(), // https://scalacenter.github.io/scalajs-bundler/cookbook.html#performance
    addCommandAlias("dev", ";  compile; fastOptJS::startWebpackDevServer; devwatch; fastOptJS::stopWebpackDevServer"),
    addCommandAlias("devwatch", "~; fastOptJS; copyFastOptJS"),
    copyFastOptJS := {
      val inDir = (crossTarget in (Compile, fastOptJS)).value
      val outDir = (crossTarget in (Compile, fastOptJS)).value / "dev"
      val files = Seq(name.value.toLowerCase + "-fastopt-loader.js",
        name.value.toLowerCase + "-fastopt.js",
        name.value.toLowerCase + "-fastopt.js.map") map { p => (inDir / p, outDir / p) }
      IO.copy(files, overwrite = true, preserveLastModified = true, preserveExecutable = true)
    }
  ).jvmSettings(
    libraryDependencies ++= Seq(
      "org.http4s"      %% "http4s-blaze-server" % Http4sVersion,
      "org.http4s"      %% "http4s-circe"        % Http4sVersion,
      "org.http4s"      %% "http4s-dsl"          % Http4sVersion,
      "org.specs2"     %% "specs2-core"          % Specs2Version % "test",
      "ch.qos.logback"  %  "logback-classic"     % LogbackVersion,
      "com.propensive" %% "magnolia" % "0.10.0",
      "org.tpolecat" %% "doobie-core"      % "0.6.0",
      "org.xerial" % "sqlite-jdbc" % "3.25.2",
    ),
    addCompilerPlugin("org.spire-math" %% "kind-projector"     % "0.9.6"),
    addCompilerPlugin("com.olegpy"     %% "better-monadic-for" % "0.2.4"),
  )

lazy val appJS = app.js
lazy val appJVM = app.jvm.settings(
  (resources in Compile) += (fastOptJS in (appJS, Compile)).value.data
)

// when running the "dev" alias, after every fastOptJS compile all artifacts are copied into
// a folder which is served and watched by the webpack devserver.
// this is a workaround for: https://github.com/scalacenter/scalajs-bundler/issues/180
lazy val copyFastOptJS = TaskKey[Unit]("copyFastOptJS", "Copy javascript files to target directory")
