package br.com.estudos

import android.animation.ValueAnimator
import android.os.Bundle
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import androidx.core.view.children
import androidx.core.view.updateLayoutParams
import br.com.estudos.databinding.CalendarLayoutBinding
import br.com.estudos.databinding.Example1CalendarDayBinding
import br.com.estudos.model.CalendarDay
import br.com.estudos.model.DayOwner
import br.com.estudos.model.DayOwner.*
import br.com.estudos.model.InDateStyle
import br.com.estudos.ui.DayBinder
import br.com.estudos.ui.ViewContainer
import br.com.estudos.utils.CalendarUtil.getLocale
import br.com.estudos.utils.next
import br.com.estudos.utils.previous
import br.com.estudos.utils.yearMonth
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.*


class MainActivity : AppCompatActivity() {

    private lateinit var binding: CalendarLayoutBinding

    private val selectedDates = mutableSetOf<LocalDate>()
    private val today = LocalDate.now()
    private val monthTitleFormatter = DateTimeFormatter.ofPattern("MMMM")
    private var monthToWeek = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = CalendarLayoutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupCalendar()

    }

    private fun setupCalendar() {
        val daysOfWeek = daysOfWeekFromLocale()

        buildLegendDays(daysOfWeek)

        val currentMonth = YearMonth.now()
        val startMonth = currentMonth.minusMonths(10)
        val endMonth = currentMonth.plusMonths(10)
        binding.exOneCalendar.setup(startMonth, endMonth, daysOfWeek.first(), currentMonth)
        //binding.exOneCalendar.scrollToMonth(currentMonth)

        binding.exOneCalendar.dayBinder = bindingDays()

        binding.exOneCalendar.monthScrollListener = {
            if (binding.exOneCalendar.maxRowCount == 6) {
                binding.exOneYearText.text = it.yearMonth.year.toString()
                binding.exOneMonthText.text = monthTitleFormatter.format(it.yearMonth)
            } else {
                // In week mode, we show the header a bit differently.
                // We show indices with dates from different months since
                // dates overflow and cells in one index can belong to different
                // months/years.
                val firstDate = it.weekDays.first().first().date
                val lastDate = it.weekDays.last().last().date
                if (firstDate.yearMonth == lastDate.yearMonth) {
                    binding.exOneYearText.text = firstDate.yearMonth.year.toString()
                    binding.exOneMonthText.text = monthTitleFormatter.format(firstDate)
                } else {
                    binding.exOneMonthText.text =
                        "${monthTitleFormatter.format(firstDate)} - ${firstDate.yearMonth.year.toString()}"
//                        "${monthTitleFormatter.format(firstDate)} - ${
//                            monthTitleFormatter.format(
//                                lastDate
//                            )
//                        }"
                    binding.exOneYearText.text = ""

                }
            }
        }

        changeCalendarTypeView(endMonth)

        binding.appCompatImageButton.setOnClickListener {
            changeMonth(PREVIOUS_MONTH)
        }
        binding.appCompatImageButton2.setOnClickListener {
            changeMonth(NEXT_MONTH)
        }
    }

    private fun buildLegendDays(daysOfWeek: Array<DayOfWeek>) {

        binding.legendLayout.root.children.forEachIndexed { index, view ->
            (view as TextView).apply {
                text = daysOfWeek[index].getDisplayName(TextStyle.SHORT, getLocale()).uppercase(
                    getLocale()
                ).take(1)
                //setTextColorRes(R.color.example_1_white_light)
            }
        }
    }

    private fun changeCalendarTypeView(endMonth: YearMonth) {
        binding.weekModeCheckBox.setOnClickListener {
            monthToWeek = !monthToWeek

            binding.weekModeCheckBox.text = if (monthToWeek)
                getString(R.string.month_calendar)
            else
                getString(R.string.week_calendar)

            val firstDate = binding.exOneCalendar.findFirstVisibleDay()?.date
                ?: return@setOnClickListener
            val lastDate = binding.exOneCalendar.findLastVisibleDay()?.date
                ?: return@setOnClickListener

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
                        inDateStyle = InDateStyle.FIRST_MONTH,
                        maxRowCount = 1,
                        hasBoundaries = false
                    )
                }

                if (monthToWeek) {
                    // We want the first visible day to remain
                    // visible when we change to week mode.
                    binding.exOneCalendar.scrollToDate(firstDate)
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

    inner class DayViewContainer(view: View) : ViewContainer(view) {
        // Will be set when this container is bound. See the dayBinder.
        lateinit var day: CalendarDay
        val textView = Example1CalendarDayBinding.bind(view).exOneDayText
        val dayTax = Example1CalendarDayBinding.bind(view).dayTax

        init {
            view.setOnClickListener {
                if (day.owner == THIS_MONTH) {
                    if (selectedDates.contains(day.date)) {
                        selectedDates.remove(day.date)
                    } else {
                        selectedDates.add(day.date)
                    }
                    binding.exOneCalendar.notifyDayChanged(day)
                }
            }
        }
    }

    private fun bindingDays() = object : DayBinder<DayViewContainer> {
        override fun create(view: View) = DayViewContainer(view)
        override fun bind(container: DayViewContainer, day: CalendarDay) {
            container.day = day
            val textView = container.textView
            val dayTax = container.dayTax
            textView.text = day.date.dayOfMonth.toString()
//            if (hasTaxToPay) {
//                dayTax.visibility = VISIBLE
//            }
            if (day.owner == THIS_MONTH) {
                when {
                    selectedDates.contains(day.date) -> {
                        textView.setTextColorRes(R.color.example_2_black)
                        textView.setBackgroundResource(R.drawable.example_1_selected_bg)
                    }
                    today == day.date -> {
                        dayTax.visibility = GONE
                        textView.setTextColorRes(R.color.example_1_white)
                        textView.setBackgroundResource(R.drawable.example_3_today_bg)
                    }
                    else -> {
                        textView.setTextColorRes(R.color.example_2_black)
                        textView.background = null
                    }
                }
            } else {
                textView.setTextColorRes(R.color.brown_700)
                textView.background = null
            }
        }
    }

    private fun changeMonth(dayOwner: DayOwner) {
        binding.exOneCalendar.findFirstVisibleMonth()?.let {

            when (dayOwner) {
                PREVIOUS_MONTH -> {
                    binding.exOneCalendar.smoothScrollToMonth(
                        it.yearMonth.previous
                    )
                }
                NEXT_MONTH -> {
                    binding.exOneCalendar.smoothScrollToMonth(
                        it.yearMonth.next
                    )
                }
                else -> {
                    null
                }
            }
        }
    }
}
