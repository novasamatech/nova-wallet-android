package io.novafoundation.nova.feature_account_impl.data.signer

import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.requireAccountIdIn
import io.novafoundation.nova.runtime.extrinsic.signer.NovaSigner
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.runtime.AccountId
import io.novasama.substrate_sdk_android.runtime.extrinsic.signer.SignerPayloadExtrinsic

abstract class LeafSigner(
    private val metaAccount: MetaAccount,
) : NovaSigner {

    override suspend fun signerAccountId(chain: Chain): AccountId {
        return metaAccount.requireAccountIdIn(chain)
    }

    override suspend fun modifyPayload(payloadExtrinsic: SignerPayloadExtrinsic): SignerPayloadExtrinsic {
        return payloadExtrinsic
    }
}
