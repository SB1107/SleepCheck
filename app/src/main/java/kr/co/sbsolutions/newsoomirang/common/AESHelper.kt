package kr.co.sbsolutions.newsoomirang.common

import android.util.Base64
import java.security.DigestException
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class AESHelper {
    private val iv = byteArrayOf(
        0x00.toByte(),
        0x01.toByte(),
        0x02.toByte(),
        0x03.toByte(),
        0x04.toByte(),
        0x05.toByte(),
        0x06.toByte(),
        0x07.toByte(),
        0x08.toByte(),
        0x09.toByte(),
        0x0A.toByte(),
        0x0B.toByte(),
        0x0C.toByte(),
        0x0D.toByte(),
        0x0E.toByte(),
        0x0F.toByte()
    )
//    private  val iv   = ByteArray(16)
private val key = byteArrayOf(
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
    fun encryptAES128(plainText: String): ByteArray {
        val cipher = Cipher.getInstance("AES/CBC/PKCS7Padding")
        val secretKey = SecretKeySpec(key, "AES")
        val spec = IvParameterSpec(iv)

        cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec)

        return cipher.doFinal(plainText.toByteArray())
    }

    fun decryptAES128(encryptedText: ByteArray): String {
        val cipher = Cipher.getInstance("AES/CBC/PKCS7Padding")
        val secretKey = SecretKeySpec(key, "AES")
        val spec = IvParameterSpec(iv)

        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

        return String(cipher.doFinal(encryptedText))
    }
//
    fun sha256(data: ByteArray): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(data)
    }
//    private fun hashSHA256(): ByteArray  {
//        val hash: ByteArray
//        try {
//            val md = MessageDigest.getInstance("SHA-256") // 길이에 따라 16Byte → AES128, 24Byte → AES192, 32Byte → AES256 로 처리된다고 함.
//
//
//            md.update(key)
//            hash = md.digest() // Hash 계산을 완료하여 Hash 값의 바이트 배열을 생성
//        } catch (e: CloneNotSupportedException) {
//            throw DigestException("couldn't make digest of partial content")
//        }
//        return hash
//    }
//
//    fun decrypt(value: String): String {
//        cipherInstance.init(Cipher.DECRYPT_MODE, keySpec, IvParameterSpec(iv))
//        val byteArrayDecText = Base64.decode(value, Base64.DEFAULT)
//        return String(cipherInstance.doFinal(byteArrayDecText))
//    }
//    fun encode(value : String): String {
//        cipherInstance.init(Cipher.ENCRYPT_MODE , keySpec , IvParameterSpec(iv))
//            val byteArrayEncodeText = Base64.encode(cipherInstance.doFinal(value.toByteArray()), Base64.DEFAULT)
//            return String(byteArrayEncodeText)
//    }
}