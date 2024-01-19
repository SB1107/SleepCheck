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
import kr.co.sbsolutions.newsoomirang.presenter.main.ServiceCommend
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

    override fun onResume() {
        super.onResume()
        chartSetting()
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

                        if(xCountResetFlag && graphCount > 50f){
                            binding.actionMeasurer.chart.xAxis.resetAxisMaximum()
                            xCountResetFlag = false
                        }
                        queueList.offer(Entry(graphCount++, it.toFloat()))
                        dataSetList.values = queueList.toList()
                        lineDataList.notifyDataChanged()
                        binding.actionMeasurer.chart.invalidate()
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

            isAutoScaleMinMaxEnabled = true
            setPinchZoom(false)
            isDoubleTapToZoomEnabled = false
            description = null
            data = lineDataList
            setScaleEnabled(false)
            legend.isEnabled = false


            xAxis.setDrawAxisLine(false)
            xAxis.setDrawLabels(false)
            xAxis.setDrawGridLines(false)
            xAxis.axisMaximum = 50f

            axisLeft.textColor = Color.parseColor("#8B80F8")
            axisLeft.setDrawAxisLine(false)
            axisLeft.setDrawGridLines(false)
            axisLeft.setDrawLabels(false)

            axisRight.setDrawAxisLine(false)
            axisRight.setDrawLabels(false)
            axisRight.setDrawGridLines(false)
        }

        queueList.clear()
        graphCount = 0F
        xCountResetFlag = true

        dataSetList.lineWidth = 2f
        dataSetList.color = Color.parseColor("#8B80F8")
        dataSetList.setCircleColor(Color.TRANSPARENT)
        dataSetList.setDrawHorizontalHighlightIndicator(false)
        dataSetList.isHighlightEnabled = false

        dataSetList.mode = LineDataSet.Mode.CUBIC_BEZIER
        dataSetList.setDrawCircles(false)
    }

    private fun showChargingDialog() {
        ChargingInfoDialog(object : AlertListener {
            override fun onConfirm() {
                viewModel.sleepDataCreate().apply {
                    activityViewModel.setCommend(ServiceCommend.START)
                }
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