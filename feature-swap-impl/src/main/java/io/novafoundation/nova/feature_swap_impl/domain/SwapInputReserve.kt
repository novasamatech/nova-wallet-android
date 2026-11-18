package io.novafoundation.nova.feature_swap_impl.domain

import io.novafoundation.nova.common.utils.isZero
import io.novafoundation.nova.feature_swap_api.domain.model.SwapFee
import io.novafoundation.nova.feature_swap_api.domain.model.SwapMaxAdditionalAmountDeduction
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.existentialDepositInPlanks
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.totalCanBeDroppedBelowMinimumBalance
import io.novafoundation.nova.feature_wallet_api.data.repository.AccountInfoRepository
import io.novasama.substrate_sdk_android.runtime.AccountId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import javax.inject.Inject

internal class SwapInputReserve @Inject constructor(
    private val assetSourceRegistry: AssetSourceRegistry,
    private val accountInfoRepository: AccountInfoRepository
) {

    suspend fun requiredFor(assetIn: Chain.Asset, firstSegment: SwapFee.SwapSegment): SwapMaxAdditionalAmountDeduction {
        val operation = firstSegment.operation
        val routeReserve = operation.additionalMaxAmountDeduction()
        if (assetIn.type != Chain.Asset.Type.Native) return routeReserve

        val existentialDeposit = assetSourceRegistry.existentialDepositInPlanks(assetIn)
        if (routeReserve.fromCountedTowardsEd >= existentialDeposit) return routeReserve

        // A route may allow reaping (e.g. XCM Execute), but existing consumers still need the source account.
        val executingAccount = firstSegment.fee.submissionFee.submissionOrigin.executingAccount
        return if (canDropBelowEd(assetIn, executingAccount)) routeReserve else SwapMaxAdditionalAmountDeduction(existentialDeposit)
    }

    suspend fun canDropBelowEd(asset: Chain.Asset, executingAccount: AccountId): Boolean {
        if (asset.type != Chain.Asset.Type.Native) return assetSourceRegistry.totalCanBeDroppedBelowMinimumBalance(asset)

        return accountInfoRepository.getAccountInfo(asset.chainId, executingAccount).consumers.isZero
    }
}
