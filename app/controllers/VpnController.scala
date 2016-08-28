package controllers

import java.util.Map.Entry
import javax.inject._

import com.typesafe.config.ConfigObject
import models.VpnServer
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.ws.WSClient
import play.api.mvc._
import services.VpnService

import scala.collection.JavaConversions._
import scala.concurrent.Future

@Singleton
class VpnController @Inject()(ws: WSClient, vpnService: VpnService, configuration: play.api.Configuration) extends Controller {

  def vpn = Action {
    val apiKey: Option[String] = configuration.getString("google.maps.api.key")
    val servers: Option[List[VpnServer]] = readVpnServersFromConfig()
    if (apiKey.isDefined && servers.isDefined)
      Ok(views.html.vpn(apiKey.get, servers.get))
    else if (servers.isEmpty)
      ServiceUnavailable("Unable to parse VPN server configuration.")
    else
      ServiceUnavailable("Google Maps API key not configured.")
  }

  def location = Action.async {
    ws.url("http://ip-api.com/json").get().map { response =>
      val contentType = response.header("Content-Type").getOrElse("application/json")
      Ok(response.body).as(contentType)
    }
  }

  def servers = Action {
    val vpnServers: Option[List[VpnServer]] = readVpnServersFromConfig()

    if (vpnServers.isDefined)
      Ok(vpnServers.get.toString())
    else
      ServiceUnavailable("Unable to parse VPN server configuration.")
  }

  def userName = Action {
    val response: String = vpnService.userName("Gummiboot")
    Ok(s"username: $response")
  }

  def connect = Action {
    val response: String = vpnService.openVpn("eu1.vpn.goldenfrog.com", "Gummiboot")
    Ok(s"Go Check: $response")
  }

  def ping = Action.async {
    val futurePing: Future[Double] = scala.concurrent.Future {
      vpnService.averagePingToHost("localhost")
    }
    futurePing.map(i => Ok("Got result: " + i))
  }

  def readVpnServersFromConfig(): Option[List[VpnServer]] = {
    val vpnConfigurationObjects: Option[java.util.List[_ <: ConfigObject]] = configuration.getObjectList("vpn.servers")
    if (vpnConfigurationObjects.isDefined) {
      def servers = for {
        configObject <- vpnConfigurationObjects.get
        entry: Entry[String, AnyRef] <- configObject.unwrapped().entrySet()
      } yield new VpnServer(entry.getKey, entry.getValue.asInstanceOf[String])
      Some(servers.toList)
    }
    else None
  }

}
