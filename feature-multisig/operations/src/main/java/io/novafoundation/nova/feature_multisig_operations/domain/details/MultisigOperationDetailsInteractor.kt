package io.novafoundation.nova.feature_multisig_operations.domain.details

import com.google.gson.Gson
import io.novafoundation.nova.common.data.network.runtime.binding.WeightV2
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_account_api.data.ethereum.transaction.TransactionOrigin
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicSplitter
import io.novafoundation.nova.feature_account_api.data.extrinsic.execution.ExtrinsicExecutionResult
import io.novafoundation.nova.feature_account_api.data.extrinsic.execution.requireOk
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_account_api.data.multisig.composeMultisigAsMulti
import io.novafoundation.nova.feature_account_api.data.multisig.composeMultisigCancelAsMulti
import io.novafoundation.nova.feature_account_api.data.multisig.model.MultisigAction
import io.novafoundation.nova.feature_account_api.data.multisig.model.PendingMultisigOperation
import io.novafoundation.nova.feature_account_api.data.multisig.model.userAction
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.MultisigMetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.requireAccountIdIn
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.balances.model.ChainAssetBalance
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.balances.queryAccountBalanceCatching
import io.novafoundation.nova.runtime.di.ExtrinsicSerialization
import io.novafoundation.nova.runtime.ext.utilityAsset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.runtime.definitions.types.generics.GenericCall
import io.novasama.substrate_sdk_android.runtime.extrinsic.builder.ExtrinsicBuilder
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

interface MultisigOperationDetailsInteractor {

    fun callDetails(call: GenericCall.Instance): String

    suspend fun estimateActionFee(operation: PendingMultisigOperation): Fee?

    suspend fun performAction(operation: PendingMultisigOperation): Result<ExtrinsicExecutionResult>

    fun signatoryFlow(signatoryMetaId: Long): Flow<MetaAccount>

    suspend fun getSignatoryBalance(signatory: MetaAccount, chain: Chain): Result<ChainAssetBalance>
}

@FeatureScope
class RealMultisigOperationDetailsInteractor @Inject constructor(
    private val extrinsicService: ExtrinsicService,
    private val extrinsicSplitter: ExtrinsicSplitter,
    private val accountRepository: AccountRepository,
    private val assetSourceRegistry: AssetSourceRegistry,
    @ExtrinsicSerialization
    private val extrinsicGson: Gson,
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

    override suspend fun performAction(operation: PendingMultisigOperation): Result<ExtrinsicExecutionResult> {
        val action = operation.userAction().toInternalAction() ?: return Result.failure(IllegalStateException("No action found"))

        return when (action) {
            Action.APPROVE -> performApprove(operation)
            Action.REJECT -> performReject(operation)
        }
    }

    override fun signatoryFlow(signatoryMetaId: Long): Flow<MetaAccount> {
        return accountRepository.metaAccountFlow(signatoryMetaId)
    }

    override suspend fun getSignatoryBalance(signatory: MetaAccount, chain: Chain): Result<ChainAssetBalance> {
        val asset = chain.utilityAsset
        val signatoryAccountId = signatory.requireAccountIdIn(chain)
        return assetSourceRegistry.sourceFor(asset).balance.queryAccountBalanceCatching(chain, asset, signatoryAccountId)
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

    private suspend fun performApprove(operation: PendingMultisigOperation): Result<ExtrinsicExecutionResult> {
        val call = operation.call
        requireNotNull(call) { "Call data not found" }

        return extrinsicService.submitExtrinsicAndAwaitExecution(
            chain = operation.chain,
            origin = TransactionOrigin.WalletWithId(operation.signatoryMetaId)
        ) { buildingContext ->
            val weight = extrinsicSplitter.estimateCallWeight(buildingContext.signer, call, buildingContext.chain)
            approve(operation, weight)
        }.requireOk()
    }

    private suspend fun performReject(operation: PendingMultisigOperation): Result<ExtrinsicExecutionResult> {
        return extrinsicService.submitExtrinsicAndAwaitExecution(
            chain = operation.chain,
            origin = TransactionOrigin.WalletWithId(operation.signatoryMetaId)
        ) {
            reject(operation)
        }.requireOk()
    }

    private suspend fun ExtrinsicBuilder.approve(
        operation: PendingMultisigOperation,
        maxWeight: WeightV2
    ) {
        val selectedAccount = accountRepository.getSelectedMetaAccount() as MultisigMetaAccount

        val approveCall = runtime.composeMultisigAsMulti(
            multisigMetaAccount = selectedAccount,
            maybeTimePoint = operation.timePoint,
            call = operation.call!!,
            maxWeight = maxWeight
        )

        call(approveCall)
    }

    private suspend fun ExtrinsicBuilder.reject(operation: PendingMultisigOperation) {
        val selectedAccount = accountRepository.getSelectedMetaAccount() as MultisigMetaAccount

        val approveCall = runtime.composeMultisigCancelAsMulti(
            multisigMetaAccount = selectedAccount,
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
