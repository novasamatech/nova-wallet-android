package io.novafoundation.nova.feature_assets.presentation.receive.flow

import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_assets.domain.assets.search.AssetSearchInteractor
import io.novafoundation.nova.feature_assets.domain.common.AssetGroup
import io.novafoundation.nova.feature_assets.domain.common.AssetWithOffChainBalance
import io.novafoundation.nova.feature_assets.presentation.AssetPayload
import io.novafoundation.nova.feature_assets.presentation.AssetsRouter
import io.novafoundation.nova.feature_assets.presentation.balance.common.ControllableAssetCheckMixin
import io.novafoundation.nova.feature_assets.presentation.flow.AssetFlowViewModel
import io.novafoundation.nova.feature_assets.presentation.model.AssetModel
import io.novafoundation.nova.feature_crowdloan_api.domain.contributions.ContributionsInteractor
import io.novafoundation.nova.feature_currency_api.domain.CurrencyInteractor
import kotlinx.coroutines.flow.Flow

class AssetReceiveFlowViewModel(
    router: AssetsRouter,
    interactor: AssetSearchInteractor,
    currencyInteractor: CurrencyInteractor,
    contributionsInteractor: ContributionsInteractor,
    controllableAssetCheck: ControllableAssetCheckMixin,
    accountUseCase: SelectedAccountUseCase,
) : AssetFlowViewModel(
    router,
    interactor,
    currencyInteractor,
    contributionsInteractor,
    controllableAssetCheck,
    accountUseCase
) {
    override fun searchAssetsFlow(): Flow<Map<AssetGroup, List<AssetWithOffChainBalance>>> {
        return interactor.searchAssetsFlow(query, totalContributedByAssetsFlow)
    }

    override fun assetClicked(assetModel: AssetModel) {
        validate(assetModel) {
            openNextScreen(assetModel)
        }
    }

    private fun openNextScreen(assetModel: AssetModel) {
        val chainAsset = assetModel.token.configuration
        val assePayload = AssetPayload(chainAsset.chainId, chainAsset.id)
        router.openReceive(assePayload)
    }
}
