package br.com.estudos.calendar

import java.time.LocalDate

interface CalendaCallBack {
    fun onDateClicked(date: LocalDate)
}