package io.novafoundation.nova.feature_governance_impl.presentation.tindergov.cards

import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.common.domain.ExtendedLoadingState.Loading
import io.novafoundation.nova.common.domain.ExtendedLoadingState.Loaded
import io.novafoundation.nova.common.domain.ExtendedLoadingState.Error
import io.novafoundation.nova.common.domain.map
import io.novafoundation.nova.common.utils.share
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ReferendumId
import io.novafoundation.nova.feature_governance_api.domain.referendum.list.ReferendumPreview
import io.novafoundation.nova.feature_governance_api.domain.tindergov.TinderGovInteractor
import io.novafoundation.nova.feature_wallet_api.domain.AssetUseCase
import io.novafoundation.nova.feature_wallet_api.presentation.model.AmountModel
import io.novafoundation.nova.feature_wallet_api.presentation.model.mapAmountToAmountModel
import java.math.BigInteger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private typealias SummaryLoadingState = Map<ReferendumId, ExtendedLoadingState<String?>>
private typealias AmountLoadingState = Map<ReferendumId, ExtendedLoadingState<AmountModel?>>

class TinderGovCardsDetailsLoaderFactory(
    private val interactor: TinderGovInteractor,
    private val assetUseCase: AssetUseCase
) {

    fun create(coroutineScope: CoroutineScope): TinderGovCardDetailsLoader {
        return TinderGovCardDetailsLoader(interactor, assetUseCase, coroutineScope)
    }
}

class TinderGovCardDetailsLoader(
    private val interactor: TinderGovInteractor,
    private val assetUseCase: AssetUseCase,
    private val coroutineScope: CoroutineScope
) : CoroutineScope by coroutineScope {

    private val assetFlow = assetUseCase.currentAssetFlow()
        .share()

    private val summaryMutex = Mutex()
    private val amountMutex = Mutex()

    private val _cardsSummary = MutableStateFlow<SummaryLoadingState>(emptyMap())
    private val _cardsAmount = MutableStateFlow<AmountLoadingState>(emptyMap())

    val cardsSummaryFlow: Flow<SummaryLoadingState> = _cardsSummary
    val cardsAmountFlow: Flow<AmountLoadingState> = _cardsAmount

    suspend fun reloadSummary(referendumPreview: ReferendumPreview, coroutineScope: CoroutineScope) {
        summaryMutex.withLock {
            _cardsSummary.update { it - referendumPreview.id }
        }

        loadSummary(referendumPreview)
    }

    suspend fun reloadAmount(referendumPreview: ReferendumPreview) {
        amountMutex.withLock {
            _cardsAmount.update { it - referendumPreview.id }
        }

        loadAmount(referendumPreview)
    }

    fun loadSummary(referendumPreview: ReferendumPreview) {
        coroutineScope.launch {
            val id = referendumPreview.id
            if (_cardsSummary.first().containsKey(id)) return@launch

            setSummaryLoadingState(id, Loading)
            runCatching { interactor.loadReferendumSummary(id) }
                .onSuccess { setSummaryLoadingState(id, Loaded(it)) }
                .onFailure { setSummaryLoadingState(id, Error(it)) }
        }
    }

    fun loadAmount(referendumPreview: ReferendumPreview) {
        coroutineScope.launch {
            val id = referendumPreview.id
            if (_cardsAmount.first().containsKey(id)) return@launch

            setAmountLoadingState(id, Loading)
            runCatching { interactor.loadReferendumAmount(referendumPreview) }
                .onSuccess { setAmountLoadingState(id, Loaded(it)) }
                .onFailure { setAmountLoadingState(id, Error(it)) }
        }
    }

    private suspend fun setSummaryLoadingState(id: ReferendumId, summary: ExtendedLoadingState<String?>) {
        summaryMutex.withLock {
            _cardsSummary.update { it.minus(id).plus(id to summary) }
        }
    }

    private suspend fun setAmountLoadingState(id: ReferendumId, amount: ExtendedLoadingState<BigInteger?>) {
        amountMutex.withLock {
            val amountModel = amount.map {
                it ?: return@map null
                val asset = assetFlow.first()
                mapAmountToAmountModel(it, asset)
            }

            _cardsAmount.update { it.plus(id to amountModel) }
        }
    }
}

private fun <T> MutableStateFlow<T>.update(updater: (T) -> T) {
    value = updater(value)
}
