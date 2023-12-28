package io.novafoundation.nova.feature_swap_impl.data.repository

import io.novafoundation.nova.core_db.dao.OperationDao
import io.novafoundation.nova.core_db.model.operation.OperationBaseLocal
import io.novafoundation.nova.core_db.model.operation.OperationLocal
import io.novafoundation.nova.core_db.model.operation.SwapTypeLocal.AssetWithAmount
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicSubmission
import io.novafoundation.nova.feature_swap_api.domain.model.SwapExecuteArgs
import io.novafoundation.nova.feature_swap_api.domain.model.SwapFee
import io.novafoundation.nova.feature_swap_api.domain.model.feeAsset
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.runtime.ext.localId
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

interface SwapTransactionHistoryRepository {

    suspend fun insertPendingSwap(
        chainAsset: Chain.Asset,
        swapArgs: SwapExecuteArgs,
        fee: SwapFee,
        txSubmission: ExtrinsicSubmission
    )
}

class RealSwapTransactionHistoryRepository(
    private val operationDao: OperationDao,
    private val chainRegistry: ChainRegistry,
) : SwapTransactionHistoryRepository {

    override suspend fun insertPendingSwap(
        chainAsset: Chain.Asset,
        swapArgs: SwapExecuteArgs,
        fee: SwapFee,
        txSubmission: ExtrinsicSubmission
    ) {
        val chain = chainRegistry.getChain(chainAsset.chainId)

        val localOperation = with(swapArgs) {
            OperationLocal.manualSwap(
                hash = txSubmission.hash,
                originAddress = chain.addressOf(txSubmission.submissionOrigin.requestedOrigin),
                assetId = chainAsset.localId,
                // Insert fee regardless of who actually paid it
                fee = feeAsset.withAmountLocal(fee.networkFee.amount),
                amountIn = assetIn.withAmountLocal(swapLimit.expectedAmountIn),
                amountOut = assetOut.withAmountLocal(swapLimit.expectedAmountOut),
                status = OperationBaseLocal.Status.PENDING,
                source = OperationBaseLocal.Source.APP
            )
        }

        operationDao.insert(localOperation)
    }

    private fun Chain.Asset.withAmountLocal(amount: Balance): AssetWithAmount {
        return AssetWithAmount(
            assetId = localId,
            amount = amount
        )
    }
}
