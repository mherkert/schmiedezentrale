package services

import scala.sys.process.ProcessLogger

class StringProcessLogger extends ProcessLogger {
  private val messages = new StringBuilder
  def lines = messages.toString

  def buffer[T](f: => T): T = {
    messages.clear
    f
  }
  def err(s: => String) { messages.append(s+"\n"); () }
  def out(s: => String) { messages.append(s+"\n"); () }
}
