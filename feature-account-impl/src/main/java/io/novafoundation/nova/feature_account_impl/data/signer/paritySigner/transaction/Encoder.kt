package io.novafoundation.nova.feature_account_impl.data.signer.paritySigner.transaction

import jp.co.soramitsu.fearless_utils.runtime.extrinsic.signer.SignerPayloadExtrinsic
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.signer.encodedCallData
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.signer.encodedExtensions
import jp.co.soramitsu.fearless_utils.runtime.extrinsic.signer.genesisHash
import jp.co.soramitsu.fearless_utils.scale.dataType.compactInt
import jp.co.soramitsu.fearless_utils.scale.dataType.toByteArray

fun SignerPayloadExtrinsic.paritySignerTxPayload(): ByteArray {
    return accountId + transientCallData() + encodedExtensions() + genesisHash
}

private fun SignerPayloadExtrinsic.transientCallData(): ByteArray {
    val encodedCallData = encodedCallData()
    val encodedCallSize = encodedCallData.size.toBigInteger()
    val encodedCallCompact = compactInt.toByteArray(encodedCallSize)

    return encodedCallCompact + encodedCallData
}
