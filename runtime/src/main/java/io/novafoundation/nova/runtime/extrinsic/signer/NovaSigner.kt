package io.novafoundation.nova.runtime.extrinsic.signer

import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.runtime.AccountId
import io.novasama.substrate_sdk_android.runtime.extrinsic.signer.Signer
import io.novasama.substrate_sdk_android.runtime.extrinsic.signer.SignerPayloadExtrinsic

interface NovaSigner : Signer {

    suspend fun signerAccountId(chain: Chain): AccountId

    suspend fun modifyPayload(payloadExtrinsic: SignerPayloadExtrinsic): SignerPayloadExtrinsic
}

interface FeeSigner : NovaSigner {

    /**
     * In contrast with [signerAccountId] which for [FeeSigner] is supposed to return an account id derived from a fake keypair,
     * This method returns a real account id that later will sign the transaction we're calculating fee
     * This is useful for the client code to understand which account which actually pay the fee since it might differ from the requested account id
     */
    suspend fun actualFeeSignerId(chain: Chain): AccountId

    /**
     * Similar to [actualFeeSignerId] but returns accountId that was specified as the transaction origin
     * It might not be equal to [actualFeeSignerId] if [Signer] modifies the payload
     */
    suspend fun requestedFeeSignerId(chain: Chain): AccountId

    override suspend fun modifyPayload(payloadExtrinsic: SignerPayloadExtrinsic): SignerPayloadExtrinsic {
        throw NotImplementedError("This method should not be called")
    }
}
