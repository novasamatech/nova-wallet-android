package io.novafoundation.nova.feature_swap_impl.domain.interactor

import io.novafoundation.nova.common.utils.Percent
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicHash
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_swap_api.domain.model.SwapDirection
import io.novafoundation.nova.feature_swap_api.domain.model.SwapFee
import io.novafoundation.nova.feature_swap_api.domain.model.SwapQuote
import io.novafoundation.nova.feature_swap_api.domain.model.flip
import io.novafoundation.nova.feature_swap_api.domain.swap.SwapService
import io.novafoundation.nova.feature_swap_impl.data.SwapSettings
import io.novafoundation.nova.feature_swap_impl.data.SwapSettingsSharedState
import io.novafoundation.nova.feature_swap_impl.data.toExecuteArgs
import io.novafoundation.nova.feature_swap_impl.data.toQuoteArgs
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.runtime.ext.commissionAsset
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
import java.math.BigInteger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.mapNotNull

class SwapSettingsNotReadyException : Exception()

class SwapInteractor(
    private val swapService: SwapService,
    private val swapSharedState: SwapSettingsSharedState,
    private val chainRegistry: ChainRegistry,
    private val walletRepository: WalletRepository,
    private val accountRepository: AccountRepository,
) {

    suspend fun availableAssets(coroutineScope: CoroutineScope): List<Asset> {
        val chainsWithAssets = swapService.assetsAvailableForSwap(coroutineScope)
        val metaAccount = accountRepository.getSelectedMetaAccount()
        return walletRepository.getSupportedAssets(metaAccount.id)
            .filter {
                val chainAsset = it.token.configuration
                val fullId = FullChainAssetId(chainAsset.chainId, chainAsset.id)
                chainsWithAssets.contains(fullId)
            }
    }

    fun assetIn(): Flow<Asset?> {
        return swapSharedState.selectedOption
            .map { it.assetIn }
            .distinctUntilChanged()
    }

    fun assetOut(): Flow<Asset?> {
        return swapSharedState.selectedOption
            .map { it.assetOut }
            .distinctUntilChanged()
    }

    fun feeAsset(): Flow<Asset?> {
        return swapSharedState.selectedOption
            .mapNotNull { it.feeAsset }
            .distinctUntilChanged()
            .map {
                val metaAccount = accountRepository.getSelectedMetaAccount()
                walletRepository.getAsset(metaAccount.id, it)
            }
    }

    fun validationFlow(): Flow<String?> {
        return flowOf { "" }
    }

    fun quotes(): Flow<Result<SwapQuote?>> {
        return swapSharedState.selectedOption
            .map { it.toQuoteArgs() }
            .mapLatest {
                if (it == null) {
                    return@mapLatest Result.success(null)
                }

                swapService.quote(it)
            }
    }

    fun fee(): Flow<SwapFee> {
        return swapSharedState.selectedOption
            .mapNotNull { it.toExecuteArgs() }
            .mapLatest {
                swapService.estimateFee(it)
            }
    }

    // put it to the validation
    fun observeAssetsPairAvailability(coroutineScope: CoroutineScope): Flow<Boolean> {
        return swapSharedState.selectedOption
            .map { it.assetOut?.token?.configuration to it.assetIn?.token?.configuration }
            .distinctUntilChanged()
            .map { (assetOut, assetIn) ->
                assetOut ?: return@map true
                assetIn ?: return@map true
                swapService.availableSwapDirectionsFor(assetOut, coroutineScope)
                    .any { it.chainId == assetIn.chainId && it.assetId == assetIn.id }
            }
    }

    fun settings(): Flow<SwapSettings> {
        return swapSharedState.selectedOption
    }

    suspend fun swap(): Result<ExtrinsicHash> {
        val settings = swapSharedState.selectedOption.value
        val args = settings.toExecuteArgs() ?: return Result.failure(SwapSettingsNotReadyException())
        return swapService.swap(args)
    }

    suspend fun setAssetIn(asset: Asset) {
        swapSharedState.setAssetIn(asset)

        if (swapSharedState.selectedOption.value.feeAsset == null) {
            val chain = chainRegistry.getChain(asset.token.configuration.chainId)
            swapSharedState.setFeeAsset(chain.commissionAsset)
        }
    }

    fun setAssetOut(asset: Asset) {
        swapSharedState.setAssetOut(asset)
    }

    fun setAmount(amount: BigInteger?, swapDirection: SwapDirection) {
        swapSharedState.setAmount(amount, swapDirection)
    }

    fun setSlippage(slippage: Percent) {
        swapSharedState.setSlippage(slippage)
    }

    fun flipAssets(): SwapSettings {
        val settings = swapSharedState.selectedOption.value
        val newSettings = settings.copy(
            assetIn = settings.assetOut,
            assetOut = settings.assetIn,
            swapDirection = settings.swapDirection?.flip()
        )

        swapSharedState.setState(newSettings)

        return newSettings
    }

    fun clear() {
        swapSharedState.clear()
    }
}
