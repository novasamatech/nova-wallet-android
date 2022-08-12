package io.novafoundation.nova.feature_account_impl.data.signer.paritySigner.uos

object UOS {

    interface UOSPreludeValue {
        val value: Byte
    }

    fun createUOSPayload(
        payload: ByteArray,
        contentCode: UOSPreludeValue,
        cryptoCode: UOSPreludeValue,
        payloadCode: UOSPreludeValue
    ): ByteArray {
        return byteArrayOf(contentCode.value, cryptoCode.value, payloadCode.value) + payload
    }
}

class SimpleUOSPreludeValue(override val value: Byte) : UOS.UOSPreludeValue
