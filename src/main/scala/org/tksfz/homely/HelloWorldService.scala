package org.tksfz.homely

import cats.effect.Effect
import io.circe.Json
import org.http4s.HttpService
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.tksfz.homely.discovery.Scanner

class HelloWorldService[F[_]: Effect] extends Http4sDsl[F] {

  val service: HttpService[F] = {
    HttpService[F] {
      case GET -> Root / "hello" =>
        val html =
          new Scanner().findAllHttp().mkString("\n")
        Ok(html)
    }
  }
}
