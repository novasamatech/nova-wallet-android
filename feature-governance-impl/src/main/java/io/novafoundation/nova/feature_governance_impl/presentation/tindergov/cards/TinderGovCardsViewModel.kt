package io.novafoundation.nova.feature_governance_impl.presentation.tindergov.cards

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.base.TitleAndMessage
import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.common.domain.isError
import io.novafoundation.nova.common.domain.isLoaded
import io.novafoundation.nova.common.domain.map
import io.novafoundation.nova.common.domain.orLoading
import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.common.mixin.actionAwaitable.confirmingOrDenyingAction
import io.novafoundation.nova.common.navigation.awaitResponse
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.formatTokenAmount
import io.novafoundation.nova.common.utils.formatting.format
import io.novafoundation.nova.common.utils.onEachWithPrevious
import io.novafoundation.nova.common.utils.orFalse
import io.novafoundation.nova.common.utils.safeSubList
import io.novafoundation.nova.common.utils.sendEvent
import io.novafoundation.nova.feature_governance_api.data.model.TinderGovBasketItem
import io.novafoundation.nova.feature_governance_api.data.model.VotingPower
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ReferendumId
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.VoteType
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.amountMultiplier
import io.novafoundation.nova.feature_governance_api.domain.referendum.list.ReferendumPreview
import io.novafoundation.nova.feature_governance_api.domain.tindergov.TinderGovInteractor
import io.novafoundation.nova.feature_governance_api.domain.tindergov.VotingPowerState
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.feature_governance_impl.presentation.GovernanceRouter
import io.novafoundation.nova.feature_governance_impl.presentation.common.info.ReferendumInfoPayload
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.common.ReferendumFormatter
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.vote.setup.tindergov.TinderGovVoteRequester
import io.novafoundation.nova.feature_governance_impl.presentation.tindergov.cards.adapter.TinderGovCardRvItem
import io.novafoundation.nova.feature_wallet_api.domain.AssetUseCase
import io.novafoundation.nova.feature_wallet_api.domain.getCurrentAsset
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.feature_wallet_api.presentation.model.AmountModel
import kotlinx.coroutines.flow.Flow
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
    private val tinderGovVoteRequester: TinderGovVoteRequester,
    private val assetUseCase: AssetUseCase,
    private val resourceManager: ResourceManager
) : BaseViewModel() {

    companion object {
        const val CARD_STACK_SIZE = 3
    }

    private val cardsBackground = createCardsBackground()

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

    val insufficientBalanceChangeAction = actionAwaitableMixinFactory.confirmingOrDenyingAction<TitleAndMessage>()

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
    }

    fun back() {
        router.back()
    }

    fun ayeClicked(position: Int) = writeVote(position, VoteType.AYE)

    fun abstainClicked(position: Int) = writeVote(position, VoteType.ABSTAIN)

    fun nayClicked(position: Int) = writeVote(position, VoteType.NAY)

    fun openReadMore(item: TinderGovCardRvItem) {
        router.openReferendumInfo(ReferendumInfoPayload(item.id.value))
    }

    fun onCardAppeared(position: Int) {
        topCardIndex.value = position

        // Get 3 first referenda to load content for
        val referenda = sortedReferendaFlow.value.safeSubList(position, position + CARD_STACK_SIZE)

        // Load summary and amount for each referendum
        referenda.forEach {
            tinderGovCardDetailsLoader.loadSummary(it)
            tinderGovCardDetailsLoader.loadAmount(it)
        }
    }

    fun onBasketClicked() {
        router.openTinderGovBasket()
    }

    fun editVotingPowerClicked() {
        launch {
            val topReferendum = topReferendumWithDetails.first()
            tinderGovVoteRequester.openRequest(TinderGovVoteRequester.Request(topReferendum.referendum.id.value))
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

            val votingPowerState = interactor.getVotingPowerState()
            val isSufficientAmount = when (votingPowerState) {
                VotingPowerState.Empty -> openSetVotingPowerScreen(referendum.id)
                is VotingPowerState.InsufficientAmount -> showInsufficientBalanceDialog(referendum.id, votingPowerState.votingPower)

                is VotingPowerState.SufficientAmount -> true
            }

            if (isSufficientAmount) {
                interactor.addItemToBasket(referendum.id, voteType)
                checkAllReferendaWasVotedAndOpenBasket()
            } else {
                _rewindCardEvent.sendEvent()
            }

            isVotingInProgress.value = false
        }
    }

    private suspend fun openSetVotingPowerScreen(referendumId: ReferendumId): Boolean {
        val response = tinderGovVoteRequester.awaitResponse(TinderGovVoteRequester.Request(referendumId.value))

        return response.success
    }

    private suspend fun showInsufficientBalanceDialog(referendumId: ReferendumId, votingPower: VotingPower): Boolean {
        val asset = assetUseCase.getCurrentAsset()
        val chainAsset = asset.token.configuration

        val amount = chainAsset.amountFromPlanks(votingPower.amount)
        val formattedAmount = amount.formatTokenAmount(chainAsset.symbol)
        val conviction = votingPower.conviction.amountMultiplier().format()

        val title = resourceManager.getString(R.string.swipe_gov_insufficient_balance_dialog_title)
        val message = resourceManager.getString(R.string.swipe_gov_insufficient_balance_dialog_message, formattedAmount, conviction)
        val openChangeVotingPowerScreen = insufficientBalanceChangeAction.awaitAction(TitleAndMessage(title, message))

        return if (openChangeVotingPowerScreen) {
            openSetVotingPowerScreen(referendumId)
        } else {
            false
        }
    }

    private suspend fun checkAllReferendaWasVotedAndOpenBasket() {
        val basket = interactor.getTinderGovBasket()
            .associateBy { it.referendumId }
        val referenda = sortedReferendaFlow.value

        val allReferendaVoted = referenda.all { it.id in basket }
        if (allReferendaVoted) {
            router.openTinderGovBasket()
        }
    }

    private fun mapReferendumToUi(
        referendumPreview: ReferendumPreview,
        summary: ExtendedLoadingState<String?>?,
        amount: ExtendedLoadingState<AmountModel?>?,
        backgroundRes: Int
    ): TinderGovCardRvItem {
        return TinderGovCardRvItem(
            referendumPreview.id,
            summary = summary?.map { mapSummaryToUi(it, referendumPreview) }.orLoading(),
            requestedAmount = amount.orLoading(),
            backgroundRes = backgroundRes,
        )
    }

    private fun mapSummaryToUi(summary: String?, referendumPreview: ReferendumPreview): String {
        return if (summary.isNullOrBlank()) {
            referendumFormatter.formatReferendumName(referendumPreview)
        } else {
            summary
        }
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
        return sortedReferenda.mapIndexed { index, referendum ->
            val backgroundRes = cardsBackground[index % cardsBackground.size]

            val summary = summaries[referendum.id]
            val amount = amounts[referendum.id]
            mapReferendumToUi(referendum, summary, amount, backgroundRes)
        }
    }

    private fun mapBasketModel(items: List<TinderGovBasketItem>): BasketLabelModel {
        return BasketLabelModel(
            items = items.size,
            backgroundColorRes = if (items.isEmpty()) R.color.icon_inactive else R.color.icon_accent,
            textColorRes = if (items.isEmpty()) R.color.button_text_inactive else R.color.text_primary,
            textRes = if (items.isEmpty()) R.string.swipe_gov_cards_voting_list_empty else R.string.swipe_gov_cards_voting_list,
            imageTintRes = if (items.isEmpty()) R.color.icon_inactive else R.color.chip_icon,
        )
    }

    private fun observeReferendaAndAddToCards() {
        combine(interactor.observeReferendaAvailableToVote(this), basketFlow) { referenda, basket ->
            ReferendaWithBasket(referenda, basket)
        }
            .addNewReferendaToCards()
            .launchIn(this)
    }

    private fun Flow<ReferendaWithBasket>.addNewReferendaToCards(): Flow<ReferendaWithBasket> {
        return onEachWithPrevious { old, new ->
            val oldBasket = old?.basket.orEmpty()
            val newReferenda = new.referenda
            val newBasket = new.basket

            val currentReferenda = sortedReferendaFlow.value
            val currentReferendaIds = currentReferenda.map { it.id }.toMutableSet()

            val isBasketItemRemoved = oldBasket.size > newBasket.size

            val result = if (isBasketItemRemoved) {
                newReferenda.filter { it.id !in newBasket } // Take only referenda that are not in basket
            } else {
                val newComingReferenda = newReferenda.filter { it.id !in currentReferendaIds && it.id !in newBasket }
                currentReferenda + newComingReferenda // Take old referenda and new coming referenda
            }

            sortedReferendaFlow.value = result

            if (isBasketItemRemoved) {
                _resetCards.sendEvent()
            }
        }
    }

    private fun createCardsBackground(): List<Int> {
        return listOf(
            R.drawable.tinder_gov_card_background_1,
            R.drawable.tinder_gov_card_background_2,
            R.drawable.tinder_gov_card_background_3,
            R.drawable.tinder_gov_card_background_4,
            R.drawable.tinder_gov_card_background_5,
            R.drawable.tinder_gov_card_background_6,
            R.drawable.tinder_gov_card_background_5,
            R.drawable.tinder_gov_card_background_4,
            R.drawable.tinder_gov_card_background_3,
            R.drawable.tinder_gov_card_background_2,
            R.drawable.tinder_gov_card_background_1,
            R.drawable.tinder_gov_card_background_0,
        )
    }
}

private class ReferendaWithBasket(
    val referenda: List<ReferendumPreview>,
    val basket: Map<ReferendumId, TinderGovBasketItem>
)

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
