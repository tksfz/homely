package org.tksfz.homely

import doobie._
import doobie.implicits._

import cats.effect.{ContextShift, IO}
import cats.effect.concurrent.MVar
import org.tksfz.homely.db.{DbResource, Initial}
import org.tksfz.homely.discovery.Scanner
import org.tksfz.homely.resources.Resource

import scala.concurrent.duration.Duration

sealed trait ScanStatus
case object ScanNotStarted extends ScanStatus
case class ScanInProgress(checkAfter: Duration) extends ScanStatus
case class ScanDone(resources: Seq[Resource], nmapRaw: String) extends ScanStatus

/** Facade over resource discovery and db */
class ResourceStore(xa: Transactor[IO])(implicit cs: ContextShift[IO]) {
  /**
    * The result of scans is always persisted to the database.
    * So the UI always fetches from the db.
    */
  // Only allow one scan at a time
  val mvar: MVar[IO, Seq[Resource]] = null

  val scanner = new Scanner()
  val db = new Initial()

  def getFromDb(): IO[List[(Int, DbResource)]] = {
    db.findAll.transact(xa)
  }

  def rescanAndSave() = {
    for {
      resources <- IO { scanner.findAllHttp() }
      saved <- db.mergeAll(resources).transact(xa)
    } yield {
      saved
    }
  }

  //Get in progress or most recent since start of server
  def getScanStatus(): IO[ScanStatus] = ???
}
