package io.novafoundation.nova.runtime.multiNetwork.chain.model

import java.math.BigInteger

sealed interface StatemineAssetId {

    @JvmInline
    value class ScaleEncoded(val scaleHex: String) : StatemineAssetId

    @JvmInline
    value class Number(val value: BigInteger) : StatemineAssetId
}

fun StatemineAssetId.asNumberOrNull(): BigInteger? {
    return (this as? StatemineAssetId.Number)?.value
}


fun StatemineAssetId.asNumberOrThrow(): BigInteger {
    return (this as StatemineAssetId.Number).value
}
