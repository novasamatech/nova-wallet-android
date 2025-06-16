package io.novafoundation.nova.feature_account_impl.data.signer.multisig

import io.novafoundation.nova.common.base.errors.SigningCancelledException
import io.novafoundation.nova.common.data.memory.SingleValueCache
import io.novafoundation.nova.common.data.network.runtime.binding.WeightV2
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.validation.ValidationStatus
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicSplitter
import io.novafoundation.nova.feature_account_api.data.multisig.composeMultisigAsMulti
import io.novafoundation.nova.feature_account_api.data.multisig.validation.MultisigExtrinsicValidationPayload
import io.novafoundation.nova.feature_account_api.data.multisig.validation.MultisigExtrinsicValidationRequestBus
import io.novafoundation.nova.feature_account_api.data.multisig.validation.SignatoryFeePaymentMode
import io.novafoundation.nova.feature_account_api.data.signer.CallExecutionType
import io.novafoundation.nova.feature_account_api.data.signer.NovaSigner
import io.novafoundation.nova.feature_account_api.data.signer.SignerProvider
import io.novafoundation.nova.feature_account_api.data.signer.SigningContext
import io.novafoundation.nova.feature_account_api.data.signer.intersect
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.MultisigMetaAccount
import io.novafoundation.nova.feature_account_impl.presentation.multisig.MultisigSigningPresenter
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.runtime.AccountId
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.GenericCall
import io.novasama.substrate_sdk_android.runtime.extrinsic.builder.ExtrinsicBuilder
import io.novasama.substrate_sdk_android.runtime.extrinsic.signer.SignedRaw
import io.novasama.substrate_sdk_android.runtime.extrinsic.signer.SignerPayloadRaw
import javax.inject.Inject

@FeatureScope
class MultisigSignerFactory @Inject constructor(
    private val chainRegistry: ChainRegistry,
    private val accountRepository: AccountRepository,
    private val extrinsicSplitter: ExtrinsicSplitter,
    private val multisigExtrinsicValidationEventBus: MultisigExtrinsicValidationRequestBus,
    private val multisigSigningPresenter: MultisigSigningPresenter,
) {

    fun create(metaAccount: MultisigMetaAccount, signerProvider: SignerProvider, isRoot: Boolean): MultisigSigner {
        return MultisigSigner(
            chainRegistry = chainRegistry,
            accountRepository = accountRepository,
            extrinsicSplitter = extrinsicSplitter,
            signerProvider = signerProvider,
            isRootSigner = isRoot,
            multisigExtrinsicValidationEventBus = multisigExtrinsicValidationEventBus,
            multisigSigningPresenter = multisigSigningPresenter,
            multisigAccount = metaAccount,

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
    private val multisigExtrinsicValidationEventBus: MultisigExtrinsicValidationRequestBus,
    private val multisigSigningPresenter: MultisigSigningPresenter,
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
        if (isRootSigner) {
            acknowledgeMultisigOperation()
        }

        val actualCall = getWrappedCall()
        delegateSigner().setSignerDataForSubmission(context)
        val delegatedCall = getWrappedCall()

        // We intentionally do validation before wrapping to pass the actual call to the validation
        validateExtrinsic(context.chain, actualCall = actualCall, delegatedCall = delegatedCall)

        wrapCallsInAsMultiForSubmission()
    }

    context(ExtrinsicBuilder)
    override suspend fun setSignerDataForFee(context: SigningContext) {
        delegateSigner().setSignerDataForFee(context)

        wrapCallsInProxyForFee()
    }

    override suspend fun signRaw(payload: SignerPayloadRaw): SignedRaw {
        multisigSigningPresenter.signingIsNotSupported()
        throw SigningCancelledException()
    }

    override suspend fun maxCallsPerTransaction(): Int? {
        return delegateSigner().maxCallsPerTransaction()
    }

    private suspend fun acknowledgeMultisigOperation() {
        val resume = multisigSigningPresenter.acknowledgeMultisigOperation(multisigAccount, signatoryMetaAccount())
        if (!resume) throw SigningCancelledException()
    }

    context(ExtrinsicBuilder)
    private suspend fun validateExtrinsic(
        chain: Chain,
        actualCall: GenericCall.Instance,
        delegatedCall: GenericCall.Instance,
    ) {
        val validationPayload = MultisigExtrinsicValidationPayload(
            multisig = multisigAccount,
            signatory = signatoryMetaAccount(),
            chain = chain,
            signatoryFeePaymentMode = determineSignatoryFeePaymentMode(actualCall),
            delegatedCall = delegatedCall
        )

        val requestBusPayload = MultisigExtrinsicValidationRequestBus.Request(validationPayload)
        val validationResponse = multisigExtrinsicValidationEventBus.handle(requestBusPayload)

        val validationStatus = validationResponse.validationResult.getOrNull()
        if (validationStatus !is ValidationStatus.NotValid) return

        multisigSigningPresenter.presentValidationFailure(validationStatus.reason)

        throw SigningCancelledException()
    }

    context(ExtrinsicBuilder)
    private fun determineSignatoryFeePaymentMode(actualCall: GenericCall.Instance): SignatoryFeePaymentMode {
        return if (isRootSigner) {
            SignatoryFeePaymentMode.PaysSubmissionFee(
                actualCall = actualCall
            )
        } else {
            SignatoryFeePaymentMode.NothingToPay
        }
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
