package controllers

import javax.inject._

import com.typesafe.config.ConfigObject
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.ws.WSClient
import play.api.mvc._
import services.{StringProcessLogger, VpnService}
import views.formdata.VpnServerData

import scala.collection.JavaConversions._
import scala.concurrent.Future

@Singleton
class Vpn @Inject()(ws: WSClient, vpnService: VpnService, configuration: play.api.Configuration, processLogger: StringProcessLogger, val messagesApi: MessagesApi) extends Controller with I18nSupport {

  def vpn = Action {
    try {
      val apiKey: String = readApiKeyFromConfig()
      val serverMap: Map[String, AnyRef] = readVpnServersFromConfig()
      Ok(views.html.vpn(apiKey, serverMap, Vpn.createVpnForm))
    } catch {
      case e: IllegalStateException => ServiceUnavailable(s"Unable to parse application configuration: ${e.getMessage}")
    }
  }

  def connect = Action { implicit request =>
    try {
      val apiKey: String = readApiKeyFromConfig()
      val serverMap: Map[String, AnyRef] = readVpnServersFromConfig()
      val form: Form[VpnServerData] = Vpn.createVpnForm.bindFromRequest()
      val hostname: String = serverMap(form.get.location).toString
      val password: String = form.get.password
      val response: String = vpnService.openVpn(hostname, password)
      Ok(views.html.vpn(apiKey, serverMap, Vpn.createVpnForm))
    } catch {
      case e: IllegalStateException => ServiceUnavailable(s"Unable to parse application configuration: ${e.getMessage}")
    }
  }

  def location = Action.async {
    ws.url("http://ip-api.com/json").get().map { response =>
      val contentType = response.header("Content-Type").getOrElse("application/json")
      Ok(response.body).as(contentType)
    }
  }

  def servers = Action {
    try {
      val serverMap: Map[String, AnyRef] = readVpnServersFromConfig()
      Ok(serverMap.toString())
    } catch {
      case e: IllegalStateException => ServiceUnavailable(s"Unable to parse application configuration: ${e.getMessage}")
    }
  }

  def userName = Action {
    val response: String = vpnService.userName("Gummiboot")
    Ok(s"username: $response")
  }

  def find = Action {
    val stream: Stream[String] = vpnService.find(processLogger)
    Ok(stream.toString() + "\n" + processLogger.lines)
  }

  def ping = Action.async {
    val futurePing: Future[Double] = scala.concurrent.Future {
      vpnService.averagePingToHost("localhost")
    }
    futurePing.map(i => Ok("Got result: " + i))
  }

  def readApiKeyFromConfig(): String = {
    configuration.getString("google.maps.api.key").getOrElse(throw new IllegalStateException("Application property [google.maps.api.key] not defined."))
  }

  def readVpnServersFromConfig(): Map[String, AnyRef] = {
    val configObject: ConfigObject = configuration.getObject("vpn.servers").getOrElse(throw new IllegalStateException("Application property [vpn.servers] not defined."))
    configObject.unwrapped().toMap
  }
}

object Vpn {
  val createVpnForm = Form(
    mapping(
      "location" -> nonEmptyText, "password" -> nonEmptyText
    )(VpnServerData.apply)(VpnServerData.unapply)
  )
}