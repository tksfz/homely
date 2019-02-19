package org.tksfz.homely.resources

sealed trait ResourceType {
  def displayLabel: String = this.getClass.getSimpleName
  def icon: String = this.getClass.getSimpleName
}

// Server apps
case object Proxmox extends ResourceType
case object Freenas extends ResourceType
case object Pihole extends ResourceType

// Desktop apps
case object Plex extends ResourceType
case object Transmission extends ResourceType
case object Bitwarden extends ResourceType

// IOT
case object Dropcam extends ResourceType

// Devices
case class Router(brand: String, isGateway: Boolean) extends ResourceType
case class Printer(brand: String) extends ResourceType

// Non-HTTP services
case object Ssh extends ResourceType
case object Wireguard extends ResourceType

sealed trait ResourceCategory
case object ServerApp extends ResourceCategory
case object App extends ResourceCategory
case object Iot extends ResourceCategory
case object NetworkDevice extends ResourceCategory
case object OtherDevice extends ResourceCategory
case object NonHttpService extends ResourceCategory