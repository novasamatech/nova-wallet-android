package io.novafoundation.nova.feature_account_impl.data.signer.derivative

import io.novafoundation.nova.common.base.errors.SigningCancelledException
import io.novafoundation.nova.common.data.memory.SingleValueCache
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.utils.Modules
import io.novafoundation.nova.common.utils.composeCall
import io.novafoundation.nova.feature_account_api.data.multisig.validation.SignatoryFeePaymentMode
import io.novafoundation.nova.feature_account_api.data.signer.CallExecutionType
import io.novafoundation.nova.feature_account_api.data.signer.NovaSigner
import io.novafoundation.nova.feature_account_api.data.signer.SignerProvider
import io.novafoundation.nova.feature_account_api.data.signer.SigningContext
import io.novafoundation.nova.feature_account_api.data.signer.SubmissionHierarchy
import io.novafoundation.nova.feature_account_api.data.signer.intersect
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.DerivativeMetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_impl.data.signer.LeafSigner
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.runtime.AccountId
import io.novasama.substrate_sdk_android.runtime.RuntimeSnapshot
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.GenericCall
import io.novasama.substrate_sdk_android.runtime.extrinsic.builder.ExtrinsicBuilder
import io.novasama.substrate_sdk_android.runtime.extrinsic.signer.SignedRaw
import io.novasama.substrate_sdk_android.runtime.extrinsic.signer.SignerPayloadRaw
import javax.inject.Inject

@FeatureScope
class DerivativeSignerFactory @Inject constructor(
    private val accountRepository: AccountRepository,
) {

    fun create(metaAccount: DerivativeMetaAccount, signerProvider: SignerProvider, isRoot: Boolean): DerivativeSigner {
        return DerivativeSigner(
            accountRepository = accountRepository,
            signerProvider = signerProvider,
            isRootSigner = isRoot,
            derivativeAccount = metaAccount,
        )
    }
}

class DerivativeSigner(
    private val derivativeAccount: DerivativeMetaAccount,
    private val accountRepository: AccountRepository,
    private val signerProvider: SignerProvider,
    private val isRootSigner: Boolean,
) : NovaSigner {

    override val metaAccount = derivativeAccount

    private val selfCallExecutionType = CallExecutionType.IMMEDIATE

    private val parentMetaAccount = SingleValueCache {
        computeParentMetaAccount()
    }

    private val delegateSigner = SingleValueCache {
        signerProvider.nestedSignerFor(parentMetaAccount())
    }

    override suspend fun getSigningHierarchy(): SubmissionHierarchy {
        return delegateSigner().getSigningHierarchy() + SubmissionHierarchy(metaAccount, selfCallExecutionType)
    }

    override suspend fun callExecutionType(): CallExecutionType {
        return delegateSigner().callExecutionType().intersect(selfCallExecutionType)
    }

    override suspend fun submissionSignerAccountId(chain: Chain): AccountId {
        return delegateSigner().submissionSignerAccountId(chain)
    }

    context(ExtrinsicBuilder)
    override suspend fun setSignerDataForSubmission(context: SigningContext) {
        if (isRootSigner) {
            acknowledgeDerivativeOperation()
        }

        val callInsideAsDerivative = getWrappedCall()

        // We intentionally do validation before wrapping to pass the actual call to the validation
        validateExtrinsic(context.chain, callInsideAsDerivative)

        wrapCallsInAsDerivative()

        delegateSigner().setSignerDataForSubmission(context)
    }

    context(ExtrinsicBuilder)
    override suspend fun setSignerDataForFee(context: SigningContext) {
        delegateSigner().setSignerDataForFee(context)

        wrapCallsInAsDerivative()
    }

    override suspend fun signRaw(payload: SignerPayloadRaw): SignedRaw {
        // TODO derivative: signing not supported
        throw SigningCancelledException()
    }

    override suspend fun maxCallsPerTransaction(): Int? {
        return delegateSigner().maxCallsPerTransaction()
    }

    // TODO derivative: acknowledge derivative operation
    private suspend fun acknowledgeDerivativeOperation() {
//        val resume = multisigSigningPresenter.acknowledgeMultisigOperation(derivativeAccount, parentMetaAccount())
//        if (!resume) throw SigningCancelledException()
    }

    context(ExtrinsicBuilder)
    private suspend fun validateExtrinsic(
        chain: Chain,
        callInsideAsDerivative: GenericCall.Instance,
    ) {
//        val validationPayload = MultisigExtrinsicValidationPayload(
//            multisig = derivativeAccount,
//            signatory = parentMetaAccount(),
//            chain = chain,
//            signatoryFeePaymentMode = determineSignatoryFeePaymentMode(),
//            callInsideAsMulti = callInsideAsDerivative
//        )
//
//        val requestBusPayload = MultisigExtrinsicValidationRequestBus.Request(validationPayload)
//        multisigExtrinsicValidationEventBus.handle(requestBusPayload)
//            .validationResult
//            .onSuccess {
//                if (it is ValidationStatus.NotValid) {
//                    multisigSigningPresenter.presentValidationFailure(it.reason)
//                    throw SigningCancelledException()
//                }
//            }
//            .onFailure {
//                throw it
//            }
    }

    context(ExtrinsicBuilder)
    private suspend fun determineSignatoryFeePaymentMode(): SignatoryFeePaymentMode {
        // Our direct signatory only pay fees if it is a LeafSigner
        // Otherwise it is paid by signer's own delegate
        return if (delegateSigner() is LeafSigner) {
            SignatoryFeePaymentMode.PaysSubmissionFee
        } else {
            SignatoryFeePaymentMode.NothingToPay
        }
    }


    context(ExtrinsicBuilder)
    private fun wrapCallsInAsDerivative() {
        val call = getWrappedCall()

        val derivativeCall = runtime.composeUtilityAsDerivative(derivativeAccount.index, call)

        resetCalls()
        call(derivativeCall)
    }

    private fun RuntimeSnapshot.composeUtilityAsDerivative(
        index: Int,
        call: GenericCall.Instance
    ): GenericCall.Instance {
        return composeCall(
            moduleName = Modules.UTILITY,
            callName = "as_derivative",
            arguments = mapOf(
                "index" to index.toBigInteger(),
                "call" to call
            )
        )
    }

    private suspend fun computeParentMetaAccount(): MetaAccount {
        return accountRepository.getMetaAccount(derivativeAccount.parentMetaId)
    }
}
