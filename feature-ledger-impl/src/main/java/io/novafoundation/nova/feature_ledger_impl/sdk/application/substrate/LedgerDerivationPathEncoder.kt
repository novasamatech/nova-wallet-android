package io.novafoundation.nova.feature_ledger_impl.sdk.application.substrate

import jp.co.soramitsu.fearless_utils.encrypt.junction.Junction

fun List<Junction>.encodeToByteArray(): ByteArray = fold(ByteArray(0)) { acc, junction ->
    // Bip32Encoder currently encodes chain codes as big endian, so we need to reverse them to get little endian encoding
    // TODO add this ability to library
    acc + junction.chaincode.reversedArray()
}
