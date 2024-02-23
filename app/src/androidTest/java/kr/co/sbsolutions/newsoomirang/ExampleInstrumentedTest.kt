package kr.co.sbsolutions.newsoomirang

import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import kr.co.sbsolutions.newsoomirang.common.AESHelper
import kr.co.sbsolutions.newsoomirang.common.hexToString
import kr.co.sbsolutions.newsoomirang.common.prefixToHex

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
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
}