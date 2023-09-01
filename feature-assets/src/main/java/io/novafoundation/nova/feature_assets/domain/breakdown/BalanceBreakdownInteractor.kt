package io.novafoundation.nova.feature_assets.domain.breakdown

import io.novafoundation.nova.common.utils.formatting.ABBREVIATED_SCALE
import io.novafoundation.nova.common.utils.percentage
import io.novafoundation.nova.common.utils.unite
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_assets.domain.breakdown.BalanceBreakdown.PercentageAmount
import io.novafoundation.nova.feature_wallet_api.data.repository.BalanceLocksRepository
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.model.BalanceBreakdownIds
import io.novafoundation.nova.feature_wallet_api.domain.model.BalanceLock
import io.novafoundation.nova.feature_wallet_api.domain.model.ExternalBalance
import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.balanceId
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
    val breakdown: List<BreakdownItem>
) {
    companion object {
        fun empty(): BalanceBreakdown {
            return BalanceBreakdown(
                total = BigDecimal.ZERO,
                transferableTotal = PercentageAmount(amount = BigDecimal.ZERO, percentage = BigDecimal.ZERO),
                locksTotal = PercentageAmount(amount = BigDecimal.ZERO, percentage = BigDecimal.ZERO),
                breakdown = emptyList()
            )
        }
    }

    class PercentageAmount(val amount: BigDecimal, val percentage: BigDecimal)

    class BreakdownItem(val id: String, val token: Token, val amountInPlanks: BigInteger) {
        val tokenAmount by lazy { token.amountFromPlanks(amountInPlanks) }

        val fiatAmount by lazy { token.amountToFiat(tokenAmount) }
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
        externalBalancesFlow: Flow<List<ExternalBalance>>
    ): Flow<BalanceBreakdown> {
        return accountRepository.selectedMetaAccountFlow().flatMapLatest { metaAccount ->
            unite(
                assetsFlow,
                balanceLocksRepository.observeLocksForMetaAccount(metaAccount),
                externalBalancesFlow
            ) { assets, locks, externalBalances ->
                if (assets == null) {
                    BalanceBreakdown.empty()
                } else {
                    val assetsByChainId = assets.associateBy { it.token.configuration.fullId }
                    val locksItems = mapLocks(assetsByChainId, locks.orEmpty())
                    val externalBalancesItems = mapExternalBalances(assetsByChainId, externalBalances.orEmpty())
                    val reserved = getReservedBreakdown(assets)

                    val breakdown = locksItems + externalBalancesItems + reserved

                    val totalAmount = calculateTotalBalance(assets, externalBalancesItems)
                    val (transferablePercentage, locksPercentage) = percentage(
                        scale = ABBREVIATED_SCALE,
                        totalAmount.transferableFiat,
                        totalAmount.locksFiat
                    )
                    BalanceBreakdown(
                        total = totalAmount.totalFiat,
                        transferableTotal = PercentageAmount(totalAmount.transferableFiat, transferablePercentage),
                        locksTotal = PercentageAmount(totalAmount.locksFiat, locksPercentage),
                        breakdown = breakdown.sortedByDescending { it.fiatAmount }
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
                    token = asset.token,
                    amountInPlanks = lock.amountInPlanks,
                )
            }
        }
    }

    private fun mapExternalBalances(
        assetsByChainId: Map<FullChainAssetId, Asset>,
        externalBalances: List<ExternalBalance>
    ): List<BalanceBreakdown.BreakdownItem> {
        return externalBalances.mapNotNull { externalBalance ->
            assetsByChainId[externalBalance.chainAssetId]?.let { asset ->
                BalanceBreakdown.BreakdownItem(
                    id = externalBalance.type.balanceId,
                    token = asset.token,
                    amountInPlanks = externalBalance.amount,
                )
            }
        }
    }

    private fun calculateTotalBalance(
        assets: List<Asset>,
        externalBalancesItems: List<BalanceBreakdown.BreakdownItem>
    ): TotalAmount {
        val externalBalancesTotal = externalBalancesItems.sumOf { it.fiatAmount }
        var total = externalBalancesTotal
        var transferable = BigDecimal.ZERO
        var locks = externalBalancesTotal

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
                    id = BalanceBreakdownIds.RESERVED,
                    token = it.token,
                    amountInPlanks = it.reservedInPlanks
                )
            }
    }
}
