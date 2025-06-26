package io.novafoundation.nova.feature_account_api.data.signer

import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.runtime.extrinsic.signer.SignerPayloadRawWithChain
import io.novafoundation.nova.runtime.extrinsic.signer.withChain
import io.novafoundation.nova.runtime.extrinsic.signer.withoutChain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novasama.substrate_sdk_android.runtime.AccountId
import io.novasama.substrate_sdk_android.runtime.extrinsic.builder.ExtrinsicBuilder
import io.novasama.substrate_sdk_android.runtime.extrinsic.signer.SignedRaw
import io.novasama.substrate_sdk_android.runtime.extrinsic.signer.Signer
import io.novasama.substrate_sdk_android.runtime.extrinsic.signer.SignerPayloadRaw

interface NovaSigner : Signer {

    /**
     * Determines execution type of the actual call (e.g. transfer)
     * Implementation node: signers that delegate signing to nested signers should intersect their execution type with the nested one
     */
    suspend fun callExecutionType(): CallExecutionType

    /**
     * Meta account this signer was created for
     * This is the same value that was passed to [SignerProvider.rootSignerFor] or [SignerProvider.nestedSignerFor]
     */
    val metaAccount: MetaAccount

    /**
     * Returns full signing hierarchy for root and nested signers
     */
    suspend fun getSigningHierarchy(): SubmissionHierarchy

    /**
     * Modify the extrinsic to enrich it with the signing data relevant for this type
     * In all situations, at least nonce and signature will be required
     * However some signers may even modify the call (e.g. Proxied signer will wrap the current call into proxy call)
     *
     * This should only be called after all other extrinsic information has been set, including all non-signer related extensions and calls
     * So, this should be the final operation that modifies the extrinsic, followed just by [ExtrinsicBuilder.buildExtrinsic]
     */
    context(ExtrinsicBuilder)
    suspend fun setSignerDataForSubmission(context: SigningContext)

    /**
     * Same as [setSignerDataForSubmission] but should use fake signature so signed extrinsic can be safely used for fee calculation
     * This may also apply certain optimizations like hard-coding the nonce or other values to speedup the extrinsic construction
     * and thus, fee calculation
     *
     * This should only be called after all other extrinsic information has been set, including all non-signer related extensions and calls
     * So, this should be the final operation that modifies the extrinsic, followed just by [ExtrinsicBuilder.buildExtrinsic]
     */
    context(ExtrinsicBuilder)
    suspend fun setSignerDataForFee(context: SigningContext)

    /**
     * Return accountId of a signer that will actually sign this extrinsic
     * For example, for Proxied account the actual signer is its Proxy
     */
    suspend fun submissionSignerAccountId(chain: Chain): AccountId

    /**
     * Determines whether this particular instance of signer imposes additional limits to the number of calls
     * it is possible to add to a single transaction.
     * This is useful for signers that run in resource-constrained environment and thus cannot handle large transactions, e.g. Ledger
     */
    suspend fun maxCallsPerTransaction(): Int?

    // TODO this is a temp solution to workaround Polkadot Vault requiring chain id to sign a raw message
    // This method should be removed once Vault behavior is improved
    suspend fun signRawWithChain(payload: SignerPayloadRawWithChain): SignedRaw {
        return signRaw(payload.withoutChain())
    }
}

context(ExtrinsicBuilder)
suspend fun NovaSigner.setSignerData(context: SigningContext, mode: SigningMode) {
    when (mode) {
        SigningMode.FEE -> setSignerDataForFee(context)
        SigningMode.SUBMISSION -> setSignerDataForSubmission(context)
    }
}

suspend fun NovaSigner.signRaw(payloadRaw: SignerPayloadRaw, chainId: ChainId?): SignedRaw {
    return if (chainId != null) {
        signRawWithChain(payloadRaw.withChain(chainId))
    } else {
        signRaw(payloadRaw)
    }
}
