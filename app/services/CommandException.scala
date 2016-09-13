package services

class CommandException(message: String = null, cause: Throwable = null) extends
  RuntimeException(CommandException.defaultMessage(message, cause), cause) {
}

object CommandException {
  def defaultMessage(message: String, cause: Throwable) =
    if (message != null) message
    else if (cause != null) cause.toString
    else null
}