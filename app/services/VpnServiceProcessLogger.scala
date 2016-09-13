package services

import scala.sys.process.ProcessLogger

class VpnServiceProcessLogger extends ProcessLogger{
  override def out(s: =>String): Unit = ???

  override def buffer[T](f: =>T): T = ???

  override def err(s: => String): Unit = ???
}
