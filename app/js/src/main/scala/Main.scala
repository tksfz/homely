import cats.effect.IO
import outwatch.dom._
import outwatch.dom.dsl._
import monix.execution.Scheduler.Implicits.global
import monix.reactive.Observable
import org.scalajs.dom.{Blob, XMLHttpRequest}
import org.scalajs.dom.ext.{Ajax, AjaxException}
import outwatch.http.Http.{BodyType, Request, Response}
import io.circe.generic.auto._
import io.circe.parser.decode
import monix.execution.Ack
import org.tksfz.homely.resources.Resource
import outwatch.ProHandler

import scala.concurrent.Future
import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport
import scala.scalajs.js.typedarray.ArrayBuffer

@js.native
@JSImport("@shopify/draggable/lib/sortable", "default")
class Sortable(conatainers: js.Any, options: js.Any) extends js.Any

object Main {

  def appContainer(content: Observable[VNode]): IO[VNode] = {
    val burgerMenuToggleIO = toggleHandler(false)
      .map(_.mapObservable(if (_) cls := "is-active" else cls := ""))
    burgerMenuToggleIO.map { burgerMenuActive =>
      div(
        htmlTag("nav")(cls := "navbar has-shadow", role := "navigation", attr("aria-label") := "main navigation",
          div(cls := "navbar-brand",
            a(cls := "navbar-item", href := "https://bulma.io",
              span(cls := "icon", i(cls := "fas fa-home fa-2x")),
              span(h1(cls := "title is-4", styleAttr := "padding-left: 4px; margin-left: 4px", "Homely"))
            ),
            a(role := "button", cls := "navbar-burger burger", attr("aria-label") := "menu", attr("aria-expanded") := "false",
              data.target := "navmenu", burgerMenuActive: Observable[Attr], onClick() --> burgerMenuActive,
              span(attr("aria-hidden") := "true"),
              span(attr("aria-hidden") := "true"),
              span(attr("aria-hidden") := "true"),
            )
          ),
          div(id := "navmenu", cls := "navbar-menu", burgerMenuActive: Observable[Attr],
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
  }

  def main(args: Array[String]): Unit = {
    val resources = request(Observable(Request("http://localhost:8000/resources")))
      .map(_.response.toString)
      .map(decode[Seq[Resource]])
      .map(_.getOrElse(Nil))

    val dragger = Sink.create[Unit] { _ =>
      import org.scalajs.dom.document
      Future.successful {
        new Sortable(document.querySelectorAll(".columns"), js.Dynamic.literal(draggable = ".column"))
        Ack.Continue
      }
    }

    val html = resources.map { resources =>
      div(onSnabbdomInsert() --> dragger, cls := "columns is-multiline", resources.map { resource =>
        div(cls := "column is-one-fifth",
          appCard(s"resource-images/${resource.resourceType.icon}",
            resource.uri,
            resource.resourceType.displayLabel
          ),
        )
      })
    }

    //OutWatch.renderReplace("#app", myComponent).unsafeRunSync()
    (for {
      content <- Main.appContainer(html)
      rendered <- OutWatch.renderInto("#app", content)
    } yield {
      rendered
    }).unsafeRunSync()
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

  private def toggleHandler(seed: Boolean): IO[ProHandler[Unit, Boolean]] = {
    Handler.create[Unit]
      .map(_.transformObservable(_.scan(false) { (f, _) => !f }))
  }
}