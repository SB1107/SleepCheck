package kr.co.sbsolutions.newsoomirang.common

import org.junit.Assert
import org.junit.Test
import java.nio.charset.StandardCharsets

class AESHelperTest{

    @Test
    fun `암호화 테스트`(){
//        val item = "30 B0 75 67 46 56 D9 D3 CE 3E 70 A1 8A 1D 3F EE".replace("","").toByteArray()
//        val a = AESHelper().decryptAES128(item)
         val key = byteArrayOf(
            0x2B.toByte(),
            0x7E.toByte(),
            0x15.toByte(),
            0x16.toByte(),
            0x28.toByte(),
            0xAE.toByte(),
            0xD2.toByte(),
            0xA6.toByte(),
            0xAB.toByte(),
            0xF7.toByte(),
            0x15.toByte(),
            0x88.toByte(),
            0x09.toByte(),
            0xCF.toByte(),
            0x4F.toByte(),
            0x3C.toByte()
        )
        Assert.assertEquals(String(key, StandardCharsets.UTF_8) , "12")
    }
}