package io.novafoundation.nova.feature_dapp_impl.presentation.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import kotlinx.android.synthetic.main.fragment_search_dapp.searchDappCancel
import kotlinx.android.synthetic.main.fragment_search_dapp.searchDappList
import kotlinx.android.synthetic.main.fragment_search_dapp.searchDappSearhContainer
import kotlinx.android.synthetic.main.fragment_search_dapp.searchDappSearhGroup
import kotlinx.android.synthetic.main.fragment_search_dapp.searhDappQuery
import javax.inject.Inject

class DappSearchFragment : BaseBottomSheetFragment<DAppSearchViewModel>(), SearchDappAdapter.Handler {

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

        searchDappCancel.setOnClickListener { viewModel.cancelClicked() }

        searhDappQuery.requestFocus()
        searhDappQuery.content.showSoftKeyboard()
    }

    override fun inject() {
        FeatureUtils.getFeature<DAppFeatureComponent>(this, DAppFeatureApi::class.java)
            .dAppSearchComponentFactory()
            .create(this)
            .inject(this)
    }

    override fun subscribe(viewModel: DAppSearchViewModel) {
        searhDappQuery.content.bindTo(viewModel.query, lifecycleScope)

        viewModel.searchResults.observe(::submitListPreservingViewPoint)
    }

    override fun itemClicked(searchResult: DappSearchResult) {
        searhDappQuery.hideSoftKeyboard()

        viewModel.searchResultClicked(searchResult)
    }

    private fun submitListPreservingViewPoint(data: List<Any?>) {
        val recyclerViewState = searchDappList.layoutManager!!.onSaveInstanceState()

        adapter.submitList(data) {
            searchDappList.layoutManager!!.onRestoreInstanceState(recyclerViewState)
        }
    }
}
