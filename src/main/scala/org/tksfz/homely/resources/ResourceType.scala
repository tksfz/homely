package org.tksfz.homely.resources

sealed trait ResourceType {
  def displayLabel: String = this.getClass.getSimpleName
  def icon: String = this.getClass.getSimpleName
  val category: ResourceCategory
}

// Server apps
sealed trait ServerAppResourceType extends ResourceType { val category = ServerApp }
case object Proxmox extends ServerAppResourceType
case object Freenas extends ServerAppResourceType
case object Pihole extends ServerAppResourceType

// Desktop apps
sealed trait AppResourceType extends ResourceType { val category = App }
case object Plex extends AppResourceType
case object Transmission extends AppResourceType
case object Bitwarden extends AppResourceType

// IOT
sealed trait IotResourceType extends ResourceType { val category = Iot }
case object Dropcam extends IotResourceType

// Devices
case class Router(brand: String, isGateway: Boolean) extends ResourceType { val category = NetworkDevice }
case class Printer(brand: String) extends ResourceType { val category = OtherDevice }

// Non-HTTP services
sealed trait NonHttpResourceType extends ResourceType { val category = NonHttpService }
case object Ssh extends NonHttpResourceType
case object Wireguard extends NonHttpResourceType

sealed trait ResourceCategory
case object ServerApp extends ResourceCategory
case object App extends ResourceCategory
case object Iot extends ResourceCategory
case object NetworkDevice extends ResourceCategory
case object OtherDevice extends ResourceCategory
case object NonHttpService extends ResourceCategory