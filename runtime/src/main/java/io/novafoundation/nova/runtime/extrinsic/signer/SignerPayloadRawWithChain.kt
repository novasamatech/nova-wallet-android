package io.novafoundation.nova.runtime.extrinsic.signer

import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novasama.substrate_sdk_android.runtime.AccountId
import io.novasama.substrate_sdk_android.runtime.extrinsic.signer.SignerPayloadRaw

class SignerPayloadRawWithChain(
    val chainId: ChainId,
    val message: ByteArray,
    val accountId: AccountId,
)

fun SignerPayloadRawWithChain.withoutChain(): SignerPayloadRaw {
    return SignerPayloadRaw(message, accountId)
}

fun SignerPayloadRaw.withChain(chainId: ChainId): SignerPayloadRawWithChain {
    return SignerPayloadRawWithChain(chainId = chainId, message = message, accountId = accountId)
}
