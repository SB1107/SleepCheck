package kr.co.sbsolutions.newsoomirang.presenter.main.history

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.google.android.material.card.MaterialCardView
import kr.co.sbsolutions.newsoomirang.R
import kr.co.sbsolutions.newsoomirang.common.Cons.TAG
import kr.co.sbsolutions.newsoomirang.common.toDate
import kr.co.sbsolutions.newsoomirang.common.toDayString
import kr.co.sbsolutions.newsoomirang.common.toDp2Px
import kr.co.sbsolutions.newsoomirang.common.toHourMinute
import kr.co.sbsolutions.newsoomirang.data.entity.SleepDetailResult
import kr.co.sbsolutions.newsoomirang.databinding.RowHistoryItemSleepBinding
import kr.co.sbsolutions.newsoomirang.databinding.RowHistoryItemSnoreBinding
import kr.co.sbsolutions.newsoomirang.databinding.RowHistoryNoDataItemBinding
import java.util.concurrent.TimeUnit

class HistoryAdapter : ListAdapter<SleepDetailResult, RecyclerView.ViewHolder>(object : DiffUtil.ItemCallback<SleepDetailResult>() {
    override fun areItemsTheSame(oldItem: SleepDetailResult, newItem: SleepDetailResult): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: SleepDetailResult, newItem: SleepDetailResult): Boolean {
        return oldItem == newItem
    }

}) {
    lateinit var context: Context
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        context = parent.context
        val inflater = LayoutInflater.from(context)
        return when (viewType) {
            0 -> {
                ItemSleepViewHolder(RowHistoryItemSleepBinding.inflate(inflater, parent, false))
            }
            1 -> {
                ItemSnoreViewHolder(RowHistoryItemSnoreBinding.inflate(inflater, parent, false))
            }
            else -> {
                ItemNotDataViewHolder(RowHistoryNoDataItemBinding.inflate(inflater, parent, false))
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        getItem(position).let {
            when (it.type) {
                0 -> {
                    (holder as ItemSleepViewHolder).bind(it)
                }
                1 -> {
                    (holder as ItemSnoreViewHolder).bind(it)
                }
                else -> {
                    (holder as ItemNotDataViewHolder)
                }
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return getItem(position).type
    }

    inner class ItemNotDataViewHolder(
        private val binding: RowHistoryNoDataItemBinding
    ) : RecyclerView.ViewHolder(binding.root) {}

    inner class ItemSleepViewHolder(
        private val binding: RowHistoryItemSleepBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        @SuppressLint("UseCompatLoadingForDrawables", "SetTextI18n")
        fun bind(result: SleepDetailResult) {
            val startedAt = result.startedAt?.toDate("yyyy-MM-dd HH:mm:ss")
            val endedAt = result.endedAt?.toDate("yyyy-MM-dd HH:mm:ss")
            val endedAtString = endedAt?.toDayString("M월 d일 E요일") ?: ""
            val durationString: String = (startedAt?.toDayString("HH:mm") + "~" + endedAt?.toDayString("HH:mm"))
            binding.resultDateTextView.text = endedAtString
            binding.resultDateTextView.setCompoundDrawablesRelativeWithIntrinsicBounds(context.getDrawable(R.drawable.bottom_menu_1_off), null, null, null)
            binding.resultDurationTextView.text = "$durationString 수면"
            binding.actionType.root.visibility = View.GONE
            binding.clLayout.setOnClickListener {
                binding.actionType.root.visibility = if (binding.actionType.root.visibility == View.VISIBLE) View.GONE else View.VISIBLE
            }

            val milliseconds: Long = (endedAt?.time ?: 0) - (startedAt?.time ?: 0)
            val min = (TimeUnit.MILLISECONDS.toMinutes(milliseconds).toInt() * 60).toHourMinute()
            val sleepTime = (result.sleepTime * 60).toHourMinute()
            val resultAsleep = (result.asleepTime * 60).toHourMinute()
            binding.actionType.resultTotalTextView.text = min
            binding.actionType.resultRealTextView.text = sleepTime
            binding.actionType.resultAsleepTextView.text = resultAsleep
            //
            // apnea_state = 3 : 나쁨, 2 : 중간, 1: 좋음, 0: 측정불가
            binding.actionType.indicatorLayout.layoutParams = when (result.apneaState) {
                3 -> {
                    RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT)
                        .apply {
                            setMargins(0, 40, 0, 0)
                            addRule(RelativeLayout.ALIGN_PARENT_RIGHT)
                        }
                }

                2 -> {
                    RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT)
                        .apply {
                            setMargins(0, 40, 0, 0)
                            addRule(RelativeLayout.CENTER_HORIZONTAL)
                        }
                }

                else -> {
                    RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT)
                        .apply {
                            setMargins(0, 40, 0, 0)
                            addRule(RelativeLayout.ALIGN_PARENT_LEFT)
                        }
                }
            }
            binding.actionType.resultTotalApenaTextView.text = "${result.apneaCount} 회"
            binding.actionType.resultApena10TextView.text = "${result.apnea10} 회"
            binding.actionType.resultApena30TextView.text = "${result.apnea30} 회"
            binding.actionType.resultApena60TextView.text = "${result.apnea60} 회"

            binding.actionType.pose1TextView.text = (result.straightPosition * 60).toHourMinute()
            percentLayout((result.straightPosition.toDouble() / (result.sleepTime).toDouble()), binding.actionType.pose1PercentTextView, binding.actionType.pose1ProgressView)

            binding.actionType.pose2TextView.text = (result.leftPosition * 60).toHourMinute()
            percentLayout(result.leftPosition.toDouble() / (result.sleepTime).toDouble(), binding.actionType.pose2PercentTextView, binding.actionType.pose2ProgressView)

            binding.actionType.pose3TextView.text = (result.rightPosition * 60).toHourMinute()
            percentLayout(result.rightPosition.toDouble() / (result.sleepTime).toDouble(), binding.actionType.pose3PercentTextView, binding.actionType.pose3ProgressView)

            binding.actionType.pose4TextView.text = (result.downPosition * 60).toHourMinute()
            percentLayout(result.downPosition.toDouble() / (result.sleepTime).toDouble(), binding.actionType.pose4PercentTextView, binding.actionType.pose4ProgressView)

            binding.actionType.pose5TextView.text = (result.wakeTime * 60).toHourMinute()
            percentLayout(result.wakeTime.toDouble() / (result.sleepTime).toDouble(), binding.actionType.pose5PercentTextView, binding.actionType.pose5ProgressView)
            initChart(binding.actionType.chart, arrayListOf())
            result.sleepPattern?.let {
                it.split("")
                    .filter { it.trim() != "" }
                    .forEach { value ->
                        addEntry(binding.actionType.chart, value.toDouble())
                    }
            }


        }
    }

    @SuppressLint("SetTextI18n")
    private fun percentLayout(percent: Double, textView: AppCompatTextView, cardView: MaterialCardView) {
        textView.text = String.format("%.1f", percent * 100) + "%"
        cardView.layoutParams = (cardView.layoutParams as RelativeLayout.LayoutParams).apply {
            width = context.toDp2Px((percent * 2 * 100).toFloat()).toInt()
        }

    }

    inner class ItemSnoreViewHolder(
        private val binding: RowHistoryItemSnoreBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("UseCompatLoadingForDrawables", "SetTextI18n")
        fun bind(result: SleepDetailResult) {
            val startedAt = result.startedAt?.toDate("yyyy-MM-dd HH:mm:ss")
            val endedAt = result.endedAt?.toDate("yyyy-MM-dd HH:mm:ss")
            val endedAtString = endedAt?.toDayString("M월 d일 E요일") ?: ""
            val durationString: String = (startedAt?.toDayString("HH:mm") + "~" + endedAt?.toDayString("HH:mm"))
            binding.resultDateTextView.text = endedAtString
            binding.resultDateTextView.setCompoundDrawablesRelativeWithIntrinsicBounds(context.getDrawable(R.drawable.bottom_menu_2_off), null, null, null)
            binding.resultDurationTextView.text = "$durationString 수면"
            binding.actionType.root.visibility = View.GONE
            binding.clLayout.setOnClickListener {
                binding.actionType.root.visibility = if (binding.actionType.root.visibility == View.VISIBLE) View.GONE else View.VISIBLE
            }

            val milliseconds: Long = (endedAt?.time ?: 0) - (startedAt?.time ?: 0)
            val min = (TimeUnit.MILLISECONDS.toMinutes(milliseconds).toInt() * 60).toHourMinute()
            val sleepTime = (result.sleepTime * 60).toHourMinute()
            val resultAsleep = (result.asleepTime * 60).toHourMinute()
            binding.actionType.resultTotalTextView.text = min
            binding.actionType.resultRealTextView.text = sleepTime
            binding.actionType.resultAsleepTextView.text = resultAsleep
            binding.actionType.snoreTimeTextView.text = (result.snoreTime * 60).toHourMinute()

            binding.actionType.pose1TextView.text = (result.straightPosition * 60).toHourMinute()
            percentLayout(result.straightPosition.toDouble() / (result.sleepTime).toDouble(), binding.actionType.pose1PercentTextView, binding.actionType.pose1ProgressView)

            binding.actionType.pose2TextView.text = (result.leftPosition * 60).toHourMinute()
            percentLayout(result.leftPosition.toDouble() / (result.sleepTime).toDouble(), binding.actionType.pose2PercentTextView, binding.actionType.pose2ProgressView)

            binding.actionType.pose3TextView.text = (result.rightPosition * 60).toHourMinute()
            percentLayout(result.rightPosition.toDouble() / (result.sleepTime).toDouble(), binding.actionType.pose3PercentTextView, binding.actionType.pose3ProgressView)

            binding.actionType.pose4TextView.text = (result.downPosition * 60).toHourMinute()
            percentLayout(result.downPosition.toDouble() / (result.sleepTime).toDouble(), binding.actionType.pose4PercentTextView, binding.actionType.pose4ProgressView)

            binding.actionType.pose5TextView.text = (result.wakeTime * 60).toHourMinute()
            percentLayout(result.wakeTime.toDouble() / (result.sleepTime).toDouble(), binding.actionType.pose5PercentTextView, binding.actionType.pose5ProgressView)
            initChart(binding.actionType.chart, arrayListOf())
            result.sleepPattern?.let {
                it.split("")
                    .filter { it.trim() != "" }
                    .forEach { value ->
                        addEntry(binding.actionType.chart, value.toDouble())
                    }
            }
        }
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun initChart(lineChart: LineChart, values: ArrayList<Entry>) {


        // 데이터 갯수 7 개
        lineChart.background = context.getDrawable(R.color.clear) // 배경색
        lineChart.description.isEnabled = false // 설명 제거
        lineChart.setTouchEnabled(true) // 터치 제거
        lineChart.setPinchZoom(false)
        lineChart.isDragEnabled = false
        lineChart.isDragXEnabled = true
        lineChart.isDragYEnabled = false
        lineChart.setScaleEnabled(false)
        lineChart.isDoubleTapToZoomEnabled = false
        lineChart.setDrawGridBackground(false) // 배경 그리드 제거
        lineChart.isHighlightPerDragEnabled = false
        lineChart.isHighlightPerTapEnabled = false
        lineChart.setNoDataText("차트 데이터가 없습니다.") // 차트 데이터가 없을 때 문구
        lineChart.setNoDataTextColor(context.getColor(R.color.color_FFFFFF))
        lineChart.isScrollContainer = true
        lineChart.setExtraOffsets(0f, 0f, 0f, 0f)
        lineChart.setViewPortOffsets(0f, 0f, 0f, 0f)
        lineChart.isAutoScaleMinMaxEnabled = true

        // 차트 데이터 설명
        val legend = lineChart.legend
        legend.isEnabled = false

        // XAxis
        val xAxis = lineChart.xAxis
        xAxis.setDrawGridLines(true)
        xAxis.setDrawAxisLine(true)
        xAxis.setDrawLabels(false)
        xAxis.isGranularityEnabled = false
        xAxis.granularity = 1f
        xAxis.setAvoidFirstLastClipping(false)
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.textSize = 12f
        xAxis.textColor = context.getColor(R.color.clear)
        xAxis.gridColor = context.getColor(R.color.clear)
        xAxis.axisLineColor = context.getColor(R.color.clear)


        // Y Axis - Left
        val leftAxis = lineChart.axisLeft
        leftAxis.isEnabled = false
        leftAxis.setDrawLabels(false)
        leftAxis.axisLineWidth = 0f
        leftAxis.axisLineColor = context.getColor(R.color.clear)
        leftAxis.gridColor = context.getColor(R.color.clear)

        // Y Axis - Right
        val rightAxis = lineChart.axisRight
        rightAxis.isEnabled = false
        //    setChartData();
        val lineDataSet = LineDataSet(values, "")
        lineDataSet.setDrawIcons(false) // 아이콘 표시
        lineDataSet.setDrawCircles(false)
        lineDataSet.color = context.getColor(R.color.color_FFFFFF)
        lineDataSet.lineWidth = 2f
        lineDataSet.valueTextColor = context.getColor(R.color.clear)
        lineDataSet.mode = LineDataSet.Mode.LINEAR
        val data = LineData(lineDataSet)
        data.isHighlightEnabled = false
        lineChart.data = data
    }

    private fun addEntry(lineChart: LineChart, num: Double) {
        var data = lineChart.data
        if (data == null) {
            data = LineData()
            lineChart.data = data
        }
        var set = data.getDataSetByIndex(0)
        if (set == null) {
            set = createSet()
            data.addDataSet(set)
        }
        data.addEntry(Entry(set.entryCount.toFloat(), num.toFloat()), 0)
        data.notifyDataChanged()
        lineChart.notifyDataSetChanged()
        lineChart.setVisibleXRangeMaximum(300f)
        lineChart.moveViewTo(data.entryCount.toFloat(), 50f, YAxis.AxisDependency.LEFT)
    }

    private fun createSet(): LineDataSet {
        val lineDataSet = LineDataSet(null, "Real-time Line Data")
        lineDataSet.setDrawIcons(false) // 아이콘 표시
        lineDataSet.setDrawCircles(false)
        lineDataSet.color = context.getColor(R.color.colorAccent)
        lineDataSet.lineWidth = 2f
        lineDataSet.valueTextColor = context.getColor(R.color.clear)
        lineDataSet.mode = LineDataSet.Mode.CUBIC_BEZIER
        return lineDataSet
    }
}



