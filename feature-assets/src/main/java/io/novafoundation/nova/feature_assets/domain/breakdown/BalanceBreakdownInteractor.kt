package io.novafoundation.nova.feature_assets.domain.breakdown

import io.novafoundation.nova.common.utils.percentageOf
import io.novafoundation.nova.common.utils.unite
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_assets.domain.breakdown.BalanceBreakdown.Companion.CROWDLOAN_ID
import io.novafoundation.nova.feature_assets.domain.locks.BalanceLocksRepository
import io.novafoundation.nova.feature_crowdloan_api.data.repository.ContributionsRepository
import io.novafoundation.nova.feature_crowdloan_api.domain.contributions.Contribution
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.model.BalanceLock
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.runtime.ext.utilityAsset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigDecimal
import java.math.BigInteger
import kotlinx.coroutines.flow.Flow
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

        fun empty(): BalanceBreakdown {
            return BalanceBreakdown(
                BigDecimal.ZERO,
                PercentageAmount(BigDecimal.ZERO, BigDecimal.ZERO),
                PercentageAmount(BigDecimal.ZERO, BigDecimal.ZERO),
                listOf()
            )
        }
    }

    class PercentageAmount(val amount: BigDecimal, val percentage: BigDecimal)

    class BreakdownItem(val id: String, val chainAsset: Chain.Asset, val fiatAmount: BigDecimal)
}

class BalanceBreakdownInteractor(
    private val accountRepository: AccountRepository,
    private val balanceLocksRepository: BalanceLocksRepository,
    private val contributionsRepository: ContributionsRepository
) {

    private class TotalAmount(
        val total: BigDecimal,
        val transferable: BigDecimal,
        val locks: BigDecimal,
    )

    fun balanceBreakdownFlow(assetsFlow: Flow<List<Asset>>): Flow<BalanceBreakdown> {
        return accountRepository.selectedMetaAccountFlow().flatMapLatest { metaAccount ->
            unite(
                assetsFlow,
                balanceLocksRepository.observeLocksForMetaAccount(metaAccount),
                contributionsRepository.observeContributions(metaAccount)
            ) { assets, locks, contributions ->
                if (assets == null) {
                    BalanceBreakdown.empty()
                } else {
                    val locksOrEmpty = locks?.let { mapLocks(assets, it) } ?: listOf()
                    val contributionsOrEmpty = contributions?.let { mapContributions(assets, it) } ?: listOf()
                    val reserved = getReservedBreakdown(assets)

                    val breakdown = locksOrEmpty + contributionsOrEmpty + reserved

                    val totalAmount = calculateTotalBalance(assets, contributionsOrEmpty)
                    val transferablePercentage = totalAmount.transferable.percentageOf(totalAmount.total)
                    val locksPercentage = totalAmount.locks.percentageOf(totalAmount.total)

                    BalanceBreakdown(
                        totalAmount.total,
                        BalanceBreakdown.PercentageAmount(totalAmount.transferable, transferablePercentage),
                        BalanceBreakdown.PercentageAmount(totalAmount.locks, locksPercentage),
                        breakdown.sortedByDescending { it.fiatAmount }
                    )
                }
            }
        }
    }

    private fun mapLocks(assets: List<Asset>, locks: List<BalanceLock>): List<BalanceBreakdown.BreakdownItem> {
        val assetsByChainId = assets.associateBy { it.token.configuration.chainId to it.token.configuration.id }
        return locks.mapNotNull { lock ->
            val chainAsset = assetsByChainId[lock.chainAsset.chainId to lock.chainAsset.id]
            if (chainAsset == null) {
                null
            } else {
                val token = chainAsset.token

                val amount = token.amountFromPlanks(lock.amountInPlanks)
                BalanceBreakdown.BreakdownItem(
                    lock.id,
                    lock.chainAsset,
                    token.priceOf(amount)
                )
            }
        }
    }

    private fun mapContributions(assets: List<Asset>, contributions: List<Contribution>): List<BalanceBreakdown.BreakdownItem> {
        val assetsByChainId = assets.associateBy { it.token.configuration.chainId to it.token.configuration.id }

        return contributions.groupBy { it.chain.id to it.chain.utilityAsset.id }
            .mapNotNull { (chainAndAssetId, chainContributions) ->
                val chainAsset = assetsByChainId[chainAndAssetId]
                if (chainAsset == null) {
                    null
                } else {
                    val token = assetsByChainId.getValue(chainAndAssetId).token

                    val totalAmountInPlanks = chainContributions.sumOf { it.amountInPlanks }
                    BalanceBreakdown.BreakdownItem(
                        CROWDLOAN_ID,
                        token.configuration,
                        token.priceOf(token.amountFromPlanks(totalAmountInPlanks))
                    )
                }
            }
    }

    private fun calculateTotalBalance(
        assets: List<Asset>,
        contributions: List<BalanceBreakdown.BreakdownItem>
    ): TotalAmount {
        val contributionsTotal = contributions.sumOf { it.fiatAmount }
        var total = contributionsTotal
        var transferable = BigDecimal.ZERO
        var locks = contributionsTotal

        assets.forEach { asset ->
            total += asset.token.priceOf(asset.total)
            transferable += asset.token.priceOf(asset.transferable)
            locks += asset.token.priceOf(asset.locked)
        }

        return TotalAmount(total, transferable, locks)
    }

    private fun getReservedBreakdown(assets: List<Asset>): List<BalanceBreakdown.BreakdownItem> {
        return assets
            .filter { it.reservedInPlanks > BigInteger.ZERO }
            .map {
                BalanceBreakdown.BreakdownItem(
                    BalanceBreakdown.RESERVED_ID,
                    it.token.configuration,
                    it.token.priceOf(it.reserved)
                )
            }
    }
}
