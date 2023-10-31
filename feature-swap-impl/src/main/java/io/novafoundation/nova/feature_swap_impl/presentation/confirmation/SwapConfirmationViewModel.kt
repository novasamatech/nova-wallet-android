package io.novafoundation.nova.feature_swap_impl.presentation.confirmation

import io.novafoundation.nova.common.address.AddressModel
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Perbill
import io.novafoundation.nova.common.utils.asPerbill
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.common.utils.formatting.format
import io.novafoundation.nova.common.utils.images.Icon
import io.novafoundation.nova.common.utils.toPercent
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletModel
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_account_api.presenatation.chain.icon
import io.novafoundation.nova.feature_swap_impl.R
import io.novafoundation.nova.feature_swap_impl.presentation.common.PriceImpactFormatter
import io.novafoundation.nova.feature_swap_impl.presentation.common.SwapRateFormatter
import io.novafoundation.nova.feature_swap_impl.presentation.confirmation.SwapConfirmationPayload.AmountWithAsset
import io.novafoundation.nova.feature_swap_impl.presentation.confirmation.model.SwapConfirmationDetailsModel
import io.novafoundation.nova.feature_swap_impl.presentation.views.SwapAssetView
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.feature_wallet_api.presentation.model.AmountModel
import io.novafoundation.nova.feature_wallet_api.presentation.model.AssetPayload
import io.novafoundation.nova.feature_wallet_api.presentation.model.fullChainAssetId
import io.novafoundation.nova.feature_wallet_api.presentation.model.mapAmountToAmountModel
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.ChainWithAsset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chainWithAsset
import java.math.BigDecimal
import java.math.BigInteger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class SwapConfirmationViewModel(
    private val resourceManager: ResourceManager,
    private val swapConfirmationPayload: SwapConfirmationPayload,
    private val walletRepository: WalletRepository,
    private val accountRepository: AccountRepository,
    private val chainRegistry: ChainRegistry,
    private val swapRateFormatter: SwapRateFormatter,
    private val priceImpactFormatter: PriceImpactFormatter,
    private val walletUiUseCase: WalletUiUseCase
) : BaseViewModel() {

    val swapDetails = MutableStateFlow<SwapConfirmationDetailsModel?>(null)

    val wallet: Flow<WalletModel> = walletUiUseCase.selectedWalletUiFlow(showAddressIcon = true)
        .shareInBackground()

    val account: Flow<AddressModel> = flowOf {
        AddressModel("J6b7XsdA42gafKso3gSsd9KsapTtASfr2z4rPt", resourceManager.getDrawable(R.drawable.ic_nova_logo))
    }

    init {
        launch {
            initPayload()
        }
    }

    fun rateClicked() {
        TODO("Not yet implemented")
    }

    fun priceDifferenceClicked() {
        TODO("Not yet implemented")
    }

    fun slippageClicked() {
        TODO("Not yet implemented")
    }

    fun networkFeeClicked() {
        TODO("Not yet implemented")
    }

    fun accountClicked() {
        TODO("Not yet implemented")
    }

    fun confirmButtonClicked() {
        TODO("Not yet implemented")
    }

    private suspend fun initPayload() {
        val metaAccount = accountRepository.getSelectedMetaAccount()
        val chainWithAssetIn = getChainWithAsset(swapConfirmationPayload.amountWithAssetIn.assetPayload)
        val chainWithAssetOut = getChainWithAsset(swapConfirmationPayload.amountWithAssetOut.assetPayload)
        swapDetails.value = SwapConfirmationDetailsModel(
            assetInDetails = formatAssetDetails(metaAccount, chainWithAssetIn, swapConfirmationPayload.amountWithAssetIn.amount),
            assetOutDetails = formatAssetDetails(metaAccount, chainWithAssetOut, swapConfirmationPayload.amountWithAssetOut.amount),
            rate = formatRate(swapConfirmationPayload.rate, chainWithAssetIn.asset, chainWithAssetOut.asset),
            priceDifference = formatPriceDifference(swapConfirmationPayload.priceDifference.asPerbill()),
            slippage = swapConfirmationPayload.slippage.asPerbill().format(),
            networkFee = formatNetworkFee(metaAccount, swapConfirmationPayload.networkFee)
        )
    }

    private suspend fun getChainWithAsset(assetPayload: AssetPayload): ChainWithAsset {
        val fullChainAssetId = assetPayload.fullChainAssetId
        return chainRegistry.chainWithAsset(fullChainAssetId.chainId, fullChainAssetId.assetId)
    }

    private suspend fun formatAssetDetails(metaAccount: MetaAccount, chainWithAsset: ChainWithAsset, amountInPlanks: BigInteger): SwapAssetView.Model {
        val amount = formatAmount(metaAccount, chainWithAsset.asset, amountInPlanks)
        return SwapAssetView.Model(
            assetIcon = chainWithAsset.asset.icon(),
            amount = amount,
            networkImage = Icon.FromLink(chainWithAsset.chain.icon),
            networkName = chainWithAsset.chain.name
        )
    }

    private fun formatRate(rate: BigDecimal, assetIn: Chain.Asset, assetOut: Chain.Asset): String {
        return swapRateFormatter.format(rate, assetIn, assetOut)
    }

    private fun formatPriceDifference(priceDifference: Perbill): CharSequence? {
        return priceImpactFormatter.format(priceDifference.toPercent())
    }

    private suspend fun formatNetworkFee(metaAccount: MetaAccount, amountWithAsset: AmountWithAsset): AmountModel {
        val fullChainAssetId = amountWithAsset.assetPayload.fullChainAssetId
        val chainWithAsset = chainRegistry.chainWithAsset(fullChainAssetId.chainId, fullChainAssetId.assetId)
        return formatAmount(metaAccount, chainWithAsset.asset, amountWithAsset.amount)
    }

    private suspend fun formatAmount(metaAccount: MetaAccount, chainAsset: Chain.Asset, amount: BigInteger): AmountModel {
        val asset = walletRepository.getAsset(metaAccount.id, chainAsset)!!
        return mapAmountToAmountModel(amount, asset.token, includeZeroFiat = false, estimatedFiat = true)
    }
}
