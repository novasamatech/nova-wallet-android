package io.novafoundation.nova.feature_account_api.data.signer

import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.runtime.AccountId
import io.novasama.substrate_sdk_android.runtime.extrinsic.builder.ExtrinsicBuilder
import io.novasama.substrate_sdk_android.runtime.extrinsic.signer.Signer

interface NovaSigner : Signer {

    /**
     * Meta account this signer was created for
     * This is the same value that was passed to [SignerProvider.rootSignerFor] or [SignerProvider.nestedSignerFor]
     */
    val metaAccount: MetaAccount

    /**
     * Modify the extrinsic to enrich it with the signing data relevant for this type
     * In all situations, at least nonce and signature will be required
     * However some signers may even modify the call (e.g. Proxied signer will wrap the current call into proxy call)
     *
     * This should only be called after all other extrinsic information has been set, including all non-signer related extensions and calls
     * So, this should be the final operation that modifies the extrinsic, followed just by [ExtrinsicBuilder.buildExtrinsic]
     */
    context(ExtrinsicBuilder)
    suspend fun setSignerData(context: SigningContext)

    /**
     * Same as [setSignerData] but should use fake signature so signed extrinsic can be safely used for fee calculation
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
    suspend fun actualSignerAccountId(chain: Chain): AccountId

    /**
     * Determines whether this particular instance of signer imposes additional limits to the number of calls
     * it is possible to add to a single transaction.
     * This is useful for signers that run in resource-constrained environment and thus cannot handle large transactions, e.g. Ledger
     */
    suspend fun maxCallsPerTransaction(): Int?
}
