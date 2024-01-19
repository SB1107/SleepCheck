package kr.co.sbsolutions.newsoomirang.presenter.main.breathing

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kr.co.sbsolutions.newsoomirang.R
import kr.co.sbsolutions.newsoomirang.common.LimitedQueue
import kr.co.sbsolutions.newsoomirang.databinding.FragmentBreathingBinding
import kr.co.sbsolutions.newsoomirang.presenter.main.AlertListener
import kr.co.sbsolutions.newsoomirang.presenter.main.ChargingInfoDialog
import kr.co.sbsolutions.newsoomirang.presenter.main.MainViewModel
import kr.co.sbsolutions.newsoomirang.presenter.sensor.SensorActivity
import java.util.Locale

@AndroidEntryPoint
class BreathingFragment : Fragment() {

    private val viewModel: BreathingViewModel by viewModels()
    private val activityViewModel: MainViewModel by activityViewModels()
    private val binding: FragmentBreathingBinding by lazy {
        FragmentBreathingBinding.inflate(layoutInflater)
    }
    private val queueList = LimitedQueue<Entry>(50)
    private val dataSetList = LineDataSet(queueList.toList(), "Label")
    private val lineDataList = LineData(dataSetList).apply { setDrawValues(false) }
    private var xCountResetFlag = true
    private var graphCount = 0f
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.batteryTextView.visibility = View.GONE
        setObservers()

        binding.startButton.setOnClickListener {
            viewModel.startClick()
        }
        binding.stopButton.setOnClickListener {
            viewModel.stopClick()
        }

    }

    private fun setObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                //유저이름 전달
                launch {
                    viewModel.userName.collectLatest {
                        binding.tvName.text = it
                    }
                }
                //액티비티 뷰모델 -> 프래그먼트 뷰모델로 데이터 전달
                launch {
                    activityViewModel.changeSBSensorInfo.collectLatest {
                        viewModel.onChangeSBSensorInfo(it)
                        activityViewModel.getService()?.let { service ->
                            viewModel.setService(service)
                        }
                    }
                }
                //기기 연결 안되었을시 기기 등록 페이지 이동
                launch {
                    viewModel.gotoScan.collectLatest {
                        startActivity(Intent(this@BreathingFragment.context, SensorActivity::class.java))
                    }
                }
                //배터리 상태
                launch {
                    viewModel.batteryState.collectLatest {
                        setBatteryInfo(it)
                    }
                }

                //기기 베터리 여부에 따라 버튼 활성 및 문구 변경
                launch {
                    viewModel.canMeasurement.collectLatest {
                        binding.tvNameDes2.text = if (it) "아직 호흡정보가 없습니다.\n시작을 눌러주세요." else "기기 배터리 부족으로 측정이 불가합니다."
                        binding.startButton.visibility = if (it) View.VISIBLE else View.GONE
                    }
                }
                //시작버튼 누를시 팝업 이벤트
                launch {
                    viewModel.showMeasurementAlert.collectLatest {
                        showChargingDialog()
                    }
                }
                //타이머 설정
                launch {
                    viewModel.measuringTimer.collectLatest {
                        if (it.second >= 5) {
                            viewModel.setMeasuringState(MeasuringState.Record)
                        }
                        binding.actionMeasurer.timerTextView.text = String.format(Locale.KOREA, "%02d:%02d:%02d", it.first, it.second, it.third)
                    }
                }
                launch {
                    viewModel.capacitanceFlow.collectLatest {

                        //데이터를 x축의 max까지 채워서 그리기
                        if (queueList.size < 50) {
                            for (i in 0..49) {
                                queueList.offer(Entry(i.toFloat(), it.toFloat()))
                            }
                            dataSetList.values = queueList.toList()
                        } else if (xCountResetFlag && graphCount > 50) {
                            binding.actionMeasurer.chart.xAxis.resetAxisMaximum()
                            xCountResetFlag = false
                        }
                        queueList.offer(Entry(graphCount++, it.toFloat()))
                        dataSetList.values = queueList.toList()
                        lineDataList.notifyDataChanged()
                        binding.actionMeasurer.chart.invalidate()
                        binding.actionMeasurer.chart.notifyDataSetChanged()
                    }
                }
                //UI 변경
                launch {
                    viewModel.measuringState.collectLatest {
                        when (it) {
                            MeasuringState.InIt -> {
                                binding.initGroup.visibility = View.VISIBLE
                                binding.actionMeasurer.root.visibility = View.GONE
                                binding.actionResult.root.visibility = View.GONE
                                binding.startButton.visibility = View.VISIBLE
                                binding.stopButton.visibility = View.GONE
                                chartSetting()
                            }

                            MeasuringState.FiveRecode -> {

                                binding.initGroup.visibility = View.GONE
                                binding.actionMeasurer.root.visibility = View.VISIBLE
                                binding.actionResult.root.visibility = View.GONE
                                binding.startButton.visibility = View.GONE
                                binding.stopButton.visibility = View.VISIBLE
                                binding.actionMeasurer.timerTextView.visibility = View.VISIBLE
                                binding.actionMeasurer.analyLayout.visibility = View.GONE
                                binding.actionMeasurer.measureStateLayout.visibility = View.GONE

                            }

                            MeasuringState.Record -> {
                                binding.initGroup.visibility = View.GONE
                                binding.actionMeasurer.root.visibility = View.VISIBLE
                                binding.actionResult.root.visibility = View.GONE
                                binding.startButton.visibility = View.GONE
                                binding.stopButton.visibility = View.VISIBLE
                                binding.actionMeasurer.timerTextView.visibility = View.VISIBLE
                                binding.actionMeasurer.analyLayout.visibility = View.GONE
                                binding.actionMeasurer.measureStateLayout.visibility = View.VISIBLE
                            }

                            MeasuringState.Analytics -> {
                                binding.initGroup.visibility = View.GONE
                                binding.actionMeasurer.root.visibility = View.VISIBLE
                                binding.actionResult.root.visibility = View.GONE
                                binding.startButton.visibility = View.GONE
                                binding.stopButton.visibility = View.VISIBLE
                                binding.actionMeasurer.timerTextView.visibility = View.GONE
                                binding.actionMeasurer.analyLayout.visibility = View.VISIBLE

                            }

                            MeasuringState.Result -> {
                                binding.initGroup.visibility = View.GONE
                                binding.actionMeasurer.root.visibility = View.GONE
                                binding.actionResult.root.visibility = View.VISIBLE
                                binding.startButton.visibility = View.VISIBLE
                                binding.stopButton.visibility = View.GONE
                            }
                        }
                    }
                }

            }

        }
    }


    @SuppressLint("UseCompatLoadingForDrawables")
    private fun chartSetting() {
        binding.actionMeasurer.chart.apply {
            clear()
            background = requireActivity().getDrawable(R.color.color_061629) // 배경색
            description.isEnabled = false // 설명 제거
            setTouchEnabled(false) // 터치 제거
            setScaleEnabled(false)
            isAutoScaleMinMaxEnabled = true
            setPinchZoom(false)
            isDoubleTapToZoomEnabled = false
            isDragEnabled = false
            isDragXEnabled = true
            isDragYEnabled = false
            setDrawGridBackground(false) // 배경 그리드 제거
            isHighlightPerDragEnabled = false
            isHighlightPerTapEnabled = false
            setNoDataText("") // 차트 데이터가 없을 때 문구
            setNoDataTextColor(requireActivity().getColor(R.color.color_FFFFFF))
            isScrollContainer = true
            setExtraOffsets(0f, 0f, 0f, 0f)
            setViewPortOffsets(0f, 0f, 0f, 0f)
        }.also {
            it.legend.isEnabled = false
            it.axisLeft.isEnabled = false
            it.axisRight.isEnabled = false
        }
        queueList.clear()
        graphCount = 50f
        xCountResetFlag = true
        dataSetList.apply {
            lineWidth = 2f
            setCircleColor(Color.TRANSPARENT)
            setDrawHorizontalHighlightIndicator(false)
            mode = LineDataSet.Mode.CUBIC_BEZIER
            setDrawCircles(false)
            dataSetList.valueTextColor = requireActivity().getColor(R.color.clear)
            dataSetList.color = requireActivity().getColor(R.color.color_4482CC)
        }
    }

    private fun showChargingDialog() {
        ChargingInfoDialog(object : AlertListener {
            override fun onConfirm() {
                viewModel.sleepDataCreate()
            }
        }).show(requireActivity().supportFragmentManager, "")
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun setBatteryInfo(batteryInfo: String) {
        binding.batteryTextView.visibility = View.VISIBLE
        if (batteryInfo.toInt() <= 25) {
            binding.batteryTextView.setCompoundDrawablesWithIntrinsicBounds(null, null, requireActivity().getDrawable(R.drawable.ic_battery_1), null)
        } else if (batteryInfo.toInt() in 26..50) {
            binding.batteryTextView.setCompoundDrawablesWithIntrinsicBounds(null, null, requireActivity().getDrawable(R.drawable.ic_battery_2), null)
        } else if (batteryInfo.toInt() in 51..75) {
            binding.batteryTextView.setCompoundDrawablesWithIntrinsicBounds(null, null, requireActivity().getDrawable(R.drawable.ic_battery_3), null)
        } else {
            binding.batteryTextView.setCompoundDrawablesWithIntrinsicBounds(null, null, requireActivity().getDrawable(R.drawable.ic_battery), null)
        }
        binding.batteryTextView.text = "센서 배터리 $batteryInfo%"
    }
}