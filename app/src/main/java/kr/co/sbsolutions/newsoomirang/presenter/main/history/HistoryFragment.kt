package kr.co.sbsolutions.newsoomirang.presenter.main.history

import android.annotation.SuppressLint
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
import androidx.recyclerview.widget.LinearLayoutManager
import com.kizitonwose.calendar.core.WeekDay
import com.kizitonwose.calendar.view.WeekDayBinder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kr.co.sbsolutions.newsoomirang.R
import kr.co.sbsolutions.newsoomirang.common.Cons.TAG
import kr.co.sbsolutions.newsoomirang.common.showAlertDialog
import kr.co.sbsolutions.newsoomirang.common.toDp2Px
import kr.co.sbsolutions.newsoomirang.data.entity.SleepDateEntity
import kr.co.sbsolutions.newsoomirang.databinding.FragmentHistoryBinding
import kr.co.sbsolutions.newsoomirang.databinding.RowHistoryNoDataItemBinding
import kr.co.sbsolutions.newsoomirang.presenter.login.KaKaoLoginHelper
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@AndroidEntryPoint
class HistoryFragment : Fragment() {

    private val viewModel: HistoryViewModel by viewModels()
    private val binding: FragmentHistoryBinding by lazy {
        FragmentHistoryBinding.inflate(layoutInflater)
    }
    private val noDataBinding: RowHistoryNoDataItemBinding by lazy {
        RowHistoryNoDataItemBinding.inflate(layoutInflater)
    }
    private var mSelectedDate: LocalDate? = null
    private val adapter = HistoryAdapter()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.getWeekSleepData()
        bindViews()
        setObservers()
    }

    @SuppressLint("SetTextI18n")
    private fun bindViews() {
        binding.historyRecyclerView.layoutManager = LinearLayoutManager(requireContext().applicationContext, LinearLayoutManager.VERTICAL, false)

        //adpter 작업 필요함
        binding.historyRecyclerView.adapter = adapter

        binding.btnShared.setOnClickListener {
            viewModel.shareKaKao()
        }

    }

    @SuppressLint("SetTextI18n")
    private fun setCalendarView(sleepData: SleepDateEntity) {
        val currentDate = LocalDate.now()
        val currentMonth = YearMonth.now()
        val startDate: LocalDate = currentMonth.minusMonths(100).atEndOfMonth()
        val endDate: LocalDate = currentMonth.plusMonths(100).atEndOfMonth()
        mSelectedDate = currentDate

        binding.weekCalendarView.dayBinder = object : WeekDayBinder<DayViewContainer> {
            override fun bind(container: DayViewContainer, data: WeekDay) {
                container.dateView.text = data.date.dayOfMonth.toString()
                container.dateLayout.setOnClickListener {
                    mSelectedDate = data.date
                    binding.weekCalendarView.notifyCalendarChanged()
                    mSelectedDate?.let {
                        viewModel.getDetailSleepData(it)
                    }
//                        ?: adapter.ItemSleepViewHolder(binding)

                }
                val dateString: String = data.date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                container.progressCardView.layoutParams.height = this@HistoryFragment.context?.toDp2Px(0f)?.toInt() ?: 0
                sleepData.result?.data?.filter { it.day == dateString }?.forEach { value ->
                    if (value.minute >= 900) {
                        container.progressCardView.layoutParams.height = this@HistoryFragment.context?.toDp2Px(70f)?.toInt() ?: 0
                    } else {
                        val height = value.minute.toDouble() / 900.0 * 70
                        container.progressCardView.layoutParams.height = this@HistoryFragment.context?.toDp2Px(height.toFloat())?.toInt() ?: 0
                    }
                }
                container.progressCardView.requestLayout()
                // 선택 시
                if (data.date.isEqual(mSelectedDate)) {
                    container.dateView.setTextColor(requireActivity().getColor(R.color.color_4482CC))
                    container.calendarCardView.setCardBackgroundColor(requireActivity().getColor(R.color.color_FFFFFF))
                } else {
                    container.dateView.setTextColor(requireActivity().getColor(R.color.color_FFFFFF))
                    container.calendarCardView.setCardBackgroundColor(requireActivity().getColor(R.color.clear))
                }
            }

            override fun create(view: View): DayViewContainer {
                return DayViewContainer(view)
            }
        }

        binding.weekCalendarView.setup(startDate, endDate, DayOfWeek.SUNDAY)
        binding.weekCalendarView.scrollToWeek(currentDate)
        binding.weekCalendarView.weekScrollListener =
            { week ->
                val firstDate = week.days[0].date
                val lastDate = week.days[week.days.size - 1].date
                if (firstDate.year == lastDate.year && firstDate.monthValue == lastDate.monthValue) {
                    binding.dateTextView.text = firstDate.year.toString() + "." + firstDate.monthValue.toString()
                } else {
                    binding.dateTextView.text = firstDate.year.toString() + "." + firstDate.monthValue + " ~ " + lastDate.year + "." + lastDate.monthValue
                }
            }
        mSelectedDate?.let {
            viewModel.getDetailSleepData(it)
        }
    }


    private fun setObservers() {
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.sleepWeekData.collectLatest {
                        setCalendarView(it)
                    }
                }
                launch {
                    viewModel.sleepDataDetailData.collectLatest {
                        Log.d(TAG, "setObservers: $it")
                        adapter.submitList(it.toMutableList())
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