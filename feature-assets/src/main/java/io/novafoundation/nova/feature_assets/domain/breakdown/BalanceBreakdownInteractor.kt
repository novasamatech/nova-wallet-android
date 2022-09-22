package io.novafoundation.nova.feature_assets.domain.breakdown

import io.novafoundation.nova.common.utils.formatting.ABBREVIATED_PRECISION
import io.novafoundation.nova.common.utils.percentage
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
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainAssetId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
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
                PercentageAmount(amount = BigDecimal.ZERO, percentage = BigDecimal.ZERO),
                PercentageAmount(amount = BigDecimal.ZERO, percentage = BigDecimal.ZERO),
                listOf()
            )
        }
    }

    class PercentageAmount(val amount: BigDecimal, val percentage: BigDecimal)

    class BreakdownItem(val id: String, val asset: Asset, val tokenAmount: BigDecimal, val fiatAmount: BigDecimal)
}

class BalanceBreakdownInteractor(
    private val accountRepository: AccountRepository,
    private val balanceLocksRepository: BalanceLocksRepository,
    private val contributionsRepository: ContributionsRepository
) {

    private class TotalAmount(
        val totalFiat: BigDecimal,
        val transferableFiat: BigDecimal,
        val locksFiat: BigDecimal,
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
                    val assetsByChainId = assets.associateBy { it.token.configuration.chainId to it.token.configuration.id }
                    val locksOrEmpty = locks?.let { mapLocks(assetsByChainId, it) }.orEmpty()
                    val contributionsOrEmpty = contributions?.let { mapContributions(assetsByChainId, it) }.orEmpty()
                    val reserved = getReservedBreakdown(assets)

                    val breakdown = locksOrEmpty + contributionsOrEmpty + reserved

                    val totalAmount = calculateTotalBalance(assets, contributionsOrEmpty)
                    val (transferablePercentage, locksPercentage) = percentage(
                        scale = ABBREVIATED_PRECISION,
                        totalAmount.transferableFiat,
                        totalAmount.locksFiat
                    )
                    BalanceBreakdown(
                        totalAmount.totalFiat,
                        BalanceBreakdown.PercentageAmount(totalAmount.transferableFiat, transferablePercentage),
                        BalanceBreakdown.PercentageAmount(totalAmount.locksFiat, locksPercentage),
                        breakdown.sortedByDescending { it.fiatAmount }
                    )
                }
            }
        }
    }

    private fun mapLocks(
        assetsByChainId: Map<Pair<ChainId, ChainAssetId>, Asset>,
        locks: List<BalanceLock>
    ): List<BalanceBreakdown.BreakdownItem> {
        return locks.mapNotNull { lock ->
            val asset = assetsByChainId[lock.chainAsset.chainId to lock.chainAsset.id]
            if (asset == null) {
                null
            } else {
                val token = asset.token
                val tokenAmount = token.amountFromPlanks(lock.amountInPlanks)
                BalanceBreakdown.BreakdownItem(
                    id = lock.id,
                    asset = asset,
                    tokenAmount = tokenAmount,
                    fiatAmount = token.priceOf(tokenAmount)
                )
            }
        }
    }

    private fun mapContributions(
        assetsByChainId: Map<Pair<ChainId, ChainAssetId>, Asset>,
        contributions: List<Contribution>
    ): List<BalanceBreakdown.BreakdownItem> {
        return contributions.groupBy { it.chain.id to it.chain.utilityAsset.id }
            .mapNotNull { (chainAndAssetId, chainContributions) ->
                val asset = assetsByChainId[chainAndAssetId]
                if (asset == null) {
                    null
                } else {
                    val token = asset.token

                    val totalAmountInPlanks = chainContributions.sumOf { it.amountInPlanks }
                    val tokenAmount = token.amountFromPlanks(totalAmountInPlanks)
                    BalanceBreakdown.BreakdownItem(
                        id = CROWDLOAN_ID,
                        asset = asset,
                        tokenAmount = tokenAmount,
                        fiatAmount = token.priceOf(tokenAmount)
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
                    id = BalanceBreakdown.RESERVED_ID,
                    asset = it,
                    tokenAmount = it.reserved,
                    fiatAmount = it.token.priceOf(it.reserved)
                )
            }
    }
}
