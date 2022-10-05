package br.com.estudos

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import br.com.estudos.calendar.CalendaCallBack
import br.com.estudos.databinding.ActivityMain2Binding
//import java.time.LocalDate
import org.joda.time.*

class MainActivity2 : AppCompatActivity() {
    private lateinit var binding: ActivityMain2Binding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMain2Binding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.exOneCalendar.setupCalendar(R.color.red_800, calendarCallBack())

    }

    private fun calendarCallBack() = object : CalendaCallBack {
        override fun onDateClicked(date: LocalDate) {
            val day = date
        }

    }

}