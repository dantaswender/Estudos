package br.com.estudos.calendar

import br.com.estudos.utils.next
import br.com.estudos.utils.previous
import com.kizitonwose.calendarview.model.DayOwner
import com.kizitonwose.calendarview.model.InDateStyle
import com.kizitonwose.calendarview.model.OutDateStyle
import kotlinx.coroutines.Job
import org.joda.time.*
import java.util.*

//import java.time.DayOfWeek
//import java.time.LocalDate
//import java.time.YearMonth
//import java.time.temporal.WeekFields

internal data class MonthConfig(
    internal val outDateStyle: OutDateStyle,
    internal val inDateStyle: InDateStyle,
    internal val maxRowCount: Int,
    internal val startMonth: YearMonth,
    internal val endMonth: YearMonth,
    internal val hasBoundaries: Boolean,
    internal val job: Job
) {

    internal val months: List<CalendarMonth> = run {
        return@run if (hasBoundaries) {
            generateBoundedMonths(startMonth, endMonth, maxRowCount, inDateStyle, outDateStyle, job)
        } else {
            generateUnboundedMonths(startMonth, endMonth, maxRowCount, inDateStyle, outDateStyle, job)
        }
    }

    internal companion object {

        private val uninterruptedJob = Job()

        fun generateBoundedMonths(
            startMonth: YearMonth,
            endMonth: YearMonth,
            maxRowCount: Int,
            inDateStyle: InDateStyle,
            outDateStyle: OutDateStyle,
            job: Job = uninterruptedJob
        ): List<CalendarMonth> {
            val months = mutableListOf<CalendarMonth>()
            var currentMonth = startMonth
            while (currentMonth <= endMonth && job.isActive) {
                val generateInDates = when (inDateStyle) {
                    InDateStyle.ALL_MONTHS -> true
                    InDateStyle.FIRST_MONTH -> currentMonth == startMonth
                    InDateStyle.NONE -> false
                }

                val weekDaysGroup =
                    generateWeekDays(currentMonth, generateInDates, outDateStyle)

                val calendarMonths = mutableListOf<CalendarMonth>()
                val numberOfSameMonth = weekDaysGroup.size roundDiv maxRowCount
                var indexInSameMonth = 0
                calendarMonths.addAll(
                    weekDaysGroup.chunked(maxRowCount) { monthDays ->
                        // Use monthDays.toList() to create a copy of the ephemeral list.
                        CalendarMonth(currentMonth, monthDays.toList(), indexInSameMonth++, numberOfSameMonth)
                    }
                )

                months.addAll(calendarMonths)
                if (currentMonth != endMonth) currentMonth = currentMonth.next else break
            }

            return months
        }

        internal fun generateUnboundedMonths(
            startMonth: YearMonth,
            endMonth: YearMonth,
            maxRowCount: Int,
            inDateStyle: InDateStyle,
            outDateStyle: OutDateStyle,
            job: Job = uninterruptedJob
        ): List<CalendarMonth> {

            val allDays = mutableListOf<CalendarDay>()
            var currentMonth = startMonth
            while (currentMonth <= endMonth && job.isActive) {

                val generateInDates = when (inDateStyle) {
                    InDateStyle.FIRST_MONTH, InDateStyle.ALL_MONTHS -> currentMonth == startMonth
                    InDateStyle.NONE -> false
                }

                allDays.addAll(
                    generateWeekDays(currentMonth, generateInDates, OutDateStyle.NONE).flatten()
                )
                if (currentMonth != endMonth) currentMonth = currentMonth.next else break
            }

            val allDaysGroup = allDays.chunked(7).toList()

            val calendarMonths = mutableListOf<CalendarMonth>()
            val calMonthsCount = allDaysGroup.size roundDiv maxRowCount
            allDaysGroup.chunked(maxRowCount) { ephemeralMonthWeeks ->
                val monthWeeks = ephemeralMonthWeeks.toMutableList()

                if (monthWeeks.last().size < 7 && outDateStyle == OutDateStyle.END_OF_ROW || outDateStyle == OutDateStyle.END_OF_GRID) {
                    val lastWeek = monthWeeks.last()
                    val lastDay = lastWeek.last()
                    val outDates = (1..7 - lastWeek.size).map {
                        CalendarDay(lastDay.date.plusDays(it), DayOwner.NEXT_MONTH)
                    }
                    monthWeeks[monthWeeks.lastIndex] = lastWeek + outDates
                }

                while (monthWeeks.size < maxRowCount && outDateStyle == OutDateStyle.END_OF_GRID ||
                    monthWeeks.size == maxRowCount && monthWeeks.last().size < 7 && outDateStyle == OutDateStyle.END_OF_GRID
                ) {
                    val lastDay = monthWeeks.last().last()

                    val nextRowDates = (1..7).map {
                        CalendarDay(lastDay.date.plusDays(it), DayOwner.NEXT_MONTH)
                    }

                    if (monthWeeks.last().size < 7) {
                        monthWeeks[monthWeeks.lastIndex] = (monthWeeks.last() + nextRowDates).take(7)
                    } else {
                        monthWeeks.add(nextRowDates)
                    }
                }

                calendarMonths.add(
                    CalendarMonth(startMonth, monthWeeks, calendarMonths.size, calMonthsCount)
                )
            }

            return calendarMonths
        }

        private fun getMaxDay(monthOfYear: Int): Int {
            val cal = Calendar.getInstance()
            cal[Calendar.MONTH] = monthOfYear - 1
            cal[Calendar.DAY_OF_MONTH] = 1
            return cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        }

        internal fun generateWeekDays(
            yearMonth: YearMonth,
            generateInDates: Boolean,
            outDateStyle: OutDateStyle
        ): List<List<CalendarDay>> {
            val year = yearMonth.year
            val month: Int = yearMonth.monthOfYear

            val days = getMaxDay(month)

            val thisMonthDays = (1..days).map {
                CalendarDay(LocalDate(year, month, it), DayOwner.THIS_MONTH)
            }

            val weekDaysGroup = if (generateInDates) {

                val groupByWeekOfMonth = thisMonthDays.groupBy {
                    it.date.get(DateTimeFieldType.weekOfWeekyear())
                }.values.toMutableList()


                val firstWeek = groupByWeekOfMonth.first()
                if (firstWeek.size < 7) {
                    val previousMonth: YearMonth = yearMonth.previous
                    val inDates = (1..getMaxDay(yearMonth.monthOfYear - 1)).toList()
                        .takeLast(7 - firstWeek.size).map {
                            CalendarDay(
                                LocalDate(previousMonth.year, previousMonth.monthOfYear, it),
                                DayOwner.PREVIOUS_MONTH
                            )
                        }
                    groupByWeekOfMonth[0] = inDates + firstWeek
                }
                groupByWeekOfMonth
            } else {
                thisMonthDays.chunked(7).toMutableList()
            }

            if (outDateStyle == OutDateStyle.END_OF_ROW || outDateStyle == OutDateStyle.END_OF_GRID) {
                if (weekDaysGroup.last().size < 7) {
                    val lastWeek = weekDaysGroup.last()
                    val lastDay = lastWeek.last()
                    val outDates = (1..7 - lastWeek.size).map {
                        CalendarDay(lastDay.date.plusDays(it), DayOwner.NEXT_MONTH)
                    }
                    weekDaysGroup[weekDaysGroup.lastIndex] = lastWeek + outDates
                }

                // Add more rows to form a 6 x 7 grid
                if (outDateStyle == OutDateStyle.END_OF_GRID) {
                    while (weekDaysGroup.size < 6) {
                        val lastDay = weekDaysGroup.last().last()
                        val nextRowDates = (1..7).map {
                            CalendarDay(lastDay.date.plusDays(it), DayOwner.NEXT_MONTH)
                        }
                        weekDaysGroup.add(nextRowDates)
                    }
                }
            }

            return weekDaysGroup
        }
    }
}

private infix fun Int.roundDiv(other: Int): Int {
    val div = this / other
    val rem = this % other
    return if (rem == 0) div else div + 1
}

