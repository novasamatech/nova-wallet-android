package io.novafoundation.nova.common.presentation.scan

import com.google.zxing.BarcodeFormat
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.journeyapps.barcodescanner.Decoder
import com.journeyapps.barcodescanner.DecoderFactory
import java.util.EnumMap

class AlternatingDecoderFactory(
    private val decodeFormats: Collection<BarcodeFormat>? = null,
    private val hints: Map<DecodeHintType, *>? = null,
    private val characterSet: String? = null,
) : DecoderFactory {

    override fun createDecoder(baseHints: Map<DecodeHintType, *>): Decoder {
        val allHints: MutableMap<DecodeHintType, Any?> = EnumMap(DecodeHintType::class.java)

        allHints.putAll(baseHints)

        hints?.let(allHints::putAll)

        decodeFormats?.let {
            allHints[DecodeHintType.POSSIBLE_FORMATS] = decodeFormats
        }

        characterSet?.let {
            allHints[DecodeHintType.CHARACTER_SET] = characterSet
        }

        val reader = MultiFormatReader().apply { setHints(allHints) }

        return AlternatingDecoder(reader)
    }
}
