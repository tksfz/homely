package org.tksfz.homely.discovery

import org.tksfz.homely.resources.ResourceType

case class Resource(
                   uri: String,
                   resourceType: ResourceType
                   )

case class ScanResult(url: String, httpHeaders: String, htmlTitle: String)

class Scanner {

  import DetectorImplicits._

  def findAllHttp(): Seq[Resource] = {
    nmap("-F -oX - --script=http-headers --script=http-title 192.168.1.0/24").flatMap { result =>
      println(result)
      DerivedResourceDetector.gen[ResourceType].detect(result).map { resourceType =>
        Resource(result.url, resourceType)
      }
    }
  }

  def nmap(args: String): Seq[ScanResult] = {
    import scala.sys.process._
    import scala.xml._
    val xmlStr = ("nmap " + args).lineStream.mkString("")
    val xml = XML.withSAXParser(XML.parser).loadString(xmlStr)
    (for {
      host <- xml \ "host"
      port <- host \ "ports" \ "port"
    } yield {
      val address = host \ "address" \@ "addr"
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
        Some(ScanResult(url = address, httpHeaders = httpHeaders.getOrElse(""), htmlTitle = htmlTitle.getOrElse("")))
      } else {
        None
      }
    }).flatten
  }
}

object Scanner {
  def main(args: Array[String]):Unit = {
    val scanner = new Scanner
    val resources = scanner.findAllHttp()
    println(resources.mkString("\n"))
  }
}