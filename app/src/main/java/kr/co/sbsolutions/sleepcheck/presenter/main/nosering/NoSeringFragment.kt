package kr.co.sbsolutions.sleepcheck.presenter.main.nosering

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
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
import com.gun0912.tedpermission.PermissionListener
import com.gun0912.tedpermission.normal.TedPermission
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kr.co.sbsolutions.sleepcheck.R
import kr.co.sbsolutions.sleepcheck.common.Cons
import kr.co.sbsolutions.sleepcheck.common.Cons.TAG
import kr.co.sbsolutions.sleepcheck.common.setOnSingleClickListener
import kr.co.sbsolutions.sleepcheck.common.showAlertDialog
import kr.co.sbsolutions.sleepcheck.common.showAlertDialogWithCancel
import kr.co.sbsolutions.sleepcheck.databinding.FragmentNoSeringBinding
import kr.co.sbsolutions.sleepcheck.presenter.main.AlertListener
import kr.co.sbsolutions.sleepcheck.presenter.main.BluetoothFragment
import kr.co.sbsolutions.sleepcheck.presenter.main.BluetoothState
import kr.co.sbsolutions.sleepcheck.presenter.main.ChargingInfoDialog
import kr.co.sbsolutions.sleepcheck.presenter.main.MainViewModel
import kr.co.sbsolutions.sleepcheck.presenter.main.ServiceCommend
import kr.co.sbsolutions.sleepcheck.presenter.main.breathing.MeasuringState
import kr.co.sbsolutions.sleepcheck.presenter.sensor.SensorActivity
import java.util.Locale

@AndroidEntryPoint
class NoSeringFragment : BluetoothFragment() {
    override val viewModel: NoSeringViewModel by viewModels()
    private val activityViewModel: MainViewModel by activityViewModels()
    private val binding: FragmentNoSeringBinding by lazy {
        FragmentNoSeringBinding.inflate(layoutInflater)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return binding.root
    }

    override fun onResume() {
        super.onResume()
//        viewModel.noSeringResult()
        viewModel.setBatteryInfo()
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.batteryTextView.visibility = View.GONE
        viewLifecycleOwner.lifecycleScope.launch {
            val intensity = viewModel.getIntensity().first()
            binding.motorCheckBox.isEnabled  = intensity.first
            when (intensity.second) {
                0 -> {
                    binding.type0Chip.isChecked = true
                    binding.type1Chip.isChecked = false
                    binding.type2Chip.isChecked = false
                }
                1 -> {
                    binding.type0Chip.isChecked = false
                    binding.type1Chip.isChecked = true
                    binding.type2Chip.isChecked = false
                }
                2 -> {
                    binding.type0Chip.isChecked = false
                    binding.type1Chip.isChecked = false
                    binding.type2Chip.isChecked = true
                }
                else -> {
                }
            }
        }

        setObservers()
        binding.motorCheckBox.setOnCheckedChangeListener { _, isChecked ->
            run {
                viewModel.setMotorCheckBox(isChecked)
            }
        }
        binding.type0Chip.setOnSingleClickListener {
            binding.type0Chip.isChecked = true
            binding.type1Chip.isChecked = false
            binding.type2Chip.isChecked = false
            viewModel.setType(0)
        }
        binding.type1Chip.setOnSingleClickListener {
            binding.type0Chip.isChecked = false
            binding.type1Chip.isChecked = true
            binding.type2Chip.isChecked = false
            viewModel.setType(1)
        }
        binding.type2Chip.setOnSingleClickListener {
            binding.type0Chip.isChecked = false
            binding.type1Chip.isChecked = false
            binding.type2Chip.isChecked = true
            viewModel.setType(2)
        }

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
    }

    private fun setObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                //유저이름 전달
                launch{
                    viewModel.userName.collect {
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

                //기기 연결 안되었을시 기기 등록 페이지 이동
                launch {
                    viewModel.gotoScan.collectLatest {
                        startActivity(Intent(this@NoSeringFragment.context, SensorActivity::class.java))
                    }
                }
                //배터리 상태
                launch{
                    viewModel.batteryState.collectLatest {
                        setBatteryInfo(it)
                    }
                }
                // 블루투스 연결 팝업
                launch {
                    viewModel.connectAlert.collectLatest {
                        showConnectDialog()
                    }
                }
                    launch {
                        viewModel.isHomeBleProgressBar.collectLatest {
                            Log.e(TAG, "isHomeBleProgressBar2: 1 = ${it.first} 2 = ${it.second}")
                            binding.icBleProgress.tvDeviceId.text = it.second
                            binding.icBleProgress.root.visibility =
                                if (it.first) View.VISIBLE else View.GONE
                        }
                }

//                //기기 베터리 여부에 따라 버튼 활성 및 문구 변경
//                launch {
//                    viewModel.canMeasurement.collectLatest {
//                        binding.tvNameDes2.text = if (it) "시작버튼을 눌러\n코골이을 측정해 보세요" else "기기 배터리 부족으로 측정이 불가합니다..\n기기를 충전해 주세요"
//                        binding.startButton.visibility = if (it) View.VISIBLE else View.GONE
//                        if (!it) {
//                            viewModel.setMeasuringState(MeasuringState.Charging)
//                        }
//                    }
//                }
                //시작버튼 누를시 팝업 이벤트
                launch(Dispatchers.Main) {
                    viewModel.showMeasurementAlert.collectLatest {
                        showChargingDialog()
                    }
                }
//                //타이머 설정
                launch(Dispatchers.Main) {
                    viewModel.measuringTimer.collectLatest {
                        viewModel.setMeasuringState(MeasuringState.Record)
                        binding.actionMeasurer.timerTextView.text = String.format(Locale.KOREA, "%02d:%02d:%02d", it.first, it.second, it.third)
                    }
                }
                //300미만 취소 시
                launch(Dispatchers.Main) {
                    viewModel.showMeasurementCancelAlert.collectLatest {
                        requireActivity().showAlertDialogWithCancel(R.string.common_title, getString(R.string.data_insuffcient), confirmAction = {
                            binding.actionMeasurer.timerTextView.text = String.format(Locale.KOREA, "%02d:%02d:%02d", 0, 0, 0)
                            viewModel.cancelClick()
                        })
                    }
                }
                launch {
                    viewModel.motorCheckBox.collectLatest {
                        binding.motorCheckBox.isChecked = it
                        binding.type0Chip.isEnabled = it
                        binding.type1Chip.isEnabled = it
                        binding.type2Chip.isEnabled = it
                    }
                }

                launch {
                    viewModel.intensity.collectLatest {
                        when (it) {
                            0 -> {
                                binding.type0Chip.performClick()
                            }
                            1 -> {
                                binding.type1Chip.performClick()
                            }
                            2 -> {
                                binding.type2Chip.performClick()
                            }
                        }
                    }
                }
                launch {
                    viewModel.canMeasurementAndBluetoothButtonState.collectLatest {
                        Log.e(Cons.TAG, "canMeasurementAndBluetoothButtonState: 2 = ${it.first} 2 = ${it.second}" )
//                        binding.tvNameDes2.text = if (it) "시작버튼을 눌러\n코골이을 측정해 보세요" else "기기 배터리 부족으로 측정이 불가합니다..\n기기를 충전해 주세요"
                        binding.startButton.visibility = if (it.first) View.VISIBLE else View.GONE
                        if (!it.first) {
                            viewModel.setMeasuringState(MeasuringState.Charging)
                        }
                        binding.startButton.text = getBluetoothState(it.second).getStartButtonText(requireContext())
                        val isDisconnect = it.second.contains("시작").not()
                        binding.tvNameDes2.text = if (isDisconnect) {
                            if (it.first.not()) getString(R.string.not_measurable)else getString(R.string.connect_button)
                        } else if (it.first.not()) getString(R.string.not_measurable)else getString(R.string.snoring_start)
                        val isEnabled = it.second.contains(getString(R.string.start))
                        setBluetoothStateIcon(getBluetoothState(it.second))
                        delay(500)
                        Log.d(TAG, "setObservers111: $isEnabled")
                        binding.motorCheckBox.isEnabled = isEnabled
                        binding.type0Chip.isEnabled = isEnabled
                        binding.type1Chip.isEnabled = isEnabled
                        binding.type2Chip.isEnabled = isEnabled
                    }
                }
                launch {
                    viewModel.isProgressBar.collect {
                        binding.actionProgress.clProgress.visibility = if (it) View.VISIBLE else View.GONE
                    }
                }
                launch {
                    viewModel.isRegisteredMessage.collectLatest {
                        requireActivity().showAlertDialogWithCancel(message = it,  confirmButtonText = R.string.main_measuring_text, confirmAction = {
                            viewModel.forceStartClick()
                        }, cancelButtonText = R.string.setting_general_bluetooth_text , cancelAction = {
                            viewModel.bluetoothConnect()
                        })
                    }
                }
                launch{
                    activityViewModel.isResultProgressBar.collectLatest {
                        if (it.isShow.not()) {
                            viewModel.setMeasuringState(MeasuringState.InIt)
                        }
                    }
                }
                //UI 변경
                launch {
                    viewModel.measuringState.collectLatest {
//                        Log.d(TAG, "setObservers: ${it}")
                        when (it) {
                            MeasuringState.InIt, MeasuringState.FiveRecode -> {
                                binding.initGroup.visibility = View.VISIBLE
                                binding.actionMeasurer.root.visibility = View.GONE
                                binding.actionMeasurer.clMeasure.visibility = View.VISIBLE
                                binding.actionResult.root.visibility = View.GONE
                                binding.startButton.visibility = View.VISIBLE
                                binding.stopButton.visibility = View.GONE
                            }

                            MeasuringState.Charging -> {
                                binding.initGroup.visibility = View.VISIBLE
                                binding.actionMeasurer.root.visibility = View.GONE
                                binding.actionResult.root.visibility = View.GONE
                                binding.startButton.visibility = View.GONE
                                binding.stopButton.visibility = View.GONE
                            }

                            MeasuringState.Record -> {
                                binding.initGroup.visibility = View.GONE
                                binding.actionMeasurer.root.visibility = View.VISIBLE
                                binding.actionResult.root.visibility = View.GONE
                                binding.startButton.visibility = View.GONE
                                binding.stopButton.visibility = View.VISIBLE
                                binding.actionMeasurer.timerTextView.visibility = View.VISIBLE
                                binding.actionMeasurer.analyLayout.visibility = View.GONE
                            }

                            MeasuringState.Analytics -> {
                                binding.initGroup.visibility = View.GONE
                                binding.actionMeasurer.root.visibility = View.VISIBLE
                                binding.actionResult.root.visibility = View.GONE
                                binding.startButton.visibility = View.GONE
                                binding.stopButton.visibility = View.VISIBLE
                                binding.actionMeasurer.timerTextView.visibility = View.GONE
                                binding.actionMeasurer.analyLayout.visibility = View.VISIBLE
                                binding.actionMeasurer.clMeasure.visibility = View.GONE

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



    private fun showChargingDialog() {
        ChargingInfoDialog(object : AlertListener {
            override fun onConfirm() {
                lifecycleScope.launch {
                    viewModel.sleepDataCreate().collectLatest {
                        if (it) {
                            activityViewModel.setCommend(ServiceCommend.START)
                            delay(2000)
                            viewModel.ralDataRemovedObservers()
                        }
                    }
                }
            }
        }).show(requireActivity().supportFragmentManager, "")
    }
    private fun isPermission(): Flow<Boolean> = callbackFlow {

        if (ContextCompat.checkSelfPermission(requireActivity(), Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_DENIED) {
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
        }else{
            trySend(true)
            close()
        }
        awaitClose()
    }

    override fun setBluetoothStateIcon(bluetoothState : BluetoothState){
        binding.tvBluetooth.setCompoundDrawablesWithIntrinsicBounds(null, null, requireActivity().getDrawable(bluetoothState.getImage()), null)
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
            binding.batteryTextView.setCompoundDrawablesWithIntrinsicBounds(null, null, requireActivity().getDrawable(R.drawable.new_ic_battery_1), null)
        } else if (batteryInfo.toInt() in 26..50) {
            binding.batteryTextView.setCompoundDrawablesWithIntrinsicBounds(null, null, requireActivity().getDrawable(R.drawable.new_ic_battery_2), null)
        } else if (batteryInfo.toInt() in 51..75) {
            binding.batteryTextView.setCompoundDrawablesWithIntrinsicBounds(null, null, requireActivity().getDrawable(R.drawable.new_ic_battery_3), null)
        } else {
            binding.batteryTextView.setCompoundDrawablesWithIntrinsicBounds(null, null, requireActivity().getDrawable(R.drawable.new_ic_battery), null)
        }
        binding.batteryTextView.text =  getString(R.string.battery_Info,  batteryInfo).plus("%")
    }
}