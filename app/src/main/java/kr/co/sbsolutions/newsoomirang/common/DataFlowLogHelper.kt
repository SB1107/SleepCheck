package kr.co.sbsolutions.newsoomirang.common

import android.util.Log
import kr.co.sbsolutions.newsoomirang.common.Cons.TAG

class DataFlowLogHelper(private val logHelper: LogHelper) {

    private var case1 : Int = 0
    private var case2 : Int = 0
    private var case3 : Int = 0
    private var case4 : Int = 0
    private var case5 : Int = 0

    fun countCase1() { case1 +=1 }
    fun countCase2() { case2 +=1 }
    fun countCase3() { case3 +=1 }
    fun countCase4() { case4 +=1 }
    fun countCase5() {
        case5 +=1
        Log.d(TAG, "countCase5: $case5")
    }

    fun onCaseLog(){
        logHelper.insertLog("case1: $case1 case2: $case2 case3: $case3 case4: $case4 case5: $case5")
    }
}