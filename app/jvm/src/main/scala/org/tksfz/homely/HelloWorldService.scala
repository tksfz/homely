package org.tksfz.homely

import cats.effect.Effect
import io.circe.syntax._
import io.circe.generic.auto._
import org.http4s.{HttpService, MediaType, StaticFile}
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.headers.`Content-Type`
import org.tksfz.homely.discovery.{Scanner}
import org.tksfz.homely.resources._

class HelloWorldService[F[_]: Effect] extends Http4sDsl[F] {

  val service: HttpService[F] = {
    HttpService[F] {
      case GET -> Root / "hello" =>
        val resources = Seq(Resource("asdf", Proxmox))
        val html = new Scanner().findAllHttp().map { resource =>
          s"<a href='${resource.uri}'><img src='public/resource-images/${resource.resourceType.icon}'></img>${resource.resourceType.displayLabel}</a>"
        }.mkString("\n")
        Ok(html).withContentType(`Content-Type`(MediaType.`text/html`))
      case req @ GET -> "public" /: path =>
        StaticFile.fromResource("/public" + path.toString, Some(req)).getOrElseF(NotFound())
      case GET -> Root / "resources" =>
        val resources = new Scanner().findAllHttp()
        Ok(resources.asJson).withContentType(`Content-Type`(MediaType.`application/json`))
    }
  }
}
