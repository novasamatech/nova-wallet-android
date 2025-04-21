package io.novafoundation.nova.feature_multisig_operations.domain.details

import com.google.gson.Gson
import io.novafoundation.nova.common.data.network.runtime.binding.WeightV2
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.utils.coerceToUnit
import io.novafoundation.nova.feature_account_api.data.ethereum.transaction.TransactionOrigin
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicSplitter
import io.novafoundation.nova.feature_account_api.data.extrinsic.execution.requireOk
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_account_api.data.multisig.composeMultisigAsMulti
import io.novafoundation.nova.feature_account_api.data.multisig.composeMultisigCancelAsMulti
import io.novafoundation.nova.feature_account_api.data.multisig.model.MultisigAction
import io.novafoundation.nova.feature_account_api.data.multisig.model.PendingMultisigOperation
import io.novafoundation.nova.feature_account_api.data.multisig.model.userAction
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.MultisigMetaAccount
import io.novafoundation.nova.runtime.di.ExtrinsicSerialization
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.GenericCall
import io.novasama.substrate_sdk_android.runtime.extrinsic.builder.ExtrinsicBuilder
import javax.inject.Inject

interface MultisigOperationDetailsInteractor {

    fun callDetails(call: GenericCall.Instance): String

    suspend fun estimateActionFee(operation: PendingMultisigOperation): Fee?

    suspend fun performAction(operation: PendingMultisigOperation): Result<Unit>
}

@FeatureScope
class RealMultisigOperationDetailsInteractor @Inject constructor(
    private val extrinsicService: ExtrinsicService,
    private val extrinsicSplitter: ExtrinsicSplitter,
    private val accountRepository: AccountRepository,
    @ExtrinsicSerialization
    private val extrinsicGson: Gson
) : MultisigOperationDetailsInteractor {

    override fun callDetails(call: GenericCall.Instance): String {
       return extrinsicGson.toJson(call)
    }

    override suspend fun estimateActionFee(operation: PendingMultisigOperation): Fee? {
        val action = operation.userAction().toInternalAction() ?: return null

        return when (action) {
            Action.APPROVE -> estimateApproveFee(operation)
            Action.REJECT -> estimateRejectFee(operation)
        }
    }

    override suspend fun performAction(operation: PendingMultisigOperation): Result<Unit> {
        val action = operation.userAction().toInternalAction() ?: return Result.failure(IllegalStateException("No action found"))

        return when (action) {
            Action.APPROVE -> performApprove(operation)
            Action.REJECT -> performReject(operation)
        }
    }

    private suspend fun estimateApproveFee(operation: PendingMultisigOperation): Fee? {
        if (operation.call == null) return null

        return extrinsicService.estimateFee(
            chain = operation.chain,
            origin = TransactionOrigin.WalletWithId(operation.signatoryMetaId)
        ) {
            // Use zero weight to speed up fee calculation
            approve(operation, maxWeight = WeightV2.zero())
        }
    }

    private suspend fun estimateRejectFee(operation: PendingMultisigOperation): Fee? {
        return extrinsicService.estimateFee(
            chain = operation.chain,
            origin = TransactionOrigin.WalletWithId(operation.signatoryMetaId)
        ) {
            // Use zero weight to speed up fee calculation
            reject(operation)
        }
    }

    private suspend fun performApprove(operation: PendingMultisigOperation): Result<Unit> {
        val call = operation.call
        requireNotNull(call) { "Call data not found" }

        return extrinsicService.submitExtrinsicAndAwaitExecution(
            chain = operation.chain,
            origin = TransactionOrigin.WalletWithId(operation.signatoryMetaId)
        ) { buildingContext ->
            val weight = extrinsicSplitter.estimateCallWeight(buildingContext.signer, call, buildingContext.chain)
            approve(operation, weight)
        }
            .requireOk()
            .coerceToUnit()
    }

    private suspend fun performReject(operation: PendingMultisigOperation): Result<Unit> {
        val call = operation.call
        requireNotNull(call) { "Call data not found" }

        return extrinsicService.submitExtrinsicAndAwaitExecution(
            chain = operation.chain,
            origin = TransactionOrigin.WalletWithId(operation.signatoryMetaId)
        ) {
            reject(operation)
        }
            .requireOk()
            .coerceToUnit()
    }

    private suspend fun ExtrinsicBuilder.approve(
        operation: PendingMultisigOperation,
        maxWeight: WeightV2
    ) {
        val selectedAccount = accountRepository.getSelectedMetaAccount() as MultisigMetaAccount

        val approveCall = runtime.composeMultisigAsMulti(
            threshold = selectedAccount.threshold,
            otherSignatories = selectedAccount.otherSignatories,
            maybeTimePoint = operation.timePoint,
            call = operation.call!!,
            maxWeight = maxWeight
        )

        call(approveCall)
    }

    private suspend fun ExtrinsicBuilder.reject(operation: PendingMultisigOperation) {
        val selectedAccount = accountRepository.getSelectedMetaAccount() as MultisigMetaAccount

        val approveCall = runtime.composeMultisigCancelAsMulti(
            threshold = selectedAccount.threshold,
            otherSignatories = selectedAccount.otherSignatories,
            maybeTimePoint = operation.timePoint,
            callHash = operation.callHash
        )

        call(approveCall)
    }

    private fun MultisigAction.toInternalAction(): Action? {
        return when (this) {
            is MultisigAction.CanApprove -> Action.APPROVE
            is MultisigAction.CanReject -> Action.REJECT
            is MultisigAction.Signed -> null
        }
    }

    private enum class Action {
        APPROVE, REJECT
    }
}
