package io.novafoundation.nova.feature_account_impl.data.signer.paritySigner.multiFrame

import io.novafoundation.nova.common.utils.toByteArray
import java.nio.ByteOrder

object LegacyMultiPart {

    private val MULTI_FRAME_TYPE: ByteArray = byteArrayOf(0x00)

    fun createSingle(payload: ByteArray): ByteArray {
        val frameCount: Short = 1
        val frameIndex: Short = 0

        return MULTI_FRAME_TYPE +
            frameCount.encodeMultiPartNumber() +
            frameIndex.encodeMultiPartNumber() +
            payload
    }

    fun createMultiple(payloads: List<ByteArray>): List<ByteArray> {
        val frameCount = payloads.size

        val prefix = MULTI_FRAME_TYPE + frameCount.encodeMultiPartNumber()

        return payloads.mapIndexed { index, payload ->
            prefix + index.encodeMultiPartNumber() + payload
        }
    }

    private fun Number.encodeMultiPartNumber(): ByteArray = toShort().toByteArray(ByteOrder.BIG_ENDIAN)
}
