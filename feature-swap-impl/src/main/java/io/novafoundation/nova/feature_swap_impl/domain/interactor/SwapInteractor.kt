package io.novafoundation.nova.feature_swap_impl.domain.interactor

import io.novafoundation.nova.common.utils.Percent
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicHash
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_swap_api.domain.model.SwapDirection
import io.novafoundation.nova.feature_swap_api.domain.model.SwapQuote
import io.novafoundation.nova.feature_swap_api.domain.model.flip
import io.novafoundation.nova.feature_swap_api.domain.swap.SwapService
import io.novafoundation.nova.feature_swap_impl.data.SwapSettings
import io.novafoundation.nova.feature_swap_impl.data.SwapSettingsSharedState
import io.novafoundation.nova.feature_swap_impl.data.toArgs
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigInteger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull

class SwapSettingsNotReadyException : Exception()

class SwapInteractor(
    private val swapService: SwapService,
    private val swapSharedState: SwapSettingsSharedState,
    private val walletRepository: WalletRepository,
    private val accountRepository: AccountRepository
) {

    fun assetIn(): Flow<Asset?> {
        return combine(accountRepository.selectedMetaAccountFlow(), swapSharedState.selectedOption) { metaAccount, settings ->
            val asset = settings.assetIn ?: return@combine null
            walletRepository.getAsset(metaAccount.id, asset) ?: return@combine null
        }.distinctUntilChanged()
    }

    fun assetOut(): Flow<Asset?> {
        return combine(accountRepository.selectedMetaAccountFlow(), swapSharedState.selectedOption) { metaAccount, settings ->
            val asset = settings.assetOut ?: return@combine null
            walletRepository.getAsset(metaAccount.id, asset) ?: return@combine null
        }.distinctUntilChanged()
    }

    fun quotes(): Flow<Result<SwapQuote>> {
        //TODO: mapLatest
        return swapSharedState.selectedOption
            .mapNotNull { it.toArgs() }
            .map { swapService.quote(it) }
    }

    fun settings(): Flow<SwapSettings> {
        return swapSharedState.selectedOption
    }

    suspend fun swap(): Result<ExtrinsicHash> {
        val settings = swapSharedState.selectedOption.value
        val args = settings.toArgs() ?: return Result.failure(SwapSettingsNotReadyException())
        return swapService.swap(args)
    }

    fun setAssetIn(assetIn: Chain.Asset) {
        swapSharedState.setAssetIn(assetIn)
    }

    fun setAssetOut(assetOut: Chain.Asset) {
        swapSharedState.setAssetOut(assetOut)
    }

    fun setAmount(amount: BigInteger, swapDirection: SwapDirection) {
        swapSharedState.setAmount(amount, swapDirection)
    }

    fun setSlippage(slippage: Percent) {
        swapSharedState.setSlippage(slippage)
    }

    fun flipAssets() {
        val settings = swapSharedState.selectedOption.value
        swapSharedState.setState(
            settings.copy(
                assetIn = settings.assetOut,
                assetOut = settings.assetIn,
                swapDirection = settings.swapDirection?.flip()
            )
        )
    }

    fun clear() {
        swapSharedState.clear()
    }
}
