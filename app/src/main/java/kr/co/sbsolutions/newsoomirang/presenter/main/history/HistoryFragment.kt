package kr.co.sbsolutions.newsoomirang.presenter.main.history

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kr.co.sbsolutions.newsoomirang.R
import kr.co.sbsolutions.newsoomirang.common.showAlertDialog
import kr.co.sbsolutions.newsoomirang.common.showYearDialog
import kr.co.sbsolutions.newsoomirang.databinding.FragmentHistoryBinding
import kr.co.sbsolutions.newsoomirang.presenter.main.history.detail.HistoryDetailActivity
import java.time.LocalDate

@AndroidEntryPoint
class HistoryFragment : Fragment() {

    private val viewModel: HistoryViewModel by viewModels()
    private val binding: FragmentHistoryBinding by lazy {
        FragmentHistoryBinding.inflate(layoutInflater)
    }
    private  val clickItem : (String , String) -> Unit = object : (String , String) -> Unit {
        override fun invoke(id: String , date : String) {
            requireActivity().startActivity(Intent(requireActivity(), HistoryDetailActivity::class.java).apply {
                putExtra("id", id)
                putExtra("date", date)
            })
        }
    }
    private var mSelectedDate: LocalDate = LocalDate.now()
    private val adapter = HistoryAdapter(clickItem)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindViews()
        setObservers()
        viewModel.getYearSleepData(mSelectedDate.year.toString())
    }

    @SuppressLint("SetTextI18n")
    private fun bindViews() {
        binding.dateTextView.text = mSelectedDate.year.toString()
        binding.dateTextView.setOnClickListener {
            requireActivity().showYearDialog(mSelectedDate.year, null) {
                mSelectedDate = LocalDate.of(it, 1, 1)
                binding.dateTextView.text = mSelectedDate.year.toString()
                viewModel.getYearSleepData(mSelectedDate.year.toString())
            }
        }
        binding.historyRecyclerView.layoutManager = LinearLayoutManager(requireContext().applicationContext, LinearLayoutManager.VERTICAL, false)

        //adpter 작업 필요함
        binding.historyRecyclerView.adapter = adapter

    }

    private fun setObservers() {
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.sleepYearData.collectLatest {
                        it.result?.data?.let {list ->
                            adapter.submitList(list.toMutableList())
                        }
                    }
                }
                launch {
                    viewModel.errorMessage.collectLatest {
                        requireActivity().showAlertDialog(R.string.common_title, it)
                    }
                }
                launch {
                    viewModel.isProgressBar.collect{
                        binding.actionProgress.clProgress.visibility = if(it)  View.VISIBLE  else View.GONE
                    }
                }
            }
        }
    }
}