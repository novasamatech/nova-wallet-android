package io.novafoundation.nova.feature_assets.domain.breakdown

import io.novafoundation.nova.common.utils.accumulateFlatten
import io.novafoundation.nova.common.utils.percentageOf
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_assets.domain.breakdown.BalanceBreakdown.Companion.CROWDLOAN_ID
import io.novafoundation.nova.feature_assets.domain.locks.BalanceLocksRepository
import io.novafoundation.nova.feature_crowdloan_api.data.repository.ContributionsRepository
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.runtime.ext.utilityAsset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigDecimal
import java.math.BigInteger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

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
        val metaAccountFlow = accountRepository.selectedMetaAccountFlow()

        val locksFlow = metaAccountFlow.flatMapLatest { metaAccount ->
            locksBreakdownFlow(assetsFlow, metaAccount)
        }
        val contributionsFlow = metaAccountFlow.flatMapLatest { metaAccount ->
            contributionsBreakdownFlow(assetsFlow, metaAccount)
        }
        val reservedFlow = assetsFlow.map { getReservedBreakdown(it) }

        val breakdownFlow = accumulateFlatten(locksFlow, contributionsFlow, reservedFlow)

        return combine(assetsFlow, breakdownFlow) { assets, breakdown ->
            val contributions = contributionsFlow.first()

            val totalAmount = calculateTotalBalance(assets, contributions)
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

    private fun locksBreakdownFlow(assetsFlow: Flow<List<Asset>>, metaAccount: MetaAccount): Flow<List<BalanceBreakdown.BreakdownItem>> {
        return combine(assetsFlow, balanceLocksRepository.observeLocksForMetaAccount(metaAccount)) { assets, locks ->
            val assetsByChainId = assets.associateBy { it.token.configuration.chainId to it.token.configuration.id }
            locks.filter { assetsByChainId.containsKey(it.chainAsset.chainId to it.chainAsset.id) }
                .map { lock ->
                    val token = assetsByChainId.getValue(lock.chainAsset.chainId to lock.chainAsset.id)
                        .token

                    val amount = token.amountFromPlanks(lock.amountInPlanks)
                    BalanceBreakdown.BreakdownItem(
                        lock.id,
                        lock.chainAsset,
                        token.priceOf(amount)
                    )
                }
        }
    }

    private fun contributionsBreakdownFlow(assetsFlow: Flow<List<Asset>>, metaAccount: MetaAccount): Flow<List<BalanceBreakdown.BreakdownItem>> {
        return combine(assetsFlow, contributionsRepository.observeContributions(metaAccount)) { assets, contributions ->
            val assetsByChainId = assets.associateBy { it.token.configuration.chainId to it.token.configuration.id }

            contributions.groupBy { it.chain.id to it.chain.utilityAsset.id }
                .filter { (chainAndAssetId, _) -> assetsByChainId.containsKey(chainAndAssetId) }
                .map { (chainAndAssetId, contributions) ->
                    val token = assetsByChainId.getValue(chainAndAssetId).token

                    val totalAmountInPlanks = contributions.sumOf { it.amountInPlanks }
                    BalanceBreakdown.BreakdownItem(
                        CROWDLOAN_ID,
                        token.configuration,
                        token.priceOf(token.amountFromPlanks(totalAmountInPlanks))
                    )
                }
        }
    }
}
