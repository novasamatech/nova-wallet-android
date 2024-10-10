package io.novafoundation.nova.feature_governance_impl.presentation.tindergov.cards

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import io.noties.markwon.Markwon
import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.base.TitleAndMessage
import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.common.mixin.actionAwaitable.confirmingOrDenyingAction
import io.novafoundation.nova.common.navigation.awaitResponse
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.formatTokenAmount
import io.novafoundation.nova.common.utils.formatting.format
import io.novafoundation.nova.common.utils.onEachWithPrevious
import io.novafoundation.nova.common.utils.sendEvent
import io.novafoundation.nova.feature_governance_api.data.model.TinderGovBasketItem
import io.novafoundation.nova.feature_governance_api.data.model.VotingPower
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ReferendumId
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.VoteType
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.amountMultiplier
import io.novafoundation.nova.feature_governance_api.domain.referendum.list.ReferendumPreview
import io.novafoundation.nova.feature_governance_impl.domain.summary.ReferendaSummaryInteractor
import io.novafoundation.nova.feature_governance_impl.domain.referendum.tindergov.TinderGovBasketInteractor
import io.novafoundation.nova.feature_governance_impl.domain.referendum.tindergov.TinderGovInteractor
import io.novafoundation.nova.feature_governance_impl.domain.referendum.tindergov.VotingPowerState
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.feature_governance_impl.presentation.GovernanceRouter
import io.novafoundation.nova.feature_governance_impl.presentation.common.info.ReferendumInfoPayload
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.vote.setup.tindergov.TinderGovVoteRequester
import io.novafoundation.nova.feature_governance_impl.presentation.tindergov.cards.adapter.TinderGovCardRvItem
import io.novafoundation.nova.feature_governance_impl.presentation.tindergov.cards.model.CardWithDetails
import io.novafoundation.nova.feature_governance_impl.presentation.tindergov.cards.model.ReferendaCounterModel
import io.novafoundation.nova.feature_governance_impl.presentation.tindergov.cards.model.ReferendaWithBasket
import io.novafoundation.nova.feature_wallet_api.domain.AssetUseCase
import io.novafoundation.nova.feature_wallet_api.domain.TokenUseCase
import io.novafoundation.nova.feature_wallet_api.domain.getCurrentAsset
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.feature_wallet_api.presentation.model.AmountModel
import io.novafoundation.nova.feature_wallet_api.presentation.model.mapAmountToAmountModel
import io.novafoundation.nova.runtime.ext.fullId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class TinderGovCardsViewModel(
    private val router: GovernanceRouter,
    private val interactor: TinderGovInteractor,
    private val basketInteractor: TinderGovBasketInteractor,
    private val actionAwaitableMixinFactory: ActionAwaitableMixin.Factory,
    private val tinderGovVoteRequester: TinderGovVoteRequester,
    private val assetUseCase: AssetUseCase,
    private val resourceManager: ResourceManager,
    private val referendaSummaryInteractor: ReferendaSummaryInteractor,
    private val tokenUseCase: TokenUseCase,
    private val cardsMarkdown: Markwon,
) : BaseViewModel() {

    companion object {
        const val CARD_STACK_SIZE = 3
    }

    private val cardsBackground = createCardsBackground()

    private val topCardIndex = MutableStateFlow(0)
    private val sortedReferendaFlow = MutableStateFlow(listOf<CardWithDetails>())

    private val basketFlow = basketInteractor.observeTinderGovBasket()
        .map { it.associateBy { it.referendumId } }
        .shareInBackground()

    val cardsFlow = sortedReferendaFlow.map { mapCards(it) }

    val retryReferendumInfoLoadingAction = actionAwaitableMixinFactory.confirmingOrDenyingAction<Unit>()

    val insufficientBalanceChangeAction = actionAwaitableMixinFactory.confirmingOrDenyingAction<TitleAndMessage>()

    private val _skipCardEvent = MutableLiveData<Event<Unit>>()
    val skipCardEvent: LiveData<Event<Unit>> = _skipCardEvent

    private val _rewindCardEvent = MutableLiveData<Event<Unit>>()
    val rewindCardEvent: LiveData<Event<Unit>> = _rewindCardEvent

    private val _resetCards = MutableLiveData<Event<Unit>>()
    val resetCardsEvent: LiveData<Event<Unit>> = _resetCards

    private val topCardFlow = combine(topCardIndex, sortedReferendaFlow) { topCardIndex, referenda ->
        referenda.getOrNull(topCardIndex) ?: return@combine null
    }
        .distinctUntilChanged()
        .shareInBackground()

    private var isVotingInProgress = MutableStateFlow(false)

    val isCardDraggingAvailable = isVotingInProgress.map { !it }

    val basketModelFlow = basketFlow
        .map { items -> mapBasketModel(items.values.toList()) }

    private val votingReferendaCounterFlow = combine(basketFlow, sortedReferendaFlow) { basket, referenda ->
        val currentBasketSize = referenda.count { it.id in basket }
        ReferendaCounterModel(currentBasketSize, referenda.size)
    }.shareInBackground()

    val referendumCounterFlow = votingReferendaCounterFlow.map {
        if (it.hasReferendaToVote()) {
            val currentItemIndex = it.remainingReferendaToVote
            resourceManager.getString(R.string.swipe_gov_cards_counter, currentItemIndex)
        } else {
            resourceManager.getString(R.string.swipe_gov_cards_no_referenda_to_vote)
        }
    }

    val hasReferendaToVote = votingReferendaCounterFlow.map { it.hasReferendaToVote() }
        .distinctUntilChanged()

    val placeholderTextFlow = basketFlow.map {
        if (it.isEmpty()) {
            resourceManager.getString(R.string.swipe_gov_card_placeholder_basket_empty_text)
        } else {
            resourceManager.getString(R.string.swipe_gov_card_placeholder_basket_full_text)
        }
    }
        .distinctUntilChanged()

    val showConfirmButtonFlow = basketFlow.map { it.isNotEmpty() }
        .distinctUntilChanged()

    init {
        observeReferendaAndAddToCards()

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
    }

    fun onBasketClicked() {
        launch {
            if (basketFlow.first().isEmpty()) return@launch

            router.openTinderGovBasket()
        }
    }

    fun editVotingPowerClicked() {
        launch {
            val topReferendum = topCardFlow.first()
            val topReferendumId = topReferendum?.id?.value ?: return@launch
            val request = TinderGovVoteRequester.Request(topReferendumId)
            tinderGovVoteRequester.openRequest(request)
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
                basketInteractor.addItemToBasket(referendum.id, voteType)
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
        val basket = basketInteractor.getTinderGovBasket()
            .associateBy { it.referendumId }
        val referenda = sortedReferendaFlow.value

        val allReferendaVoted = referenda.all { it.id in basket }
        if (allReferendaVoted) {
            router.openTinderGovBasket()
        }
    }

    private fun mapReferendumToUi(
        referendumPreview: CardWithDetails,
        backgroundRes: Int
    ): TinderGovCardRvItem {
        return TinderGovCardRvItem(
            referendumPreview.id,
            summary = cardsMarkdown.toMarkdown(referendumPreview.summary),
            requestedAmount = referendumPreview.amount,
            backgroundRes = backgroundRes,
        )
    }

    private fun mapCards(
        sortedReferenda: List<CardWithDetails>
    ): List<TinderGovCardRvItem> {
        return sortedReferenda.mapIndexed { index, referendum ->
            val backgroundRes = cardsBackground[index % cardsBackground.size]

            mapReferendumToUi(referendum, backgroundRes)
        }
    }

    private fun mapBasketModel(items: List<TinderGovBasketItem>): BasketLabelModel {
        return BasketLabelModel(
            items = items.size,
            backgroundColorRes = if (items.isEmpty()) R.color.icon_inactive else R.color.icon_accent,
            textColorRes = if (items.isEmpty()) R.color.button_text_inactive else R.color.text_primary,
            textRes = if (items.isEmpty()) R.string.swipe_gov_cards_voting_list_empty else R.string.swipe_gov_cards_full_list,
            imageTintRes = if (items.isEmpty()) R.color.icon_inactive else R.color.chip_icon,
        )
    }

    private fun observeReferendaAndAddToCards() {
        combine(interactor.observeReferendaAvailableToVote(this), basketFlow) { referenda, basket ->
            val referendaIds = referenda.map { it.id }
            val summaries = referendaSummaryInteractor.getReferendaSummaries(referendaIds, viewModelScope)
            val amounts = referenda.associate { it.id to it.getAmountModel() }

            val referendaCards = referenda.mapToCardsAndFilterBySummaries(summaries, amounts)

            ReferendaWithBasket(referendaCards, basket)
        }
            .addNewReferendaToCards()
            .launchIn(this)
    }

    private fun List<ReferendumPreview>.mapToCardsAndFilterBySummaries(
        summaries: Map<ReferendumId, String>,
        amounts: Map<ReferendumId, AmountModel?>
    ): List<CardWithDetails> {
        return mapNotNull {
            val summary = summaries[it.id] ?: return@mapNotNull null

            CardWithDetails(it.id, summary, amounts[it.id])
        }
    }

    private suspend fun ReferendumPreview.getAmountModel(): AmountModel? {
        val treasuryRequest = interactor.getReferendumAmount(this)
        return treasuryRequest?.let {
            val token = tokenUseCase.getToken(treasuryRequest.chainAsset.fullId)
            mapAmountToAmountModel(treasuryRequest.amount, token)
        }
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
                loadFirstCards()
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
