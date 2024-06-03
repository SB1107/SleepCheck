package kr.co.sbsolutions.sleepcheck.presenter.main.history

import android.view.View
import com.kizitonwose.calendar.view.ViewContainer
import kr.co.sbsolutions.sleepcheck.databinding.CalendarDayLayoutBinding

class DayViewContainer(view: View) : ViewContainer(view) {

    var dateView = CalendarDayLayoutBinding.bind(view).calendarDayText
    var progressCardView = CalendarDayLayoutBinding.bind(view).progressCardView
    var calendarCardView = CalendarDayLayoutBinding.bind(view).calendarCardView
    var dateLayout = CalendarDayLayoutBinding.bind(view).dateLayout



    /*var dayView: AppCompatTextView? = null
    var progressCardView: MaterialCardView? = null
    var calendarCardView: MaterialCardView? = null
    var dateLayout: RelativeLayout? = null

    fun DayViewContainer(view: View) {
        super(view)
        dayView = view.findViewById<AppCompatTextView>(R.id.calendarDayText)
        progressCardView = view.findViewById<MaterialCardView>(R.id.progressCardView)
        calendarCardView = view.findViewById<MaterialCardView>(R.id.calendarCardView)
        dateLayout = view.findViewById<RelativeLayout>(R.id.date_layout)
    }*/
}