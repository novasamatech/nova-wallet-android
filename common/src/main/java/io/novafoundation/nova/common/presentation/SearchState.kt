package io.novafoundation.nova.common.presentation

sealed class SearchState<out T> {

    object NoInput : SearchState<Nothing>()

    object Loading : SearchState<Nothing>()

    object NoResults : SearchState<Nothing>()

    class Success<T>(val data: List<T>, val headerTitle: String) : SearchState<T>()
}
