package views.formdata

case class VpnServerData(location: String) {
  override def toString: String =
    "(" + location + ")"
}
