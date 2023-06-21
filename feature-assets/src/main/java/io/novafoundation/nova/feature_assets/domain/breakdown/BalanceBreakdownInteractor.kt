package io.novafoundation.nova.feature_assets.domain.breakdown

import io.novafoundation.nova.common.utils.formatting.ABBREVIATED_PRECISION
import io.novafoundation.nova.common.utils.percentage
import io.novafoundation.nova.common.utils.unite
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_assets.domain.breakdown.BalanceBreakdown.Companion.CROWDLOAN_ID
import io.novafoundation.nova.feature_wallet_api.data.repository.BalanceLocksRepository
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.model.BalanceLock
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.runtime.ext.fullId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import java.math.BigDecimal
import java.math.BigInteger

class BalanceBreakdown(
    val total: BigDecimal,
    val transferableTotal: PercentageAmount,
    val locksTotal: PercentageAmount,
    val contributions: List<BreakdownItem>,
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
                listOf(),
                listOf()
            )
        }
    }

    class PercentageAmount(val amount: BigDecimal, val percentage: BigDecimal)

    class BreakdownItem(val id: String, val asset: Asset, val amountInPlanks: BigInteger) {
        val tokenAmount by lazy { asset.token.amountFromPlanks(amountInPlanks) }

        val fiatAmount by lazy { asset.token.amountToFiat(tokenAmount) }
    }
}

class BalanceBreakdownInteractor(
    private val accountRepository: AccountRepository,
    private val balanceLocksRepository: BalanceLocksRepository
) {

    private class TotalAmount(
        val totalFiat: BigDecimal,
        val transferableFiat: BigDecimal,
        val locksFiat: BigDecimal,
    )

    fun balanceBreakdownFlow(
        assetsFlow: Flow<List<Asset>>,
        contributions: Flow<Map<FullChainAssetId, Balance>>
    ): Flow<BalanceBreakdown> {
        return accountRepository.selectedMetaAccountFlow().flatMapLatest { metaAccount ->
            unite(
                assetsFlow,
                balanceLocksRepository.observeLocksForMetaAccount(metaAccount),
                contributions
            ) { assets, locks, contributions ->
                if (assets == null) {
                    BalanceBreakdown.empty()
                } else {
                    val assetsByChainId = assets.associateBy { it.token.configuration.fullId }
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
                        contributionsOrEmpty,
                        breakdown.sortedByDescending { it.fiatAmount }
                    )
                }
            }
        }
    }

    private fun mapLocks(
        assetsByChainId: Map<FullChainAssetId, Asset>,
        locks: List<BalanceLock>
    ): List<BalanceBreakdown.BreakdownItem> {
        return locks.mapNotNull { lock ->
            assetsByChainId[lock.chainAsset.fullId]?.let { asset ->
                BalanceBreakdown.BreakdownItem(
                    id = lock.id,
                    asset = asset,
                    amountInPlanks = lock.amountInPlanks,
                )
            }
        }
    }

    private fun mapContributions(
        assetsByChainId: Map<FullChainAssetId, Asset>,
        contributions: Map<FullChainAssetId, BigInteger>
    ): List<BalanceBreakdown.BreakdownItem> {
        return contributions.mapNotNull { (chainAndAssetId, chainContributions) ->
            assetsByChainId[chainAndAssetId]?.let { asset ->
                BalanceBreakdown.BreakdownItem(
                    id = CROWDLOAN_ID,
                    asset = asset,
                    amountInPlanks = chainContributions,
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
            total += asset.token.amountToFiat(asset.total)
            transferable += asset.token.amountToFiat(asset.transferable)
            locks += asset.token.amountToFiat(asset.locked)
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
                    amountInPlanks = it.reservedInPlanks
                )
            }
    }
}
