package io.novafoundation.nova.feature_account_impl.data.signer.multisig

import io.novafoundation.nova.common.base.errors.SigningCancelledException
import io.novafoundation.nova.common.data.memory.SingleValueCache
import io.novafoundation.nova.common.data.network.runtime.binding.WeightV2
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicSplitter
import io.novafoundation.nova.feature_account_api.data.multisig.composeMultisigAsMulti
import io.novafoundation.nova.feature_account_api.data.signer.CallExecutionType
import io.novafoundation.nova.feature_account_api.data.signer.NovaSigner
import io.novafoundation.nova.feature_account_api.data.signer.SignerProvider
import io.novafoundation.nova.feature_account_api.data.signer.SigningContext
import io.novafoundation.nova.feature_account_api.data.signer.intersect
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.MultisigMetaAccount
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
// 1. support threshold 1 multisigs (including weight estimation upon submission)
// 2. certain operations cannot execute multisig (in general - CallExecutionType.DELAYED). We should add corresponding checks and validations
// Example: 1 click swaps
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

    override suspend fun callExecutionType(): CallExecutionType {
        val selfExecutionType = CallExecutionType.DELAYED
        return delegateSigner().callExecutionType().intersect(selfExecutionType)
    }

    override suspend fun submissionSignerAccountId(chain: Chain): AccountId {
        return delegateSigner().submissionSignerAccountId(chain)
    }

    context(ExtrinsicBuilder)
    override suspend fun setSignerDataForSubmission(context: SigningContext) {
        delegateSigner().setSignerDataForSubmission(context)

        wrapCallsInAsMultiForSubmission()

        // TODO multisig: implement acknowledge and validation
        // 1. Balance is enough
        // 2. There is no pending mst with this exact call
//        if (isRootSigner) {
//            acknowledgeProxyOperation(signatoryMetaAccount())
//            validateExtrinsic(context.chain)
//        }
    }

    context(ExtrinsicBuilder)
    override suspend fun setSignerDataForFee(context: SigningContext) {
        delegateSigner().setSignerDataForFee(context)

        wrapCallsInProxyForFee()
    }

    override suspend fun signRaw(payload: SignerPayloadRaw): SignedRaw {
        // TODO multisig: raw signing not supported bottom sheet
        throw SigningCancelledException()
    }

    override suspend fun maxCallsPerTransaction(): Int? {
        return delegateSigner().maxCallsPerTransaction()
    }

    context(ExtrinsicBuilder)
    private fun wrapCallsInAsMultiForSubmission() {
        // We do not calculate precise max_weight as it is only needed for the final approval
        return wrapCallsInAsMulti(maxWeight = WeightV2.zero())
    }

    context(ExtrinsicBuilder)
    private fun wrapCallsInProxyForFee() {
        wrapCallsInAsMulti(maxWeight = WeightV2.zero())
    }

    context(ExtrinsicBuilder)
    private fun wrapCallsInAsMulti(maxWeight: WeightV2) {
        val call = getWrappedCall()

        val multisigCall = runtime.composeMultisigAsMulti(
            threshold = multisigAccount.threshold,
            otherSignatories = multisigAccount.otherSignatories,
            maybeTimePoint = null,
            call = call,
            maxWeight = maxWeight
        )

        resetCalls()
        call(multisigCall)
    }

    private suspend fun computeSignatoryMetaAccount(): MetaAccount {
        return accountRepository.getMetaAccount(multisigAccount.signatoryMetaId)
    }
}
