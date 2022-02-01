package io.novafoundation.nova.feature_wallet_impl.presentation.balance.list

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.address.AddressModel
import io.novafoundation.nova.common.address.createAddressModel
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.list.toListWithHeaders
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.formatAsCurrency
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.feature_account_api.data.mappers.mapChainToUi
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.domain.model.defaultSubstrateAddress
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletInteractor
import io.novafoundation.nova.feature_wallet_api.domain.model.AssetGroup
import io.novafoundation.nova.feature_wallet_impl.data.mappers.mapAssetToAssetModel
import io.novafoundation.nova.feature_wallet_impl.presentation.AssetPayload
import io.novafoundation.nova.feature_wallet_impl.presentation.WalletRouter
import io.novafoundation.nova.feature_wallet_impl.presentation.balance.list.model.AssetGroupUi
import io.novafoundation.nova.feature_wallet_impl.presentation.balance.list.model.TotalBalanceModel
import io.novafoundation.nova.feature_wallet_impl.presentation.model.AssetModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private const val CURRENT_ICON_SIZE = 40

class BalanceListViewModel(
    private val interactor: WalletInteractor,
    private val addressIconGenerator: AddressIconGenerator,
    private val selectedAccountUseCase: SelectedAccountUseCase,
    private val router: WalletRouter,
) : BaseViewModel() {

    private val _hideRefreshEvent = MutableLiveData<Event<Unit>>()
    val hideRefreshEvent: LiveData<Event<Unit>> = _hideRefreshEvent

    val currentAddressModelFlow = currentAddressModelFlow()
        .inBackground()
        .share()

    private val balancesFlow = interactor.balancesFlow()
        .inBackground()
        .share()

    val assetsFlow = balancesFlow.map { balances ->
        balances.assets
            .mapKeys { (assetGroup, _) -> mapAssetGroupToUi(assetGroup) }
            .mapValues { (_, assets) -> assets.map(::mapAssetToAssetModel) }
            .toListWithHeaders()
    }
        .distinctUntilChanged()
        .inBackground()
        .share()

    val totalBalanceFlow = balancesFlow.map {
        TotalBalanceModel(
            totalBalanceFiat = it.totalBalanceFiat.formatAsCurrency(),
            lockedBalanceFiat = it.lockedBalanceFiat.formatAsCurrency()
        )
    }
        .inBackground()
        .share()

    fun sync() {
        viewModelScope.launch {
            val result = interactor.syncAssetsRates()

            result.exceptionOrNull()?.let(::showError)

            _hideRefreshEvent.value = Event(Unit)
        }
    }

    fun assetClicked(asset: AssetModel) {
        val payload = AssetPayload(
            chainId = asset.token.configuration.chainId,
            chainAssetId = asset.token.configuration.id
        )

        router.openAssetDetails(payload)
    }

    fun avatarClicked() {
        router.openChangeAccount()
    }

    private fun mapAssetGroupToUi(assetGroup: AssetGroup) = AssetGroupUi(
        chainUi = mapChainToUi(assetGroup.chain),
        groupBalanceFiat = assetGroup.groupBalanceFiat.formatAsCurrency()
    )

    private fun currentAddressModelFlow(): Flow<AddressModel> {
        return selectedAccountUseCase.selectedMetaAccountFlow()
            .map { addressIconGenerator.createAddressModel(it.defaultSubstrateAddress, CURRENT_ICON_SIZE, it.name) }
    }
}
