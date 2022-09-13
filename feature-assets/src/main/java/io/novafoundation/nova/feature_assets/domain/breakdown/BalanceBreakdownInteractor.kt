package io.novafoundation.nova.feature_assets.domain.breakdown

import io.novafoundation.nova.common.utils.percentageOf
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_assets.domain.locks.BalanceLocksRepository
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.model.BalanceLock
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigDecimal
import java.math.BigInteger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest

class BalanceBreakdown(
    val total: BigDecimal,
    val transferableTotal: PercentageAmount,
    val locksTotal: PercentageAmount,
    val breakdown: List<BreakdownItem>
) {
    companion object {
        const val RESERVED_ID = "reserved"
        const val CROWDLOAN_ID = "crowdloan"
    }

    class PercentageAmount(val amount: BigDecimal, val percentage: Float)

    class BreakdownItem(val id: String, val chainAsset: Chain.Asset, val fiatAmount: BigDecimal)
}

class BalanceBreakdownInteractor(
    private val accountRepository: AccountRepository,
    private val balanceLocksRepository: BalanceLocksRepository,
) {

    private class TotalAmount(
        val total: BigDecimal,
        val transferable: BigDecimal,
        val locks: BigDecimal,
    )

    fun balanceBreakdownFlow(assetsFlow: Flow<List<Asset>>): Flow<BalanceBreakdown> {
        val metaAccountFlow = accountRepository.selectedMetaAccountFlow()
        val locksFlow = metaAccountFlow
            .flatMapLatest { balanceLocksRepository.observeLocksForMetaAccount(it) }

        return combine(assetsFlow, locksFlow) { assets, locks ->
            val totalAmount = totalBalanceFromAssets(assets)
            val transferablePercentage = totalAmount.transferable.percentageOf(totalAmount.total)
            val locksPercentage = totalAmount.locks.percentageOf(totalAmount.total)

            BalanceBreakdown(
                totalAmount.total,
                BalanceBreakdown.PercentageAmount(totalAmount.transferable, transferablePercentage),
                BalanceBreakdown.PercentageAmount(totalAmount.locks, locksPercentage),
                mapBalanceBreakdown(assets, locks)
            )
        }
    }

    private fun totalBalanceFromAssets(
        assets: List<Asset>
    ): TotalAmount {
        var total = BigDecimal.ZERO
        var transferable = BigDecimal.ZERO
        var locks = BigDecimal.ZERO

        assets.forEach { asset ->
            total += asset.token.priceOf(asset.total)
            transferable += asset.token.priceOf(asset.transferable)
            locks += asset.token.priceOf(asset.locked)
        }

        return TotalAmount(total, transferable, locks)
    }

    private fun mapBalanceBreakdown(assets: List<Asset>, locks: List<BalanceLock>): List<BalanceBreakdown.BreakdownItem> {
        val assetsByChainId = assets.associateBy { it.token.configuration.chainId to it.token.configuration.id }

        val breakdownAssets = assets
            .filter { it.reservedInPlanks > BigInteger.ZERO }
            .map {
                BalanceBreakdown.BreakdownItem(
                    BalanceBreakdown.RESERVED_ID,
                    it.token.configuration,
                    it.token.priceOf(it.reserved)
                )
            }
        val breakdownLocks = locks.map { lock ->
            val token = assetsByChainId.getValue(lock.chainAsset.chainId to lock.chainAsset.id)
                .token

            val amount = token.amountFromPlanks(lock.amountInPlanks)
            BalanceBreakdown.BreakdownItem(
                lock.id,
                lock.chainAsset,
                token.priceOf(amount)
            )
        }

        return (breakdownAssets + breakdownLocks).sortedByDescending {
            it.fiatAmount
        }
    }
}
