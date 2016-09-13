package views.formdata

case class VpnServerData(location: String, password: String) {
  override def toString: String =
    "(" + location + ")"
}
