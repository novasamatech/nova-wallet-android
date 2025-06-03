package io.novafoundation.nova.runtime.extrinsic.signer

import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novasama.substrate_sdk_android.runtime.AccountId
import io.novasama.substrate_sdk_android.runtime.extrinsic.signer.SignedRaw
import io.novasama.substrate_sdk_android.runtime.extrinsic.signer.Signer
import io.novasama.substrate_sdk_android.runtime.extrinsic.signer.SignerPayloadExtrinsic
import io.novasama.substrate_sdk_android.runtime.extrinsic.signer.SignerPayloadRaw

interface NovaSigner : Signer {

    /**
     * Indicate whether it is possible to include CheckMetadataHash.Enabled into extrinsic
     * This method will become redundant once Ledger releases both Generic and Migration apps
     * After that, there wont be a need in additional check and runtime-based check will be enough
     */
    suspend fun supportsCheckMetadataHash(chain: Chain): Boolean {
        return true
    }

    suspend fun signerAccountId(chain: Chain): AccountId

    suspend fun modifyPayload(payloadExtrinsic: SignerPayloadExtrinsic): SignerPayloadExtrinsic

    // TODO this is a temp solution to workaround Polkadot Vault requiring chain id to sign a raw message
    // This method should be removed once Vault behavior is improved
    suspend fun signRawWithChain(payload: SignerPayloadRawWithChain): SignedRaw {
        return signRaw(payload.withoutChain())
    }
}

interface FeeSigner : NovaSigner {

    /**
     * Determines whether this particular instance of signer imposes additional limits to the number of calls
     * it is possible to add to a single transaction.
     * This is useful for signers that run in resource-constrained environment and thus cannot handle large transactions, e.g. Ledger
     */
    suspend fun maxCallsPerTransaction(): Int?

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

suspend fun NovaSigner.signRaw(payloadRaw: SignerPayloadRaw, chainId: ChainId?) : SignedRaw {
    return if (chainId != null) {
        signRawWithChain(payloadRaw.withChain(chainId))
    } else {
        signRaw(payloadRaw)
    }
}
