package kr.co.sbsolutions.newsoomirang

import android.annotation.SuppressLint
import android.util.Log
import android.widget.RelativeLayout
import androidx.appcompat.widget.AppCompatTextView
import com.google.android.material.card.MaterialCardView
import kr.co.sbsolutions.newsoomirang.common.Cons
import kr.co.sbsolutions.newsoomirang.common.Cons.TAG
import kr.co.sbsolutions.newsoomirang.common.toDp2Px
import kr.co.sbsolutions.newsoomirang.common.toHourMinute
import org.junit.Test

import org.junit.Assert.*
import java.math.BigInteger
import java.nio.ByteBuffer

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {

    @Test
    fun getChangeDeviceName() {
        val bleName = "APnb-0101"
        val nameCheck = bleName.contains("H")
        val bleNumber = bleName.split("-").last()
        val name = if (!nameCheck)"숨이랑 - $bleNumber" else "HSMD - $bleNumber"
        println(name)
    }

    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }
    @Test
    fun main() {
        assertEquals("1",String.format("%.1f", ) + "%")


    }
}