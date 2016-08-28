package models

class VpnServer(val location: String, val hostname: String) {
  override def toString: String =
    "(" + location + ": " + hostname + ")"
}
