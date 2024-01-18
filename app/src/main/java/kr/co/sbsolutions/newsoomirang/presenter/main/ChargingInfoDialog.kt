package kr.co.sbsolutions.newsoomirang.presenter.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kr.co.sbsolutions.newsoomirang.R
import kr.co.sbsolutions.newsoomirang.databinding.DialogChargingInfoBinding

class ChargingInfoDialog(val alertListener: AlertListener) : BottomSheetDialogFragment() {

    private val binding: DialogChargingInfoBinding by lazy {
        DialogChargingInfoBinding.inflate(layoutInflater)
    }

    override fun getTheme(): Int {
        return R.style.CustomBottomSheetDialogTheme
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btDone.setOnClickListener {  confirmTouched()}
    }
    /**
     * 확인 터치
     */
    private fun confirmTouched() {
        alertListener.onConfirm()
        dismiss()
    }
}

interface AlertListener {
    fun onConfirm()
}

