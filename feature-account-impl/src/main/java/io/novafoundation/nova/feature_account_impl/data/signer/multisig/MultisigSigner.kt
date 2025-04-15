package io.novafoundation.nova.feature_account_impl.data.signer.multisig

import io.novafoundation.nova.common.base.errors.SigningCancelledException
import io.novafoundation.nova.common.data.memory.SingleValueCache
import io.novafoundation.nova.common.data.network.runtime.binding.WeightV2
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.utils.Modules
import io.novafoundation.nova.common.utils.composeCall
import io.novafoundation.nova.common.utils.getChainIdOrThrow
import io.novafoundation.nova.feature_account_api.data.signer.NovaSigner
import io.novafoundation.nova.feature_account_api.data.signer.SignerProvider
import io.novafoundation.nova.feature_account_api.data.signer.SigningContext
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.MultisigMetaAccount
import io.novafoundation.nova.feature_account_impl.data.extrinsic.ExtrinsicSplitter
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.runtime.AccountId
import io.novasama.substrate_sdk_android.runtime.extrinsic.builder.ExtrinsicBuilder
import io.novasama.substrate_sdk_android.runtime.extrinsic.signer.SignedRaw
import io.novasama.substrate_sdk_android.runtime.extrinsic.signer.SignerPayloadRaw
import javax.inject.Inject

@FeatureScope
class MultisigSignerFactory @Inject constructor(
    private val chainRegistry: ChainRegistry,
    private val accountRepository: AccountRepository,
    private val extrinsicSplitter: ExtrinsicSplitter,
) {

    fun create(metaAccount: MultisigMetaAccount, signerProvider: SignerProvider, isRoot: Boolean): MultisigSigner {
        return MultisigSigner(
            chainRegistry = chainRegistry,
            accountRepository = accountRepository,
            extrinsicSplitter = extrinsicSplitter,
            signerProvider = signerProvider,
            isRootSigner = isRoot,
            multisigAccount = metaAccount
        )
    }
}

// TODO multisig:
// 1. do not create history elements (e.g. transfers) for delayed operations. This could be done by introducing immediate / delayed call execution separation
// 2. support threshold 1 multisigs
class MultisigSigner(
    private val multisigAccount: MultisigMetaAccount,
    private val chainRegistry: ChainRegistry,
    private val accountRepository: AccountRepository,
    private val signerProvider: SignerProvider,
    private val extrinsicSplitter: ExtrinsicSplitter,
    private val isRootSigner: Boolean,
) : NovaSigner {

    override val metaAccount = multisigAccount

    private val signatoryMetaAccount = SingleValueCache {
        computeSignatoryMetaAccount()
    }

    private val delegateSigner = SingleValueCache {
        signerProvider.nestedSignerFor(signatoryMetaAccount())
    }

    override suspend fun submissionSignerAccountId(chain: Chain): AccountId {
        return delegateSigner().submissionSignerAccountId(chain)
    }

    context(ExtrinsicBuilder)
    override suspend fun setSignerDataForSubmission(context: SigningContext) {
        wrapCallsInAsMultiForSubmission()

        delegateSigner().setSignerDataForSubmission(context)

        // TODO multisig: implement acknowledge and validation
//        if (isRootSigner) {
//            acknowledgeProxyOperation(signatoryMetaAccount())
//            validateExtrinsic(context.chain)
//        }
    }

    context(ExtrinsicBuilder)
    override suspend fun setSignerDataForFee(context: SigningContext) {
        wrapCallsInProxyForFee()

        delegateSigner().setSignerDataForFee(context)
    }

    override suspend fun signRaw(payload: SignerPayloadRaw): SignedRaw {
        // TODO multisig: raw signing not supported bottom sheet
        throw SigningCancelledException()
    }

    override suspend fun maxCallsPerTransaction(): Int? {
        return delegateSigner().maxCallsPerTransaction()
    }

    context(ExtrinsicBuilder)
    private suspend fun wrapCallsInAsMultiForSubmission() {
        val chainId = getChainIdOrThrow()
        val chain = chainRegistry.getChain(chainId)
        val call = getWrappedCall()
        val weight = extrinsicSplitter.estimateCallWeight(delegateSigner(), call, chain)

        return wrapCallsInAsMulti(maxWeight = weight)
    }

    // Wrap without verifying proxy permissions and hardcode proxy type
    // to speed up fee calculation
    context(ExtrinsicBuilder)
    private fun wrapCallsInProxyForFee() {
        wrapCallsInAsMulti(maxWeight = WeightV2.zero())
    }

    context(ExtrinsicBuilder)
    private fun wrapCallsInAsMulti(maxWeight: WeightV2) {
        val call = getWrappedCall()

        val multisigCall = runtime.composeCall(
            moduleName = Modules.MULTISIG,
            callName = "as_multi",
            arguments = mapOf(
                "threshold" to multisigAccount.threshold.toBigInteger(),
                "other_signatories" to multisigAccount.otherSignatories.sorted().map { it.value },
                "maybe_timepoint" to null,
                "call" to call,
                "max_weight" to maxWeight.toEncodableInstance()
            )
        )

        resetCalls()
        call(multisigCall)
    }

    private suspend fun computeSignatoryMetaAccount(): MetaAccount {
        return accountRepository.getMetaAccount(multisigAccount.signatoryMetaId)
    }
}
