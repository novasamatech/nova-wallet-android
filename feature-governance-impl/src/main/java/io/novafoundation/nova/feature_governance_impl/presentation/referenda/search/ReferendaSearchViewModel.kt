package io.novafoundation.nova.feature_governance_impl.presentation.referenda.search

import android.util.Log
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.domain.mapLoading
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.LOG_TAG
import io.novafoundation.nova.common.utils.combineToPair
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.common.view.PlaceholderModel
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.domain.model.accountIdIn
import io.novafoundation.nova.feature_governance_api.domain.referendum.list.ReferendaListInteractor
import io.novafoundation.nova.feature_governance_api.domain.referendum.list.ReferendumPreview
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.feature_governance_impl.data.GovernanceSharedState
import io.novafoundation.nova.feature_governance_api.presentation.GovernanceRouter
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.common.ReferendumFormatter
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.common.list.ReferendaListStateModel
import io.novafoundation.nova.feature_governance_api.presentation.referenda.details.ReferendumDetailsPayload
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.list.model.ReferendumModel
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.assetSelector.AssetSelectorFactory
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.assetSelector.WithAssetSelector
import io.novafoundation.nova.runtime.state.chain
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest

class ReferendaSearchViewModel(
    assetSelectorFactory: AssetSelectorFactory,
    private val referendaListInteractor: ReferendaListInteractor,
    private val selectedAccountUseCase: SelectedAccountUseCase,
    private val selectedAssetSharedState: GovernanceSharedState,
    private val governanceRouter: GovernanceRouter,
    private val referendumFormatter: ReferendumFormatter,
    private val resourceManager: ResourceManager
) : BaseViewModel(), WithAssetSelector {

    override val assetSelectorMixin = assetSelectorFactory.create(
        scope = this,
        amountProvider = Asset::free
    )

    val queryFlow = MutableStateFlow("")

    private val accountAndChainFlow = combineToPair(selectedAccountUseCase.selectedMetaAccountFlow(), selectedAssetSharedState.selectedOption)

    private val referendaSearchFlow = accountAndChainFlow.flatMapLatest { (metaAccount, supportedOption) ->
        val chainAndAsset = supportedOption.assetWithChain
        val accountId = metaAccount.accountIdIn(chainAndAsset.chain)

        referendaListInteractor.searchReferendaListStateFlow(metaAccount, queryFlow, accountId, supportedOption, this)
    }
        .catch { Log.e(LOG_TAG, it.message, it) }
        .inBackground()
        .shareWhileSubscribed()

    val referendaUiFlow = referendaSearchFlow.mapLoading { referenda ->
        mapReferendaListToStateList(referenda)
    }
        .inBackground()
        .shareWhileSubscribed()

    private suspend fun mapReferendaListToStateList(referenda: List<ReferendumPreview>): ReferendaListStateModel {
        val asset = assetSelectorMixin.selectedAssetFlow.first()
        val chain = selectedAssetSharedState.chain()

        val placeholder = if (referenda.isEmpty()) {
            PlaceholderModel(
                resourceManager.getString(R.string.referenda_search_placeholder),
                R.drawable.ic_placeholder
            )
        } else {
            null
        }

        val referendaUi = referenda.map { referendumFormatter.formatReferendumPreview(it, asset.token, chain) }

        return ReferendaListStateModel(placeholder, referendaUi)
    }

    fun openReferendum(referendum: ReferendumModel) {
        val payload = ReferendumDetailsPayload(referendum.id.value)
        governanceRouter.openReferendum(payload)
    }

    fun cancelClicked() {
        governanceRouter.back()
    }
}
