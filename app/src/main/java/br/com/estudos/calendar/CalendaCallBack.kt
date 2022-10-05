package br.com.estudos.calendar

//import java.time.LocalDate
import org.joda.time.*

interface CalendaCallBack {
    fun onDateClicked(date: LocalDate)
}