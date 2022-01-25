package io.novafoundation.nova.feature_dapp_impl.presentation.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.lifecycle.lifecycleScope
import coil.ImageLoader
import dev.chrisbanes.insetter.applyInsetter
import io.novafoundation.nova.common.base.BaseBottomSheetFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.utils.bindTo
import io.novafoundation.nova.common.utils.hideSoftKeyboard
import io.novafoundation.nova.common.utils.showSoftKeyboard
import io.novafoundation.nova.feature_dapp_api.di.DAppFeatureApi
import io.novafoundation.nova.feature_dapp_impl.R
import io.novafoundation.nova.feature_dapp_impl.di.DAppFeatureComponent
import io.novafoundation.nova.feature_dapp_impl.domain.search.DappSearchResult
import kotlinx.android.synthetic.main.fragment_search_dapp.*
import javax.inject.Inject

class DappSearchFragment : BaseBottomSheetFragment<DAppSearchViewModel>(), SearchDappAdapter.Handler {

    companion object {

        private const val PAYLOAD = "DappSearchFragment.PAYLOAD"

        fun getBundle(payload: SearchPayload) = bundleOf(
            PAYLOAD to payload
        )
    }

    @Inject
    lateinit var imageLoader: ImageLoader

    private val adapter by lazy(LazyThreadSafetyMode.NONE) { SearchDappAdapter(imageLoader, this) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return layoutInflater.inflate(R.layout.fragment_search_dapp, container, false)
    }

    override fun initViews() {
        searchDappSearhGroup.applyStatusBarInsets()
        searchDappSearhContainer.applyInsetter {
            type(ime = true) {
                padding()
            }
        }
        searchDappList.adapter = adapter
        searchDappList.setHasFixedSize(true)

        searchDappCancel.setOnClickListener {
            viewModel.cancelClicked()

            hideKeyboard()
        }

        searhDappQuery.requestFocus()
        searhDappQuery.content.showSoftKeyboard()
    }

    override fun inject() {
        FeatureUtils.getFeature<DAppFeatureComponent>(this, DAppFeatureApi::class.java)
            .dAppSearchComponentFactory()
            .create(this, argument(PAYLOAD))
            .inject(this)
    }

    override fun subscribe(viewModel: DAppSearchViewModel) {
        searhDappQuery.content.bindTo(viewModel.query, lifecycleScope)

        viewModel.searchResults.observe(::submitListPreservingViewPoint)

        viewModel.selectQueryTextEvent.observeEvent {
            searhDappQuery.content.selectAll()
        }
    }

    override fun itemClicked(searchResult: DappSearchResult) {
        hideKeyboard()

        viewModel.searchResultClicked(searchResult)
    }

    private fun hideKeyboard() {
        searhDappQuery.hideSoftKeyboard()
    }

    private fun submitListPreservingViewPoint(data: List<Any?>) {
        val recyclerViewState = searchDappList.layoutManager!!.onSaveInstanceState()

        adapter.submitList(data) {
            searchDappList.layoutManager!!.onRestoreInstanceState(recyclerViewState)
        }
    }
}
