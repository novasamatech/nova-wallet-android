package io.novafoundation.nova.feature_account_impl.data.signer.paritySigner.multiFrame

import io.novafoundation.nova.common.utils.toByteArray
import java.nio.ByteOrder

object LegacyMultiPart {

    private val MULTI_FRAME_TYPE: ByteArray = byteArrayOf(0x00)

    fun createSingle(
        payload: ByteArray,
    ): ByteArray {
        val frameCount: Short = 1
        val frameIndex: Short = 0

        return MULTI_FRAME_TYPE +
            frameCount.toByteArray(ByteOrder.BIG_ENDIAN) +
            frameIndex.toByteArray(ByteOrder.BIG_ENDIAN) +
            payload
    }
}
