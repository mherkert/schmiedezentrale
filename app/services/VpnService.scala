package services

import play.api.Logger

import scala.sys.process._
import scala.util.matching.Regex

class VpnService {

  private val PING_RESULT_PATTERN: Regex = "\\w \\: \\[\\d\\]\\, 84 bytes\\, [0-9\\.]+ ms \\((.*) avg\\, 0% loss\\)".r

  val commandLogger: Logger = Logger("commands")

  def averagePingToHost(hostname: String): Double = {
    commandLogger.info(s"Calculating average ping to [$hostname].")
    val result: String = ping(hostname)
    val pingResults: List[String] = PING_RESULT_PATTERN.findAllIn(result.tail).matchData.map(m => m.group(1)).toList
    pingResults.last.toDouble
  }

  def openVpn(hostname: String, password: String): String = {
    commandLogger.info(s"Open VPN connection to [$hostname].")
    sudoLogin(password,
      try {
        run(s"sudo openvpn --client --remote $hostname --dev tun --comp-lzo --auth-user-pass /home/pi/openvpn.conf --tls-client --ca /etc/openvpn/ca.vyprvpn.com.crt --management localhost 9999")
      } catch {
        case e: Exception => throw new IllegalArgumentException(s"Unable to connect to VPN. ${e.getMessage}")
      }
    )
  }

  def runUnameAsSudo(password: String): String = {
    sudoLogin(password,
      run("uname"))
  }

  def find(processLogger: ProcessLogger): Stream[String] ={
//    runLines("blah", processLogger)
    runLines("find /usr/local/Library -print", processLogger)
  }

  def userName(password: String): String = {
    sudoLogin(password,
      run("sudo id -nu")
    )
  }

  private def ping(hostname: String): String = {
    run(s"fping -C 4 $hostname")
  }

  private def sudoLogin(password: String, command: => String): String = {
    (s"echo $password" #| s"sudo -S uname").!!.trim
//    Try(run(s"echo $password" #| s"sudo -S uname")).getOrElse(throw new IllegalArgumentException("Unable to login. Check your credentials."))
    val response: String = command
    run("sudo -k")
    commandLogger.debug(response)
    response
  }

  private def run(command: ProcessBuilder): String = {
    commandLogger.debug(command.toString)
    try {
      command.!!.trim
    } catch {
      case e: Exception => throw new CommandException(message = s"Unable to process command: ${e.getMessage}", cause = e)
    }
  }

  private def run(command: String): String = {
    commandLogger.debug(command)
    try {
      s"$command".!!.trim
    } catch {
      case e: Exception => throw new CommandException(message = s"Unable to process command: ${e.getMessage}", cause = e)
    }
  }

  private def runLines(command: String, processLogger: ProcessLogger): Stream[String] = {
    commandLogger.debug(command)
    try {
      s"$command".lineStream_!(processLogger)
    } catch {
      case e: Exception => throw new CommandException(message = s"Unable to process command: ${e.getMessage}", cause = e)
    }
  }
}
