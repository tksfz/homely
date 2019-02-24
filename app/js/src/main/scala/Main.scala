import outwatch.dom._
import outwatch.dom.dsl._
import monix.execution.Scheduler.Implicits.global
import monix.reactive.Observable
import org.scalajs.dom.{Blob, XMLHttpRequest}
import org.scalajs.dom.ext.{Ajax, AjaxException}
import outwatch.http.Http.{BodyType, Request, Response}
import io.circe.generic.auto._
import io.circe.parser.decode
import org.tksfz.homely.resources.Resource

import scala.concurrent.Future
import scala.scalajs.js
import scala.scalajs.js.typedarray.ArrayBuffer

object Main {
  def main(args: Array[String]): Unit = {

    val myComponent = div("Hello World")

    val resources = request(Observable(Request("http://localhost:8000/resources")))
      .map(_.response.toString)
      .map(decode[Seq[Resource]])
      .map(_.getOrElse(Nil))

    val html = div("hi", resources
      .map { resources =>
        resources.map { resource =>
          a(href := resource.uri, resource.resourceType.displayLabel)
        }
      })

    val content = div(h1("hello world"), div("blah", html))

    //OutWatch.renderReplace("#app", myComponent).unsafeRunSync()
    OutWatch.renderInto("#app", content).unsafeRunSync()
  }
  private def toResponse(req: XMLHttpRequest): Response = {
    val body : BodyType = req.responseType match {
      case "" => req.response.asInstanceOf[String]
      case "text" => req.responseText
      case "json" => req.response.asInstanceOf[js.Dynamic]
      case "arraybuffer" => req.response.asInstanceOf[ArrayBuffer]
      case "blob" => req.response.asInstanceOf[Blob]
      case _ => req.response
    }

    Response(
      body = body,
      status = req.status,
      responseType = req.responseType,
      xhr = req,
      response = req.response
    )
  }

  private def ajax(request: Request): Future[XMLHttpRequest] = Ajax(
    method = request.method,
    url = request.url,
    data = request.data,
    timeout = request.timeout,
    headers = request.headers,
    withCredentials = request.withCredentials,
    responseType = request.responseType
  )
  private def request(observable: Observable[Request]): Observable[Response] =
    observable.flatMap { request =>
      Observable.fromFuture(
        ajax(request)
          .map(toResponse)
          .recover {
            case AjaxException(req) => toResponse(req)
          }
      )
    }.share

}