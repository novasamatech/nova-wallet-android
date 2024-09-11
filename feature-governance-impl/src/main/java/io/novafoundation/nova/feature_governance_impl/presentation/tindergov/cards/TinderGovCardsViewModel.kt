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
import io.novafoundation.nova.common.utils.onEachWithPrevious
import io.novafoundation.nova.common.utils.mapToSet
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
import kotlinx.coroutines.flow.distinctUntilChanged
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
    private val tinderGovCardDetailsLoaderFactory: TinderGovCardsDetailsLoaderFactory,
    private val interactor: TinderGovInteractor,
    private val referendumFormatter: ReferendumFormatter,
    private val actionAwaitableMixinFactory: ActionAwaitableMixin.Factory,
    private val tinderGovVoteRequester: TinderGovVoteRequester
) : BaseViewModel() {

    companion object {
        const val CARD_STACK_SIZE = 3
    }

    private val tinderGovCardDetailsLoader = tinderGovCardDetailsLoaderFactory.create(coroutineScope = this)

    private val cardsSummaryFlow = tinderGovCardDetailsLoader.cardsSummaryFlow
        .shareInBackground()

    private val cardsAmountFlow = tinderGovCardDetailsLoader.cardsAmountFlow
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

    private val _resetCards = MutableLiveData<Event<Unit>>()
    val resetCardsEvent: LiveData<Event<Unit>> = _resetCards

    private val topReferendumWithDetails = combine(topCardIndex, cardsSummaryFlow, cardsAmountFlow) { topCardIndex, cardsSummary, cardsAmount ->
        val referendum = sortedReferendaFlow.value.getOrNull(topCardIndex) ?: return@combine null
        val cardSummary = cardsSummary[referendum.id]
        val cardAmount = cardsAmount[referendum.id]

        CardWithDetails(referendum, cardSummary, cardAmount)
    }.filterNotNull()
        .distinctUntilChanged()
        .shareInBackground()

    private var isVotingInProgress = MutableStateFlow(false)

    val isCardDraggingAvailable = combine(isVotingInProgress, topReferendumWithDetails) { isVotingInProgress, cardWithDetails ->
        val summaryLoaded = cardWithDetails.summary?.isLoaded().orFalse()
        val amountLoaded = cardWithDetails.summary?.isLoaded().orFalse()
        !isVotingInProgress && summaryLoaded && amountLoaded
    }

    val basketModelFlow = basketFlow
        .map { items -> mapBasketModel(items.values.toList()) }

    init {
        observeReferendaAndAddToCards()

        setupReferendumRetryAction()

        loadFirstCards()

        openBasketIfAllReferendaVoted()
    }

    private fun openBasketIfAllReferendaVoted() {
        combine(sortedReferendaFlow, basketFlow) { referenda, basket ->
            if (referenda.isEmpty()) return@combine

            val allReferendaVoted = referenda.all { it.id in basket }
            if (allReferendaVoted) {
                router.openTinderGovBasket()
            }
        }.launchIn(this)
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

    fun onCardAppeared(position: Int) {
        topCardIndex.value = position
    }

    fun onBasketClicked() {
        router.openTinderGovBasket()
    }

    fun loadContentForCardsFromPosition(position: Int) {
        // Get 3 first referenda to load content for
        val referenda = sortedReferendaFlow.value.safeSubList(position, position + CARD_STACK_SIZE)

        // Load summary and amount for each referendum
        referenda.forEach {
            tinderGovCardDetailsLoader.loadSummary(it)
            tinderGovCardDetailsLoader.loadAmount(it)
        }
    }

    fun editVotingPowerClicked() {
        launch {
            val topReferendum = topReferendumWithDetails.first()
            tinderGovVoteRequester.openRequest(TinderGovVoteRequester.Request(topReferendum.first.id.value))
        }
    }

    private fun loadFirstCards() {
        launch {
            sortedReferendaFlow
                .filter { it.isNotEmpty() }
                .first() // Await while list of cards will be not empty

            onCardAppeared(0)
        }
    }

    private fun skipCard() {
        _skipCardEvent.sendEvent()
    }

    private fun writeVote(position: Int, voteType: VoteType) {
        if (isVotingInProgress.value) return
        isVotingInProgress.value = true

        launch {
            val referendum = sortedReferendaFlow.value.getOrNull(position)

            if (referendum == null) {
                isVotingInProgress.value = false
                return@launch
            }

            if (!interactor.isSufficientAmountToVote()) {
                val response = tinderGovVoteRequester.awaitResponse(TinderGovVoteRequester.Request(referendum.id.value))

                if (!response.success) {
                    _rewindCardEvent.sendEvent()
                    isVotingInProgress.value = false
                    return@launch
                }
            }

            interactor.addItemToBasket(referendum.id, voteType)
            isVotingInProgress.value = false
        }
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
            .map {
                val isLoadingError = it.summary?.isError().orFalse() || it.amount?.isError().orFalse()
                it.referendum to isLoadingError
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
            reloadDetailsForReferendum(referendum)
        } else {
            skipCard()
        }
    }

    private fun reloadDetailsForReferendum(referendum: ReferendumPreview) = launch {
        tinderGovCardDetailsLoader.reloadSummary(referendum, this)
        tinderGovCardDetailsLoader.reloadAmount(referendum)
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

    private fun observeReferendaAndAddToCards() {
        combineToPair(interactor.observeReferendaAvailableToVote(this), basketFlow)
            .onEachWithPrevious { old, new -> checkItemsRemovedFromBasket(old?.second.orEmpty(), new.second) }
            .onEach { (referenda, basket) -> addNewReferendaToCards(referenda, basket) }
            .launchIn(this)
    }

    private fun addNewReferendaToCards(referenda: List<ReferendumPreview>, basket: Map<ReferendumId, TinderGovBasketItem>) {
        val currentReferendaIds = sortedReferendaFlow.value.map { it.id }.toSet()
        val newReferenda = referenda.filter { it.id !in currentReferendaIds && it.id !in basket }

        sortedReferendaFlow.value += newReferenda // To add new coming referenda to the end of list
    }

    // Remove items from cards if they were removed from basket
    private fun checkItemsRemovedFromBasket(old: Map<ReferendumId, TinderGovBasketItem>, new: Map<ReferendumId, TinderGovBasketItem>) {
        if (old.size > new.size) {
            sortedReferendaFlow.value = emptyList()
            _resetCards.sendEvent()
        }
    }
}

private class CardWithDetails(
    val referendum: ReferendumPreview,
    val summary: ExtendedLoadingState<String?>?,
    val amount: ExtendedLoadingState<AmountModel?>?
) {

    override fun equals(other: Any?): Boolean {
        return other is CardWithDetails &&
            referendum.id == other.referendum.id &&
            summary == other.summary &&
            amount == other.amount
    }
}
