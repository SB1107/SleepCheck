package kr.co.sbsolutions.newsoomirang.presenter.main.setting

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import kr.co.sbsolutions.newsoomirang.R
import kr.co.sbsolutions.newsoomirang.databinding.FragmentSettingBinding

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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

            //내센서 클릭
            binding.clSensor.setOnClickListener {  }
            //개인정보 방침
            binding.clPolicy.setOnClickListener {  

            }
            //로그아웃
            binding.clLogout.setOnClickListener {  }
            //회원 탈퇴
            binding.clLeave.setOnClickListener {  }

            //디바이스 연결시에 디바이스 이름
            binding.cvDeviceName.visibility = View.GONE
            binding.tvDeviceName.text = ""
    }

}