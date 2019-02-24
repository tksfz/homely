package org.tksfz.homely.discovery

import org.tksfz.homely.resources.Bitwarden

trait BitwardenImplicits {
  implicit val bitwardenDetector: ResourceDetector[Bitwarden.type] = new ResourceDetector[Bitwarden.type] {
    override def detect(result: ScanResult) = {
      if (result.htmlTitle.contains("Bitwarden")) Some(Bitwarden) else None
    }
  }
}
