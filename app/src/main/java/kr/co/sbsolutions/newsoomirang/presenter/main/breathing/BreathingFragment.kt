package kr.co.sbsolutions.newsoomirang.presenter.main.breathing

import android.annotation.SuppressLint
import android.content.Intent
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
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kr.co.sbsolutions.newsoomirang.R
import kr.co.sbsolutions.newsoomirang.databinding.FragmentBreathingBinding
import kr.co.sbsolutions.newsoomirang.presenter.main.AlertListener
import kr.co.sbsolutions.newsoomirang.presenter.main.ChargingInfoDialog
import kr.co.sbsolutions.newsoomirang.presenter.main.MainViewModel
import kr.co.sbsolutions.newsoomirang.presenter.sensor.SensorActivity

@AndroidEntryPoint
class BreathingFragment : Fragment() {

    private val viewModel: BreathingViewModel by viewModels()
    private val activityViewModel: MainViewModel by activityViewModels()
    private val binding: FragmentBreathingBinding by lazy {
        FragmentBreathingBinding.inflate(layoutInflater)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.batteryTextView.visibility = View.GONE
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.userName.collectLatest {
                        binding.tvName.text = it
                    }
                }
                launch {
                    activityViewModel.changeSBSensorInfo.collectLatest {
                        viewModel.onChangeSBSensorInfo(it)
                    }
                }
                launch {
                    viewModel.gotoScan.collectLatest {
                        startActivity(Intent(this@BreathingFragment.context, SensorActivity::class.java))
                    }
                }
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
                launch {
                    viewModel.showMeasurementAlert.collectLatest {
                        showChargingDialog()
                    }
                }
                launch {
                    viewModel.measuringState.collectLatest {
                        when (it) {
                            MeasuringState.InIt -> {
                                binding.initGroup.visibility = View.VISIBLE
                                binding.actionMeasurer.root.visibility = View.GONE
                                binding.actionResult.root.visibility = View.GONE
                            }

                            MeasuringState.FiveRecode -> {
                                binding.initGroup.visibility = View.GONE
                                binding.actionMeasurer.root.visibility = View.VISIBLE
                                binding.actionResult.root.visibility = View.GONE
                            }

                            MeasuringState.Record -> {
                                binding.initGroup.visibility = View.GONE
                                binding.actionMeasurer.root.visibility = View.VISIBLE
                                binding.actionResult.root.visibility = View.GONE
                            }

                            MeasuringState.Analytics -> {
                                binding.initGroup.visibility = View.GONE
                                binding.actionMeasurer.root.visibility = View.VISIBLE
                                binding.actionResult.root.visibility = View.GONE
                            }

                            MeasuringState.Result -> {
                                binding.initGroup.visibility = View.GONE
                                binding.actionMeasurer.root.visibility = View.GONE
                                binding.actionResult.root.visibility = View.VISIBLE
                            }
                        }
                    }
                }

            }

        }

        binding.startButton.setOnClickListener {
            viewModel.startClick()
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