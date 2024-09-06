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
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.mapToSet
import io.novafoundation.nova.common.utils.orFalse
import io.novafoundation.nova.common.utils.safeSubList
import io.novafoundation.nova.common.utils.sendEvent
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ReferendumId
import io.novafoundation.nova.feature_governance_api.domain.referendum.list.ReferendumPreview
import io.novafoundation.nova.feature_governance_api.domain.tindergov.TinderGovInteractor
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.feature_governance_impl.presentation.GovernanceRouter
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.common.ReferendumFormatter
import io.novafoundation.nova.feature_governance_impl.presentation.tindergov.cards.adapter.TinderGovCardRvItem
import io.novafoundation.nova.feature_wallet_api.presentation.model.AmountModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
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

    val cardsFlow = combine(
        sortedReferendaFlow,
        cardsSummaryFlow,
        cardsAmountFlow,
        ::mapCards
    )

    val retryReferendumInfoLoadingAction = actionAwaitableMixinFactory.confirmingOrDenyingAction<Unit>()

    private val _skipCardEvent = MutableLiveData<Event<Unit>>()
    val skipCardEvent: LiveData<Event<Unit>> = _skipCardEvent

    private val topReferendumWithDetails = combine(topCardIndex, cardsSummaryFlow, cardsAmountFlow) { topCardIndex, cardsSummary, cardsAmount ->
        val referendum = sortedReferendaFlow.value.getOrNull(topCardIndex) ?: return@combine null
        val cardSummary = cardsSummary[referendum.id]
        val cardAmount = cardsAmount[referendum.id]

        CardWithDetails(referendum, cardSummary, cardAmount)
    }.filterNotNull()
        .distinctUntilChanged()
        .shareInBackground()

    val isCardDraggingAvailable = topReferendumWithDetails
        .map {
            val summaryLoaded = it.summary?.isLoaded().orFalse()
            val amountLoaded = it.amount?.isLoaded().orFalse()
            summaryLoaded && amountLoaded
        }

    init {
        interactor.observeReferendaAvailableToVote(this)
            .onEach { referenda ->
                val currentReferendaIds = sortedReferendaFlow.value.mapToSet { it.id }
                val newReferenda = referenda.associateBy { it.id } - currentReferendaIds

                sortedReferendaFlow.value += newReferenda.values // To add new coming referenda to the end of list
            }
            .launchIn(this)

        setupReferendumRetryAction()
    }

    fun back() {
        router.back()
    }

    fun ayeClicked(position: Int) = writeVote(position)

    fun abstainClicked(position: Int) = writeVote(position)

    fun nayClicked(position: Int) = writeVote(position)

    fun openReadMore(item: TinderGovCardRvItem) {
        showMessage("Not implemented yet")
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

    private fun skipCard() {
        _skipCardEvent.sendEvent()
    }

    private fun writeVote(position: Int) = launch {
        cardsFlow.first()
            .getOrNull(0)
            ?.let { showMessage("Not implemented yet") }
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
