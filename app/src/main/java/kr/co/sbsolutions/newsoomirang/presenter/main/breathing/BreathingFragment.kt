package kr.co.sbsolutions.newsoomirang.presenter.main.breathing

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import kr.co.sbsolutions.newsoomirang.databinding.FragmentBreathingBinding

class BreathingFragment : Fragment() {

    companion object {
        fun newInstance() = BreathingFragment()
    }

    private val viewModel: BreathingViewModel by viewModels()
    private val binding: FragmentBreathingBinding by lazy {
        FragmentBreathingBinding.inflate(layoutInflater)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return binding.root
    }

}