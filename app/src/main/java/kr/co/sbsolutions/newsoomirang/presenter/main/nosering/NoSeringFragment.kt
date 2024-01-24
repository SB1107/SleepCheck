package kr.co.sbsolutions.newsoomirang.presenter.main.nosering

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.gun0912.tedpermission.PermissionListener
import com.gun0912.tedpermission.normal.TedPermission
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kr.co.sbsolutions.newsoomirang.R
import kr.co.sbsolutions.newsoomirang.common.showAlertDialogWithCancel
import kr.co.sbsolutions.newsoomirang.databinding.FragmentNoSeringBinding
import kr.co.sbsolutions.newsoomirang.domain.audio.AudioClassificationHelper
import kr.co.sbsolutions.newsoomirang.presenter.main.AlertListener
import kr.co.sbsolutions.newsoomirang.presenter.main.ChargingInfoDialog
import kr.co.sbsolutions.newsoomirang.presenter.main.MainViewModel
import kr.co.sbsolutions.newsoomirang.presenter.main.ServiceCommend
import kr.co.sbsolutions.newsoomirang.presenter.main.breathing.MeasuringState
import kr.co.sbsolutions.newsoomirang.presenter.sensor.SensorActivity
import org.tensorflow.lite.support.label.Category
import java.util.Locale

@AndroidEntryPoint
class NoSeringFragment : Fragment() {
    private val viewModel: NoSeringViewModel by viewModels()
    private val activityViewModel: MainViewModel by activityViewModels()
    private val binding: FragmentNoSeringBinding by lazy {
        FragmentNoSeringBinding.inflate(layoutInflater)
    }
    private  val audioHelper : AudioClassificationHelper by lazy {
        AudioClassificationHelper(requireActivity(),object  : AudioClassificationHelper.AudioClassificationListener{
            override fun onError(error: String?) {
            }

            override fun onResult(results: List<Category?>?, inferenceTime: Long?) {
                viewModel.noSeringResult(results,inferenceTime)
            }
        })
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activityViewModel.getService()?.let {
        viewModel.setService(it)
        }
        isPermission()
        setObservers()
        binding.motorCheckBox.setOnCheckedChangeListener{ _ , isChecked ->
            run {
                viewModel.setMotorCheckBox(isChecked)
            }
        }
        binding.type0Chip.setOnClickListener {
            binding.type0Chip.isChecked = true
            binding.type1Chip.isChecked = false
            binding.type2Chip.isChecked = false
            viewModel.setType(0)
        }
        binding.type1Chip.setOnClickListener {
            binding.type0Chip.isChecked = false
            binding.type1Chip.isChecked = true
            binding.type2Chip.isChecked = false
            viewModel.setType(1)
        }
        binding.type2Chip.setOnClickListener {
            binding.type0Chip.isChecked = false
            binding.type1Chip.isChecked = false
            binding.type2Chip.isChecked = true
            viewModel.setType(2)
        }

        binding.startButton.setOnClickListener {
            viewModel.startClick()
        }
        binding.stopButton.setOnClickListener {
            viewModel.stopClick()
            audioHelper.stopAudioClassification()
        }

    }
    private  fun isPermission(){
        if(ContextCompat.checkSelfPermission(requireActivity(), Manifest.permission.RECORD_AUDIO) ==  PackageManager.PERMISSION_DENIED){
            binding.startButton.visibility = View.GONE
            TedPermission.create()
                .setPermissionListener(object  : PermissionListener{
                    override fun onPermissionGranted() {
                        binding.startButton.visibility = View.VISIBLE
                    }

                    override fun onPermissionDenied(deniedPermissions: MutableList<String>?) {
                        binding.startButton.visibility = View.GONE
                    }
                }).setPermissions(Manifest.permission.RECORD_AUDIO)
                .setDeniedMessage("권한을 설정해주셔야 합니다.")
                .check()
        }else{
            binding.startButton.visibility = View.VISIBLE
        }
    }

    private fun setObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                //유저이름 전달
                launch {
                    viewModel.userName.collectLatest {
                        binding.tvName.text = it
                        binding.actionResult.tvName.text = it
                    }
                }

                launch {
                    activityViewModel.noSeringResults.collectLatest {
                        viewModel.noSeringResult()
                    }
                }
                //기기 연결 안되었을시 기기 등록 페이지 이동
                launch {
                    viewModel.gotoScan.collectLatest {
                        startActivity(Intent(this@NoSeringFragment.context, SensorActivity::class.java))
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
                        binding.tvNameDes2.text = if (it) "아직 코골이정보가 없습니다.\n시작을 눌러주세요." else "기기 배터리 부족으로 측정이 불가합니다."
                        binding.startButton.visibility = if (it) View.VISIBLE else View.GONE
                    }
                }
                //시작버튼 누를시 팝업 이벤트
                launch {
                    viewModel.showMeasurementAlert.collectLatest {
                        showChargingDialog()
                    }
                }
//                //타이머 설정
                launch {
                    viewModel.measuringTimer.collectLatest {
                        binding.actionMeasurer.timerTextView.text = String.format(Locale.KOREA, "%02d:%02d:%02d", it.first, it.second, it.third)
                    }
                }
                launch {
                    viewModel.sleepDataResultFlow.collectLatest {
                        binding.actionResult.resultDateTextView.text = it.endDate
                        binding.actionResult.resultTotalTextView.text = it.resultTotal
//                        binding.actionResult.resultRealTextView.text = it.resultReal
//                        binding.actionResult.resultAsleepTextView.text = it.resultAsleep
                        binding.actionResult.resultDurationTextView.text = it.duration
                    }
                }
                //300미만 취소 시
                launch {
                    viewModel.showMeasurementCancelAlert.collectLatest {
                        requireActivity().showAlertDialogWithCancel(R.string.common_title,"측정 시간이 부족해 결과를 확인할 수 없어요. 측정을 종료할까요?",confirmAction ={
                            viewModel.cancelClick()
                        })
                    }
                }
                //UI 변경
                launch {
                    viewModel.measuringState.collectLatest {
                        when (it) {
                            MeasuringState.InIt, MeasuringState.FiveRecode -> {
                                binding.initGroup.visibility = View.VISIBLE
                                binding.actionMeasurer.root.visibility = View.GONE
                                binding.actionResult.root.visibility = View.GONE
                                binding.startButton.visibility = View.VISIBLE
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
                viewModel.sleepDataCreate().apply {
                    activityViewModel.setCommend(ServiceCommend.START)
                }
                lifecycleScope.launch{
                    audioHelper.startAudioClassification()
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
        binding.batteryTextView.text = "배터리 $batteryInfo%"
    }
}