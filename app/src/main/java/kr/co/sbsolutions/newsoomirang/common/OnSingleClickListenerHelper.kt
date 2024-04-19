package kr.co.sbsolutions.newsoomirang.common

import android.os.SystemClock
import android.util.Log
import android.view.View
import kr.co.sbsolutions.newsoomirang.common.Cons.ON_CLICK_INTERVAL
import kr.co.sbsolutions.newsoomirang.common.Cons.TAG

class OnSingleClickListenerHelper (
    private val onclickListener: (view: View) -> Unit
) : View.OnClickListener {
    private var lastClickedTime = 0L
    
    override fun onClick(v: View?) {
        
        //클릭시간
        val onClickedTime = SystemClock.elapsedRealtime()
        
        
        if ((onClickedTime - lastClickedTime) < ON_CLICK_INTERVAL) {
//            Log.d(TAG, "onClickedTime 연속 클릭 시간 : $onClickedTime - $lastClickedTime  ${onClickedTime-lastClickedTime} < $ON_CLICK_INTERVAL")
            return
        }
        
        lastClickedTime = onClickedTime
        v?.let { onclickListener.invoke(it) }.run {
//            Log.d(TAG, "onClick: 실행")
        }
    }
}