package io.novafoundation.nova.feature_governance_impl.presentation.tindergov.cards

import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.common.domain.ExtendedLoadingState.Loading
import io.novafoundation.nova.common.domain.ExtendedLoadingState.Loaded
import io.novafoundation.nova.common.domain.ExtendedLoadingState.Error
import io.novafoundation.nova.common.domain.map
import io.novafoundation.nova.common.utils.singleReplaySharedFlow
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ReferendumId
import io.novafoundation.nova.feature_governance_api.domain.referendum.list.ReferendumPreview
import io.novafoundation.nova.feature_governance_api.domain.tindergov.TinderGovInteractor
import io.novafoundation.nova.feature_governance_impl.data.GovernanceSharedState
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.feature_wallet_api.presentation.model.AmountModel
import io.novafoundation.nova.feature_wallet_api.presentation.model.mapAmountToAmountModel
import io.novafoundation.nova.runtime.state.selectedAssetFlow
import java.math.BigInteger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private typealias SummaryLoadingState = Map<ReferendumId, ExtendedLoadingState<String?>>
private typealias AmountLoadingState = Map<ReferendumId, ExtendedLoadingState<AmountModel?>>

class TinderGovCardsDataHelper(
    private val accountRepository: AccountRepository,
    private val walletRepository: WalletRepository,
    private val interactor: TinderGovInteractor,
    private val governanceSharedState: GovernanceSharedState,
) {

    private val assetFlow = combine(accountRepository.selectedMetaAccountFlow(), governanceSharedState.selectedAssetFlow()) { metaAccount, chainAsset ->
        walletRepository.getAsset(metaAccount.id, chainAsset)
    }

    private val summaryMutex = Mutex()
    private val amountMutex = Mutex()

    private val _cardsSummary = singleReplaySharedFlow<SummaryLoadingState>()
    private val _cardsAmount = singleReplaySharedFlow<AmountLoadingState>()

    val cardsSummaryFlow: Flow<SummaryLoadingState> = _cardsSummary
    val cardsAmountFlow: Flow<AmountLoadingState> = _cardsAmount

    fun init(coroutineScope: CoroutineScope) {
        coroutineScope.launch {
            _cardsSummary.emit(emptyMap())
            _cardsAmount.emit(emptyMap())
        }
    }

    suspend fun removeSummary(referendumId: ReferendumId) {
        summaryMutex.withLock {
            _cardsSummary.update { it - referendumId }
        }
    }

    suspend fun removeAmount(referendumId: ReferendumId) {
        amountMutex.withLock {
            _cardsAmount.update { it - referendumId }
        }
    }

    fun loadSummary(referendumPreview: ReferendumPreview, coroutineScope: CoroutineScope) {
        coroutineScope.launch {
            val id = referendumPreview.id
            if (containsSummary(id)) return@launch

            setSummaryLoadingState(id, Loading)
            runCatching { interactor.loadReferendumSummary(id) }
                .onSuccess { setSummaryLoadingState(id, Loaded(it)) }
                .onFailure { setSummaryLoadingState(id, Error(it)) }
        }
    }

    fun loadAmount(referendumPreview: ReferendumPreview, coroutineScope: CoroutineScope) {
        coroutineScope.launch {
            val id = referendumPreview.id
            if (containsAmount(id)) return@launch

            setAmountLoadingState(id, Loading)
            runCatching { interactor.loadReferendumAmount(referendumPreview) }
                .onSuccess { setAmountLoadingState(id, Loaded(it)) }
                .onFailure { setAmountLoadingState(id, Error(it)) }
        }
    }

    private suspend fun containsSummary(id: ReferendumId): Boolean {
        return summaryMutex.withLock { _cardsSummary.first().containsKey(id) }
    }

    private suspend fun containsAmount(id: ReferendumId): Boolean {
        return amountMutex.withLock { _cardsAmount.first().containsKey(id) }
    }

    private suspend fun setSummaryLoadingState(id: ReferendumId, summary: ExtendedLoadingState<String?>) {
        summaryMutex.withLock {
            _cardsSummary.update { it.plus(id to summary) }
        }
    }

    private suspend fun setAmountLoadingState(id: ReferendumId, amount: ExtendedLoadingState<BigInteger?>) {
        amountMutex.withLock {
            val amountModel = amount.map {
                it ?: return@map null
                val asset = assetFlow.first() ?: return@map null
                mapAmountToAmountModel(it, asset)
            }

            _cardsAmount.update { it.plus(id to amountModel) }
        }
    }
}

private suspend fun <T> MutableSharedFlow<T>.update(updater: (T) -> T) {
    emit(updater(first()))
}
