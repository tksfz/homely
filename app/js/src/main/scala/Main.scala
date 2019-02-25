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
  def appContainer(content: Observable[VNode]): VNode = {
    div(
      htmlTag("nav")(cls := "navbar has-shadow", role := "navigation", attr("aria-label") := "main navigation",
        div(cls := "navbar-brand",
          a(cls := "navbar-item", href := "https://bulma.io",
            span(cls := "icon", i(cls := "fas fa-home fa-2x")),
            span(h1(cls := "title is-4", "Homely"))
          )
        ),
        div(cls := "navbar-menu",
          div(cls := "navbar-end",
            a(cls := "navbar-item",
              span(cls := "icon", i(cls := "fas fa-cog")),
              span("Settings"),
            )
          )
        ),
      ),
      htmlTag("section")(cls := "section",
        div(cls := "container",
          div(id := "pageContent", content)
        )
      )
    )
  }

  def main(args: Array[String]): Unit = {
    val resources = request(Observable(Request("http://localhost:8000/resources")))
      .map(_.response.toString)
      .map(decode[Seq[Resource]])
      .map(_.getOrElse(Nil))

    val html = resources.map { resources =>
      div(cls := "columns is-multiline", resources.map { resource =>
        div(cls := "column is-one-fifth",
          appCard(s"resource-images/${resource.resourceType.icon}",
            resource.uri,
            resource.resourceType.displayLabel
          ),
        )
      })
    }

    //OutWatch.renderReplace("#app", myComponent).unsafeRunSync()
    OutWatch.renderInto("#app", Main.appContainer(html)).unsafeRunSync()
  }

  private def appCard(imgSrc: String, url: String, name: String) = {
    div(cls := "card",
      div(cls := "card-image has-text-centered",
        figure(cls := "image is-128x128 is-inline-block",
          img(src := imgSrc)
        )
      ),
      div(cls := "card-footer",
        a(cls := "card-footer-item", href := url,
          name)
      )
    )
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