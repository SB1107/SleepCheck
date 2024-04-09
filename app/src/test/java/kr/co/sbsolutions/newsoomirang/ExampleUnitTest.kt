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
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {

    @Test
    fun getChangeDeviceName() {
        val bleName = "ACa-0101"
        val nameCheck = bleName.contains("ABH") or bleName.contains("ACH")

        val bleNumber = bleName.split("-").last()
        val name = if (!nameCheck)"숨이랑 - $bleNumber" else "HSMD - $bleNumber"
        println(name)
    }

    @Test
    fun testtest() {
        val time = "2024-04-05 12:17:56.768"
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")
        val t = dateFormat.parse(time)
        val time2 = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(t.time)


        if (t != null) {
            println("time = ${t.time}")
            println("time = ${time2}")

        }
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