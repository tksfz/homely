import org.scalajs.sbtplugin.ScalaJSPlugin.AutoImport.scalaJSModuleKind
import sbt.Keys.version

val Http4sVersion = "0.20.0-M6"
val Specs2Version = "4.1.0"
val LogbackVersion = "1.2.3"
val circeVersion = "0.10.0"

lazy val root = (project in file("."))
  .aggregate(appJS, appJVM)

// ScalaJSBundlerPlugin docs https://github.com/scalacenter/scalajs-bundler/blob/master/manual/src/ornate/reference.md
lazy val app = (crossProject in file("app"))
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
  )
  .jsConfigure(_.enablePlugins(ScalaJSBundlerPlugin))
  .jsSettings(
    npmDependencies in Compile += "@shopify/draggable" -> "v1.0.0-beta.7",
    resolvers += "jitpack" at "https://jitpack.io",
    libraryDependencies += "com.github.outwatch" % "outwatch" % "master-SNAPSHOT",
    useYarn := true,
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
  )
  .jvmConfigure(_.enablePlugins(DockerPlugin))
  .jvmSettings(
    Compile/mainClass := Some("org.tksfz.homely.HelloWorldServer"),
    // brings assets into server package
    // https://stackoverflow.com/questions/37633251/serving-scala-js-assets
    unmanagedResourceDirectories in Compile += appJS.base / "assets",
    libraryDependencies ++= Seq(
      "org.http4s"      %% "http4s-blaze-server" % Http4sVersion,
      "org.http4s"      %% "http4s-circe"        % Http4sVersion,
      "org.http4s"      %% "http4s-dsl"          % Http4sVersion,
      "org.specs2"      %% "specs2-core"         % Specs2Version % "test",
      "ch.qos.logback"  %  "logback-classic"     % LogbackVersion,
      "com.propensive"  %% "magnolia"            % "0.10.0",
      "org.tpolecat"    %% "doobie-core"         % "0.6.0",
      "org.xerial"      % "sqlite-jdbc"          % "3.25.2",
    ),
    addCompilerPlugin("org.spire-math" %% "kind-projector"     % "0.9.6"),
    addCompilerPlugin("com.olegpy"     %% "better-monadic-for" % "0.2.4"),

    // https://github.com/marcuslonnberg/sbt-docker#defining-a-dockerfile
    dockerfile in docker := {
      val jarFile: File = sbt.Keys.`package`.in(Compile, packageBin).value
      val classpath = (managedClasspath in Compile).value
      val mainclass = mainClass.in(Compile, packageBin).value.getOrElse(sys.error("Expected exactly one main class"))
      val jarTarget = s"/app/${jarFile.getName}"
      // Make a colon separated classpath with the JAR file
      val classpathString = classpath.files.map("/app/" + _.getName)
        .mkString(":") + ":" + jarTarget
      new Dockerfile {
        // Base image
        from("openjdk:8-jre-alpine")
        run("apk", "add", "nmap", "nmap-scripts")
        // Add all files on the classpath
        add(classpath.files, "/app/")
        // Add the JAR file
        add(jarFile, jarTarget)
        // On launch run Java with the classpath and the main class
        entryPoint("java", "-cp", classpathString, mainclass)
        expose(8000)
      }
    }
  )

lazy val appJS: Project = app.js
lazy val appJVM = app.jvm.settings(
  // https://www.edc4it.com/blog/scala/exploring-options-in-making-scalajs-javascript-available-to-server.html
  resourceGenerators in Compile += Def.task {
    println("Copying scalajs-bundler output to jvm/target")
    val files = ((crossTarget in(appJS, Compile)).value / "scalajs-bundler/main" ** ("homely*.js" || "homely*.map")).get
    val mappings: Seq[(File,String)] = files pair
      Path.rebase((crossTarget in(appJS, Compile)).value / "scalajs-bundler/main",
        ((resourceManaged in  Compile).value / "public/").getAbsolutePath )
    val map: Seq[(File, File)] = mappings.map { case (s, t) => (s, file(t))}
    IO.copy(map).toSeq
  }.dependsOn(webpack in(appJS, Compile, fastOptJS))
)

// when running the "dev" alias, after every fastOptJS compile all artifacts are copied into
// a folder which is served and watched by the webpack devserver.
// this is a workaround for: https://github.com/scalacenter/scalajs-bundler/issues/180
lazy val copyFastOptJS = TaskKey[Unit]("copyFastOptJS", "Copy javascript files to target directory")
