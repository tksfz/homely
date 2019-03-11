package org.tksfz.homely.discovery

import org.tksfz.homely.resources.{Resource, ResourceType}

case class ScanResult(url: String, httpHeaders: String, htmlTitle: String)

class Scanner {

  import DetectorImplicits._

  def findAllHttp(): Seq[Resource] = {
    nmap("-p http* -sT -oX - --script=http-headers --script=http-title 192.168.1.0/24").flatMap { result =>
      DerivedResourceDetector.gen[ResourceType].detect(result).map { resourceType =>
        Resource(result.url, resourceType)
      }
    }
  }

  def nmap(args: String): Seq[ScanResult] = {
    import scala.sys.process._
    import scala.xml._
    val stream = ("nmap " + args).lineStream
    val xmlStr = stream.mkString("")
    val xml = XML.withSAXParser(XML.parser).loadString(xmlStr)
    (for {
      host <- xml \ "host"
      port <- host \ "ports" \ "port"
    } yield {
      val hostname = host \ "hostnames" \ "hostname" \@ "name"
      val scriptOutputs =
        (for {
          script <- port \ "script"
        } yield {
          val id = script \@ "id"
          val output = script \@ "output"
          id -> output
        }).toMap
      val httpHeaders = scriptOutputs.get("http-headers")
      val htmlTitle = scriptOutputs.get("http-title")
      if (httpHeaders.isDefined || htmlTitle.isDefined) {
        val url = mkUrl(host = host \ "address" \@ "addr", port = (port \@ "portid").toInt, protocolName = port \ "service" \@ "name")
        Some(ScanResult(url = url, httpHeaders = httpHeaders.getOrElse(""), htmlTitle = htmlTitle.getOrElse("")))
      } else {
        None
      }
    }).flatten
  }

  private[this] def mkUrl(host: String, port: Int, protocolName: String) = {
    val scheme = protocolName.split('-').head // "http-alt" => "http"
    val portSuffix = (protocolName, port) match {
      case ("http", 80) | ("https", 443) => ""
      case _ => s":$port"
    }
    s"${scheme}://$host$portSuffix"
  }
}

object Scanner {
  def main(args: Array[String]):Unit = {
    val scanner = new Scanner
    val resources = scanner.findAllHttp()
    println(resources.mkString("\n"))
  }
}