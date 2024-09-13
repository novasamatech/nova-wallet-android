package io.novafoundation.nova.feature_governance_impl.presentation.tindergov.basket

import androidx.annotation.ColorRes
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.common.mixin.actionAwaitable.awaitAction
import io.novafoundation.nova.common.mixin.actionAwaitable.confirmingAction
import io.novafoundation.nova.common.mixin.actionAwaitable.confirmingOrDenyingAction
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.colorSpan
import io.novafoundation.nova.common.utils.formatting.spannable.SpannableFormatter
import io.novafoundation.nova.common.utils.toSpannable
import io.novafoundation.nova.common.utils.toggle
import io.novafoundation.nova.feature_governance_api.data.model.TinderGovBasketItem
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.VoteType
import io.novafoundation.nova.feature_governance_api.domain.referendum.list.ReferendumPreview
import io.novafoundation.nova.feature_governance_api.domain.tindergov.TinderGovInteractor
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.feature_governance_impl.data.GovernanceSharedState
import io.novafoundation.nova.feature_governance_impl.presentation.GovernanceRouter
import io.novafoundation.nova.feature_governance_impl.presentation.common.info.ReferendumInfoPayload
import io.novafoundation.nova.feature_governance_impl.presentation.common.voters.VotersFormatter
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.common.ReferendumFormatter
import io.novafoundation.nova.feature_governance_impl.presentation.tindergov.basket.adpter.TinderGovBasketRvItem
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.state.chainAsset
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class TinderGovBasketViewModel(
    private val governanceSharedState: GovernanceSharedState,
    private val router: GovernanceRouter,
    private val interactor: TinderGovInteractor,
    private val votersFormatter: VotersFormatter,
    private val referendumFormatter: ReferendumFormatter,
    private val actionAwaitableMixinFactory: ActionAwaitableMixin.Factory,
    private val resourceManager: ResourceManager
) : BaseViewModel() {

    val removeReferendumAction = actionAwaitableMixinFactory.confirmingOrDenyingAction<String>()

    val itemsWasRemovedFromBasketAction = actionAwaitableMixinFactory.confirmingAction<Unit>()

    val inEditModeFlow = MutableStateFlow(false)

    private val availableToVoteReferendaFlow = interactor.observeReferendaAvailableToVote(coroutineScope = this)
        .map { it.associateBy { it.id } }
        .shareInBackground()

    private val basketItemsFlow = interactor.observeTinderGovBasket()
        .shareInBackground()

    val editModeButtonText = inEditModeFlow.map {
        if (it) {
            resourceManager.getString(R.string.common_done)
        } else {
            resourceManager.getString(R.string.common_edit)
        }
    }

    val basketFlow = combine(availableToVoteReferendaFlow, basketItemsFlow) { referendaById, basketItems ->
        basketItems.mapNotNull {
            val referendum = referendaById[it.referendumId] ?: return@mapNotNull null
            mapBasketItem(governanceSharedState.chainAsset(), it, referendum)
        }
    }

    init {
        validateReferendaAndRemove()
    }

    fun backClicked() {
        router.back()
    }

    fun onItemClicked(item: TinderGovBasketRvItem) {
        router.openReferendumInfo(ReferendumInfoPayload(item.id.value))
    }

    fun onItemDeleteClicked(item: TinderGovBasketRvItem) {
        launch {
            val refId = referendumFormatter.formatId(item.id)
            val title = resourceManager.getString(R.string.tinder_gov_basket_remove_item_confirm_title, refId)

            if (removeReferendumAction.awaitAction(title)) {
                val referendum = basketItemsFlow.first()
                    .associateBy { it.referendumId }
                    .get(item.id) ?: return@launch

                interactor.removeReferendumFromBasket(referendum)

                closeScreenIfBasketIsEmpty()
            }
        }
    }

    fun voteClicked() {
        router.openConfirmTinderGovVote()
    }

    fun toggleEditMode() {
        inEditModeFlow.toggle()
    }

    private fun mapBasketItem(
        chainAsset: Chain.Asset,
        item: TinderGovBasketItem,
        referendum: ReferendumPreview
    ): TinderGovBasketRvItem {
        val voteType = when (item.voteType) {
            VoteType.AYE -> resourceManager.getString(R.string.tinder_gov_aye_format).withColor(R.color.text_positive)
            VoteType.NAY -> resourceManager.getString(R.string.tinder_gov_nay_format).withColor(R.color.text_negative)
            VoteType.ABSTAIN -> resourceManager.getString(R.string.tinder_gov_abstain_format).withColor(R.color.text_secondary)
        }

        val votesAmount = chainAsset.amountFromPlanks(item.amount)
        val votes = votersFormatter.formatVotes(votesAmount, item.voteType, item.conviction)
            .withColor(R.color.text_secondary)

        return TinderGovBasketRvItem(
            id = item.referendumId,
            idStr = referendumFormatter.formatId(item.referendumId),
            title = referendumFormatter.formatReferendumName(referendum),
            subtitle = SpannableFormatter.format(voteType, votes)
        )
    }

    private fun String.withColor(@ColorRes color: Int): CharSequence {
        return toSpannable(colorSpan(resourceManager.getColor(color)))
    }

    private fun validateReferendaAndRemove() {
        launch {
            val basket = basketItemsFlow.first()
                .associateBy { it.referendumId }

            val availableToVoteReferenda = availableToVoteReferendaFlow.first()

            val removedReferenda = basket.filterKeys { availableToVoteReferenda.contains(it).not() }
            if (removedReferenda.isNotEmpty()) {
                interactor.removeBasketItems(removedReferenda.values)

                itemsWasRemovedFromBasketAction.awaitAction()

                closeScreenIfBasketIsEmpty()
            }
        }
    }

    private suspend fun closeScreenIfBasketIsEmpty() {
        if (interactor.isBasketEmpty()) {
            router.back()
        }
    }
}
