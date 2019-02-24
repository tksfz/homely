package org.tksfz.homely.discovery

import org.tksfz.homely.resources.Router

trait RouterImplicits {
  implicit val routerDetector: ResourceDetector[Router] = new ResourceDetector[Router] {
    override def detect(result: ScanResult) = {
      // TODO: detect if IP address is known gateway
      if (result.htmlTitle.contains("EdgeOS")) {
        Some(Router("EdgeRouter", false))
      } else if (result.httpHeaders.contains("TomatoUSB")) {
        Some(Router("Tomato", false))
      } else if (result.htmlTitle.contains("ASUS Router")) {
        Some(Router("ASUS Router", false))
      } else {
        None
      }
    }
  }
}
