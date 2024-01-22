package kr.co.sbsolutions.newsoomirang.presenter.main.history

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.kizitonwose.calendar.core.WeekDay
import com.kizitonwose.calendar.view.WeekDayBinder
import dagger.hilt.android.AndroidEntryPoint
import kr.co.sbsolutions.newsoomirang.R
import kr.co.sbsolutions.newsoomirang.databinding.FragmentHistoryBinding
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

@AndroidEntryPoint
class HistoryFragment : Fragment() {
    /*
        companion object {
            fun newInstance() = HistoryFragment()
        }*/

    private val viewModel: HistoryViewModel by viewModels()
    private val binding: FragmentHistoryBinding by lazy {
        FragmentHistoryBinding.inflate(layoutInflater)
    }

    private var mSelectedDate: LocalDate? = null


    //--------------------------------------------------------------------------------------------
    // MARK : Bind Area
    //--------------------------------------------------------------------------------------------
    //--------------------------------------------------------------------------------------------
    // MARK : Local variables
    //--------------------------------------------------------------------------------------------

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_history, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bindViews()

    }

    /*override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(HistoryViewModel::class.java)
        // TODO: Use the ViewModel
    }*/

    private fun bindViews() {

        binding.historyRecyclerView.layoutManager = LinearLayoutManager(requireContext().applicationContext, LinearLayoutManager.VERTICAL, false)

        //adpter 작업 필요함
//        binding.historyRecyclerView.adapter = mHistoryAdapter

        val currentDate = LocalDate.now()
        val currentMonth = YearMonth.now()
        val startDate: LocalDate = currentMonth.minusMonths(100).atEndOfMonth()
        val endDate: LocalDate = currentMonth.plusMonths(100).atEndOfMonth()
        mSelectedDate = currentDate

        viewModel.sleepWeekData
        binding.weekCalendarView.dayBinder = object : WeekDayBinder<DayViewContainer> {
            override fun bind(container: DayViewContainer, data: WeekDay) {
                container.dateView.text = data.date.dayOfMonth.toString()

                container.dateLayout.setOnClickListener {
                    mSelectedDate = data.date
                    binding.weekCalendarView.notifyCalendarChanged()

                }

            }

            override fun create(view: View): DayViewContainer {
                return DayViewContainer(view)
            }
        }

        binding.weekCalendarView.setup(startDate, endDate, DayOfWeek.SUNDAY)
        binding.weekCalendarView.scrollToWeek(currentDate)
        binding.weekCalendarView.weekScrollListener = { week ->

            var firstDate = week.days[0].date
            var lastDate = week.days[week.days.size - 1].date
            if (firstDate.year == lastDate.year && firstDate.monthValue == lastDate.monthValue) {
                binding.dateTextView.text =  firstDate.year.toString() + "." + firstDate.monthValue.toString()
            } else {
                binding.dateTextView.text = firstDate.year.toString() + "." + firstDate.monthValue + " ~ " + lastDate.year + "." + lastDate.monthValue
            }
        }


//        binding.weekCalendarView.dayBinder(object : WeekDayBinder<DayViewContainer?> {
//            fun bind(container: DayViewContainer, weekDay: WeekDay) {
//                container.dateView.setText("" + weekDay.date.dayOfMonth)
//                container.dateLayout.setOnClickListener {
//                    mSelectedDate = weekDay.date
//                    binding.weekCalendarView.notifyCalendarChanged()
//                    sleepDataDetail()
//                }
//                val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
//                val dateString = weekDay.date.format(dateTimeFormatter)
//                container.progressCardView.layoutParams.height = UIHelper.getInstance().dp2px(mActivity, 0)
//                //
//                for (value in mDateList) {
//                    if (value.getDay().equals(dateString)) {
//                        if (value.getMinute() >= 900) {
//                            container.progressCardView.layoutParams.height = UIHelper.getInstance().dp2px(mActivity, 70f)
//                        } else {
//                            val height = value.getMinute() as Double / 900.0 * 70
//                            container.progressCardView.layoutParams.height = UIHelper.getInstance().dp2px(mActivity, height.toFloat())
//                        }
//                        break
//                    }
//                }
//                container.progressCardView.requestLayout()
//
//                // 선택 시
//                if (weekDay.date.isEqual(mSelectedDate)) {
//                    container.dayView.setTextColor(mActivity.getColor(R.color.color_4482CC))
//                    container.calendarCardView.setCardBackgroundColor(mActivity.getColor(R.color.color_FFFFFF))
//                } else {
//                    container.dayView.setTextColor(mActivity.getColor(R.color.color_FFFFFF))
//                    container.calendarCardView.setCardBackgroundColor(mActivity.getColor(R.color.clear))
//                }
//            }
//
//            override fun create(view: View): DayViewContainer {
//                return DayViewContainer(view)
//            }
//        })

    }

}