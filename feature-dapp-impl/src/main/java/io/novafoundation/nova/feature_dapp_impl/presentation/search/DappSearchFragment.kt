package io.novafoundation.nova.feature_dapp_impl.presentation.search

import android.view.View
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import coil.ImageLoader
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.domain.isLoaded
import io.novafoundation.nova.common.domain.isLoading
import io.novafoundation.nova.common.domain.onLoaded
import io.novafoundation.nova.common.utils.insets.applyNavigationBarInsets
import io.novafoundation.nova.common.utils.insets.applyStatusBarInsets
import io.novafoundation.nova.common.utils.bindTo
import io.novafoundation.nova.common.utils.insets.ImeInsetsState
import io.novafoundation.nova.common.utils.keyboard.hideSoftKeyboard
import io.novafoundation.nova.common.utils.keyboard.showSoftKeyboard
import io.novafoundation.nova.common.view.dialog.warningDialog
import io.novafoundation.nova.feature_dapp_api.di.DAppFeatureApi
import io.novafoundation.nova.feature_dapp_impl.R
import io.novafoundation.nova.feature_dapp_impl.databinding.FragmentSearchDappBinding
import io.novafoundation.nova.feature_dapp_impl.di.DAppFeatureComponent
import io.novafoundation.nova.feature_dapp_impl.domain.search.DappSearchResult
import io.novafoundation.nova.feature_dapp_impl.presentation.main.DappCategoriesAdapter
import javax.inject.Inject

class DappSearchFragment : BaseFragment<DAppSearchViewModel, FragmentSearchDappBinding>(), SearchDappAdapter.Handler, DappCategoriesAdapter.Handler {

    companion object {

        private const val PAYLOAD = "DappSearchFragment.PAYLOAD"

        fun getBundle(payload: SearchPayload) = bundleOf(
            PAYLOAD to payload
        )
    }

    override fun createBinding() = FragmentSearchDappBinding.inflate(layoutInflater)

    @Inject
    lateinit var imageLoader: ImageLoader

    private val categoriesAdapter by lazy(LazyThreadSafetyMode.NONE) { DappCategoriesAdapter(imageLoader, this) }

    private val adapter by lazy(LazyThreadSafetyMode.NONE) { SearchDappAdapter(this) }

    override fun applyInsets(rootView: View) {
        binder.searchDappSearch.applyStatusBarInsets()
        binder.searchDappSearhContainer.applyNavigationBarInsets(consume = false, imeInsets = ImeInsetsState.ENABLE_IF_SUPPORTED)
    }

    override fun initViews() {
        binder.searchDappCategories.adapter = categoriesAdapter
        binder.searchDappList.adapter = adapter
        binder.searchDappList.setHasFixedSize(true)

        binder.searchDappSearch.cancel.setOnClickListener {
            viewModel.cancelClicked()

            hideKeyboard()
        }

        binder.searchDappSearch.searchInput.requestFocus()
        binder.searchDappSearch.searchInput.content.showSoftKeyboard()
    }

    override fun inject() {
        FeatureUtils.getFeature<DAppFeatureComponent>(this, DAppFeatureApi::class.java)
            .dAppSearchComponentFactory()
            .create(this, argument(PAYLOAD))
            .inject(this)
    }

    override fun subscribe(viewModel: DAppSearchViewModel) {
        setupDAppNotInCatalogWarning()
        binder.searchDappSearch.searchInput.content.bindTo(viewModel.query, lifecycleScope)

        viewModel.searchResults.observe(::submitListPreservingViewPoint)

        viewModel.selectQueryTextEvent.observeEvent {
            binder.searchDappSearch.searchInput.content.selectAll()
        }

        viewModel.categoriesFlow.observe {
            binder.searchDappCategoriesShimmering.isVisible = it.isLoading()
            binder.searchDappCategories.isVisible = it.isLoaded()
            it.onLoaded { categoriesAdapter.submitList(it) }
        }
    }

    override fun itemClicked(searchResult: DappSearchResult) {
        hideKeyboard()

        viewModel.searchResultClicked(searchResult)
    }

    private fun hideKeyboard() {
        binder.searchDappSearch.searchInput.hideSoftKeyboard()
    }

    private fun submitListPreservingViewPoint(data: List<Any?>) {
        val recyclerViewState = binder.searchDappList.layoutManager!!.onSaveInstanceState()

        adapter.submitList(data) {
            binder.searchDappList.layoutManager!!.onRestoreInstanceState(recyclerViewState)
        }
    }

    private fun setupDAppNotInCatalogWarning() {
        viewModel.dAppNotInCatalogWarning.awaitableActionLiveData.observeEvent { event ->
            warningDialog(
                context = providedContext,
                onPositiveClick = { event.onCancel() },
                positiveTextRes = R.string.common_close,
                negativeTextRes = R.string.dapp_url_warning_open_anyway,
                onNegativeClick = { event.onSuccess(Unit) },
                styleRes = R.style.AccentNegativeAlertDialogTheme
            ) {
                setTitle(R.string.dapp_url_warning_title)

                setMessage(requireContext().getString(R.string.dapp_url_warning_subtitle, event.payload.supportEmail))
            }
        }
    }

    override fun onCategoryClicked(id: String) {
        viewModel.onCategoryClicked(id)
    }
}
