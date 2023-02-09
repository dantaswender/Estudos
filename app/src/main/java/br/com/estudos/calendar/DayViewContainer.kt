package br.com.estudos.calendar

import android.view.View
import br.com.estudos.databinding.Example1CalendarDayBinding
import com.kizitonwose.calendarview.model.DayOwner

interface CalendarCallBack{
    fun selectDay(date: CalendarDay)
}

class DayViewContainer(view: View, callBack: CalendarCallBack) : ViewContainer(view) {
    // Will be set when this container is bound. See the dayBinder.
    lateinit var day: CalendarDay
    val textView = Example1CalendarDayBinding.bind(view).exOneDayText

    init {
        view.setOnClickListener {
            if (day.owner == DayOwner.THIS_MONTH) {
                callBack.selectDay(day)
            }
        }
    }
}