package models

object ThemeColor {
  def default: ThemeColor = ThemeColor(0,"default", "orange.css")
}

case class ThemeColor(id: Int, name: String, filename: String)
{
}


