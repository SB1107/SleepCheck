package kr.co.sbsolutions.sleepcheck.presenter.main.setting

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
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
import kr.co.sbsolutions.sleepcheck.common.WebType
import kr.co.sbsolutions.sleepcheck.BuildConfig
import kr.co.sbsolutions.sleepcheck.R
import kr.co.sbsolutions.sleepcheck.common.Cons.TAG
import kr.co.sbsolutions.sleepcheck.common.getChangeDeviceName
import kr.co.sbsolutions.sleepcheck.common.setOnSingleClickListener
import kr.co.sbsolutions.sleepcheck.common.showAlertDialog
import kr.co.sbsolutions.sleepcheck.common.showAlertDialogWithCancel
import kr.co.sbsolutions.sleepcheck.databinding.FragmentSettingBinding
import kr.co.sbsolutions.sleepcheck.presenter.faq.FAQActivity
import kr.co.sbsolutions.sleepcheck.presenter.firmware.FirmwareUpdateActivity
import kr.co.sbsolutions.sleepcheck.presenter.leave.LeaveActivity
import kr.co.sbsolutions.sleepcheck.presenter.login.LoginActivity
import kr.co.sbsolutions.sleepcheck.presenter.policy.PolicyActivity
import kr.co.sbsolutions.sleepcheck.presenter.question.QuestionActivity
import kr.co.sbsolutions.sleepcheck.presenter.sensor.SensorActivity
import kr.co.sbsolutions.sleepcheck.presenter.webview.WebViewActivity
import java.util.Locale


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

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.getFirmwareVersion()
        setObservers()

        //내센서 클릭
        binding.clSensor.setOnSingleClickListener {
            startActivity(Intent(requireContext(), SensorActivity::class.java))
        }
        //센서 설명서
        binding.clGuide.setOnSingleClickListener {
            val locale = resources.configuration.locales[0]
                webViewActivity( if (locale == Locale.KOREA) WebType.TERMS2 else WebType.TERMS2EN, locale)
        }
        //개인정보 방침
        binding.clPolicy.setOnSingleClickListener {
            startActivity(Intent(requireContext(), PolicyActivity::class.java).putExtra("where", "setting"))
        }
        //문의하기
        binding.clQuestion.setOnSingleClickListener {
            startActivity(Intent(requireContext(), QuestionActivity::class.java))
        }
        //faq
        binding.clFaq.setOnSingleClickListener {
            startActivity(Intent(requireContext(), FAQActivity::class.java))
        }
        //로그아웃
        binding.clLogout.setOnSingleClickListener {
            requireActivity().showAlertDialogWithCancel(message = getString(R.string.logout_message), confirmAction = {
                viewModel.logout()
            })
        }
        // 펌웨어 업데이트
        binding.clFirmware.setOnSingleClickListener {
            startActivity(Intent(requireContext(), FirmwareUpdateActivity::class.java))
        }


        //회원 탈퇴
        binding.clLeave.setOnSingleClickListener {
            startActivity(Intent(requireContext(), LeaveActivity::class.java))
        }

        binding.tvVersionName.text = getString(R.string.app_version, BuildConfig.VERSION_NAME)
    }

    override fun onResume() {
        super.onResume()
        viewModel.getFirmwareVersion()
    }

    private fun webViewActivity(webType: WebType, locale: Locale) {
        startActivity(Intent(requireContext(), WebViewActivity::class.java).apply {
            putExtra("webTypeUrl", webType.url)
            val title = if (locale == Locale.KOREA) webType.titleKo else webType.titleEn
            putExtra("webTypeTitle", title)
        })
    }

    private fun setObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                launch {
                    viewModel.updateCheckResult.collectLatest {
                        Log.d(TAG, "setObseasdasdrvers: $it")
                        binding.ivNewFirmware.visibility = if (it) View.VISIBLE else View.GONE
                    }
                }

                launch {
                    viewModel.logoutResult.collect {
                        startActivity(Intent(activity, LoginActivity::class.java))
                    }
                }

                launch {
                    viewModel.errorMessage.collectLatest {
                        requireActivity().showAlertDialog(R.string.common_title, it)
                    }
                }
                launch {
                    viewModel.deviceName.collectLatest {
                        it?.let {
                            if (it == "") {
                                binding.tvDeviceName.visibility = View.GONE
                            } else {
                                binding.tvDeviceName.visibility = View.VISIBLE
                                binding.tvDeviceName.text = it.getChangeDeviceName()
                            }
                        } ?: ""
                    }
                }
            }
        }
    }
}