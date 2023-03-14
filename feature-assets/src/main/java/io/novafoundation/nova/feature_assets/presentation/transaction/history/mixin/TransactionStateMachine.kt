package io.novafoundation.nova.feature_assets.presentation.transaction.history.mixin

import io.novafoundation.nova.common.data.model.DataPage
import io.novafoundation.nova.common.data.model.PageOffset
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.TransactionFilter
import io.novafoundation.nova.feature_wallet_api.domain.model.Operation

object TransactionStateMachine {

    const val PAGE_SIZE = 25
    private const val SCROLL_OFFSET = PAGE_SIZE / 2

    sealed class State(
        val allAvailableFilters: Set<TransactionFilter>,
        val usedFilters: Set<TransactionFilter>
    ) {

        interface WithData {
            val data: List<Operation>
        }

        class Empty(
            allAvailableFilters: Set<TransactionFilter>,
            usedFilters: Set<TransactionFilter>
        ) : State(allAvailableFilters, usedFilters)

        class EmptyProgress(
            allAvailableFilters: Set<TransactionFilter>,
            usedFilters: Set<TransactionFilter>
        ) : State(allAvailableFilters, usedFilters)

        class Data(
            val nextPageOffset: PageOffset.Loadable,
            override val data: List<Operation>,
            allAvailableFilters: Set<TransactionFilter>,
            usedFilters: Set<TransactionFilter>,
        ) : State(allAvailableFilters, usedFilters), WithData

        class NewPageProgress(
            val nextPageOffset: PageOffset.Loadable,
            override val data: List<Operation>,
            allAvailableFilters: Set<TransactionFilter>,
            usedFilters: Set<TransactionFilter>,
        ) : State(allAvailableFilters, usedFilters), WithData

        class FullData(
            override val data: List<Operation>,
            allAvailableFilters: Set<TransactionFilter>,
            usedFilters: Set<TransactionFilter>,
        ) : State(allAvailableFilters, usedFilters), WithData
    }

    sealed class Action {

        class Scrolled(val currentItemIndex: Int) : Action()

        data class CachePageArrived(
            val newPage: DataPage<Operation>,
            val accountChanged: Boolean
        ) : Action()

        data class NewPage(val newPage: DataPage<Operation>, val loadedWith: Set<TransactionFilter>) : Action()

        data class PageError(val error: Throwable) : Action()

        class FiltersChanged(val newUsedFilters: Set<TransactionFilter>) : Action()
    }

    sealed class SideEffect {

        data class LoadPage(
            val nextPageOffset: PageOffset.Loadable,
            val filters: Set<TransactionFilter>,
            val pageSize: Int = PAGE_SIZE,
        ) : SideEffect()

        data class ErrorEvent(val error: Throwable) : SideEffect()

        object TriggerCache : SideEffect()
    }

    fun transition(
        action: Action,
        state: State,
        sideEffectListener: (SideEffect) -> Unit,
    ): State =
        when (action) {
            is Action.CachePageArrived -> {
                val nextOffset = action.newPage.nextOffset

                when {
                    !canUseCache(state.allAvailableFilters, state.usedFilters) -> {
                        if (action.accountChanged) {
                            // trigger cold load for new account when not able to use cache
                            sideEffectListener(SideEffect.LoadPage(nextPageOffset = PageOffset.Loadable.FirstPage, filters = state.usedFilters))

                            State.EmptyProgress(
                                allAvailableFilters = state.allAvailableFilters,
                                usedFilters = state.usedFilters
                            )
                        } else {
                            // if account is the same - ignore new page, since cache is not used
                            state
                        }
                    }
                    action.newPage.isEmpty() -> State.Empty(state.allAvailableFilters, state.usedFilters)
                    nextOffset is PageOffset.Loadable -> State.Data(
                        nextPageOffset = nextOffset,
                        data = action.newPage,
                        allAvailableFilters = state.allAvailableFilters,
                        usedFilters = state.usedFilters
                    )
                    else -> State.FullData(
                        data = action.newPage,
                        allAvailableFilters = state.allAvailableFilters,
                        usedFilters = state.usedFilters
                    )
                }
            }

            is Action.Scrolled -> {
                when (state) {
                    is State.Data -> {
                        if (action.currentItemIndex >= state.data.size - SCROLL_OFFSET) {
                            sideEffectListener(SideEffect.LoadPage(state.nextPageOffset, state.usedFilters))

                            State.NewPageProgress(
                                nextPageOffset = state.nextPageOffset,
                                data = state.data,
                                allAvailableFilters = state.allAvailableFilters,
                                usedFilters = state.usedFilters
                            )
                        } else {
                            state
                        }
                    }

                    else -> state
                }
            }

            is Action.NewPage -> {
                val page = action.newPage
                val nextPageOffset = page.nextOffset

                when (state) {
                    is State.EmptyProgress -> {
                        when {
                            action.loadedWith != state.usedFilters -> state // not relevant anymore page has arrived, still loading

                            nextPageOffset is PageOffset.FullData && page.isEmpty() -> State.Empty(
                                allAvailableFilters = state.allAvailableFilters,
                                usedFilters = state.usedFilters
                            )

                            nextPageOffset is PageOffset.FullData && page.isNotEmpty() -> State.FullData(
                                data = page,
                                allAvailableFilters = state.allAvailableFilters,
                                usedFilters = state.usedFilters
                            )

                            nextPageOffset is PageOffset.Loadable -> {
                                // we didn't load enough items but can load more -> trigger next page automatically
                                if (page.items.size < PAGE_SIZE) {
                                    sideEffectListener(SideEffect.LoadPage(nextPageOffset, state.usedFilters))

                                    State.NewPageProgress(
                                        nextPageOffset = nextPageOffset,
                                        data = page,
                                        allAvailableFilters = state.allAvailableFilters,
                                        usedFilters = state.usedFilters
                                    )
                                } else {
                                    State.Data(
                                        nextPageOffset = nextPageOffset,
                                        data = page,
                                        allAvailableFilters = state.allAvailableFilters,
                                        usedFilters = state.usedFilters
                                    )
                                }
                            }

                            else -> error("Checked all cases of sealed class PageOffset QED")
                        }
                    }

                    is State.NewPageProgress -> {
                        when(nextPageOffset) {
                            is PageOffset.Loadable -> {
                                val newData = state.data + page

                                // we want to load at least one complete page without user scrolling
                                // we also don't wont to stop loading if no relevant items were fetched
                                if (newData.size < PAGE_SIZE || page.isEmpty()) {
                                    sideEffectListener(SideEffect.LoadPage(nextPageOffset, state.usedFilters))

                                    State.NewPageProgress(
                                        nextPageOffset = nextPageOffset,
                                        data = newData,
                                        allAvailableFilters = state.allAvailableFilters,
                                        usedFilters = state.usedFilters
                                    )
                                } else {
                                    State.Data(
                                        nextPageOffset = nextPageOffset,
                                        data = newData,
                                        allAvailableFilters = state.allAvailableFilters,
                                        usedFilters = state.usedFilters
                                    )
                                }
                            }

                            PageOffset.FullData -> State.FullData(
                                data = state.data + page,
                                allAvailableFilters = state.allAvailableFilters,
                                usedFilters = state.usedFilters
                            )
                        }
                    }

                    else -> state
                }
            }

            is Action.PageError -> {
                sideEffectListener(SideEffect.ErrorEvent(action.error))

                when (state) {
                    is State.EmptyProgress -> State.Empty(
                        allAvailableFilters = state.allAvailableFilters,
                        usedFilters = state.usedFilters
                    )
                    is State.NewPageProgress -> State.Data(
                        nextPageOffset = state.nextPageOffset,
                        data = state.data,
                        allAvailableFilters = state.allAvailableFilters,
                        usedFilters = state.usedFilters
                    )
                    else -> state
                }
            }

            is Action.FiltersChanged -> {
                val newFilters = action.newUsedFilters

                if (canUseCache(state.allAvailableFilters, newFilters)) {
                    sideEffectListener(SideEffect.TriggerCache)
                } else {
                    sideEffectListener(SideEffect.LoadPage(nextPageOffset = PageOffset.Loadable.FirstPage, filters = newFilters))
                }

                State.EmptyProgress(
                    allAvailableFilters = state.allAvailableFilters,
                    usedFilters = newFilters
                )
            }
        }

    private fun canUseCache(
        allAvailableFilters: Set<TransactionFilter>,
        usedFilters: Set<TransactionFilter>
    ) = allAvailableFilters == usedFilters
}
