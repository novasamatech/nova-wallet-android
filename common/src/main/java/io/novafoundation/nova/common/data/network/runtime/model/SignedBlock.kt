package io.novafoundation.nova.common.data.network.runtime.model

import com.google.gson.annotations.SerializedName
import io.novafoundation.nova.common.utils.removeHexPrefix

class SignedBlock(val block: Block, val justification: Any?) {
    class Block(val extrinsics: List<String>, val header: Header) {
        class Header(@SerializedName("number") private val numberRaw: String, val parentHash: String?) {
            val number: Int
                get() {
                    return numberRaw.removeHexPrefix().toInt(radix = 16)
                }
        }
    }
}
