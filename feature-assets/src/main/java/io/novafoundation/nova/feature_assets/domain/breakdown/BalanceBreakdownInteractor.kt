package io.novafoundation.nova.feature_assets.domain.breakdown

import io.novafoundation.nova.common.utils.formatting.ABBREVIATED_SCALE
import io.novafoundation.nova.common.utils.orZero
import io.novafoundation.nova.common.utils.percentage
import io.novafoundation.nova.common.utils.sumByBigInteger
import io.novafoundation.nova.common.utils.unite
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_assets.domain.breakdown.BalanceBreakdown.PercentageAmount
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.data.repository.BalanceHoldsRepository
import io.novafoundation.nova.feature_wallet_api.data.repository.BalanceLocksRepository
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.model.BalanceBreakdownIds
import io.novafoundation.nova.feature_wallet_api.domain.model.BalanceHold
import io.novafoundation.nova.feature_wallet_api.domain.model.BalanceLock
import io.novafoundation.nova.feature_wallet_api.domain.model.ExternalBalance
import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.feature_wallet_api.domain.model.unlabeledReserves
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
    private val balanceLocksRepository: BalanceLocksRepository,
    private val balanceHoldsRepository: BalanceHoldsRepository,
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
                balanceHoldsRepository.observeHoldsForMetaAccount(metaAccount.id),
                externalBalancesFlow
            ) { assets, locks, holds, externalBalances ->
                if (assets == null) {
                    BalanceBreakdown.empty()
                } else {
                    val assetsByChainId = assets.associateBy { it.token.configuration.fullId }
                    val locksItems = mapLocks(assetsByChainId, locks.orEmpty())
                    val holdsItems = mapHolds(assetsByChainId, holds.orEmpty())
                    val externalBalancesItems = mapExternalBalances(assetsByChainId, externalBalances.orEmpty())

                    val holdsByAsset = holds.orEmpty()
                        .groupBy { it.chainAsset.fullId }
                        .mapValues { (_, holds) -> holds.sumByBigInteger { it.amountInPlanks } }
                    val reserved = getReservedBreakdown(assets, holdsByAsset)

                    val breakdown = locksItems + holdsItems + externalBalancesItems + reserved

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

    private fun mapHolds(
        assetsByChainId: Map<FullChainAssetId, Asset>,
        holds: List<BalanceHold>
    ): List<BalanceBreakdown.BreakdownItem> {
        return holds.mapNotNull { hold ->
            assetsByChainId[hold.chainAsset.fullId]?.let { asset ->
                BalanceBreakdown.BreakdownItem(
                    id = hold.identifier,
                    token = asset.token,
                    amountInPlanks = hold.amountInPlanks,
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

    private fun getReservedBreakdown(assets: List<Asset>, holds: Map<FullChainAssetId, Balance>): List<BalanceBreakdown.BreakdownItem> {
        return assets
            .filter { it.reservedInPlanks > BigInteger.ZERO }
            .mapNotNull {
                val labeledReserves = holds[it.token.configuration.fullId].orZero()
                val unlabeledReserves = it.unlabeledReserves(labeledReserves)
                if (unlabeledReserves <= BigInteger.ZERO) return@mapNotNull null

                BalanceBreakdown.BreakdownItem(
                    id = BalanceBreakdownIds.RESERVED,
                    token = it.token,
                    amountInPlanks = unlabeledReserves
                )
            }
    }
}
