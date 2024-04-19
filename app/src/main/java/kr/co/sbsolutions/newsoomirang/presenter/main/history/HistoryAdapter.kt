package kr.co.sbsolutions.newsoomirang.presenter.main.history

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintLayout
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
import kr.co.sbsolutions.newsoomirang.common.setOnSingleClickListener
import kr.co.sbsolutions.newsoomirang.common.toDate
import kr.co.sbsolutions.newsoomirang.common.toDayString
import kr.co.sbsolutions.newsoomirang.common.toDp2Px
import kr.co.sbsolutions.newsoomirang.common.toHourMinute
import kr.co.sbsolutions.newsoomirang.data.entity.SleepDateResult
import kr.co.sbsolutions.newsoomirang.data.entity.SleepDetailResult
import kr.co.sbsolutions.newsoomirang.databinding.RowHistoryItemSleepBinding
import kr.co.sbsolutions.newsoomirang.databinding.RowHistoryItemYearBinding
import kr.co.sbsolutions.newsoomirang.databinding.RowHistoryNoDataItemBinding
import java.util.concurrent.TimeUnit

class HistoryAdapter(private  val clickItem : (String , String) -> Unit) : ListAdapter<SleepDateResult, RecyclerView.ViewHolder>(object :
    DiffUtil.ItemCallback<SleepDateResult>() {
    override fun areItemsTheSame(oldItem: SleepDateResult, newItem: SleepDateResult): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(
        oldItem: SleepDateResult,
        newItem: SleepDateResult
    ): Boolean {
        return oldItem == newItem
    }

}) {
    lateinit var context: Context
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        context = parent.context
        val inflater = LayoutInflater.from(context)
        return when (viewType) {
            0, 1 -> {
                ItemSleepViewHolder(RowHistoryItemYearBinding.inflate(inflater, parent, false))
            }

            else -> {
                ItemNotDataViewHolder(RowHistoryNoDataItemBinding.inflate(inflater, parent, false))
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        getItem(position).let {
            when (it.type) {
                0, 1 -> {
                    (holder as ItemSleepViewHolder).apply {
                        bind(it , clickItem)
                    }
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
        binding: RowHistoryNoDataItemBinding
    ) : RecyclerView.ViewHolder(binding.root) {}

    inner class ItemSleepViewHolder(
        private val binding: RowHistoryItemYearBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("UseCompatLoadingForDrawables", "SetTextI18n")
        fun bind(result: SleepDateResult, clickItem: (String , String) -> Unit) {

//            Log.d(TAG, "type: ${result.type}, 시작: ${result.startedAt} $result")
//            if (bindingAdapterPosition == 0) {
//                binding.clLayout.performClick()
//            }


            //list View
            result.endedAt?.let { it ->
                val endedAt = it.toDate("yyyy-MM-dd HH:mm:ss")
                binding.resultDateTextView.text = endedAt?.toDayString("M월 d일 E요일")

                result.startedAt?.let { itStartedAt ->
                    val startedAt = itStartedAt.toDate("yyyy-MM-dd HH:mm:ss")
                    val durationString =
                        (startedAt?.toDayString("HH:mm") + "~" + (endedAt?.toDayString("HH:mm")))

                    val milliseconds: Long = (endedAt?.time ?: 0) - (startedAt?.time ?: 0)
                    val min =
                        (TimeUnit.MILLISECONDS.toMinutes(milliseconds).toInt() * 60).toHourMinute()

//                    //총 수면 시간
//                    setResultUi(
//                        binding.actionType.actionNewResult.resultTotalTextView,
//                        binding.actionType.actionNewResult.resultTotalTitleTextView,
//                        min
//                    )

                    val durationType = if(result.type == 0) durationString.plus(" 수면") else durationString.plus(" 코골이")

                    binding.resultDurationTextView.text = durationType


                    binding.clLayout.setOnSingleClickListener {
                        clickItem.invoke(result.id , durationType)
                    }
                } ?: run {
                    binding.resultDurationTextView.visibility = View.GONE
                }

            } ?: run {
                binding.resultDateTextView.visibility = View.GONE
                binding.resultDurationTextView.visibility = View.GONE
            }

            //type
            if (result.type == 1) {
                binding.resultDateTextView.setCompoundDrawablesWithIntrinsicBounds(
                    context.getDrawable(
                        R.drawable.bottom_menu_2_off
                    ), null, null, null
                )
//                binding.actionType.actionSleepingGraph.root.visibility = View.GONE
//                binding.actionType.actionApneaResult.root.visibility = View.GONE

            } else if (result.type == 0) {
                binding.resultDateTextView.setCompoundDrawablesRelativeWithIntrinsicBounds(context.getDrawable(R.drawable.bottom_menu_1_off), null, null, null)
            }

//
//            //잠들 때까지 걸린 시간
//            setResultUi(binding.actionType.actionNewResult.resultAsleepTextView, binding.actionType.actionNewResult.resultAsleepTitleTextView, (result.asleepTime?.times(60))?.toHourMinute())
//
//            //코골이 시간
//            setResultUi(binding.actionType.actionNewResult.resultSnoreTimeTextView, binding.actionType.actionNewResult.snoreTimeTitleTextView, (result.snoreTime?.times(60))?.toHourMinute())
//
//            //깊은잠 시간
//            setResultUi(binding.actionType.actionNewResult.resultDeepSleepTextView, binding.actionType.actionNewResult.resultDeepSleepTitleTextView, (result.deepSleepTime?.times(60))?.toHourMinute())
//
//            //뒤척임 횟수
//            setResultUi(binding.actionType.actionNewResult.resultSleepMoveTextView, binding.actionType.actionNewResult.resultSleepMoveTitleTextView, result.moveCount, " 회")
//
////            Log.d(TAG, "bind1: ${result.apneaCount}")
//
//            result.apneaCount?.let { apneaCount ->
//                val params2 = binding.actionType.actionSleepingGraph.vLeft.layoutParams as ConstraintLayout.LayoutParams
//                params2.horizontalBias = apneaCount * 0.01f
//
//
//                binding.actionType.actionSleepingGraph.tvTotalApnea.text = apneaCount.toString()
//                binding.actionType.actionSleepingGraph.vLeft.layoutParams = params2
//                binding.actionType.actionApneaResult.resultTotalApneaTextView.text = "$apneaCount 회"
//
//            } ?: run {
//                binding.actionType.actionSleepingGraph.root.visibility = View.GONE
//                binding.actionType.actionApneaResult.resultTotalApneaTextView.visibility = View.GONE
//                binding.actionType.actionApneaResult.resultTotalApneaTitleTextView.visibility = View.GONE
//            }

//
//            //무호흡 횟수
//            setResultUi(
//                binding.actionType.actionApneaResult.resultApnea10TextView,
//                binding.actionType.actionApneaResult.resultApnea10TitleTextView,
//                result.apnea10,
//                " 회"
//            )
//
//            setResultUi(
//                binding.actionType.actionApneaResult.resultApnea30TextView,
//                binding.actionType.actionApneaResult.resultApnea30TitleTextView,
//                result.apnea30,
//                " 회"
//            )
//
//            setResultUi(
//                binding.actionType.actionApneaResult.resultApnea60TextView,
//                binding.actionType.actionApneaResult.resultApnea60TitleTextView,
//                result.apnea60,
//                " 회"
//            )
//
//
//            val positionViews = listOf(
//                Triple(
//                    binding.actionType.actionSleepPosition.pose1PercentTextView,
//                    binding.actionType.actionSleepPosition.pose1TextView,
//                    binding.actionType.actionSleepPosition.pose1ProgressView
//                ),
//                Triple(
//                    binding.actionType.actionSleepPosition.pose2PercentTextView,
//                    binding.actionType.actionSleepPosition.pose2TextView,
//                    binding.actionType.actionSleepPosition.pose2ProgressView
//                ),
//                Triple(
//                    binding.actionType.actionSleepPosition.pose3PercentTextView,
//                    binding.actionType.actionSleepPosition.pose3TextView,
//                    binding.actionType.actionSleepPosition.pose3ProgressView
//                ),
//                Triple(
//                    binding.actionType.actionSleepPosition.pose4PercentTextView,
//                    binding.actionType.actionSleepPosition.pose4TextView,
//                    binding.actionType.actionSleepPosition.pose4ProgressView
//                ),
//                Triple(
//                    binding.actionType.actionSleepPosition.pose5PercentTextView,
//                    binding.actionType.actionSleepPosition.pose5TextView,
//                    binding.actionType.actionSleepPosition.pose5ProgressView
//                )
//            )
//            val positionVale = listOf(
//                Pair(
//                    result.straightPositionTime,
//                    result.straightPer
//                ),
//                Pair(
//                    result.leftPositionTime,
//                    result.leftPer
//                ),
//                Pair(
//                    result.rightPositionTime,
//                    result.rightPer
//                ),
//                Pair(
//                    result.downPositionTime,
//                    result.downPer
//                ),
//                Pair(
//                    result.wakeTime,
//                    result.wakePer
//                )
//            )
//
//            binding.actionType.actionSleepPosition.root.visibility = if (!isCheckSumVis
//                    (
//                    totalTime = result.sleepTime,
//                    timeList = positionVale,
//                    views = positionViews
//                )
//            ) View.GONE else View.VISIBLE
//
//
//            //수면 패턴
//            initChart(binding.actionType.actionSleepPatten.chart, arrayListOf())
//            result.sleepPattern?.let {
//                it.split("")
//                    .filter { it.trim() != "" }
//                    .forEach { value ->
//                        addEntry(binding.actionType.actionSleepPatten.chart, value.toDouble())
//                    }
//            } ?: run { binding.actionType.actionSleepPatten.root.visibility = View.GONE }


        }
    }

    @SuppressLint("SetTextI18n")
    private fun percentLayout(
        percent: Double,
        textView: AppCompatTextView,
        cardView: MaterialCardView
    ) {
        textView.text = String.format("%.1f", percent * 100) + "%"
        cardView.layoutParams = (cardView.layoutParams as RelativeLayout.LayoutParams).apply {
            width = context.toDp2Px((percent * 2 * 100).toFloat()).toInt()
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

    @SuppressLint("SetTextI18n")
    private fun <T> setResultUi(
        textView: AppCompatTextView,
        pairView: AppCompatTextView,
        value: T?,
        unit: String = ""
    ) {
        value?.let { data ->
            textView.text = "${data}$unit"
        } ?: run {
            textView.visibility = View.GONE
            pairView.visibility = View.GONE
        }
    }


    @SuppressLint("SetTextI18n")
    fun isCheckSumVis(
        totalTime: Int?,
        timeList: List<Pair<Int?, Int?>>,
        views: List<Triple<AppCompatTextView, AppCompatTextView, MaterialCardView>>
    ): Boolean {
        if (totalTime == null) return false

        timeList.forEachIndexed { index, (first, second) ->
            views[index].first.text = "$second%"
            views[index].second.text = first?.times(60)?.toHourMinute() ?: "-"
            if (second == null){
                return false
            }
            val width = (second.toDouble() ?: 0.0) * 2
            views[index].third.layoutParams = (views[index].third.layoutParams as? RelativeLayout.LayoutParams)?.apply {
                this.width = context.toDp2Px(width.toFloat()).toInt()
            }
        }

        return true
    }

}



