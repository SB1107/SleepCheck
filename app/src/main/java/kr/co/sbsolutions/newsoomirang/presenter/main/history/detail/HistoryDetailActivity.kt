package kr.co.sbsolutions.newsoomirang.presenter.main.history.detail

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.RelativeLayout
import androidx.activity.viewModels
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.google.android.material.card.MaterialCardView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kr.co.sbsolutions.newsoomirang.R
import kr.co.sbsolutions.newsoomirang.common.Cons.TAG
import kr.co.sbsolutions.newsoomirang.common.showAlertDialog
import kr.co.sbsolutions.newsoomirang.common.toDate
import kr.co.sbsolutions.newsoomirang.common.toDp2Px
import kr.co.sbsolutions.newsoomirang.common.toHourMinute
import kr.co.sbsolutions.newsoomirang.data.entity.SleepDetailResult
import kr.co.sbsolutions.newsoomirang.databinding.ActivityHistoryDetailBinding
import kr.co.sbsolutions.newsoomirang.presenter.BaseActivity
import kr.co.sbsolutions.newsoomirang.presenter.BaseViewModel
import java.util.concurrent.TimeUnit

@AndroidEntryPoint
class HistoryDetailActivity : BaseActivity() {
    private val viewModel: HistoryDetailViewModel by viewModels()
    private val binding: ActivityHistoryDetailBinding by lazy {
        ActivityHistoryDetailBinding.inflate(layoutInflater)
    }
    override fun newBackPressed() {
        finish()
    }
    override fun injectViewModel(): BaseViewModel {
        return  viewModel
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        intent?.let {
          it.getStringExtra("id")?.let { id ->
            viewModel.getSleepData(id)
          }
          it.getStringExtra("date")?.let { date ->
            binding.actionBar.toolbarTitle.text = date
          }
        }
        binding.actionType.root.visibility = View.GONE
        bindViews()
        setObservers()
    }

    private fun bindViews() {
        binding.actionBar.backButton.setOnClickListener{
            finish()
        }

    }
    private fun setObservers() {
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.errorMessage.collectLatest {
                        showAlertDialog(R.string.common_title, it)
                    }
                }
                launch {
                    viewModel.isProgressBar.collect{
                        Log.e(TAG, "isProgressBar: ${it}" )
                        binding.actionProgress.clProgress.visibility = if(it)  View.VISIBLE  else View.GONE
                        binding.actionType.root.visibility = if(it.not())  View.VISIBLE  else View.GONE
                    }
                }
                launch {
                    viewModel.sleepDataDetailData.collectLatest {
                        setSleepDataDetailData(it)
                    }
                }
            }
        }
    }

    private  fun setSleepDataDetailData(result: SleepDetailResult) {
        result.endedAt?.let { it ->
            val endedAt = it.toDate("yyyy-MM-dd HH:mm:ss")
            result.startedAt?.let { itStartedAt ->
                val startedAt = itStartedAt.toDate("yyyy-MM-dd HH:mm:ss")
                val milliseconds: Long = (endedAt?.time ?: 0) - (startedAt?.time ?: 0)
                val min =
                    (TimeUnit.MILLISECONDS.toMinutes(milliseconds).toInt() * 60).toHourMinute()
//                    //총 수면 시간
                    setResultUi(
                        binding.actionType.actionNewResult.resultTotalTextView,
                        binding.actionType.actionNewResult.resultTotalTitleTextView,
                        min
                    )

            }

            //잠들 때까지 걸린 시간
            setResultUi(binding.actionType.actionNewResult.resultAsleepTextView, binding.actionType.actionNewResult.resultAsleepTitleTextView, (result.asleepTime?.times(60))?.toHourMinute())

            //코골이 시간
            setResultUi(binding.actionType.actionNewResult.resultSnoreTimeTextView, binding.actionType.actionNewResult.snoreTimeTitleTextView, (result.snoreTime?.times(60))?.toHourMinute())

            //깊은잠 시간
            setResultUi(binding.actionType.actionNewResult.resultDeepSleepTextView, binding.actionType.actionNewResult.resultDeepSleepTitleTextView, (result.deepSleepTime?.times(60))?.toHourMinute())

            //뒤척임 횟수
            setResultUi(binding.actionType.actionNewResult.resultSleepMoveTextView, binding.actionType.actionNewResult.resultSleepMoveTitleTextView, result.moveCount, " 회")

//            Log.d(TAG, "bind1: ${result.apneaCount}")

            result.apneaCount?.let { apneaCount ->
                val params2 = binding.actionType.actionSleepingGraph.vLeft.layoutParams as ConstraintLayout.LayoutParams
                params2.horizontalBias = apneaCount * 0.01f


                binding.actionType.actionSleepingGraph.tvTotalApnea.text = apneaCount.toString()
                binding.actionType.actionSleepingGraph.vLeft.layoutParams = params2
                binding.actionType.actionApneaResult.resultTotalApneaTextView.text = "$apneaCount 회"

            } ?: run {
                binding.actionType.actionSleepingGraph.root.visibility = View.GONE
                binding.actionType.actionApneaResult.resultTotalApneaTextView.visibility = View.GONE
                binding.actionType.actionApneaResult.resultTotalApneaTitleTextView.visibility = View.GONE
            }


            //무호흡 횟수
            setResultUi(
                binding.actionType.actionApneaResult.resultApnea10TextView,
                binding.actionType.actionApneaResult.resultApnea10TitleTextView,
                result.apnea10,
                " 회"
            )

            setResultUi(
                binding.actionType.actionApneaResult.resultApnea30TextView,
                binding.actionType.actionApneaResult.resultApnea30TitleTextView,
                result.apnea30,
                " 회"
            )

            setResultUi(
                binding.actionType.actionApneaResult.resultApnea60TextView,
                binding.actionType.actionApneaResult.resultApnea60TitleTextView,
                result.apnea60,
                " 회"
            )


            val positionViews = listOf(
                Triple(
                    binding.actionType.actionSleepPosition.pose1PercentTextView,
                    binding.actionType.actionSleepPosition.pose1TextView,
                    binding.actionType.actionSleepPosition.pose1ProgressView
                ),
                Triple(
                    binding.actionType.actionSleepPosition.pose2PercentTextView,
                    binding.actionType.actionSleepPosition.pose2TextView,
                    binding.actionType.actionSleepPosition.pose2ProgressView
                ),
                Triple(
                    binding.actionType.actionSleepPosition.pose3PercentTextView,
                    binding.actionType.actionSleepPosition.pose3TextView,
                    binding.actionType.actionSleepPosition.pose3ProgressView
                ),
                Triple(
                    binding.actionType.actionSleepPosition.pose4PercentTextView,
                    binding.actionType.actionSleepPosition.pose4TextView,
                    binding.actionType.actionSleepPosition.pose4ProgressView
                ),
                Triple(
                    binding.actionType.actionSleepPosition.pose5PercentTextView,
                    binding.actionType.actionSleepPosition.pose5TextView,
                    binding.actionType.actionSleepPosition.pose5ProgressView
                )
            )
            val positionVale = listOf(
                Pair(
                    result.straightPositionTime,
                    result.straightPer
                ),
                Pair(
                    result.leftPositionTime,
                    result.leftPer
                ),
                Pair(
                    result.rightPositionTime,
                    result.rightPer
                ),
                Pair(
                    result.downPositionTime,
                    result.downPer
                ),
                Pair(
                    result.wakeTime,
                    result.wakePer
                )
            )

            binding.actionType.actionSleepPosition.root.visibility = if (!isCheckSumVis
                    (
                    totalTime = result.sleepTime,
                    timeList = positionVale,
                    views = positionViews
                )
            ) View.GONE else View.VISIBLE


            //수면 패턴
            initChart(binding.actionType.actionSleepPatten.chart, arrayListOf())
            result.sleepPattern?.let {
                it.split("")
                    .filter { it.trim() != "" }
                    .forEach { value ->
                        addEntry(binding.actionType.actionSleepPatten.chart, value.toDouble())
                    }
            } ?: run { binding.actionType.actionSleepPatten.root.visibility = View.GONE }



        }
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
                this.width = toDp2Px(width.toFloat()).toInt()
            }
        }

        return true
    }


    @SuppressLint("UseCompatLoadingForDrawables")
    private fun initChart(lineChart: LineChart, values: ArrayList<Entry>) {


        // 데이터 갯수 7 개
        lineChart.background = getDrawable(R.color.clear) // 배경색
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
        lineChart.setNoDataTextColor(getColor(R.color.color_FFFFFF))
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
        xAxis.textColor = getColor(R.color.clear)
        xAxis.gridColor = getColor(R.color.clear)
        xAxis.axisLineColor = getColor(R.color.clear)


        // Y Axis - Left
        val leftAxis = lineChart.axisLeft
        leftAxis.isEnabled = false
        leftAxis.setDrawLabels(false)
        leftAxis.axisLineWidth = 0f
        leftAxis.axisLineColor = getColor(R.color.clear)
        leftAxis.gridColor = getColor(R.color.clear)

        // Y Axis - Right
        val rightAxis = lineChart.axisRight
        rightAxis.isEnabled = false
        //    setChartData();
        val lineDataSet = LineDataSet(values, "")
        lineDataSet.setDrawIcons(false) // 아이콘 표시
        lineDataSet.setDrawCircles(false)
        lineDataSet.color = getColor(R.color.color_FFFFFF)
        lineDataSet.lineWidth = 2f
        lineDataSet.valueTextColor = getColor(R.color.clear)
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
        lineDataSet.color = getColor(R.color.colorAccent)
        lineDataSet.lineWidth = 2f
        lineDataSet.valueTextColor = getColor(R.color.clear)
        lineDataSet.mode = LineDataSet.Mode.CUBIC_BEZIER
        return lineDataSet
    }
}