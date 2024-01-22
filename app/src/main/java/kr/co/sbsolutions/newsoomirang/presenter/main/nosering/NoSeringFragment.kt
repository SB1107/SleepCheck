package kr.co.sbsolutions.newsoomirang.presenter.main.nosering

import android.annotation.SuppressLint
import android.content.Intent
import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.github.mikephil.charting.data.Entry
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kr.co.sbsolutions.newsoomirang.R
import kr.co.sbsolutions.newsoomirang.databinding.FragmentBreathingBinding
import kr.co.sbsolutions.newsoomirang.databinding.FragmentNoSeringBinding
import kr.co.sbsolutions.newsoomirang.presenter.main.AlertListener
import kr.co.sbsolutions.newsoomirang.presenter.main.ChargingInfoDialog
import kr.co.sbsolutions.newsoomirang.presenter.main.MainViewModel
import kr.co.sbsolutions.newsoomirang.presenter.main.ServiceCommend
import kr.co.sbsolutions.newsoomirang.presenter.main.breathing.BreathingViewModel
import kr.co.sbsolutions.newsoomirang.presenter.main.breathing.MeasuringState
import kr.co.sbsolutions.newsoomirang.presenter.sensor.SensorActivity
import java.util.Locale

class NoSeringFragment : Fragment() {
    private val viewModel: NoSeringViewModel by viewModels()
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setObservers()
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
                //액티비티 뷰모델 -> 프래그먼트 뷰모델로 데이터 전달
                launch {
                    activityViewModel.changeSBSensorInfo.collectLatest {
                        viewModel.onChangeSBSensorInfo(it)
                        activityViewModel.getService()?.let { service ->
                            viewModel.setService(service)
                        }
                    }
                }
//                launch {
//                    activityViewModel.breathingResults.collectLatest {
//                        viewModel.sleepDataResult()
//                    }
//                }
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
//                launch {
//                    viewModel.measuringTimer.collectLatest {
//                        if (it.second >= 5) {
//                            viewModel.setMeasuringState(MeasuringState.Record)
//                        }
//                        binding.actionMeasurer.timerTextView.text = String.format(Locale.KOREA, "%02d:%02d:%02d", it.first, it.second, it.third)
//                    }
//                }
//                launch {
//                    viewModel.sleepDataResultFlow.collectLatest {
//                        binding.actionResult.resultDateTextView.text = it.endDate
//                        binding.actionResult.resultTotalTextView.text = it.resultTotal
//                        binding.actionResult.resultRealTextView.text = it.resultReal
//                        binding.actionResult.resultAsleepTextView.text = it.resultAsleep
//                        binding.actionResult.resultDurationTextView.text = it.duration
//                        when (it.apneaState) {
//                            3 -> {
//                                binding.actionResult.IndicatorsLeft.visibility = View.GONE
//                                binding.actionResult.IndicatorsCenter.visibility = View.GONE
//                                binding.actionResult.IndicatorsEnd.visibility = View.VISIBLE
//                            }
//
//                            2 -> {
//                                binding.actionResult.IndicatorsLeft.visibility = View.GONE
//                                binding.actionResult.IndicatorsCenter.visibility = View.VISIBLE
//                                binding.actionResult.IndicatorsEnd.visibility = View.GONE
//                            }
//
//                            else -> {
//                                binding.actionResult.IndicatorsLeft.visibility = View.VISIBLE
//                                binding.actionResult.IndicatorsCenter.visibility = View.GONE
//                                binding.actionResult.IndicatorsEnd.visibility = View.GONE
//                            }
//                        }
////                        binding.actionResult.tvState.
//                    }
//                }
                //UI 변경
                launch {
                    viewModel.measuringState.collectLatest {
                        when (it) {
                            MeasuringState.InIt , MeasuringState.FiveRecode -> {
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
//                viewModel.sleepDataCreate().apply {
//                    activityViewModel.setCommend(ServiceCommend.START)
//                }
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