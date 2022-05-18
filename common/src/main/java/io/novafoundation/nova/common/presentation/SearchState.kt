package io.novafoundation.nova.common.presentation

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.utils.setVisible
import io.novafoundation.nova.common.utils.submitListPreservingViewPoint
import io.novafoundation.nova.common.view.PlaceholderView
import kotlinx.coroutines.flow.Flow

sealed class SearchState<out T> {
    
    object NoInput : SearchState<Nothing>()

    object Loading : SearchState<Nothing>()

    object NoResults : SearchState<Nothing>()

    class Success<T>(val data: List<T>, val headerTitle: String) : SearchState<T>()
}


fun <T> BaseFragment<*>.observeSearchState(
    state: Flow<SearchState<T>>,
    recyclerView: RecyclerView,
    progress: View,
    placeholder: PlaceholderView,
    header: TextView,
    headerGroup: View,
    adapter: ListAdapter<T, *>
) {
    state.observe {
        recyclerView.setVisible(it is SearchState.Success, falseState = View.INVISIBLE)
        progress.setVisible(it is SearchState.Loading, falseState = View.INVISIBLE)
        placeholder.setVisible(it is SearchState.NoResults || it is SearchState.NoInput)
        headerGroup.setVisible(it is SearchState.Success)

        when (it) {
            SearchState.NoInput -> {
                placeholder.setImage(R.drawable.ic_placeholder)
                placeholder.setText(getString(R.string.search_recipient_welcome_v2_2_0))
            }
            SearchState.NoResults -> {
                placeholder.setImage(R.drawable.ic_no_search_results)
                placeholder.setText(getString(R.string.staking_validator_search_empty_title))
            }
            SearchState.Loading -> {}
            is SearchState.Success -> {
                header.text = it.headerTitle

                adapter.submitListPreservingViewPoint(it.data, recyclerView)
            }
        }
    }
}
