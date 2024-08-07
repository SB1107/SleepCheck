package kr.co.sbsolutions.sleepcheck.presenter.main.breathing

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.gun0912.tedpermission.PermissionListener
import com.gun0912.tedpermission.normal.TedPermission
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kr.co.sbsolutions.sleepcheck.R
import kr.co.sbsolutions.sleepcheck.common.Cons.TAG
import kr.co.sbsolutions.sleepcheck.common.LimitedQueue
import kr.co.sbsolutions.sleepcheck.common.setOnSingleClickListener
import kr.co.sbsolutions.sleepcheck.common.showAlertDialog
import kr.co.sbsolutions.sleepcheck.common.showAlertDialogWithCancel
import kr.co.sbsolutions.sleepcheck.databinding.FragmentBreathingBinding
import kr.co.sbsolutions.sleepcheck.presenter.main.AlertListener
import kr.co.sbsolutions.sleepcheck.presenter.main.BluetoothFragment
import kr.co.sbsolutions.sleepcheck.presenter.main.BluetoothState
import kr.co.sbsolutions.sleepcheck.presenter.main.ChargingInfoDialog
import kr.co.sbsolutions.sleepcheck.presenter.main.MainViewModel
import kr.co.sbsolutions.sleepcheck.presenter.main.ServiceCommend
import kr.co.sbsolutions.sleepcheck.presenter.sensor.SensorActivity
import java.util.Locale

@AndroidEntryPoint
class BreathingFragment : BluetoothFragment() {
    override val viewModel: BreathingViewModel by viewModels()
    private val activityViewModel: MainViewModel by activityViewModels()

    private val binding: FragmentBreathingBinding by lazy {
        FragmentBreathingBinding.inflate(layoutInflater)
    }
    private lateinit var job: Job

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
        binding.startButton.setOnSingleClickListener {
            lifecycleScope.launch {
                isPermission().collectLatest {
                    if (it) {
                        viewModel.startClick()
                    }
                }
            }
        }
        binding.stopButton.setOnSingleClickListener {
            viewModel.stopClick()
        }
        /*binding.batteryTextView.setOnSingleClickListener {
            clickCount++
//            Log.d(TAG, "setBluetoothStateIcon1: $clickCount")
            if (clickCount == 10 ){
                binding.tvBluetooth.visibility = View.VISIBLE
//                Log.d(TAG, "setBluetoothStateIcon2: $clickCount")
            } else if (clickCount == 20){
                binding.tvBluetooth.visibility = View.INVISIBLE
//                Log.d(TAG, "setBluetoothStateIcon3: $clickCount")
                clickCount = 0
            }
        }*/
    }

    override fun onResume() {
        super.onResume()
        chartSetting()
//        viewModel.sleepDataResult()
        viewModel.setBatteryInfo()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (::job.isInitialized) {
            job.cancel()
        }
    }

    private fun setObservers() {
        if (::job.isInitialized) {
            job.cancel()
        }
        job = lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {

                //유저이름 전달
                launch {
                    viewModel.userName.collect {
//                        Log.d(TAG, "setObservers: $it ")
                        binding.tvName.text = it
                        binding.actionResult.tvName.text = it
                    }
                }
                launch {
                    viewModel.errorMessage.collectLatest {
                        requireActivity().showAlertDialog(R.string.common_title, it)
                    }
                }
                launch {
                    viewModel.blueToothErrorMessage.collectLatest {
                        requireActivity().showAlertDialogWithCancel(R.string.common_title,
                            it,
                            confirmAction = {
                                viewModel.reConnectBluetooth{
                                    viewModel.forceUploadResetUIAndTimer()
                                }
                            }
                        )
                    }
                }
                // 블루투스 연결 팝업
                launch {
                    viewModel.connectAlert.collect {
                        showConnectDialog()
                    }
                }

                //기기 연결 안되었을시 기기 등록 페이지 이동
                launch {
                    viewModel.gotoScan.collectLatest {
                        startActivity(
                            Intent(
                                this@BreathingFragment.context,
                                SensorActivity::class.java
                            )
                        )
                    }
                }
                //배터리 상태
                launch {
                    activityViewModel.batteryState.collectLatest {
                        setBatteryInfo(it)
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
                        viewModel.setMeasuringState(if (it.second >= 5 || it.first > 0) MeasuringState.Record else MeasuringState.FiveRecode)
                        binding.actionMeasurer.timerTextView.text = String.format(
                            Locale.KOREA,
                            "%02d:%02d:%02d",
                            it.first,
                            it.second,
                            it.third
                        )
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

                //300미만 취소 시
                launch {
                    viewModel.showMeasurementCancelAlert.collectLatest {
                        requireActivity().showAlertDialogWithCancel(
                            R.string.common_title,
                            getString(R.string.data_insuffcient),
                            confirmAction = {
                                binding.actionMeasurer.timerTextView.text =
                                    String.format(Locale.KOREA, "%02d:%02d:%02d", 0, 0, 0)
                                graphCount = 0f
                                if (queueList.isNotEmpty()) {
                                    queueList.clear()
                                    dataSetList.clear()
                                }
                                lineDataList.notifyDataChanged()

                                viewModel.cancelClick()
                            })
                    }
                }
                launch {
                    viewModel.canMeasurementAndBluetoothButtonState.collect {
                        Log.e(TAG, "canMeasurementAndBluetoothButtonState: 1 = ${it.first} 2 = ${it.second}")
//                        binding.tvNameDes2.text = if (it) "시작버튼을 눌러\n호흡을 측정해 보세요" else "기기 배터리 부족으로 측정이 불가합니다.\n기기를 충전해 주세요"
                        binding.startButton.visibility = if (it.first) View.VISIBLE else View.GONE
                        if (!it.first) {
                            viewModel.setMeasuringState(MeasuringState.Charging)
                        }
                        binding.startButton.text = getBluetoothState(it.second).getStartButtonText(context = requireContext())
                        val isDisconnect = it.second.contains(getString(R.string.start)).not()
                        binding.tvNameDes2.text = if (isDisconnect) {
                            if (it.first.not()) getString(R.string.not_measurable) else getString(R.string.connect_button)
                        } else if (it.first.not()) getString(R.string.not_measurable) else getString(R.string.breathing_start_info_message)
                        setBluetoothStateIcon(getBluetoothState(it.second))
                    }
                }
                launch {
                    viewModel.isProgressBar.collect {
                        binding.actionProgress.clProgress.visibility =
                            if (it) View.VISIBLE else View.GONE
                    }
                }

                launch {
                    activityViewModel.isResultProgressBar.collectLatest {
                        if (it.isShow.not()) {
                            viewModel.setMeasuringState(MeasuringState.InIt)
                        }
                    }
                }
                launch {
                    activityViewModel.isHomeBleProgressBar.collectLatest {
                        Log.e(TAG, "isHomeBleProgressBar: 1 = ${it.first} 2 = ${it.second}")
                        binding.icBleProgress.tvDeviceId.text = it.second
                        binding.icBleProgress.root.visibility =
                            if (it.first) View.VISIBLE else View.GONE
                    }
                }
                //블루투스 재연결시 호출
                launch {
                    viewModel.isHomeBleProgressBar.collectLatest {
                        Log.e(TAG, "isHomeBleProgressBar2: 1 = ${it.first} 2 = ${it.second}")
                        binding.icBleProgress.tvDeviceId.text = it.second
                        binding.icBleProgress.root.visibility =
                            if (it.first) View.VISIBLE else View.GONE
                    }
                }

                //UI 변경
                launch {
                    viewModel.measuringState.collectLatest {
                        when (it) {
                            MeasuringState.InIt -> {
//                                Log.d(TAG, "setObservers: Init")
                                binding.initGroup.visibility = View.VISIBLE
                                binding.actionMeasurer.root.visibility = View.GONE
                                binding.actionResult.root.visibility = View.GONE
                                binding.startButton.visibility = View.VISIBLE
                                binding.stopButton.visibility = View.GONE
                                chartSetting()
                            }

                            MeasuringState.Charging -> {
//                                Log.d(TAG, "setObservers: Charging")
                                binding.initGroup.visibility = View.VISIBLE
                                binding.actionMeasurer.root.visibility = View.GONE
                                binding.actionResult.root.visibility = View.GONE
                                binding.startButton.visibility = View.GONE
                                binding.stopButton.visibility = View.GONE
                            }

                            MeasuringState.FiveRecode -> {
//                                Log.d(TAG, "setObservers: FiveRecode")
                                binding.initGroup.visibility = View.GONE
                                binding.actionMeasurer.root.visibility = View.VISIBLE
                                binding.actionResult.root.visibility = View.GONE
                                binding.startButton.visibility = View.INVISIBLE
                                binding.stopButton.visibility = View.VISIBLE
                                binding.actionMeasurer.timerTextView.visibility = View.VISIBLE
                                binding.actionMeasurer.analyLayout.visibility = View.GONE
                                binding.actionMeasurer.chart.visibility = View.VISIBLE
                                binding.actionMeasurer.recordInfoTextView.visibility = View.VISIBLE
                                binding.actionMeasurer.measureStateLayout.visibility = View.GONE

                            }

                            MeasuringState.Record -> {
//                                Log.d(TAG, "setObservers: Recode")
                                binding.initGroup.visibility = View.GONE
                                binding.actionMeasurer.root.visibility = View.VISIBLE
                                binding.actionResult.root.visibility = View.GONE
                                binding.startButton.visibility = View.INVISIBLE
                                binding.stopButton.visibility = View.VISIBLE
                                binding.actionMeasurer.timerTextView.visibility = View.VISIBLE
                                binding.actionMeasurer.analyLayout.visibility = View.GONE
                                binding.actionMeasurer.chart.visibility = View.GONE
                                binding.actionMeasurer.recordInfoTextView.visibility = View.GONE
                                binding.actionMeasurer.measureStateLayout.visibility = View.VISIBLE
                            }

                            MeasuringState.Analytics -> {
//                                Log.d(TAG, "setObservers: Analytics")
                                binding.initGroup.visibility = View.GONE
                                binding.actionMeasurer.root.visibility = View.VISIBLE
                                binding.actionResult.root.visibility = View.GONE
                                binding.startButton.visibility = View.INVISIBLE
                                binding.stopButton.visibility = View.VISIBLE
                                binding.actionMeasurer.timerTextView.visibility = View.GONE
                                binding.actionMeasurer.analyLayout.visibility = View.VISIBLE

                            }

                            MeasuringState.Result -> {
//                                Log.d(TAG, "setObservers: Result")
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
        dataSetList.color = Color.parseColor("#3FFFFF")
        dataSetList.setCircleColor(Color.TRANSPARENT)
        dataSetList.setDrawHorizontalHighlightIndicator(false)
        dataSetList.isHighlightEnabled = false

        dataSetList.mode = LineDataSet.Mode.CUBIC_BEZIER
        dataSetList.setDrawCircles(false)
    }


    private fun showChargingDialog() {

        ChargingInfoDialog(object : AlertListener {
            override fun onConfirm() {
                lifecycleScope.launch(Dispatchers.Main) {
                    viewModel.sleepDataCreate().collect {
                        if (it) {
                            activityViewModel.setCommend(ServiceCommend.START)
                            delay(2000)
                            viewModel.dataRemovedObservers()
                        }
                    }
                }
            }
        }).show(requireActivity().supportFragmentManager, "")
    }

    private fun isPermission(): Flow<Boolean> = callbackFlow {

        if (ContextCompat.checkSelfPermission(
                requireActivity(),
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_DENIED
        ) {
            TedPermission.create()
                .setPermissionListener(object : PermissionListener {
                    override fun onPermissionGranted() {
                        trySend(true)
                        close()
                    }

                    override fun onPermissionDenied(deniedPermissions: MutableList<String>?) {
                        trySend(false)
                        close()
                    }
                }).setPermissions(Manifest.permission.RECORD_AUDIO)
                .setDeniedMessage(getString(R.string.snoring_permissions))
                .check()
        } else {
            trySend(true)
            close()
        }
        awaitClose()
    }


    @SuppressLint("UseCompatLoadingForDrawables")
    override fun setBluetoothStateIcon(bluetoothState: BluetoothState) {
        binding.tvBluetooth.setCompoundDrawablesWithIntrinsicBounds(
            null,
            null,
            requireActivity().getDrawable(bluetoothState.getImage()),
            null
        )
        binding.tvBluetooth.text = bluetoothState.getText(requireContext())
    }

    @SuppressLint("UseCompatLoadingForDrawables", "SetTextI18n")
    override fun setBatteryInfo(batteryInfo: String) {
        if (batteryInfo.isEmpty()) {
            binding.batteryTextView.visibility = View.GONE
            return
        }
        binding.batteryTextView.visibility = View.VISIBLE
        if (batteryInfo.toInt() <= 25) {
            binding.batteryTextView.setCompoundDrawablesWithIntrinsicBounds(
                null,
                null,
                requireActivity().getDrawable(R.drawable.new_ic_battery_1),
                null
            )
        } else if (batteryInfo.toInt() in 26..50) {
            binding.batteryTextView.setCompoundDrawablesWithIntrinsicBounds(
                null,
                null,
                requireActivity().getDrawable(R.drawable.new_ic_battery_2),
                null
            )
        } else if (batteryInfo.toInt() in 51..75) {
            binding.batteryTextView.setCompoundDrawablesWithIntrinsicBounds(
                null,
                null,
                requireActivity().getDrawable(R.drawable.new_ic_battery_3),
                null
            )
        } else {
            binding.batteryTextView.setCompoundDrawablesWithIntrinsicBounds(
                null,
                null,
                requireActivity().getDrawable(R.drawable.new_ic_battery),
                null
            )
        }
        binding.batteryTextView.text = getString(R.string.battery_Info, batteryInfo).plus("%")
    }
}