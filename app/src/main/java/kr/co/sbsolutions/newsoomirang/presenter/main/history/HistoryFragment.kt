package kr.co.sbsolutions.newsoomirang.presenter.main.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.kizitonwose.calendar.core.WeekDay
import com.kizitonwose.calendar.view.WeekDayBinder
import dagger.hilt.android.AndroidEntryPoint
import kr.co.sbsolutions.newsoomirang.R
import kr.co.sbsolutions.newsoomirang.databinding.FragmentHistoryBinding
import java.time.format.DateTimeFormatter

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

        viewModel.sleepWeekData
        /*binding.weekCalendarView.dayBinder = object : WeekDayBinder<DayViewContainer> {
            override fun bind(container: DayViewContainer, data: WeekDay) {
                TODO("Not yet implemented")


            }

            override fun create(view: View): DayViewContainer {
                TODO("Not yet implemented")
            }
        }*/

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