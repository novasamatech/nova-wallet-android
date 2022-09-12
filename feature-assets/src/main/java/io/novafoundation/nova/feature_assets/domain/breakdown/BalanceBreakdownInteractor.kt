package io.novafoundation.nova.feature_assets.domain.breakdown

import io.novafoundation.nova.common.utils.percentageOf
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_assets.domain.locks.BalanceLocksRepository
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
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
    val transferableTotal: BigDecimal,
    val transferablePercentage: Float,
    val locksTotal: BigDecimal,
    val locksPercentage: Float,
    val breakdown: List<BreakdownItem>
) {
    companion object {
        const val RESERVED = "reserved"
        const val CROWDLOAN = "crowdloan"
    }

    class BreakdownItem(val id: String, val chainAsset: Chain.Asset, val fiatAmount: BigDecimal)
}

class BalanceBreakdownInteractor(
    private val accountRepository: AccountRepository,
    private val walletRepository: WalletRepository,
    private val balanceLocksRepository: BalanceLocksRepository,
) {

    fun balanceBreakdownFlow(assetsFlow: Flow<List<Asset>>): Flow<BalanceBreakdown> {
        val metaAccountFlow = accountRepository.selectedMetaAccountFlow()
        val locksFlow = metaAccountFlow
            .flatMapLatest { balanceLocksRepository.observeLocksForMetaAccount(it) }

        return combine(assetsFlow, locksFlow) { assets, locks ->
            val (total, transferableTotal, locksTotal) = balanceFromAssets(assets)

            BalanceBreakdown(
                total,
                transferableTotal,
                transferableTotal.percentageOf(total),
                locksTotal,
                locksTotal.percentageOf(total),
                mapBalanceBreakdown(assets, locks)
            )
        }
    }

    private fun balanceFromAssets(
        assets: List<Asset>
    ): Triple<BigDecimal, BigDecimal, BigDecimal> {
        return assets.fold(Triple(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO)) { (total, transferable, locked), asset ->
            val assetTotalFiat = asset.token.priceOf(asset.total)
            val assetTransferableFiat = asset.token.priceOf(asset.transferable)
            val assetLockedFiat = asset.token.priceOf(asset.locked)

            Triple((total + assetTotalFiat), (transferable + assetTransferableFiat), (locked + assetLockedFiat))
        }
    }

    private fun mapBalanceBreakdown(assets: List<Asset>, locks: List<BalanceLock>): List<BalanceBreakdown.BreakdownItem> {
        val assetsByChainId = assets.groupBy { it.token.configuration.chainId }
            .mapValues { entry ->
                entry.value.associateBy { it.token.configuration.id }
            }

        val breakdownAssets = assets
            .filter { it.reservedInPlanks > BigInteger.ZERO }
            .map {
                BalanceBreakdown.BreakdownItem(
                    BalanceBreakdown.RESERVED,
                    it.token.configuration,
                    it.reserved
                )
            }
        val breakdownLocks = locks.map { lock ->
            val token = assetsByChainId.getValue(lock.chainAsset.chainId)
                .getValue(lock.chainAsset.id)
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
