package kr.co.sbsolutions.newsoomirang.common

import android.util.Log
import kr.co.sbsolutions.newsoomirang.BuildConfig
import kr.co.sbsolutions.newsoomirang.common.Cons.TAG
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec


class AESHelper {

    private val ivData = (BuildConfig.ivData).hexToBytes()
    private val key = (BuildConfig.key).hexToBytes()

    fun encryptAES128(plainText: ByteArray): ByteArray {
        val tempByte = ByteArray(16).apply { fill(0.toByte(), 0, 16) }
        plainText.forEachIndexed { index, value ->
            tempByte[index] = value
            Log.d(TAG, "encryptAES128: ${tempByte[index]}")
        }
//        val byttte = ByteArray(16).fill(0,plainText.size, 0.toByte())
        val cipher = Cipher.getInstance("AES/CBC/NoPadding")
            .apply {
                init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), IvParameterSpec(ivData))
            }
        return cipher.doFinal(tempByte)
    }

    fun decryptAES128(encryptedText: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/CBC/NoPadding")
            .apply {
                init(
                    Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"),
                    IvParameterSpec(ivData)
                )
            }

        return cipher.doFinal(encryptedText)
    }
}