package io.novafoundation.nova.common.utils

import android.util.Base64
import io.novasama.substrate_sdk_android.hash.Hasher.blake2b256
import org.bouncycastle.jcajce.provider.digest.SHA256
import org.bouncycastle.jcajce.provider.digest.SHA512
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

fun String.hmacSHA256(secret: String): ByteArray {
    val chiper: Mac = Mac.getInstance("HmacSHA256")
    val secretKeySpec = SecretKeySpec(secret.toByteArray(), "HmacSHA256")
    chiper.init(secretKeySpec)

    return chiper.doFinal(this.toByteArray())
}

fun ByteArray.substrateAccountId(): ByteArray {
    return if (size > 32) {
        this.blake2b256()
    } else {
        this
    }
}

fun ByteArray.sha512(): ByteArray {
    val digits = SHA512.Digest()
    return digits.digest(this)
}

fun ByteArray.sha256(): ByteArray {
    val digest = SHA256.Digest()

    return digest.digest(this)
}

fun String.md5(): String {
    val hasher = MessageDigest.getInstance("MD5")

    return hasher.digest(encodeToByteArray()).decodeToString()
}

fun ByteArray.md5(): String {
    val hasher = MessageDigest.getInstance("MD5")

    return hasher.digest(this).decodeToString()
}

fun ByteArray.toBase64() = Base64.encodeToString(this, Base64.NO_WRAP)
