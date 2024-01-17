package kr.co.sbsolutions.newsoomirang.presenter.main.setting

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kr.co.sbsolutions.newsoomirang.R
import kr.co.sbsolutions.newsoomirang.common.WebType
import kr.co.sbsolutions.newsoomirang.common.addFlag
import kr.co.sbsolutions.newsoomirang.databinding.FragmentSettingBinding
import kr.co.sbsolutions.newsoomirang.presenter.login.LoginActivity
import kr.co.sbsolutions.newsoomirang.presenter.main.MainActivity
import kr.co.sbsolutions.newsoomirang.presenter.policy.PolicyActivity
import kr.co.sbsolutions.newsoomirang.presenter.sensor.SensorActivity
import kr.co.sbsolutions.newsoomirang.presenter.webview.WebViewActivity

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
                startActivity(Intent(requireContext(), PolicyActivity::class.java))
            }
            //로그아웃
            binding.clLogout.setOnClickListener {
                viewModel.logout()
            }
            //회원 탈퇴
            binding.clLeave.setOnClickListener {

            }

            //디바이스 연결시에 디바이스 이름
            binding.cvDeviceName.visibility = View.GONE
            binding.tvDeviceName.text = ""

        lifecycleScope.launch {
            launch {
                lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    viewModel.logoutResult.collect {
                        startActivity(Intent(activity, LoginActivity::class.java))
                    }
                }
            }
        }
    }
}