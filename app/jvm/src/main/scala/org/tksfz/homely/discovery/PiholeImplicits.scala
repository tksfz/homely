package org.tksfz.homely.discovery

import org.tksfz.homely.resources.Pihole

trait PiholeImplicits {
  implicit val piholeDetector: ResourceDetector[Pihole.type] = new ResourceDetector[Pihole.type] {
    override def detect(result: ScanResult) = {
      if (result.htmlTitle.contains("pihole")) Some(Pihole) else None
    }
  }
}
