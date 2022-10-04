package br.com.estudos.calendar

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import androidx.core.view.children
import androidx.core.view.updateLayoutParams
import br.com.estudos.R
import br.com.estudos.databinding.Example1CalendarDayBinding
import br.com.estudos.calendar.model.CalendarDay
import br.com.estudos.calendar.model.CalendarMonth
import br.com.estudos.calendar.model.DayOwner
import br.com.estudos.calendar.model.InDateStyle
import br.com.estudos.calendar.ui.DayBinder
import br.com.estudos.calendar.ui.ViewContainer
import br.com.estudos.databinding.CalendarLayoutBinding
import br.com.estudos.daysOfWeekFromLocale
import br.com.estudos.setTextColorRes
import br.com.estudos.utils.CalendarUtil
import br.com.estudos.utils.next
import br.com.estudos.utils.previous
import br.com.estudos.utils.yearMonth
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle


fun Context.getLayoutInflater() =
    this.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

class FoldableCalendar(context: Context, attrs: AttributeSet) : LinearLayout(context, attrs) {

    private var binding: CalendarLayoutBinding
    private val minusMonth = 10L
    private val plusMonth = 10L
    private val selectedDay = mutableListOf<LocalDate>()
    private val today = LocalDate.now()
    private val monthTitleFormatter = DateTimeFormatter.ofPattern("MMMM")
    private var monthToWeek = false

    init {
        binding = CalendarLayoutBinding.inflate(LayoutInflater.from(context), this, true)
        attrs.let {
            val styledAttributes =
                context.obtainStyledAttributes(it, R.styleable.CalendarView, 0, 0)
            styledAttributes.recycle()
        }

    }

    fun setupCalendar(backGroundColor: Int? = null, callBack: CalendaCallBack) {
        val daysOfWeek = daysOfWeekFromLocale()

        buildLegendDays(daysOfWeek)

        val currentMonth = YearMonth.now()
        val startMonth = currentMonth.minusMonths(minusMonth)
        val endMonth = currentMonth.plusMonths(plusMonth)

        binding.exOneCalendar.setup(startMonth, endMonth, daysOfWeek.first(), currentMonth)

        binding.exOneCalendar.dayBinder = bindingDays(backGroundColor, callBack)

        binding.exOneCalendar.monthScrollListener = { monthScrollListener(it) }

        changeCalendarTypeView(endMonth)

        binding.appCompatImageButton.setOnClickListener {
            changeMonth(DayOwner.PREVIOUS_MONTH)
        }
        binding.appCompatImageButton2.setOnClickListener {
            changeMonth(DayOwner.NEXT_MONTH)
        }
    }

    private fun buildLegendDays(daysOfWeek: Array<DayOfWeek>) {

        binding.legendLayout.root.children.forEachIndexed { index, view ->
            (view as TextView).apply {
                text = daysOfWeek[index].getDisplayName(TextStyle.SHORT, CalendarUtil.getLocale())
                    .uppercase(
                        CalendarUtil.getLocale()
                    ).take(1)
                //setTextColorRes(R.color.example_1_white_light)
            }
        }
    }

    private fun bindingDays(bg: Int?, callBack: CalendaCallBack) =
        object : DayBinder<DayViewContainer> {
            override fun create(view: View) = DayViewContainer(view, callBack)
            override fun bind(container: DayViewContainer, day: CalendarDay) {
                container.day = day
                val dayOfCalendar = container.dayOfCalendar
                val textView = container.textView
                val dayTax = container.dayTax
                textView.text = day.date.dayOfMonth.toString()
//            if (hasTaxToPay) {
                dayTax.hasTaxToPay()
//            }
                when {
                    selectedDay.contains(day.date) -> {
                        textView.setTextColorRes(R.color.example_2_black)
                        backgroundDay(dayOfCalendar, bg)
                    }
                    today == day.date -> {
                        dayTax.visibility = GONE
                        bg?.let {
                            dayOfCalendar.setTint(it)
                        }
                        dayOfCalendar.setBackgroundResource(R.drawable.example_3_today_bg)
                        textView.setTextColorRes(R.color.example_1_white)
                    }
                    else -> {
                        textView.setTextColorRes(
                            if (day.owner == DayOwner.THIS_MONTH)
                                R.color.example_2_black
                            else
                                R.color.red_800
                        )
                        dayOfCalendar.background = null
                    }
                }
            }
        }

    private fun monthScrollListener(calendarMonth: CalendarMonth) {
        return if (binding.exOneCalendar.maxRowCount == 6) {
            binding.exOneYearText.text = calendarMonth.yearMonth.year.toString()
            binding.exOneMonthText.text = monthTitleFormatter.format(calendarMonth.yearMonth)
        } else {
            // In week mode, we show the header a bit differently.
            // We show indices with dates from different months since
            // dates overflow and cells in one index can belong to different
            // months/years.
            val firstDate = calendarMonth.weekDays.first().first().date
            val lastDate = calendarMonth.weekDays.last().last().date
            if (firstDate.yearMonth == lastDate.yearMonth) {
                binding.exOneYearText.text = firstDate.yearMonth.year.toString()
                binding.exOneMonthText.text = monthTitleFormatter.format(firstDate)
            } else {
                binding.exOneMonthText.text =
                    "${monthTitleFormatter.format(firstDate)} - ${firstDate.yearMonth.year}"
                binding.exOneYearText.text = ""

            }
        }
    }

    private fun changeCalendarTypeView(endMonth: YearMonth) {
        binding.weekModeCheckBox.setOnClickListener {
            monthToWeek = !monthToWeek

            binding.weekModeCheckBox.text = if (monthToWeek)
                context.getString(R.string.month_calendar)
            else
                context.getString(R.string.week_calendar)

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

    private fun changeMonth(dayOwner: DayOwner) {
        binding.exOneCalendar.findFirstVisibleMonth()?.let {

            when (dayOwner) {
                DayOwner.PREVIOUS_MONTH -> {
                    binding.exOneCalendar.smoothScrollToMonth(
                        it.yearMonth.previous
                    )
                }
                DayOwner.NEXT_MONTH -> {
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

    fun ConstraintLayout.setTint(colorRes: Int) {
        this.backgroundTintList = context.resources.getColorStateList(colorRes)
    }

    private fun backgroundDay(textView: ConstraintLayout, bg: Int?) {
        bg?.let {
            val border = GradientDrawable()

            border.cornerRadius = 10F
            border.setStroke(3, context.resources.getColorStateList(it))
            textView.background = border
        }
    }

    private fun View.hasTaxToPay() {
        this.visibility = VISIBLE
    }

    inner class DayViewContainer(view: View, callBack: CalendaCallBack) : ViewContainer(view) {
        // Will be set when this container is bound. See the dayBinder.
        lateinit var day: CalendarDay
        val dayOfCalendar: ConstraintLayout = Example1CalendarDayBinding.bind(view).dayOfCalendar
        val textView: AppCompatTextView = Example1CalendarDayBinding.bind(view).exOneDayText
        val dayTax: View = Example1CalendarDayBinding.bind(view).dayTax

        init {
            view.setOnClickListener {
                if (day.date != today) {
                    if (selectedDay.contains(day.date)) {
                        selectedDay.remove(day.date)
                    } else {
                        selectedDay.clear()
                        selectedDay.add(day.date)
                        callBack.onDateClicked(day.date)
                    }

                    binding.exOneCalendar.notifyCalendarChanged()
                    binding.exOneCalendar.notifyDayChanged(day)
                }
            }
        }
    }
}