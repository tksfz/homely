package org.tksfz.homely.discovery

import org.tksfz.homely.resources.Proxmox

trait ProxmoxImplicits {
  implicit val proxmoxDetector: ResourceDetector[Proxmox.type] = new ResourceDetector[Proxmox.type] {
    override def detect(result: ScanResult) = {
      if (result.htmlTitle.contains("proxmox")) Some(Proxmox) else None
    }
  }
}
