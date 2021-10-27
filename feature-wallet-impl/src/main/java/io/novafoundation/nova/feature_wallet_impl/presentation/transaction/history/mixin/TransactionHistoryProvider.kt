package io.novafoundation.nova.feature_wallet_impl.presentation.transaction.history.mixin

import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.daysFromMillis
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.common.utils.invoke
import io.novafoundation.nova.common.utils.lazyAsync
import io.novafoundation.nova.feature_account_api.presenatation.account.AddressDisplayUseCase
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletInteractor
import io.novafoundation.nova.feature_wallet_api.domain.model.Operation
import io.novafoundation.nova.feature_wallet_impl.data.mappers.mapOperationToOperationModel
import io.novafoundation.nova.feature_wallet_impl.data.mappers.mapOperationToParcel
import io.novafoundation.nova.feature_wallet_impl.presentation.WalletRouter
import io.novafoundation.nova.feature_wallet_impl.presentation.model.OperationModel
import io.novafoundation.nova.feature_wallet_impl.presentation.model.OperationParcelizeModel
import io.novafoundation.nova.feature_wallet_impl.presentation.transaction.filter.HistoryFiltersProvider
import io.novafoundation.nova.feature_wallet_impl.presentation.transaction.history.mixin.TransactionStateMachine.Action
import io.novafoundation.nova.feature_wallet_impl.presentation.transaction.history.mixin.TransactionStateMachine.State
import io.novafoundation.nova.feature_wallet_impl.presentation.transaction.history.model.DayHeader
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TransactionHistoryProvider(
    private val walletInteractor: WalletInteractor,
    private val iconGenerator: AddressIconGenerator,
    private val router: WalletRouter,
    private val historyFiltersProvider: HistoryFiltersProvider,
    private val resourceManager: ResourceManager,
    private val addressDisplayUseCase: AddressDisplayUseCase,
    private val chainRegistry: ChainRegistry,
    private val chainId: ChainId,
    private val assetId: Int
) : TransactionHistoryMixin, CoroutineScope by CoroutineScope(Dispatchers.Default) {

    private val domainState = MutableStateFlow<State>(
        State.EmptyProgress(filters = historyFiltersProvider.currentFilters())
    )

    private val chainAsync by lazyAsync {
        chainRegistry.getChain(chainId)
    }

    override val state = domainState.map(::mapOperationHistoryStateToUi)
        .inBackground()
        .shareIn(this, started = SharingStarted.Eagerly, replay = 1)

    private val cachedPage = walletInteractor.operationsFirstPageFlow(chainId, assetId)
        .distinctUntilChangedBy { it.cursorPage }
        .onEach { performTransition(Action.CachePageArrived(it.cursorPage, it.accountChanged)) }
        .map { it.cursorPage }
        .inBackground()
        .shareIn(this, SharingStarted.Eagerly, replay = 1)

    init {
        historyFiltersProvider.filtersFlow()
            .distinctUntilChanged()
            .onEach { performTransition(Action.FiltersChanged(it)) }
            .launchIn(this)
    }

    override fun scrolled(currentIndex: Int) {
        launch { performTransition(Action.Scrolled(currentIndex)) }
    }

    override suspend fun syncFirstOperationsPage(): Result<*> {
        return walletInteractor.syncOperationsFirstPage(
            chainId = chainId,
            chainAssetId = assetId,
            pageSize = TransactionStateMachine.PAGE_SIZE,
            filters = historyFiltersProvider.allFilters
        )
    }

    override fun transactionClicked(transactionModel: OperationModel) {
        launch {
            val operations = (domainState.first() as? State.WithData)?.data ?: return@launch

            val clickedOperation = operations.first { it.id == transactionModel.id }

            withContext(Dispatchers.Main) {
                when (val payload = mapOperationToParcel(clickedOperation, resourceManager)) {
                    is OperationParcelizeModel.Transfer -> {
                        router.openTransferDetail(payload)
                    }

                    is OperationParcelizeModel.Extrinsic -> {
                        router.openExtrinsicDetail(payload)
                    }

                    is OperationParcelizeModel.Reward -> {
                        router.openRewardDetail(payload)
                    }
                }
            }
        }
    }

    private suspend fun performTransition(action: Action) = withContext(Dispatchers.Default) {
        val newState = TransactionStateMachine.transition(action, domainState.value) { sideEffect ->
            when (sideEffect) {
                is TransactionStateMachine.SideEffect.ErrorEvent -> {
                    // ignore errors here, they are bypassed to client of mixin
                }
                is TransactionStateMachine.SideEffect.LoadPage -> loadNewPage(sideEffect)
                TransactionStateMachine.SideEffect.TriggerCache -> triggerCache()
            }
        }

        domainState.value = newState
    }

    private fun triggerCache() {
        val cached = cachedPage.replayCache.firstOrNull()

        cached?.let {
            launch { performTransition(Action.CachePageArrived(it, accountChanged = false)) }
        }
    }

    private fun loadNewPage(sideEffect: TransactionStateMachine.SideEffect.LoadPage) {
        launch {
            walletInteractor.getOperations(chainId, assetId, sideEffect.pageSize, sideEffect.nextCursor, sideEffect.filters)
                .onFailure {
                    performTransition(Action.PageError(error = it))
                }.onSuccess {
                    performTransition(Action.NewPage(newPage = it, loadedWith = sideEffect.filters))
                }
        }
    }

    private suspend fun mapOperationHistoryStateToUi(state: State): TransactionHistoryUi.State {
        return when (state) {
            is State.Empty -> TransactionHistoryUi.State.Empty
            is State.EmptyProgress -> TransactionHistoryUi.State.EmptyProgress
            is State.Data -> TransactionHistoryUi.State.Data(transformData(state.data))
            is State.FullData -> TransactionHistoryUi.State.Data(transformData(state.data))
            is State.NewPageProgress -> TransactionHistoryUi.State.Data(transformData(state.data))
        }
    }

    private suspend fun transformData(data: List<Operation>): List<Any> {
        val accountIdentifier = addressDisplayUseCase.createIdentifier()
        val chain = chainAsync()

        val operations = data.map { operation ->
            mapOperationToOperationModel(chain, operation, accountIdentifier, resourceManager, iconGenerator)
        }

        return regroup(operations)
    }

    private fun regroup(operations: List<OperationModel>): List<Any> {

        return operations.groupBy { it.time.daysFromMillis() }
            .map { (daysSinceEpoch, transactions) ->
                val header = DayHeader(daysSinceEpoch)

                listOf(header) + transactions
            }.flatten()
    }
}
