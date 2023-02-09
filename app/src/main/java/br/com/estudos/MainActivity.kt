package br.com.estudos

import android.animation.ValueAnimator
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import androidx.core.view.children
import androidx.core.view.updateLayoutParams
import br.com.estudos.calendar.*
import br.com.estudos.databinding.ActivityMainBinding
import br.com.estudos.utils.CalendarUtil
import br.com.estudos.utils.CalendarUtil.daysOfWeek
import br.com.estudos.utils.next
import br.com.estudos.utils.setTextColorRes
import br.com.estudos.utils.yearMonth
import com.kizitonwose.calendarview.model.DayOwner
import com.kizitonwose.calendarview.model.InDateStyle
import org.joda.time.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val selectedDates = mutableSetOf<LocalDate>()
    private val today = LocalDate.now()
    private lateinit var oldCalendarDay: CalendarDay

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)

        setContentView(binding.root)

        selectedDates.add(today)

        val daysOfWeek = daysOfWeek(CalendarUtil.getLocale())

        binding.legendLayout.root.children.forEachIndexed { index, view ->
            (view as TextView).apply {
                text = daysOfWeek[index]
            }
        }

        val currentMonth = YearMonth.now()
        val startMonth = currentMonth.minusMonths(1452)
        val endMonth = currentMonth.plusMonths(1452)

        binding.exOneCalendar.setup(startMonth, endMonth, currentMonth)
        binding.exOneCalendar.scrollToMonth(currentMonth)

        binding.exOneCalendar.dayBinder = object : DayBinder<DayViewContainer> {
            override fun create(view: View) = DayViewContainer(view, object : CalendarCallBack {
                override fun selectDay(calendarDay: CalendarDay) {

                    if (this@MainActivity::oldCalendarDay.isInitialized) {
                        binding.exOneCalendar.notifyDayChanged(this@MainActivity.oldCalendarDay)
                        selectedDates.remove(this@MainActivity.oldCalendarDay.date)
                    } else {
                        binding.exOneCalendar.notifyDayChanged(
                            CalendarDay(
                                today,
                                DayOwner.THIS_MONTH
                            )
                        )
                        selectedDates.remove(today)
                    }

                    selectedDates.add(calendarDay.date)

                    binding.exOneCalendar.notifyDayChanged(calendarDay)
                    this@MainActivity.oldCalendarDay = calendarDay
                }

            })

            override fun bind(container: DayViewContainer, day: CalendarDay) {
                container.day = day
                val textView = container.textView
                textView.text = day.date.dayOfMonth.toString()
                if (day.owner == DayOwner.THIS_MONTH) {
                    when {
                        selectedDates.contains(day.date) -> {
                            textView.setTextColorRes(R.color.example_1_bg)
                            textView.setBackgroundResource(R.drawable.example_1_selected_bg)
                        }
                        today == day.date -> {
                            textView.setTextColorRes(R.color.example_1_white)
                            textView.setBackgroundResource(R.drawable.example_1_today_bg)
                        }
                        else -> {
                            textView.setTextColorRes(R.color.example_1_white)
                            textView.background = null
                        }
                    }
                } else {
                    textView.setTextColorRes(R.color.example_1_white_light)
                    textView.background = null
                }
            }
        }

        binding.exOneCalendar.monthScrollListener = {
            if (binding.exOneCalendar.maxRowCount == 6) {
                binding.exOneYearText.text = it.yearMonth.year.toString()
                binding.exOneMonthText.text = it.yearMonth.toString()
            } else {
                // In week mode, we show the header a bit differently.
                // We show indices with dates from different months since
                // dates overflow and cells in one index can belong to different
                // months/years.
                val firstDate = it.weekDays.first().first().date
                val lastDate = it.weekDays.last().last().date
                if (firstDate.yearMonth == lastDate.yearMonth) {
                    binding.exOneYearText.text = firstDate.yearMonth.year.toString()
                    binding.exOneMonthText.text = firstDate.toString()
                } else {
                    binding.exOneMonthText.text =
                        "${firstDate} - ${lastDate}"
                    if (firstDate.year == lastDate.year) {
                        binding.exOneYearText.text = firstDate.yearMonth.year.toString()
                    } else {
                        binding.exOneYearText.text =
                            "${firstDate.yearMonth.year} - ${lastDate.yearMonth.year}"
                    }
                }
            }
        }

        binding.weekModeCheckBox.setOnCheckedChangeListener { _, monthToWeek ->
            val firstDate = binding.exOneCalendar.findFirstVisibleDay()?.date
                ?: return@setOnCheckedChangeListener
            val lastDate = binding.exOneCalendar.findLastVisibleDay()?.date
                ?: return@setOnCheckedChangeListener

            val oneWeekHeight = binding.exOneCalendar.daySize.height
            val oneMonthHeight = oneWeekHeight * 6

            val oldHeight = if (monthToWeek) oneMonthHeight else oneWeekHeight
            val newHeight = if (monthToWeek) oneWeekHeight else oneMonthHeight

            // Animate calendar height changes.
            val animator = ValueAnimator.ofInt(oldHeight, newHeight)
            animator.addUpdateListener { animator ->
                binding.exOneCalendar.updateLayoutParams {
                    height = animator.animatedValue as Int
                }
            }

            // When changing from month to week mode, we change the calendar's
            // config at the end of the animation(doOnEnd) but when changing
            // from week to month mode, we change the calendar's config at
            // the start of the animation(doOnStart). This is so that the change
            // in height is visible. You can do this whichever way you prefer.

            animator.doOnStart {
                if (!monthToWeek) {
                    binding.exOneCalendar.updateMonthConfiguration(
                        inDateStyle = InDateStyle.ALL_MONTHS,
                        maxRowCount = 6,
                        hasBoundaries = true
                    )
                }
            }
            animator.doOnEnd {
                if (monthToWeek) {
                    binding.exOneCalendar.updateMonthConfiguration(
                        inDateStyle = InDateStyle.ALL_MONTHS,
                        maxRowCount = 1,
                        hasBoundaries = true
                    )
                }

                if (monthToWeek) {
                    // We want the first visible day to remain
                    // visible when we change to week mode.
                    binding.exOneCalendar.scrollToDate(
                        if (selectedDates.isNullOrEmpty())
                            today
                        else
                            selectedDates.first()
                    )
                } else {
                    // When changing to month mode, we choose current
                    // month if it is the only one in the current frame.
                    // if we have multiple months in one frame, we prefer
                    // the second one unless it's an outDate in the last index.
                    if (firstDate.yearMonth == lastDate.yearMonth) {
                        binding.exOneCalendar.scrollToMonth(firstDate.yearMonth)
                    } else {
                        // We compare the next with the last month on the calendar so we don't go over.
                        binding.exOneCalendar.scrollToMonth(
                            minOf(
                                firstDate.yearMonth.next,
                                endMonth
                            )
                        )
                    }
                }
            }
            animator.duration = 250
            animator.start()
        }
    }
}