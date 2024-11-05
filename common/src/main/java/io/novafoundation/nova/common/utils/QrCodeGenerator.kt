package io.novafoundation.nova.common.utils

import android.graphics.Bitmap
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.google.zxing.qrcode.encoder.Encoder
import com.google.zxing.qrcode.encoder.QRCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class QrCodeGenerator(
    private val firstColor: Int,
    private val secondColor: Int
) {

    companion object {
        private const val RECEIVE_QR_SCALE_SIZE = 1024
        private const val PADDING_SIZE = 2

        // Max binary payload length with ErrorCorrectionLevel.H is 1273 bytes however nginx still fails with this amount
        // With 1000 it works well
        // See https://stackoverflow.com/a/11065449
        const val MAX_PAYLOAD_LENGTH = 512
    }

    fun generateQrCode(input: String): QRCode {
        val hints = HashMap<EncodeHintType, String>()
        return Encoder.encode(input, ErrorCorrectionLevel.H, hints)
    }

    suspend fun generateQrBitmap(input: String): Bitmap {
        return withContext(Dispatchers.Default) {
            val qrCode = generateQrCode(input)
            val byteMatrix = qrCode.matrix
            val width = byteMatrix.width + PADDING_SIZE
            val height = byteMatrix.height + PADDING_SIZE
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            for (y in 0 until height) {
                for (x in 0 until width) {
                    if (y == 0 || y > byteMatrix.height || x == 0 || x > byteMatrix.width) {
                        bitmap.setPixel(x, y, secondColor)
                    } else {
                        bitmap.setPixel(x, y, if (byteMatrix.get(x - PADDING_SIZE / 2, y - PADDING_SIZE / 2).toInt() == 1) firstColor else secondColor)
                    }
                }
            }
            Bitmap.createScaledBitmap(bitmap, RECEIVE_QR_SCALE_SIZE, RECEIVE_QR_SCALE_SIZE, false)
        }
    }
}
