package io.novafoundation.nova.feature_assets.presentation.transaction.history.mixin

import android.util.Log
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.LOG_TAG
import io.novafoundation.nova.common.utils.daysFromMillis
import io.novafoundation.nova.common.utils.inBackground
import io.novafoundation.nova.common.utils.invoke
import io.novafoundation.nova.common.utils.lazyAsync
import io.novafoundation.nova.common.utils.singleReplaySharedFlow
import io.novafoundation.nova.feature_account_api.presenatation.account.AddressDisplayUseCase
import io.novafoundation.nova.feature_assets.domain.WalletInteractor
import io.novafoundation.nova.feature_assets.presentation.AssetsRouter
import io.novafoundation.nova.feature_assets.presentation.model.OperationParcelizeModel
import io.novafoundation.nova.feature_assets.presentation.transaction.filter.HistoryFiltersProviderFactory
import io.novafoundation.nova.feature_assets.presentation.transaction.history.mixin.TransactionHistoryUi.State.ListState
import io.novafoundation.nova.feature_assets.presentation.transaction.history.mixin.state_machine.TransactionStateMachine
import io.novafoundation.nova.feature_assets.presentation.transaction.history.mixin.state_machine.TransactionStateMachine.Action
import io.novafoundation.nova.feature_assets.presentation.transaction.history.mixin.state_machine.TransactionStateMachine.State
import io.novafoundation.nova.feature_assets.presentation.transaction.history.model.DayHeader
import io.novafoundation.nova.feature_currency_api.domain.interfaces.CurrencyRepository
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.AssetSourceRegistry
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.TransactionFilter
import io.novafoundation.nova.feature_wallet_api.domain.model.Operation
import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.asset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TransactionHistoryProvider(
    private val walletInteractor: WalletInteractor,
    private val router: AssetsRouter,
    private val historyFiltersProviderFactory: HistoryFiltersProviderFactory,
    private val resourceManager: ResourceManager,
    private val addressDisplayUseCase: AddressDisplayUseCase,
    private val assetsSourceRegistry: AssetSourceRegistry,
    private val chainRegistry: ChainRegistry,
    private val chainId: ChainId,
    private val assetId: Int,
    private val currencyRepository: CurrencyRepository
) : TransactionHistoryMixin, CoroutineScope by CoroutineScope(Dispatchers.Default) {

    private val domainState = singleReplaySharedFlow<State>()

    private val chainAsync by lazyAsync {
        chainRegistry.getChain(chainId)
    }

    private val chainAssetAsync by lazyAsync {
        chainRegistry.asset(chainId, assetId)
    }

    private val historyFiltersProviderAsync by lazyAsync {
        historyFiltersProviderFactory.get(
            scope = this,
            chainId = chainId,
            chainAssetId = assetId
        )
    }

    private val tokenFlow = walletInteractor.assetFlow(chainId, assetId)
        .map { it.token }

    override val state = combine(domainState, tokenFlow) { state, token ->
        mapOperationHistoryStateToUi(state, token)
    }
        .inBackground()
        .shareIn(this, started = SharingStarted.Eagerly, replay = 1)

    private val cachedPage = walletInteractor.operationsFirstPageFlow(chainId, assetId)
        .distinctUntilChangedBy { it.cursorPage }
        .onEach { performTransition(Action.CachePageArrived(it.cursorPage, it.accountChanged)) }
        .map { it.cursorPage }
        .inBackground()
        .shareIn(this, SharingStarted.Eagerly, replay = 1)

    init {
        emitInitialState()

        observeFilters()
    }

    override fun scrolled(currentIndex: Int) {
        launch { performTransition(Action.Scrolled(currentIndex)) }
    }

    override suspend fun syncFirstOperationsPage(): Result<*> {
        return walletInteractor.syncOperationsFirstPage(
            chainId = chainId,
            chainAssetId = assetId,
            pageSize = TransactionStateMachine.PAGE_SIZE,
            filters = allAvailableFilters()
        ).onFailure {
            performTransition(Action.PageError(error = it))

            Log.d(LOG_TAG, "Failed to sync operations page", it)
        }
    }

    override fun transactionClicked(transactionId: String) {
        launch {
            val operations = (domainState.first() as? State.WithData)?.data ?: return@launch

            val clickedOperation = operations.first { it.id == transactionId }

            val currency = currencyRepository.getSelectedCurrency()

            withContext(Dispatchers.Main) {
                when (val payload = mapOperationToParcel(clickedOperation, chainRegistry, resourceManager, currency)) {
                    is OperationParcelizeModel.Transfer -> {
                        router.openTransferDetail(payload)
                    }

                    is OperationParcelizeModel.Extrinsic -> {
                        router.openExtrinsicDetail(payload)
                    }

                    is OperationParcelizeModel.Reward -> {
                        router.openRewardDetail(payload)
                    }

                    is OperationParcelizeModel.PoolReward -> {
                        router.openPoolRewardDetail(payload)
                    }
                    is OperationParcelizeModel.Swap -> {
                        // TODO operation details
                    }
                }
            }
        }
    }

    private fun observeFilters() = launch {
        historyFiltersProviderAsync().filtersFlow()
            .map(::ensureOnlyAvailableFiltersUsed)
            .distinctUntilChanged()
            .onEach { performTransition(Action.FiltersChanged(it)) }
            .collect()
    }

    // We cannot currently trust HistoryFiltersProvider to not allow users to select unavailable filters so ensure it here
    private suspend fun ensureOnlyAvailableFiltersUsed(used: Set<TransactionFilter>): Set<TransactionFilter> {
        val allAvailable = allAvailableFilters()

        return allAvailable.intersect(used)
    }

    private fun emitInitialState() {
        launch {
            val initialState = State.EmptyProgress(
                allAvailableFilters = allAvailableFilters(),
                usedFilters = allAvailableFilters()
            )

            domainState.emit(initialState)
        }
    }

    private suspend fun performTransition(action: Action) = withContext(Dispatchers.Default) {
        val newState = TransactionStateMachine.transition(action, domainState.first()) { sideEffect ->
            when (sideEffect) {
                is TransactionStateMachine.SideEffect.ErrorEvent -> {
                    // ignore errors here, they are bypassed to client of mixin
                }
                is TransactionStateMachine.SideEffect.LoadPage -> loadNewPage(sideEffect)
                TransactionStateMachine.SideEffect.TriggerCache -> triggerCache()
            }
        }

        domainState.emit(newState)
    }

    private fun triggerCache() {
        val cached = cachedPage.replayCache.firstOrNull()

        cached?.let {
            launch { performTransition(Action.CachePageArrived(it, accountChanged = false)) }
        }
    }

    private fun loadNewPage(sideEffect: TransactionStateMachine.SideEffect.LoadPage) {
        launch {
            walletInteractor.getOperations(chainId, assetId, sideEffect.pageSize, sideEffect.nextPageOffset, sideEffect.filters)
                .onFailure {
                    performTransition(Action.PageError(error = it))
                }.onSuccess {
                    performTransition(Action.NewPage(newPage = it, loadedWith = sideEffect.filters))
                }
        }
    }

    private suspend fun mapOperationHistoryStateToUi(state: State, token: Token): TransactionHistoryUi.State {
        val listState = when (state) {
            is State.Empty -> ListState.Empty
            is State.EmptyProgress -> ListState.EmptyProgress
            is State.Data -> ListState.Data(transformDataToUi(state.data, token))
            is State.FullData -> ListState.Data(transformDataToUi(state.data, token))
            is State.NewPageProgress -> ListState.Data(transformDataToUi(state.data, token))
        }

        return TransactionHistoryUi.State(
            filtersButtonVisible = filterButtonVisible(),
            listState = listState
        )
    }

    private suspend fun filterButtonVisible(): Boolean {
        // 0 or 1 filters does not need filters screen since there will be nothing to change there
        return allAvailableFilters().size > 1
    }

    private suspend fun allAvailableFilters(): Set<TransactionFilter> {
        val assetSource = assetsSourceRegistry.sourceFor(chainAssetAsync())

        return assetSource.history.availableOperationFilters(chainAsync(), chainAssetAsync())
    }

    private suspend fun transformDataToUi(data: List<Operation>, token: Token): List<Any> {
        val accountIdentifier = addressDisplayUseCase.createIdentifier()
        val chain = chainAsync()

        return data.groupBy { it.time.daysFromMillis() }
            .map { (daysSinceEpoch, operationsPerDay) ->
                val header = DayHeader(daysSinceEpoch)

                val operationModels = operationsPerDay.map { operation ->
                    mapOperationToOperationModel(chain, token, operation, accountIdentifier, resourceManager)
                }

                listOf(header) + operationModels
            }.flatten()
    }
}
