package org.tksfz.homely.db

import cats.effect.{ContextShift, IO}
import doobie.Transactor
import org.tksfz.homely.resources.{Resource, ResourceType}
import doobie._
import doobie.implicits._

trait ResourceTypeImplicits {

  import io.circe.parser.decode, io.circe.syntax._, io.circe.generic.auto._

  def rtFromStr(resourceStr: String) = {
    decode[ResourceType](resourceStr).getOrElse(???)
  }

  def rtToStr(resourceType: ResourceType) = {
    resourceType.asJson.toString
  }

  implicit val rtmeta: Meta[ResourceType] = Meta[String].imap(rtFromStr)(rtToStr)

  def fromScan(resource: Resource) = {
    DbResource(resource.resourceType, resource.uri, None, None, None)
  }
}

class Initial(implicit cs: ContextShift[IO]) extends ResourceTypeImplicits {
  import scala.concurrent.ExecutionContext

  // A transactor that gets connections from java.sql.DriverManager and excutes blocking operations
  // on an unbounded pool of daemon threads. See the chapter on connection handling for more info.
  val xa = Transactor.fromDriverManager[IO](
    "org.sqlite.JDBC", // driver classname
    "jdbc:sqlite:homely.db", // connect URL (driver-specific)
  )

  val program2 = sql"select 42".query[Int].unique
  val io2 = program2.transact(xa)

  println(io2.unsafeRunSync())

  val create =
    sql"""
      CREATE TABLE IF NOT EXISTS resource(
        id INTEGER PRIMARY KEY,
        resource_type TEXT NOT NULL,
        uri TEXT NOT NULL,
        sort_order INTEGER,
        custom_name TEXT,
        custom_image BLOB);""".update.run

  println(create.transact(xa).unsafeRunSync())

  def findAll: ConnectionIO[Map[Int, DbResource]] = {
    sql"select * from resource"
      .query[(Int, String, String, Option[Int], Option[String], Option[Array[Byte]])]
      .map { case (id, resourceStr, uri, sortOrder, customName, customImage) =>
        import io.circe.parser.decode, io.circe.syntax._, io.circe.generic.auto._
        val resourceType = decode[ResourceType](resourceStr).getOrElse(???)
        id -> DbResource(resourceType, uri, sortOrder, customName, customImage)
      }
      .to[List]
      .map(_.toMap)
  }

  def mergeAll(resources: Seq[Resource]): ConnectionIO[Int] = {
    for {
      existingResources <- findAll
      existingResourcesUriAndType = existingResources.values.map(r => (r.resourceType, r.uri)).toSet
      newResources = resources.filterNot { r =>
        val key = (r.resourceType, r.uri)
        existingResourcesUriAndType.contains(key)
      }
      newDbResources = newResources.map(fromScan)
      insert <- insertAll(newDbResources)
    } yield {
      insert
    }
  }

  def insertAll(resources: Seq[DbResource]) = {
    import cats.implicits._
    val sql = """insert into resource (resource_type, uri, sort_order, custom_name, custom_image)
           |values (?, ?, ?, ?, ?)
       """.stripMargin
    Update[DbResource](sql).updateMany(resources.toList)
  }

}
