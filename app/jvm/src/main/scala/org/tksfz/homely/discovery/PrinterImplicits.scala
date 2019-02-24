package org.tksfz.homely.discovery

import org.tksfz.homely.resources.Printer

trait PrinterImplicits {
  implicit val printerDetector: ResourceDetector[Printer] = new ResourceDetector[Printer] {
    override def detect(result: ScanResult) = {
      if (result.httpHeaders.contains("HP OfficeJet")) Some(Printer("HP")) else None
    }
  }
}
