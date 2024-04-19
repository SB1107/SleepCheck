package kr.co.sbsolutions.newsoomirang.presenter.main.setting

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build.VERSION
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
import kr.co.sbsolutions.newsoomirang.common.WebType
import kr.co.sbsolutions.newsoomirang.BuildConfig
import kr.co.sbsolutions.newsoomirang.R
import kr.co.sbsolutions.newsoomirang.common.Cons.TAG
import kr.co.sbsolutions.newsoomirang.common.getChangeDeviceName
import kr.co.sbsolutions.newsoomirang.common.setOnSingleClickListener
import kr.co.sbsolutions.newsoomirang.common.showAlertDialog
import kr.co.sbsolutions.newsoomirang.common.showAlertDialogWithCancel
import kr.co.sbsolutions.newsoomirang.databinding.FragmentSettingBinding
import kr.co.sbsolutions.newsoomirang.presenter.leave.LeaveActivity
import kr.co.sbsolutions.newsoomirang.presenter.login.LoginActivity
import kr.co.sbsolutions.newsoomirang.presenter.policy.PolicyActivity
import kr.co.sbsolutions.newsoomirang.presenter.question.QuestionActivity
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

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setObservers()

        //내센서 클릭
        binding.clSensor.setOnSingleClickListener {
            startActivity(Intent(requireContext(), SensorActivity::class.java))
        }
        //센서 설명서
        binding.clGuide.setOnSingleClickListener {
            webViewActivity(WebType.TERMS2)
        }
        //개인정보 방침
        binding.clPolicy.setOnSingleClickListener {
            startActivity(Intent(requireContext(), PolicyActivity::class.java).putExtra("where", "setting"))
        }
        //문의하기
        binding.clQuestion.setOnSingleClickListener {
            startActivity(Intent(requireContext(), QuestionActivity::class.java))
        }
        //로그아웃
        binding.clLogout.setOnSingleClickListener {
            requireActivity().showAlertDialogWithCancel(message = "로그아웃 하시겠습니까?", confirmAction = {
                viewModel.logout()
            })
        }


        //회원 탈퇴
        binding.clLeave.setOnSingleClickListener {
            startActivity(Intent(requireContext(), LeaveActivity::class.java))
        }

        binding.tvVersionName.text = "앱 버전 : ${BuildConfig.VERSION_NAME}"
    }

    private fun webViewActivity(webType: WebType) {
        startActivity(Intent(requireContext(), WebViewActivity::class.java).apply {
            putExtra("webTypeUrl", webType.url)
            putExtra("webTypeTitle", webType.title)
        })
    }

    private fun setObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

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
                            if (it == ""){
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