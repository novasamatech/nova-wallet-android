package io.novafoundation.nova.feature_governance_impl.presentation.tindergov.cards

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.common.domain.isError
import io.novafoundation.nova.common.domain.isLoaded
import io.novafoundation.nova.common.domain.map
import io.novafoundation.nova.common.domain.orLoading
import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.common.mixin.actionAwaitable.confirmingOrDenyingAction
import io.novafoundation.nova.common.navigation.awaitResponse
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.combineToPair
import io.novafoundation.nova.common.utils.orFalse
import io.novafoundation.nova.common.utils.safeSubList
import io.novafoundation.nova.common.utils.sendEvent
import io.novafoundation.nova.feature_governance_api.data.model.TinderGovBasketItem
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ReferendumId
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.VoteType
import io.novafoundation.nova.feature_governance_api.domain.referendum.list.ReferendumPreview
import io.novafoundation.nova.feature_governance_api.domain.tindergov.TinderGovInteractor
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.feature_governance_impl.presentation.GovernanceRouter
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.common.ReferendumFormatter
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.vote.setup.tindergov.TinderGovVoteRequester
import io.novafoundation.nova.feature_governance_impl.presentation.tindergov.cards.adapter.TinderGovCardRvItem
import io.novafoundation.nova.feature_wallet_api.presentation.model.AmountModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class TinderGovCardsViewModel(
    private val router: GovernanceRouter,
    private val tinderGovCardsDataHelper: TinderGovCardsDataHelper,
    private val interactor: TinderGovInteractor,
    private val referendumFormatter: ReferendumFormatter,
    private val actionAwaitableMixinFactory: ActionAwaitableMixin.Factory,
    private val tinderGovVoteRequester: TinderGovVoteRequester
) : BaseViewModel() {

    private val cardsSummaryFlow = tinderGovCardsDataHelper.cardsSummaryFlow
        .shareInBackground()

    private val cardsAmountFlow = tinderGovCardsDataHelper.cardsAmountFlow
        .shareInBackground()

    private val topCardIndex = MutableStateFlow(0)
    private val sortedReferendaFlow = MutableStateFlow(listOf<ReferendumPreview>())

    private val basketFlow = interactor.observeTinderGovBasket()
        .map { it.associateBy { it.referendumId } }
        .shareInBackground()

    val cardsFlow = combine(
        sortedReferendaFlow,
        cardsSummaryFlow,
        cardsAmountFlow,
        ::mapCards
    )

    val retryReferendumInfoLoadingAction = actionAwaitableMixinFactory.confirmingOrDenyingAction<Unit>()

    private val _skipCardEvent = MutableLiveData<Event<Unit>>()
    val skipCardEvent: LiveData<Event<Unit>> = _skipCardEvent

    private val _rewindCardEvent = MutableLiveData<Event<Unit>>()
    val rewindCardEvent: LiveData<Event<Unit>> = _rewindCardEvent

    private val topReferendumWithDetails = combine(topCardIndex, cardsSummaryFlow, cardsAmountFlow) { topCardIndex, cardsSummary, cardsAmount ->
        val referendum = sortedReferendaFlow.value.getOrNull(topCardIndex) ?: return@combine null
        val cardSummary = cardsSummary[referendum.id]
        val cardAmount = cardsAmount[referendum.id]

        Triple(referendum, cardSummary, cardAmount)
    }.filterNotNull()
        .distinctUntilChangedBy { it.first.id to it.second to it.third }
        .shareInBackground()

    val isCardDraggingAvailable = topReferendumWithDetails
        .map { (_, summary, amount) ->
            val summaryLoaded = summary?.isLoaded().orFalse()
            val amountLoaded = amount?.isLoaded().orFalse()
            summaryLoaded && amountLoaded
        }

    val basketModelFlow = basketFlow
        .map { items -> mapBasketModel(items.values.toList()) }

    init {
        val referendaWithBasket = combineToPair(interactor.observeReferendaAvailableToVote(this), basketFlow)
        referendaWithBasket.onEach { (referenda, basket) ->
            val currentReferendaIds = sortedReferendaFlow.value.map { it.id }.toSet()
            val newReferenda = referenda.associateBy { it.id }
                .filter { it.key !in currentReferendaIds }
                .filter { it.key !in basket }

            sortedReferendaFlow.value += newReferenda.values // To add new coming referenda to the end of list
        }.launchIn(this)

        setupReferendumRetryAction()

        tinderGovCardsDataHelper.init(this)

        loadFirstCards()
    }

    fun back() {
        router.back()
    }

    fun ayeClicked(position: Int) = writeVote(position, VoteType.AYE)

    fun abstainClicked(position: Int) = writeVote(position, VoteType.ABSTAIN)

    fun nayClicked(position: Int) = writeVote(position, VoteType.NAY)

    fun openReadMore(item: TinderGovCardRvItem) {
        showMessage("Not implemented yet")
    }

    fun onCardOnTop(position: Int) {
        topCardIndex.value = position
    }

    fun loadContentForCardsFromPosition(position: Int) {
        // Get 3 first referenda to load content for
        val referenda = sortedReferendaFlow.value.safeSubList(position, position + 3)

        // Load summary and amount for each referendum
        referenda.forEach {
            tinderGovCardsDataHelper.loadSummary(it, this)
            tinderGovCardsDataHelper.loadAmount(it, this)
        }
    }

    private fun loadFirstCards() {
        launch {
            sortedReferendaFlow
                .filter { it.isNotEmpty() }
                .first() // Await while list of cards will be not empty

            loadContentForCardsFromPosition(0)
        }
    }

    private fun skipCard() {
        _skipCardEvent.sendEvent()
    }

    private fun writeVote(position: Int, voteType: VoteType) = launch {
        val referendum = sortedReferendaFlow.value.getOrNull(position) ?: return@launch

        if (!interactor.isSufficientAmountToVote()) {
            val response = tinderGovVoteRequester.awaitResponse(TinderGovVoteRequester.Request(referendum.id.value))

            if (!response.success) {
                _rewindCardEvent.sendEvent()
                return@launch
            }
        }

        interactor.addItemToBasket(referendum.id, voteType)
    }

    private fun mapReferendumToUi(
        referendumPreview: ReferendumPreview,
        summary: ExtendedLoadingState<String?>?,
        amount: ExtendedLoadingState<AmountModel?>?
    ): TinderGovCardRvItem {
        return TinderGovCardRvItem(
            referendumPreview.id,
            summary = summary?.map { mapSummaryToUi(it, referendumPreview) }.orLoading(),
            requestedAmount = amount.orLoading(),
            backgroundRes = R.drawable.ic_tinder_gov_entry_banner_background,
        )
    }

    private fun mapSummaryToUi(summary: String?, referendumPreview: ReferendumPreview): String {
        return summary ?: referendumFormatter.formatReferendumName(referendumPreview)
    }

    private fun setupReferendumRetryAction() {
        topReferendumWithDetails
            .map { (referendum, summary, amount) ->
                val isLoadingError = summary?.isError().orFalse() || amount?.isError().orFalse()
                referendum to isLoadingError
            }
            .distinctUntilChangedBy { (referendum, isLoadingError) -> referendum.id to isLoadingError }
            .onEach { (referendum, isLoadingError) ->
                if (isLoadingError) {
                    showRetryDialog(referendum)
                }
            }.launchIn(this)
    }

    private suspend fun showRetryDialog(referendum: ReferendumPreview) {
        val retryConfirmed = retryReferendumInfoLoadingAction.awaitAction(Unit)
        if (retryConfirmed) {
            reloadContentForReferendum(referendum)
        } else {
            skipCard()
        }
    }

    private fun reloadContentForReferendum(referendum: ReferendumPreview) = launch {
        // Remove old summary and amount for referendum
        tinderGovCardsDataHelper.removeSummary(referendum.id)
        tinderGovCardsDataHelper.removeAmount(referendum.id)

        // Load summary and amount for referendum
        tinderGovCardsDataHelper.loadSummary(referendum, this)
        tinderGovCardsDataHelper.loadAmount(referendum, this)
    }

    private fun mapCards(
        sortedReferenda: List<ReferendumPreview>,
        summaries: Map<ReferendumId, ExtendedLoadingState<String?>>,
        amounts: Map<ReferendumId, ExtendedLoadingState<AmountModel?>>
    ): List<TinderGovCardRvItem> {
        return sortedReferenda.map {
            val summary = summaries[it.id]
            val amount = amounts[it.id]
            mapReferendumToUi(it, summary, amount)
        }
    }

    private fun mapBasketModel(items: List<TinderGovBasketItem>): BasketLabelModel {
        return BasketLabelModel(
            items = items.size,
            backgroundColorRes = if (items.isEmpty()) R.color.icon_inactive else R.color.icon_accent,
            textColorRes = if (items.isEmpty()) R.color.button_text_inactive else R.color.text_primary,
            textRes = if (items.isEmpty()) R.string.tinder_gov_cards_voting_list_empty else R.string.tinder_gov_cards_voting_list,
            imageTintRes = if (items.isEmpty()) R.color.icon_inactive else R.color.chip_icon,
        )
    }
}
