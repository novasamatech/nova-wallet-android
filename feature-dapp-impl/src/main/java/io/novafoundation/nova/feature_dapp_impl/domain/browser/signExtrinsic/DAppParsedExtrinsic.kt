package io.novafoundation.nova.feature_dapp_impl.domain.browser.signExtrinsic

import jp.co.soramitsu.fearless_utils.runtime.definitions.types.generics.Era
import jp.co.soramitsu.fearless_utils.runtime.definitions.types.generics.GenericCall
import java.math.BigInteger

class DAppParsedExtrinsic(
    val address: String,
    val nonce: BigInteger,
    val specVersion: Int,
    val transactionVersion: Int,
    val genesisHash: ByteArray,
    val era: Era,
    val blockHash: ByteArray,
    val tip: BigInteger,
    val call: GenericCall.Instance
)
