package kr.co.sbsolutions.newsoomirang.presenter.main.breathing

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
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
import com.google.android.material.bottomsheet.BottomSheetDialog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kr.co.sbsolutions.newsoomirang.R
import kr.co.sbsolutions.newsoomirang.common.Cons.TAG
import kr.co.sbsolutions.newsoomirang.common.LimitedQueue
import kr.co.sbsolutions.newsoomirang.common.showAlertDialog
import kr.co.sbsolutions.newsoomirang.common.showAlertDialogWithCancel
import kr.co.sbsolutions.newsoomirang.databinding.DialogConnectInfoBinding
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
    private val connectInfoBinding: DialogConnectInfoBinding by lazy {
        DialogConnectInfoBinding.inflate(layoutInflater)
    }
    private val connectInfoDialog by lazy {
        BottomSheetDialog(requireContext()).apply {
            setContentView(connectInfoBinding.root, null)
            connectInfoBinding.btConnect.setOnClickListener {
                viewModel.connectClick()
            }
            connectInfoBinding.btLater.setOnClickListener {
                this.dismiss()
            }
        }
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
        viewModel.sleepDataResult()
        viewModel.setBatteryInfo()
    }

    private fun setObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                //유저이름 전달
                launch {
                    viewModel.userName.collect {
                        Log.d(TAG, "setObservers: $it ")
                        binding.tvName.text = it
                        binding.actionResult.tvName.text = it
                    }
                }
                launch {
                    viewModel.errorMessage.collectLatest {
                        requireActivity().showAlertDialog(R.string.common_title, it)
                    }
                }
                // 블루투스 연결 팝업
                launch {
                    viewModel.connectAlert.collect{
                        showConnectDialog()
                    }
                }

                launch {
                    activityViewModel.breathingResults.collectLatest {
                        viewModel.sleepDataResult()
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
                    activityViewModel.batteryState.collectLatest {
                        setBatteryInfo(it)
                    }
                }

                //기기 베터리 여부에 따라 버튼 활성 및 문구 변경
                launch {
                    viewModel.canMeasurement.collectLatest {
                        binding.tvNameDes2.text = if (it) "아직 호흡정보가 없습니다.\n시작을 눌러주세요." else "기기 배터리 부족으로 측정이 불가합니다.\n기기를 충전해 주세요"
                        binding.startButton.visibility = if (it) View.VISIBLE else View.GONE
                        viewModel.setMeasuringState(MeasuringState.Charging)
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
                        viewModel.setMeasuringState(if (it.second >= 5) MeasuringState.Record else MeasuringState.FiveRecode)
                        binding.actionMeasurer.timerTextView.text = String.format(Locale.KOREA, "%02d:%02d:%02d", it.first, it.second, it.third)
                    }
                }
                launch {
                    viewModel.capacitanceFlow.collectLatest {

                        if (xCountResetFlag && graphCount > 50f) {
                            binding.actionMeasurer.chart.xAxis.resetAxisMaximum()
                            xCountResetFlag = false
                        }
                        queueList.offer(Entry(graphCount++, it.toFloat()))
                        dataSetList.values = queueList.toList()
                        lineDataList.notifyDataChanged()
                        binding.actionMeasurer.chart.invalidate()
                    }
                }
                launch {
                    viewModel.sleepDataResultFlow.collectLatest {
                        binding.actionResult.resultDateTextView.text = it.endDate
                        binding.actionResult.resultTotalTextView.text = it.resultTotal
                        binding.actionResult.resultRealTextView.text = it.resultReal
                        binding.actionResult.resultAsleepTextView.text = it.resultAsleep
                        binding.actionResult.resultDurationTextView.text = it.duration
                        binding.actionResult.resultDeepSleepTextView.text = it.deepSleepTime
                        binding.actionResult.resultSleepMoveTextView.text = it.moveCount
                        when (it.apneaState) {
                            3 -> {
                                binding.actionResult.IndicatorsLeft.visibility = View.GONE
                                binding.actionResult.IndicatorsCenter.visibility = View.GONE
                                binding.actionResult.IndicatorsEnd.visibility = View.VISIBLE
                            }

                            2 -> {
                                binding.actionResult.IndicatorsLeft.visibility = View.GONE
                                binding.actionResult.IndicatorsCenter.visibility = View.VISIBLE
                                binding.actionResult.IndicatorsEnd.visibility = View.GONE
                            }

                            else -> {
                                binding.actionResult.IndicatorsLeft.visibility = View.VISIBLE
                                binding.actionResult.IndicatorsCenter.visibility = View.GONE
                                binding.actionResult.IndicatorsEnd.visibility = View.GONE
                            }
                        }
//                        binding.actionResult.tvState.
                    }
                }
                //300미만 취소 시
                launch {
                    viewModel.showMeasurementCancelAlert.collectLatest {
                        requireActivity().showAlertDialogWithCancel(R.string.common_title, "측정 시간이 부족해 결과를 확인할 수 없어요. 측정을 종료할까요?", confirmAction = {
                            binding.actionMeasurer.timerTextView.text = String.format(Locale.KOREA, "%02d:%02d:%02d", 0, 0 ,0)
                            graphCount = 0f
                            queueList.clear()
                            lineDataList.notifyDataChanged()

                            viewModel.cancelClick()
                        })
                    }
                }
                launch {
                    viewModel.bluetoothButtonState.collect{
                        binding.startButton.text = it
                    }
                }
                launch {
                    viewModel.isProgressBar.collect{
                        binding.actionProgress.clProgress.visibility = if(it)  View.VISIBLE  else View.GONE
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
                            MeasuringState.Charging ->{
                                binding.initGroup.visibility = View.VISIBLE
                                binding.actionMeasurer.root.visibility = View.GONE
                                binding.actionResult.root.visibility = View.GONE
                                binding.startButton.visibility = View.GONE
                                binding.stopButton.visibility = View.GONE
                                chartSetting()
                            }

                            MeasuringState.FiveRecode -> {

                                binding.initGroup.visibility = View.GONE
                                binding.actionMeasurer.root.visibility = View.VISIBLE
                                binding.actionResult.root.visibility = View.GONE
                                binding.startButton.visibility = View.INVISIBLE
                                binding.stopButton.visibility = View.VISIBLE
                                binding.actionMeasurer.timerTextView.visibility = View.VISIBLE
                                binding.actionMeasurer.analyLayout.visibility = View.GONE
                                binding.actionMeasurer.measureStateLayout.visibility = View.GONE

                            }

                            MeasuringState.Record -> {
                                binding.initGroup.visibility = View.GONE
                                binding.actionMeasurer.root.visibility = View.VISIBLE
                                binding.actionResult.root.visibility = View.GONE
                                binding.startButton.visibility = View.INVISIBLE
                                binding.stopButton.visibility = View.VISIBLE
                                binding.actionMeasurer.timerTextView.visibility = View.VISIBLE
                                binding.actionMeasurer.analyLayout.visibility = View.GONE
                                binding.actionMeasurer.measureStateLayout.visibility = View.VISIBLE
                            }

                            MeasuringState.Analytics -> {
                                binding.initGroup.visibility = View.GONE
                                binding.actionMeasurer.root.visibility = View.VISIBLE
                                binding.actionResult.root.visibility = View.GONE
                                binding.startButton.visibility = View.INVISIBLE
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
        dataSetList.color = Color.parseColor("#FFFFFF")
        dataSetList.setCircleColor(Color.TRANSPARENT)
        dataSetList.setDrawHorizontalHighlightIndicator(false)
        dataSetList.isHighlightEnabled = false

        dataSetList.mode = LineDataSet.Mode.CUBIC_BEZIER
        dataSetList.setDrawCircles(false)
    }

    private  fun showConnectDialog(){
        if (connectInfoDialog.isShowing) {
            connectInfoDialog.dismiss()
        }
        connectInfoDialog.show()
    }
    private fun showChargingDialog() {

        ChargingInfoDialog(object : AlertListener {
            override fun onConfirm() {
                lifecycleScope.launch {
                    viewModel.sleepDataCreate().collect{
                        if (it) {
                            activityViewModel.setCommend(ServiceCommend.START)
                        }
                    }
                }
            }
        }).show(requireActivity().supportFragmentManager, "")
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun setBatteryInfo(batteryInfo: String) {
        if (batteryInfo.isEmpty()) {
            return
        }
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
        binding.batteryTextView.text = "배터리 $batteryInfo%"
    }
}