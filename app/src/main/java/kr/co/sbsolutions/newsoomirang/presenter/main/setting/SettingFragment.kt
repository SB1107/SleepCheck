package kr.co.sbsolutions.newsoomirang.presenter.main.setting

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build.VERSION
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kr.co.sbsolutions.newsoomirang.BuildConfig
import kr.co.sbsolutions.newsoomirang.R
import kr.co.sbsolutions.newsoomirang.common.showAlertDialog
import kr.co.sbsolutions.newsoomirang.databinding.FragmentSettingBinding
import kr.co.sbsolutions.newsoomirang.presenter.leave.LeaveActivity
import kr.co.sbsolutions.newsoomirang.presenter.login.LoginActivity
import kr.co.sbsolutions.newsoomirang.presenter.policy.PolicyActivity
import kr.co.sbsolutions.newsoomirang.presenter.sensor.SensorActivity

@AndroidEntryPoint
class SettingFragment : Fragment() {
    private val viewModel: SettingViewModel by viewModels()

    private val binding: FragmentSettingBinding by lazy {
        FragmentSettingBinding.inflate(layoutInflater)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return binding.root
    }

    @SuppressLint("UnsafeRepeatOnLifecycleDetector")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //내센서 클릭
        binding.clSensor.setOnClickListener {
            startActivity(Intent(requireContext(), SensorActivity::class.java))
        }
        //개인정보 방침
        binding.clPolicy.setOnClickListener {
            startActivity(Intent(requireContext(), PolicyActivity::class.java).putExtra("where", "setting"))
        }
        //로그아웃
        binding.clLogout.setOnClickListener {
            viewModel.logout()
        }

        //회원 탈퇴
        binding.clLeave.setOnClickListener {
            startActivity(Intent(requireContext(), LeaveActivity::class.java))
        }

        //디바이스 연결시에 디바이스 이름
        binding.cvDeviceName.visibility = View.GONE
        binding.tvDeviceName.text = ""

        binding.tvVersionName.text = "앱 버전 : ${BuildConfig.VERSION_NAME}"

        lifecycleScope.launch {
            launch {
                lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    viewModel.logoutResult.collect {
                        startActivity(Intent(activity, LoginActivity::class.java))
                    }
                }
            }
            launch {
                viewModel.errorMessage.collectLatest {
                    requireActivity().showAlertDialog(R.string.common_title, it)
                }
            }
        }
    }
}