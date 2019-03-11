package org.tksfz.homely

import cats.effect._
import org.http4s.dsl.impl.Root
import cats.implicits._
import io.circe.syntax._
import io.circe.generic.auto._
import org.http4s.{HttpRoutes, MediaType, StaticFile}
import org.http4s.headers.`Content-Type`
import org.http4s.implicits._
import org.http4s.server.blaze._
import org.tksfz.homely.discovery.Scanner
import org.http4s.dsl.io._
import org.http4s.server.middleware.CORS
import org.tksfz.homely.db.Initial
import doobie._
import doobie.implicits._

import scala.concurrent.ExecutionContext.Implicits.global

object HelloWorldServer extends IOApp {
  val db = new Initial
  val xa = db.xa

  val store = new ResourceStore(xa)

  // On startup, trigger a rescan
  contextShift.shift.flatMap(_ => store.rescanAndSave()).unsafeRunAsyncAndForget()

  val helloWorldService = HttpRoutes.of[IO] {
    case GET -> Root / "hello" / name =>
      Ok(s"Hello, $name.")
    case GET -> Root / "hello" =>
      val html = new Scanner().findAllHttp().map { resource =>
        s"<a href='${resource.uri}'><img src='public/resource-images/${resource.resourceType.icon}'></img>${resource.resourceType.displayLabel}</a>"
      }.mkString("\n")
      Ok.apply(html, `Content-Type`(MediaType.text.html))
    case GET -> Root / "resources" =>
      val resources = new Scanner().findAllHttp()
      Ok(resources.asJson.toString, `Content-Type`(MediaType.application.json))
    case GET -> Root / "dbresources" =>
      for {
        resources <- db.findAll.transact[IO](xa)
        result <- Ok.apply(resources.asJson.toString, `Content-Type`(MediaType.application.json))
      } yield {
        result
      }
    case req @ GET -> Root =>
      StaticFile.fromResource("/public/index.html", global, Some(req)).getOrElseF(NotFound())
    case req @ GET -> path =>
      StaticFile.fromResource("/public" + path.toString, global, Some(req)).getOrElseF(NotFound())
  }.orNotFound

  def run(args: List[String]): IO[ExitCode] =
    BlazeServerBuilder[IO]
      .bindHttp(8000, "0.0.0.0")
      .withHttpApp(CORS(helloWorldService))
      .serve
      .compile
      .drain
      .as(ExitCode.Success)

}

