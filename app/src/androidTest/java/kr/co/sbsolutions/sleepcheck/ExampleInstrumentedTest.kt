package kr.co.sbsolutions.sleepcheck

import android.annotation.SuppressLint
import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import kr.co.sbsolutions.sleepcheck.common.AESHelper
import kr.co.sbsolutions.sleepcheck.common.Cons
import kr.co.sbsolutions.sleepcheck.common.Cons.TAG
import kr.co.sbsolutions.sleepcheck.common.hexToString
import kr.co.sbsolutions.sleepcheck.common.prefixToHex
import kr.co.sbsolutions.sleepcheck.common.toDp2Px
import kr.co.sbsolutions.sleepcheck.common.toHourMinute

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {

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
    fun useAppContext() {
        // Context of the app under test.
//        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
//        val reulst = AESHelper().decryptAES128("30 B0 75 67 46 56 D9 D3 CE 3E 70 A1 8A 1D 3F EE".replace("","").toByteArray() )
         val data = byteArrayOf(
            0x30.toByte(),
            0xB0.toByte(),
            0x75.toByte(),
            0x67.toByte(),
            0x46.toByte(),
            0x56.toByte(),
            0xD9.toByte(),
            0xD3.toByte(),
            0xCE.toByte(),
            0x3E.toByte(),
            0x70.toByte(),
            0xA1.toByte(),
            0x8A.toByte(),
            0x1D.toByte(),
            0x3F.toByte(),
            0xEE.toByte()
        )
        val resultByte = byteArrayOf(
            0xFE.toByte(),
            0x9B.toByte(),
            0x80.toByte(),
            0x03.toByte(),
            0xFA.toByte(),
            0x01.toByte(),
            0x1E.toByte(),
            0xC7.toByte(),
            0x00.toByte(),
            0x00.toByte(),
            0x00.toByte(),
            0x00.toByte(),
            0x00.toByte(),
            0x00.toByte(),
            0x00.toByte(),
            0x00.toByte()
        )
        val decry = AESHelper().decryptAES128(data)
        val encry = AESHelper().encryptAES128(decry)

        Log.e("JJ", "복호화: ${decry.hexToString()}" )
        Log.e("JJ", "암호화: ${encry.hexToString()}" )

//        assertEquals("FE 9B 80 03 FA 01 1E C7 00 00 00 00 00 00 00", strBuilder.toString())
//        assertEquals(resultByte.toHexString(HexFormat.Default), reulst)
    }
    @Test
    fun `암호화테스트`(){


        val data = byteArrayOf(
            0x30.toByte(),
            0xB0.toByte(),
            0x75.toByte(),
            0x67.toByte(),
            0x46.toByte(),
            0x56.toByte(),
            0xD9.toByte(),
            0xD3.toByte(),
            0xCE.toByte(),
            0x3E.toByte(),
            0x70.toByte(),
            0xA1.toByte(),
            0x8A.toByte(),
            0x1D.toByte(),
            0x3F.toByte(),
            0xEE.toByte()
        )

        val prefix = data.hexToString().prefixToHex()
        assertEquals(prefix,"30B07567")

    }

    @Test
    fun main() {
        val positionVale = listOf(
            Pair(
                36,
                100
            ),
            Pair(
                0,
                0
            ),
            Pair(
                0,
                0
            ),
            Pair(
                0,
                0
            ),
            Pair(
                0,
                0
            )
        )

        isCheckSumVis(timeList = positionVale)
//        assertEquals("1",String.format("%.1f", 100) + "%")
    }

    @SuppressLint("SetTextI18n", "DefaultLocale")
    fun isCheckSumVis(
        totalTime: Int = 0, timeList: List<Pair<Int?, Int?>>, ): Boolean {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
//        if (totalTime == null) {
//            return false
////            Log.d(TAG, "isCheckSumVis: 1")
//        }
//        if (totalTime == list.filterNotNull().sum()) {
//            Log.d(TAG, "$totalTime == ${list.filterNotNull().sum()}")
        try {
            timeList.forEachIndexed { index, value ->
                value.let { (first, second) ->
                    Log.d(Cons.TAG, "first: $first")
                    Log.d(Cons.TAG, "second: $second")
                    Log.d(Cons.TAG, "$index value: $value")
                    val per = "$second %"
                    val min = (first?.times(60))?.toHourMinute()
                    val width = appContext.toDp2Px(((second?.toDouble() ?: 0.0) * 2).toFloat()).toInt()
                    Log.d(TAG, "per: $per ")
                    Log.d(TAG, "min: $min ")
                    Log.d(TAG, "width: $width ")
                }
            }
        } catch (e: Exception) {
            Log.d(Cons.TAG, "isCheckSumVis: 2 ${e.message}")
            return false
        }
        return true
    }
}