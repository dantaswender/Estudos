package br.com.estudos.utils

import org.joda.time.*
import java.util.*

object CalendarUtil {

    private val brLocale = Locale("pt", "BR")
    private val localDate = LocalDate.now()

    fun getLocale() = brLocale

    fun daysOfWeek(locale: Locale): List<String> {

        val daysOfWeek = (0..6).map {
            localDate.dayOfWeek().withMinimumValue().plusDays(it).dayOfWeek().getAsShortText(locale)
                .replace(".", "")
        }

        return daysOfWeek
    }

    fun getNameOfMonth(calendarMonth: YearMonth) =
        calendarMonth.monthOfYear().getAsText(getLocale()).replaceFirstChar {
            if (it.isLowerCase())
                it.titlecase(getLocale())
            else
                it.toString()
        }

    fun getShortNameOfMonth(calendarMonth: YearMonth) =
        calendarMonth.monthOfYear().getAsShortText(getLocale())
}
