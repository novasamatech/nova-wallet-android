package io.novafoundation.nova.common.presentation.scan

import com.google.zxing.BinaryBitmap
import com.google.zxing.LuminanceSource
import com.google.zxing.Reader
import com.google.zxing.common.HybridBinarizer
import com.journeyapps.barcodescanner.Decoder
import java.util.concurrent.atomic.AtomicInteger

class AlternatingDecoder(reader: Reader) : Decoder(reader) {

    private var counter = AtomicInteger(0)

    override fun toBitmap(source: LuminanceSource): BinaryBitmap {
        val nextCounter = counter.getAndIncrement()

        val updatedSource = if (nextCounter % 2 == 0) {
            source
        } else {
            source.invert()
        }

        return BinaryBitmap(HybridBinarizer(updatedSource))
    }
}
