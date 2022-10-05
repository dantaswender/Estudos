package br.com.estudos

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import br.com.estudos.calendar.CalendarAdapter
import br.com.estudos.calendar.OnItemListener
import org.joda.time.LocalDate
import org.joda.time.YearMonth
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity() {

    private var monthYearText: TextView? = null
    private var calendarRecyclerView: RecyclerView? = null
    private var selectedDate: LocalDate? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initWidgets()
        selectedDate = LocalDate.now()
        setMonthView()
    }

    private fun initWidgets() {
        calendarRecyclerView = findViewById(R.id.calendarRecyclerView)
        monthYearText = findViewById(R.id.monthYearTV)
    }

    private fun onListener() = object : OnItemListener {
        override fun onItemClick(position: Int, dayText: String?) {
            if (dayText != "") {
                val message = "Selected Date " + dayText + " " + monthYearFromDate(selectedDate)
                Log.d ("this", message)
            }
        }

    }

    private fun setMonthView() {
        monthYearText!!.text = monthYearFromDate(selectedDate)
        val daysInMonth = daysInMonthArray(selectedDate)
        val calendarAdapter = CalendarAdapter(daysInMonth, onListener())
        val layoutManager: RecyclerView.LayoutManager = GridLayoutManager(applicationContext, 7)
        calendarRecyclerView!!.layoutManager = layoutManager
        calendarRecyclerView!!.adapter = calendarAdapter
    }

    private fun daysInMonthArray(date: LocalDate?): ArrayList<String> {
        val daysInMonthArray = ArrayList<String>()
        val yearMonth: YearMonth = YearMonth.fromDateFields(date!!.toDate())

//        int daysInMonth = yearMonth.size();
        val daysInMonth = getAllDay(yearMonth.getMonthOfYear()).size
        val firstOfMonth: LocalDate = selectedDate!!.withDayOfMonth(1)
        val dayOfWeek: Int = firstOfMonth.getDayOfWeek()
        for (i in 1..42) {
            if (i <= dayOfWeek || i > daysInMonth + dayOfWeek) {
                daysInMonthArray.add("")
            } else {
                daysInMonthArray.add((i - dayOfWeek).toString())
            }
        }
        return daysInMonthArray
    }

    private fun getAllDay(monthOfYear: Int): ArrayList<String> {
        val days = ArrayList<String>()
        val cal = Calendar.getInstance()
        cal[Calendar.MONTH] = monthOfYear - 1
        cal[Calendar.DAY_OF_MONTH] = 1
        val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val df = SimpleDateFormat("d")
        //        System.out.print(df.format(cal.getTime()));
        for (i in 0 until maxDay) {
            cal[Calendar.DAY_OF_MONTH] = i + 1
            days.add(df.format(cal.time))
            //            System.out.print(", " + df.format(cal.getTime()));
            Log.d("days", df.format(cal.time))
        }
        return days
    }

    private fun monthYearFromDate(date: LocalDate?): String {
//        DateTimeFormatter formatter = new DateTimeFormatter().parseDateTime();// ofPattern("MMMM yyyy");
        val formatter = SimpleDateFormat("MMMM yyyy")
        return formatter.format(date!!.toDate()) // format(formatter);
    }

    fun previousMonthAction(view: View?) {
        selectedDate = selectedDate!!.minusMonths(1)
        setMonthView()
    }

    fun nextMonthAction(view: View?) {
        selectedDate = selectedDate!!.plusMonths(1)
        setMonthView()
    }

    fun onItemClick(position: Int, dayText: String) {
        if (dayText != "") {
            val message = "Selected Date " + dayText + " " + monthYearFromDate(selectedDate)
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        }
    }
}