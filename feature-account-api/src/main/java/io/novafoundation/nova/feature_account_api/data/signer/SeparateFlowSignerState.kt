package io.novafoundation.nova.feature_account_api.data.signer

import io.novafoundation.nova.common.utils.MutableSharedState
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.runtime.extrinsic.signer.SignerPayloadRawWithChain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novasama.substrate_sdk_android.extensions.toHexString
import io.novasama.substrate_sdk_android.runtime.AccountId
import io.novasama.substrate_sdk_android.runtime.extrinsic.signer.SignerPayloadExtrinsic
import io.novasama.substrate_sdk_android.runtime.extrinsic.signer.encodedSignaturePayload
import io.novasama.substrate_sdk_android.runtime.extrinsic.signer.genesisHash

typealias SigningSharedState = MutableSharedState<SeparateFlowSignerState>

class SeparateFlowSignerState(val payload: SignerPayload, val metaAccount: MetaAccount)

sealed class SignerPayload {

    class Extrinsic(val extrinsic: SignerPayloadExtrinsic) : SignerPayload()

    class Raw(val raw: SignerPayloadRawWithChain) : SignerPayload()
}

fun SignerPayload.chainId(): ChainId {
    return when (this) {
        is SignerPayload.Extrinsic -> extrinsic.genesisHash.toHexString()
        is SignerPayload.Raw -> raw.chainId
    }
}

fun SignerPayload.accountId(): AccountId {
    return when (this) {
        is SignerPayload.Extrinsic -> extrinsic.accountId
        is SignerPayload.Raw -> raw.accountId
    }
}

fun SignerPayload.signaturePayload(): ByteArray {
    return when (this) {
        is SignerPayload.Extrinsic -> extrinsic.encodedSignaturePayload(hashBigPayloads = true)
        is SignerPayload.Raw -> raw.message
    }
}

fun SeparateFlowSignerState.requireExtrinsic(): SignerPayloadExtrinsic {
    require(payload is SignerPayload.Extrinsic)
    return payload.extrinsic
}
