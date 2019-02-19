package org.tksfz.homely.discovery

import org.tksfz.homely.resources.Freenas

trait FreenasImplicits {
  implicit val freenasDetector: ResourceDetector[Freenas.type] = new ResourceDetector[Freenas.type] {
    override def detect(result: ScanResult) = {
      if (result.htmlTitle.contains("FreeNAS")) Some(Freenas) else None
    }
  }
}
