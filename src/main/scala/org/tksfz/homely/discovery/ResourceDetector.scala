package org.tksfz.homely.discovery

trait ResourceDetector[T] {
  def detect(result: ScanResult): Option[T]
}

// TODO: this is temporary until we have an initial set of detectors implemented
trait LowPriorityImplicits {
  implicit def none[T] = new ResourceDetector[T] {
    override def detect(result: ScanResult) = None
  }
}

object DetectorImplicits extends ProxmoxImplicits with PiholeImplicits

object ResourceDetector extends LowPriorityImplicits

object DerivedResourceDetector {
  import magnolia._

  import language.experimental.macros

  type Typeclass[T] = ResourceDetector[T]

  def combine[T](ctx: CaseClass[ResourceDetector, T]): ResourceDetector[T] = new ResourceDetector[T] {
    override def detect(result: ScanResult) = {
      None
    }
  }

  def dispatch[T](ctx: SealedTrait[ResourceDetector, T]): ResourceDetector[T] =
    new ResourceDetector[T] {
      def detect(result: ScanResult): Option[T] = {
        ctx.subtypes.toStream.map(_.typeclass.detect(result)).collectFirst { case Some(x) => x  }
      }
    }

  /** This uses the caller's implicit scope, so the caller should import AllImplicits._ */
  implicit def gen[T]: ResourceDetector[T] = macro Magnolia.gen[T]
}
