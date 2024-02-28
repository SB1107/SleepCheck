package kr.co.sbsolutions.newsoomirang

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
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }
    @Test
    fun main() {


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

        data

        val prifx = "30B07567"
        if (prifx != ) {

        }



        val ivData = BigInteger(test, 16).toByteArray()

        println("ㅇㅇ$ivData") // 출력: "[0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15]"


    }
}