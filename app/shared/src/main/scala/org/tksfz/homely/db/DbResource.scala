package org.tksfz.homely.db

import org.tksfz.homely.resources.ResourceType

case class DbResource(resourceType: ResourceType, uri: String, order: Option[Int], customName: Option[String],
                      customImage: Option[Array[Byte]])

