package services

import play.api.Logger

import scala.sys.process._
import scala.util.Try
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
      run(s"sudo openvpn --client --remote $hostname --dev tun --comp-lzo --auth-user-pass /home/pi/openvpn.conf --tls-client --ca /etc/openvpn/ca.vyprvpn.com.crt --management localhost 9999"))
  }

  def runUnameAsSudo(password: String): String = {
    sudoLogin(password,
      run("uname"))
  }

  def userName(password: String): String = {
    sudoLogin(password,
      run("sudo id -nu")
    )
  }

  private def ping(hostname: String): String = {
    Try(run(s"fping -C 4 $hostname")).getOrElse(throw new IllegalArgumentException(s"Unable to ping host $hostname."))
  }

  private def sudoLogin(password: String, command: => String): String = {
    (s"echo $password" #| s"sudo -S uname").!!.trim
    val response: String = command
    run("sudo -k")
    commandLogger.debug(response)
    response
  }

  private def run(command: ProcessBuilder): String = {
    commandLogger.debug(command.toString)
    command.!!.trim
  }

  private def run(command: String): String = {
    commandLogger.debug(command)
    s"$command".!!.trim
  }
}
