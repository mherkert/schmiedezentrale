package controllers

import java.util.Map.Entry
import javax.inject._

import com.typesafe.config.ConfigObject
import models.VpnServer
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.ws.WSClient
import play.api.mvc._
import services.VpnService
import views.formdata.VpnServerData

import scala.collection.JavaConversions._
import scala.concurrent.Future

@Singleton
class Vpn @Inject()(ws: WSClient, vpnService: VpnService, configuration: play.api.Configuration) extends Controller {

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

  def connect = Action {
    val vpnServerData = Form(
      mapping(
        "location" -> text,
        "hostname" -> text
      )(VpnServerData.apply)(VpnServerData.unapply)
    )
    val form: Form[VpnServerData] = vpnServerData.bindFromRequest()

    Ok("Hi %s %s".format(form.get.hostname, form.get.location))
    //    val formData: Nothing = Form.form(classOf[StudentFormData]).bindFromRequest
    //    if (formData.hasErrors) {
    //      flash("error", "Please correct errors above.")
    //      return badRequest(Index.render(formData, Hobby.makeHobbyMap(null), GradeLevel.getNameList, GradePointAverage.makeGPAMap(null), Major.makeMajorMap(null)))
    //    }
    //    else {
    //      val student: Student = Student.makeInstance(formData.get)
    //      flash("success", "Student instance created/edited: " + student)
    //      return ok(Index.render(formData, Hobby.makeHobbyMap(formData.get), GradeLevel.getNameList, GradePointAverage.makeGPAMap(formData.get), Major.makeMajorMap(formData.get)))
    //    }
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

//  def connect = Action {
//    val response: String = vpnService.openVpn("eu1.vpn.goldenfrog.com", "Gummiboot")
//    Ok(s"Go Check: $response")
//  }

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
