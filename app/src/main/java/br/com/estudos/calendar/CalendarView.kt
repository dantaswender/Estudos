package br.com.estudos.calendar

import android.content.Context
import android.util.AttributeSet
import android.view.View.MeasureSpec.UNSPECIFIED
import androidx.annotation.Px
import androidx.core.content.withStyledAttributes
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import br.com.estudos.R
import br.com.estudos.utils.job
import com.kizitonwose.calendarview.model.DayOwner
import com.kizitonwose.calendarview.model.InDateStyle
import com.kizitonwose.calendarview.model.OutDateStyle
import com.kizitonwose.calendarview.model.ScrollMode
import com.kizitonwose.calendarview.ui.CalenderPageSnapHelper
import br.com.estudos.utils.Size
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.Main
import org.joda.time.YearMonth
import org.joda.time.*

typealias Completion = () -> Unit
typealias Cancellation = () -> Unit

open class CalendarView : RecyclerView {

    var dayBinder: DayBinder<*>? = null
        set(value) {
            field = value
            invalidateViewHolders()
        }

    var monthHeaderBinder: MonthHeaderFooterBinder<*>? = null
        set(value) {
            field = value
            invalidateViewHolders()
        }

    var monthFooterBinder: MonthHeaderFooterBinder<*>? = null
        set(value) {
            field = value
            invalidateViewHolders()
        }

    var monthScrollListener: MonthScrollListener? = null

    var dayViewResource = 0
        set(value) {
            if (field != value) {
                if (value == 0) throw IllegalArgumentException("'dayViewResource' attribute not provided.")
                field = value
                updateAdapterViewConfig()
            }
        }

    var monthHeaderResource = 0
        set(value) {
            if (field != value) {
                field = value
                updateAdapterViewConfig()
            }
        }

    var monthFooterResource = 0
        set(value) {
            if (field != value) {
                field = value
                updateAdapterViewConfig()
            }
        }

    var monthViewClass: String? = null
        set(value) {
            if (field != value) {
                field = value
                updateAdapterViewConfig()
            }
        }

    @Orientation
    var orientation = VERTICAL
        set(value) {
            if (field != value) {
                field = value
                (layoutManager as? CalendarLayoutManager)?.orientation = value
            }
        }

    var scrollMode = ScrollMode.CONTINUOUS
        set(value) {
            if (field != value) {
                field = value
                pagerSnapHelper.attachToRecyclerView(if (value == ScrollMode.PAGED) this else null)
            }
        }

    var inDateStyle = InDateStyle.ALL_MONTHS
        set(value) {
            if (field != value) {
                field = value
                updateAdapterMonthConfig()
            }
        }

    var outDateStyle = OutDateStyle.END_OF_ROW
        set(value) {
            if (field != value) {
                field = value
                updateAdapterMonthConfig()
            }
        }

    var maxRowCount = 6
        set(value) {
            if (!(1..6).contains(value)) throw IllegalArgumentException("'maxRowCount' should be between 1 to 6")
            if (field != value) {
                field = value
                updateAdapterMonthConfig()
            }
        }

    var hasBoundaries = true
        set(value) {
            if (field != value) {
                field = value
                updateAdapterMonthConfig()
            }
        }

    var wrappedPageHeightAnimationDuration = 200

    private val pagerSnapHelper = CalenderPageSnapHelper()

    private var startMonth: YearMonth? = null
    private var endMonth: YearMonth? = null

    private var autoSize = true
    private var autoSizeHeight = SQUARE
    private var sizedInternally = false

    internal val isVertical: Boolean
        get() = orientation == VERTICAL

    internal val isHorizontal: Boolean
        get() = !isVertical

    private var configJob: Job? = null
    private var internalConfigUpdate = false

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(attrs, 0, 0)
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) :
            super(context, attrs, defStyleAttr) {
        init(attrs, defStyleAttr, defStyleAttr)
    }

    private fun init(attributeSet: AttributeSet, defStyleAttr: Int, defStyleRes: Int) {
        if (isInEditMode) return
        setHasFixedSize(true)
        context.withStyledAttributes(
            attributeSet,
            R.styleable.CalendarView,
            defStyleAttr,
            defStyleRes
        ) {
            dayViewResource =
                getResourceId(R.styleable.CalendarView_cv_dayViewResource, dayViewResource)
            monthHeaderResource =
                getResourceId(R.styleable.CalendarView_cv_monthHeaderResource, monthHeaderResource)
            monthFooterResource =
                getResourceId(R.styleable.CalendarView_cv_monthFooterResource, monthFooterResource)
            orientation = getInt(R.styleable.CalendarView_cv_orientation, orientation)
            scrollMode = ScrollMode.values()[
                    getInt(R.styleable.CalendarView_cv_scrollMode, scrollMode.ordinal)
            ]
            outDateStyle = OutDateStyle.values()[
                    getInt(R.styleable.CalendarView_cv_outDateStyle, outDateStyle.ordinal)
            ]
            inDateStyle = InDateStyle.values()[
                    getInt(R.styleable.CalendarView_cv_inDateStyle, inDateStyle.ordinal)
            ]
            maxRowCount = getInt(R.styleable.CalendarView_cv_maxRowCount, maxRowCount)
            monthViewClass = getString(R.styleable.CalendarView_cv_monthViewClass)
            hasBoundaries = getBoolean(R.styleable.CalendarView_cv_hasBoundaries, hasBoundaries)
            wrappedPageHeightAnimationDuration = getInt(
                R.styleable.CalendarView_cv_wrappedPageHeightAnimationDuration,
                wrappedPageHeightAnimationDuration
            )
        }
        check(dayViewResource != 0) { "No value set for `cv_dayViewResource` attribute." }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (autoSize && !isInEditMode) {
            val widthMode = MeasureSpec.getMode(widthMeasureSpec)
            val widthSize = MeasureSpec.getSize(widthMeasureSpec)
            val heightMode = MeasureSpec.getMode(heightMeasureSpec)

            if (widthMode == UNSPECIFIED && heightMode == UNSPECIFIED) {
                throw UnsupportedOperationException("Cannot calculate the values for day Width/Height with the current configuration.")
            }

            // +0.5 => round to the nearest pixel
            val size = (((widthSize - (monthPaddingStart + monthPaddingEnd)) / 7f) + 0.5).toInt()

            val height = if (autoSizeHeight == SQUARE) size else autoSizeHeight
            val computedSize = daySize.copy(width = size, height = height)
            if (daySize != computedSize) {
                sizedInternally = true
                daySize = computedSize
                sizedInternally = false
                invalidateViewHolders()
            }
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    var daySize: Size = SIZE_SQUARE
        set(value) {
            field = value
            if (!sizedInternally) {
                autoSize = value == SIZE_SQUARE || value.width == SQUARE
                autoSizeHeight = value.height
                invalidateViewHolders()
            }
        }

    @Px
    var monthPaddingStart = 0
        private set

    @Px
    var monthPaddingEnd = 0
        private set

    @Px
    var monthPaddingTop = 0
        private set

    @Px
    var monthPaddingBottom = 0
        private set

    @Px
    var monthMarginStart = 0
        private set

    @Px
    var monthMarginEnd = 0
        private set

    @Px
    var monthMarginTop = 0
        private set

    @Px
    var monthMarginBottom = 0
        private set

    private val calendarLayoutManager: CalendarLayoutManager
        get() = layoutManager as CalendarLayoutManager

    private val calendarAdapter: CalendarAdapter
        get() = adapter as CalendarAdapter

    private fun updateAdapterViewConfig() {
        if (adapter != null) {
            calendarAdapter.viewConfig = ViewConfig(
                dayViewResource,
                monthHeaderResource,
                monthFooterResource,
                monthViewClass
            )
            invalidateViewHolders()
        }
    }

    private fun invalidateViewHolders() {
        if (internalConfigUpdate) return

        if (adapter == null || layoutManager == null) return
        val state = layoutManager?.onSaveInstanceState()
        adapter = adapter
        layoutManager?.onRestoreInstanceState(state)
        post { calendarAdapter.notifyMonthScrollListenerIfNeeded() }
    }

    @Suppress("NotifyDataSetChanged")
    private fun updateAdapterMonthConfig(config: MonthConfig? = null) {
        if (internalConfigUpdate) return
        if (adapter != null) {
            calendarAdapter.monthConfig = config ?: MonthConfig(
                outDateStyle,
                inDateStyle,
                maxRowCount,
                startMonth ?: return,
                endMonth ?: return,
                hasBoundaries, Job()
            )
            calendarAdapter.notifyDataSetChanged()
            post { calendarAdapter.notifyMonthScrollListenerIfNeeded() }
        }
    }

    fun setMonthPadding(
        @Px start: Int = monthPaddingStart,
        @Px top: Int = monthPaddingTop,
        @Px end: Int = monthPaddingEnd,
        @Px bottom: Int = monthPaddingBottom
    ) {
        monthPaddingStart = start
        monthPaddingTop = top
        monthPaddingEnd = end
        monthPaddingBottom = bottom
        invalidateViewHolders()
    }

    fun setMonthMargins(
        @Px start: Int = monthMarginStart,
        @Px top: Int = monthMarginTop,
        @Px end: Int = monthMarginEnd,
        @Px bottom: Int = monthMarginBottom
    ) {
        monthMarginStart = start
        monthMarginTop = top
        monthMarginEnd = end
        monthMarginBottom = bottom
        invalidateViewHolders()
    }

    fun scrollToMonth(month: YearMonth) {
        calendarLayoutManager.scrollToMonth(month)
    }

    fun smoothScrollToMonth(month: YearMonth) {
        calendarLayoutManager.smoothScrollToMonth(month)
    }

    fun scrollToDay(day: CalendarDay) {
        calendarLayoutManager.scrollToDay(day)
    }

    @JvmOverloads
    fun scrollToDate(date: LocalDate, owner: DayOwner = DayOwner.THIS_MONTH) {
        scrollToDay(CalendarDay(date, owner))
    }

    fun smoothScrollToDay(day: CalendarDay) {
        calendarLayoutManager.smoothScrollToDay(day)
    }

    @JvmOverloads
    fun smoothScrollToDate(date: LocalDate, owner: DayOwner = DayOwner.THIS_MONTH) {
        smoothScrollToDay(CalendarDay(date, owner))
    }

    fun notifyDayChanged(day: CalendarDay) {
        calendarAdapter.reloadDay(day)
    }

    @JvmOverloads
    fun notifyDateChanged(date: LocalDate, owner: DayOwner = DayOwner.THIS_MONTH) {
        notifyDayChanged(CalendarDay(date, owner))
    }

    fun notifyMonthChanged(month: YearMonth) {
        calendarAdapter.reloadMonth(month)
    }

    fun notifyCalendarChanged() {
        calendarAdapter.reloadCalendar()
    }

    fun findFirstVisibleMonth(): CalendarMonth? {
        return calendarAdapter.findFirstVisibleMonth()
    }

    fun findLastVisibleMonth(): CalendarMonth? {
        return calendarAdapter.findLastVisibleMonth()
    }

    fun findFirstVisibleDay(): CalendarDay? {
        return calendarAdapter.findFirstVisibleDay()
    }

    fun findLastVisibleDay(): CalendarDay? {
        return calendarAdapter.findLastVisibleDay()
    }

    private val scrollListenerInternal = object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {}
        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            if (newState == SCROLL_STATE_IDLE) {
                calendarAdapter.notifyMonthScrollListenerIfNeeded()
            }
        }
    }

    fun updateMonthConfiguration(
        inDateStyle: InDateStyle = this.inDateStyle,
        outDateStyle: OutDateStyle = this.outDateStyle,
        maxRowCount: Int = this.maxRowCount,
        hasBoundaries: Boolean = this.hasBoundaries
    ) {
        configJob?.cancel()
        internalConfigUpdate = true
        this.inDateStyle = inDateStyle
        this.outDateStyle = outDateStyle
        this.maxRowCount = maxRowCount
        this.hasBoundaries = hasBoundaries
        internalConfigUpdate = false
        updateAdapterMonthConfig()
    }

    fun updateMonthConfigurationAsync(
        inDateStyle: InDateStyle = this.inDateStyle,
        outDateStyle: OutDateStyle = this.outDateStyle,
        maxRowCount: Int = this.maxRowCount,
        hasBoundaries: Boolean = this.hasBoundaries,
        completion: Completion? = null
    ): Cancellation {
        configJob?.cancel()
        internalConfigUpdate = true
        this.inDateStyle = inDateStyle
        this.outDateStyle = outDateStyle
        this.maxRowCount = maxRowCount
        this.hasBoundaries = hasBoundaries
        internalConfigUpdate = false
        @OptIn(DelicateCoroutinesApi::class)
        val configJob = GlobalScope.launch {
            val monthConfig = generateMonthConfig(job)
            withContext(Main) {
                updateAdapterMonthConfig(monthConfig)
                completion?.invoke()
            }
        }
        this.configJob = configJob
        return { configJob.cancel() }
    }

    fun setup(
        startMonth: YearMonth,
        endMonth: YearMonth,
        currentMonth: YearMonth
    ) {
        configJob?.cancel()
        this.startMonth = startMonth
        this.endMonth = endMonth
        finishSetup(generateMonthConfig(Job()))
        scrollToMonth(currentMonth)
    }

    @JvmOverloads
    fun setupAsync(
        startMonth: YearMonth,
        endMonth: YearMonth,
        completion: Completion? = null
    ): Cancellation {
        configJob?.cancel()
        this.startMonth = startMonth
        this.endMonth = endMonth
        @OptIn(DelicateCoroutinesApi::class)
        val configJob = GlobalScope.launch {
            val monthConfig = generateMonthConfig(job)
            withContext(Main) {
                finishSetup(monthConfig)
                completion?.invoke()
            }
        }
        this.configJob = configJob
        return { configJob.cancel() }
    }

    private fun finishSetup(monthConfig: MonthConfig) {
        removeOnScrollListener(scrollListenerInternal)
        addOnScrollListener(scrollListenerInternal)

        layoutManager = CalendarLayoutManager(this, orientation)
        adapter = CalendarAdapter(
            this,
            ViewConfig(dayViewResource, monthHeaderResource, monthFooterResource, monthViewClass),
            monthConfig
        )
    }

    @JvmOverloads
    fun updateMonthRange(
        startMonth: YearMonth = requireStartMonth(),
        endMonth: YearMonth = requireEndMonth()
    ) {
        configJob?.cancel()
        this.startMonth = startMonth
        this.endMonth = endMonth
        val (config, diff) = getMonthUpdateData(Job())
        finishUpdateMonthRange(config, diff)
    }

    @JvmOverloads
    fun updateMonthRangeAsync(
        startMonth: YearMonth = requireStartMonth(),
        endMonth: YearMonth = requireEndMonth(),
        completion: Completion? = null
    ): Cancellation {
        configJob?.cancel()
        this.startMonth = startMonth
        this.endMonth = endMonth
        @OptIn(DelicateCoroutinesApi::class)
        val configJob = GlobalScope.launch {
            val (config, diff) = getMonthUpdateData(job)
            withContext(Main) {
                finishUpdateMonthRange(config, diff)
                completion?.invoke()
            }
        }
        this.configJob = configJob
        return { configJob.cancel() }
    }

    private fun finishUpdateMonthRange(newConfig: MonthConfig, diffResult: DiffUtil.DiffResult) {
        calendarAdapter.monthConfig = newConfig
        diffResult.dispatchUpdatesTo(calendarAdapter)
    }

    private fun getMonthUpdateData(job: Job): Pair<MonthConfig, DiffUtil.DiffResult> {
        val monthConfig = generateMonthConfig(job)
        val diffResult = DiffUtil.calculateDiff(
            MonthRangeDiffCallback(calendarAdapter.monthConfig.months, monthConfig.months),
            false
        )
        return Pair(monthConfig, diffResult)
    }

    private fun generateMonthConfig(job: Job): MonthConfig {
        return MonthConfig(
            outDateStyle,
            inDateStyle,
            maxRowCount,
            requireStartMonth(),
            requireEndMonth(),
            hasBoundaries,
            job
        )
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        configJob?.cancel()
    }

    private class MonthRangeDiffCallback(
        private val oldItems: List<CalendarMonth>,
        private val newItems: List<CalendarMonth>
    ) : DiffUtil.Callback() {

        override fun getOldListSize() = oldItems.size

        override fun getNewListSize() = newItems.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int) =
            oldItems[oldItemPosition] == newItems[newItemPosition]

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int) =
            areItemsTheSame(oldItemPosition, newItemPosition)
    }

    private fun requireStartMonth(): YearMonth = startMonth ?: throw getFieldException("startMonth")

    private fun requireEndMonth(): YearMonth = endMonth ?: throw getFieldException("endMonth")

    private fun getFieldException(field: String) =
        IllegalStateException("`$field` is not set. Have you called `setup()`?")

    companion object {
        private const val SQUARE = Int.MIN_VALUE

        val SIZE_SQUARE = Size(SQUARE, SQUARE)

        fun sizeAutoWidth(@Px height: Int) = Size(SQUARE, height)
    }
}

