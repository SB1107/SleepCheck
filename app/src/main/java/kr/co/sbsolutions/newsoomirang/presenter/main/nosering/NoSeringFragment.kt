package kr.co.sbsolutions.newsoomirang.presenter.main.nosering

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kr.co.sbsolutions.newsoomirang.R

class NoSeringFragment : Fragment() {

    companion object {
        fun newInstance() = NoSeringFragment()
    }

    private lateinit var viewModel: NoSeringViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_no_sering, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(NoSeringViewModel::class.java)
        // TODO: Use the ViewModel
    }

}