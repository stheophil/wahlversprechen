package helpers

import java.util.{Date, Calendar}

/**
 * Helper object to create [[Date]] instances via a nice syntax in test cases.
 *
 * = Example =
 *
 * {{{
 *  // 14th march 2014
 *  val myDate: Date = 2014 \ 03 \ 14
 * }}}
 */

object DateFactory {
  case class Year(year: Int) {
    def \(month: Int) = YearMonth(year, month)
  }

  case class YearMonth(year: Int, month: Int) {
    def \(day: Int) = YearMonthDay(year, month, day)
  }

  case class YearMonthDay(year: Int, month: Int, day: Int)

  implicit def int2Year(year: Int): Year = Year(year)

  implicit def yearMonthDay2Date(ymd: YearMonthDay): Date = {
    val calendar = Calendar.getInstance()

    calendar.set(Calendar.YEAR, ymd.year)
    calendar.set(Calendar.MONTH, ymd.month)
    calendar.set(Calendar.DAY_OF_MONTH, ymd.day)

    calendar.getTime
  }
}