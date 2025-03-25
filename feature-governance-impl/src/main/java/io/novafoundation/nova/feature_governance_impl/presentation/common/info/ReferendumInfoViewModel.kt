package io.novafoundation.nova.feature_governance_impl.presentation.common.info

import androidx.lifecycle.viewModelScope
import io.noties.markwon.Markwon
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.domain.filterLoaded
import io.novafoundation.nova.common.domain.isLoading
import io.novafoundation.nova.common.domain.mapLoading
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.flowOfAll
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.common.utils.withSafeLoading
import io.novafoundation.nova.feature_account_api.presenatation.account.icon.createIdentityAddressModel
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_account_api.presenatation.actions.showAddressActions
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ReferendumId
import io.novafoundation.nova.feature_governance_api.domain.referendum.details.ReferendumDetailsInteractor
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.feature_governance_impl.data.GovernanceSharedState
import io.novafoundation.nova.feature_governance_impl.domain.identity.GovernanceIdentityProviderFactory
import io.novafoundation.nova.feature_governance_impl.presentation.GovernanceRouter
import io.novafoundation.nova.feature_governance_impl.presentation.common.share.ShareReferendumMixin
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.common.ReferendumFormatter
import io.novafoundation.nova.runtime.state.chain
import io.novafoundation.nova.runtime.state.chainAsset
import io.novafoundation.nova.runtime.state.selectedOption
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class ReferendumInfoViewModel(
    private val router: GovernanceRouter,
    private val payload: ReferendumInfoPayload,
    private val interactor: ReferendumDetailsInteractor,
    private val selectedAssetSharedState: GovernanceSharedState,
    private val referendumFormatter: ReferendumFormatter,
    private val resourceManager: ResourceManager,
    private val governanceIdentityProviderFactory: GovernanceIdentityProviderFactory,
    private val addressIconGenerator: AddressIconGenerator,
    private val externalActions: ExternalActions.Presentation,
    val shareReferendumMixin: ShareReferendumMixin,
    val markwon: Markwon,
) : BaseViewModel(), ExternalActions by externalActions {

    val referendumDetailsFlow = flowOfAll {
        val governanceOption = selectedAssetSharedState.selectedOption()
        interactor.referendumDetailsFlow(ReferendumId(payload.referendumId), governanceOption, voterAccountId = null, viewModelScope)
    }
        .filterNotNull()
        .withSafeLoading()
        .shareInBackground()

    val titleFlow = referendumDetailsFlow.filterLoaded()
        .map { referendumFormatter.formatReferendumName(it) }

    val subtitleFlow = referendumDetailsFlow.filterLoaded()
        .map {
            val subtitle = it.offChainMetadata?.description ?: resourceManager.getString(R.string.referendum_description_fallback)
            markwon.toMarkdown(subtitle)
        }

    val idFlow = referendumDetailsFlow.filterLoaded()
        .map { referendumFormatter.formatId(it.id) }

    val trackFlow = referendumDetailsFlow.filterLoaded()
        .map {
            val chainAsset = selectedAssetSharedState.chainAsset()
            it.track?.let { referendumFormatter.formatReferendumTrack(it, chainAsset) }
        }

    private val proposerFlow = referendumDetailsFlow.mapLoading { it.proposer }
        .filterLoaded()

    private val proposerIdentityProvider = governanceIdentityProviderFactory.proposerProvider(proposerFlow)

    val proposerAddressModel = referendumDetailsFlow.filterLoaded()
        .map {
            it.proposer?.let { proposer ->
                addressIconGenerator.createIdentityAddressModel(
                    chain = selectedAssetSharedState.chain(),
                    accountId = proposer.accountId,
                    identityProvider = proposerIdentityProvider
                )
            }
        }.inBackground()
        .shareWhileSubscribed()

    val timeEstimation = referendumDetailsFlow.mapLoading {
        referendumFormatter.formatTimeEstimation(it.timeline.currentStatus)
    }.filterLoaded()

    val isLoadingState = referendumDetailsFlow.map { it.isLoading() }

    fun backClicked() {
        router.back()
    }

    fun shareButtonClicked() {
        launch {
            val selectedOption = selectedAssetSharedState.selectedOption()
            shareReferendumMixin.shareReferendum(
                payload.referendumId,
                selectedOption.assetWithChain.chain,
                selectedOption.additional.governanceType
            )
        }
    }

    fun proposerClicked() = launch {
        val proposer = proposerAddressModel.first()?.address ?: return@launch

        externalActions.showAddressActions(proposer, selectedAssetSharedState.chain())
    }
}
