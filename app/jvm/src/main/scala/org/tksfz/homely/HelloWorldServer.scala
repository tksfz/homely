package org.tksfz.homely

import cats.effect.{Effect, ExitCode, IO, IOApp}
import org.http4s.dsl.impl.Root
import cats.implicits._
import io.circe._
import io.circe.syntax._
import io.circe.generic.auto._
import org.http4s.{HttpRoutes, MediaType, StaticFile}
import org.http4s.headers.`Content-Type`
import org.http4s.implicits._
import org.http4s.server.blaze._
import org.tksfz.homely.discovery.Scanner
import org.tksfz.homely.resources.{Proxmox, Resource}
import org.http4s.dsl.io._
import org.http4s.server.middleware.CORS

import scala.concurrent.ExecutionContext.Implicits.global

object HelloWorldServer extends IOApp {
  val helloWorldService = HttpRoutes.of[IO] {
    case GET -> Root / "hello" / name =>
      Ok(s"Hello, $name.")
    case GET -> Root / "hello" =>
      val resources = Seq(Resource("asdf", Proxmox))
      val html = new Scanner().findAllHttp().map { resource =>
        s"<a href='${resource.uri}'><img src='public/resource-images/${resource.resourceType.icon}'></img>${resource.resourceType.displayLabel}</a>"
      }.mkString("\n")
      Ok.apply(html, `Content-Type`(MediaType.all("text" -> "html")))
    case req @ GET -> "public" /: path =>
      StaticFile.fromResource("/public" + path.toString, global, Some(req)).getOrElseF(NotFound())
    case GET -> Root / "resources" =>
      val resources = new Scanner().findAllHttp()
      Ok(resources.asJson.toString, `Content-Type`(MediaType.all("application" -> "json")))
  }.orNotFound

  def run(args: List[String]): IO[ExitCode] =
    BlazeServerBuilder[IO]
      .bindHttp(8000, "localhost")
      .withHttpApp(CORS(helloWorldService))
      .serve
      .compile
      .drain
      .as(ExitCode.Success)

}

