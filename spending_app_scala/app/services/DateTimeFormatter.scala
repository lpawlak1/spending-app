package services

import java.time.LocalDateTime


object DateTimeFormatter {
  def getDateTime(date: LocalDateTime): String = {
    date.format(java.time.format.DateTimeFormatter.ofPattern(
      "dd/MM/YYYY HH:mm"
    ))
  }
  def getDateFromString(str: String): LocalDateTime = {
    LocalDateTime.parse(
      str + " 00:00",
      java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
    )
  }

}
