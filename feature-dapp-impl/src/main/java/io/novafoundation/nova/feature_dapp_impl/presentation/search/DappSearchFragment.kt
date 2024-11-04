package io.novafoundation.nova.feature_dapp_impl.presentation.search

import androidx.core.os.bundleOf
import androidx.lifecycle.lifecycleScope
import by.kirich1409.viewbindingdelegate.viewBinding
import dev.chrisbanes.insetter.applyInsetter
import io.novafoundation.nova.common.base.BaseBottomSheetFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.utils.bindTo
import io.novafoundation.nova.common.utils.keyboard.hideSoftKeyboard
import io.novafoundation.nova.common.utils.keyboard.showSoftKeyboard
import io.novafoundation.nova.common.view.dialog.warningDialog
import io.novafoundation.nova.feature_dapp_api.di.DAppFeatureApi
import io.novafoundation.nova.feature_dapp_impl.R
import io.novafoundation.nova.feature_dapp_impl.databinding.FragmentSearchDappBinding
import io.novafoundation.nova.feature_dapp_impl.di.DAppFeatureComponent
import io.novafoundation.nova.feature_dapp_impl.domain.search.DappSearchResult

class DappSearchFragment : BaseBottomSheetFragment<DAppSearchViewModel, FragmentSearchDappBinding>(), SearchDappAdapter.Handler {

    companion object {

        private const val PAYLOAD = "DappSearchFragment.PAYLOAD"

        fun getBundle(payload: SearchPayload) = bundleOf(
            PAYLOAD to payload
        )
    }

    override val binder by viewBinding(FragmentSearchDappBinding::bind)

    private val adapter by lazy(LazyThreadSafetyMode.NONE) { SearchDappAdapter(this) }

    override fun initViews() {
        binder.searchDappSearch.applyStatusBarInsets()
        binder.searchDappSearhContainer.applyInsetter {
            type(ime = true) {
                padding()
            }
        }
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
        viewModel.dAppNotInCatalogWarning
        viewModel.selectQueryTextEvent.observeEvent {
            binder.searchDappSearch.searchInput.content.selectAll()
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
}
