package vinyldns.core

import cats.effect.IO
import org.slf4j.Logger

trait MonitoredAlgebra {

  def monitor[T](name: String)(f: => IO[T]): IO[T]

  def time[T](id: String)(f: => IO[T])(implicit logger: Logger): IO[T]

}
