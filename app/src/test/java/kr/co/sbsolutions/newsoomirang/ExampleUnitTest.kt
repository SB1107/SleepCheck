package kr.co.sbsolutions.newsoomirang

import android.annotation.SuppressLint
import android.util.Log
import android.widget.RelativeLayout
import androidx.appcompat.widget.AppCompatTextView
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.flow.first
import kr.co.sbsolutions.newsoomirang.common.Cons
import kr.co.sbsolutions.newsoomirang.common.Cons.TAG
import kr.co.sbsolutions.newsoomirang.common.toDp2Px
import kr.co.sbsolutions.newsoomirang.common.toHourMinute
import kr.co.sbsolutions.soomirang.db.SBSensorData
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
    fun breathingScore() {
        val apneaCount: Int = 50
        val noSeringTime: Int = 120
        val apneaTime: Int = 20
        val sleepTime: Int = 300
        var resultScore =
            (60 - ((apneaCount.toFloat() / apneaTime.toFloat()) * 2)) +
                    (30 - ((noSeringTime.toFloat() / apneaTime.toFloat()) * 30)) +
                    (10 - ((apneaTime.toFloat() / apneaTime.toFloat()) * 30))
        
        when {
            resultScore <= 10 -> resultScore = 10F
            resultScore >= 90 -> resultScore = 90F
            else -> "-"
        }
        println("${resultScore.toInt()}")
    }
    
    @Test
    fun noseringScore() {
        val noSeringTime: Int = 350
        val sleepTime: Int = 400
        
        var resultScore = (100 - ((noSeringTime.toFloat() / sleepTime.toFloat()) * 100))
        
        if (10 > resultScore){
            resultScore = 10F
        }
        
        println("$resultScore.toI")
    }
    
    @Test
    fun calculateSleepScore() {
        val noSeringTime: Int = 380
        val sleepTime: Int = 400
        // 1. 코골이 시간과 수면 시간의 유효성 검사
        if (noSeringTime < 0 || sleepTime <= 0) {
            throw IllegalArgumentException("코골이 시간과 수면 시간은 0보다 커야 합니다.")
        }
        
        // 2. 코골이 점수 계산
        val snoringScore = 100 - ((noSeringTime.toFloat() / sleepTime.toFloat()) * 100)
        
        // 3. 최소/최대 점수 결정
        val minScore = 10
        val maxScore = 100
        val score = minScore.coerceAtLeast(Math.min(maxScore, snoringScore.toInt()))
        
        println(score)
    }
    
    
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
    fun listTest(){
//        "1","2024-04-15 18:27:20.713","4274312","0.0156","-0.0156","-0.9828","10690"
//        "2","1970-01-01 09:00:00.400","4273402","0","-0.0312","-0.9672","10690"
//        "3","2024-04-15 18:27:21.113","4273358","0","-0.0156","-0.9828","10690"
//        "4","1970-01-01 09:00:00.800","4274466","0.0156","-0.0156","-0.9828","10690"
//        "5","1970-01-01 09:00:01.000","4272668","0.0156","0","-0.9828","10690"
//        "6","2024-04-15 18:27:21.713","4272744","0","0","-0.9828","10690"
//        "7","1970-01-01 09:00:01.400","4273632","0.0156","-0.0156","-0.9828","10690"
//        "8","2024-04-15 18:27:22.113","4268072","0.0156","-0.0156","-0.9672","10690"
//        "9","1970-01-01 09:00:01.800","4277215","0.0156","0","-0.9828","10690"
        val firstData =SBSensorData(index = 1 , time = "2024-04-15 18:27:20.713",
            capacitance = 4274312,calcAccX ="0.0156" ,calcAccY = "-0.0156", calcAccZ = "-0.9828", dataId = 10690)
        val orignalList = listOf(
            firstData,
            SBSensorData(index = 2 , time = "1970-01-01 09:00:00.400",
                capacitance = 4273402,calcAccX ="0.0156" ,calcAccY = "-0.0156", calcAccZ = "-0.9828", dataId = 10690),
            SBSensorData(index = 3 , time = "2024-04-15 18:27:21.113",
                capacitance = 4273358,calcAccX ="0.0156" ,calcAccY = "-0.0156", calcAccZ = "-0.9828", dataId = 10690),
            SBSensorData(index = 4 , time = "1970-01-01 09:00:00.800",
                capacitance = 4274466,calcAccX ="0.0156" ,calcAccY = "-0.0156", calcAccZ = "-0.9828", dataId = 10690),
            SBSensorData(index = 5 , time = "1970-01-01 09:00:01.00",
                capacitance = 4272668,calcAccX ="0.0156" ,calcAccY = "-0.0156", calcAccZ = "-0.9828", dataId = 10690),
            SBSensorData(index = 6 , time = "2024-04-15 18:27:21.713",
                capacitance = 4272744,calcAccX ="0.0156" ,calcAccY = "-0.0156", calcAccZ = "-0.9828", dataId = 10690))
        val sbList = orignalList
            .map {  if(it.time.contains("1970")){
                val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
                val newTime = firstData?.time?.let { format.parse(it) }
                val time1 = format.format((newTime?.time ?: 0) + (200 * it.index))
                it.time = time1
                it
            }else it }
        sbList.map {
            it.toArray().map {
                println("${it}")
            }

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