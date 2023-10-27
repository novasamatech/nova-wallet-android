package io.novafoundation.nova.feature_swap_impl.presentation.state

import io.novafoundation.nova.common.utils.Percent
import io.novafoundation.nova.feature_swap_api.domain.model.SwapDirection
import io.novafoundation.nova.feature_swap_api.domain.model.flip
import io.novafoundation.nova.feature_swap_api.presentation.state.SwapSettings
import io.novafoundation.nova.feature_swap_api.presentation.state.SwapSettingsState
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.runtime.ext.commissionAsset
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.flow.MutableStateFlow

class RealSwapSettingsState(
    private val chainRegistry: ChainRegistry,
    initialValue: SwapSettings = SwapSettings(),
) : SwapSettingsState {

    override val selectedOption = MutableStateFlow(initialValue)

    override suspend fun setAssetInUpdatingFee(asset: Chain.Asset) {
        val current = selectedOption.value
        val chain = chainRegistry.getChain(asset.chainId)

        val new = if (current.feeAsset == null || current.feeAsset!!.chainId != chain.id) {
            current.copy(assetIn = asset, feeAsset = chain.commissionAsset, amount = null)
        } else {
            current.copy(assetIn = asset, amount = null)
        }

        selectedOption.value = new
    }

    override fun setAssetOut(asset: Chain.Asset) {
        selectedOption.value = selectedOption.value.copy(assetOut = asset)
    }

    override fun setFeeAsset(asset: Chain.Asset) {
        selectedOption.value = selectedOption.value.copy(feeAsset = asset)
    }

    override fun setAmount(amount: Balance?, swapDirection: SwapDirection) {
        selectedOption.value = selectedOption.value.copy(amount = amount, swapDirection = swapDirection)
    }

    override fun setSlippage(slippage: Percent) {
        selectedOption.value = selectedOption.value.copy(slippage = slippage)
    }

    override suspend fun flipAssets(): SwapSettings {
        val currentSettings = selectedOption.value

        val newAssetIn = currentSettings.assetOut
        val chain = newAssetIn?.chainId?.let { chainRegistry.getChain(it) }

        val newSettings = currentSettings.copy(
            assetIn = currentSettings.assetOut,
            assetOut = currentSettings.assetIn,
            feeAsset = chain?.commissionAsset, // we reset commission asset during flipping to ensure we only allow to pay in commissionAsset or assetIn
            swapDirection = currentSettings.swapDirection?.flip()
        )
        selectedOption.value = newSettings

        return newSettings
    }
}
