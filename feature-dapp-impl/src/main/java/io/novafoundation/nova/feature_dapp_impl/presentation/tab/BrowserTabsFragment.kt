package io.novafoundation.nova.feature_dapp_impl.presentation.tab

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import coil.ImageLoader
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.view.dialog.warningDialog
import io.novafoundation.nova.feature_dapp_api.di.DAppFeatureApi
import io.novafoundation.nova.feature_dapp_impl.R
import io.novafoundation.nova.feature_dapp_impl.di.DAppFeatureComponent
import javax.inject.Inject
import kotlinx.android.synthetic.main.fragment_browser_tabs.browserTabsAddTab
import kotlinx.android.synthetic.main.fragment_browser_tabs.browserTabsCloseTabs
import kotlinx.android.synthetic.main.fragment_browser_tabs.browserTabsDone
import kotlinx.android.synthetic.main.fragment_browser_tabs.browserTabsList

class BrowserTabsFragment : BaseFragment<BrowserTabsViewModel>(), BrowserTabsAdapter.Handler {

    @Inject
    lateinit var imageLoader: ImageLoader

    private val adapter by lazy(LazyThreadSafetyMode.NONE) {
        BrowserTabsAdapter(imageLoader, this)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return layoutInflater.inflate(R.layout.fragment_browser_tabs, container, false)
    }

    override fun initViews() {
        requireView().applyStatusBarInsets()

        browserTabsList.layoutManager = GridLayoutManager(requireContext(), 2)
        browserTabsList.adapter = adapter

        browserTabsCloseTabs.setOnClickListener { viewModel.closeAllTabs() }
        browserTabsAddTab.setOnClickListener { viewModel.addTab() }
        browserTabsDone.setOnClickListener { viewModel.done() }
    }

    override fun inject() {
        FeatureUtils.getFeature<DAppFeatureComponent>(this, DAppFeatureApi::class.java)
            .browserTabsComponentFactory()
            .create(this)
            .inject(this)
    }

    override fun subscribe(viewModel: BrowserTabsViewModel) {
        setupCloseAllTabsDialogue()

        viewModel.tabsFlow.observe {
            adapter.submitList(it)
        }
    }

    override fun tabClicked(item: BrowserTabRvItem) {
        viewModel.openTab(item.tabId)
    }

    override fun tabCloseClicked(item: BrowserTabRvItem) {
        viewModel.closeTab(item.tabId)
    }


    private fun setupCloseAllTabsDialogue() {
        viewModel.closeAllTabsConfirmation.awaitableActionLiveData.observeEvent { event ->
            warningDialog(
                context = providedContext,
                onPositiveClick = { event.onSuccess(Unit) },
                positiveTextRes = R.string.browser_tabs_close_all,
                negativeTextRes = R.string.common_cancel,
                onNegativeClick = { event.onCancel() },
                styleRes = R.style.AccentNegativeAlertDialogTheme_Reversed
            ) {
                setTitle(R.string.close_dapp_tabs_title)

                setMessage(R.string.close_dapp_tabs_message)
            }
        }
    }
}
