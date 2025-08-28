package io.novafoundation.nova.feature_dapp_impl.presentation.tab

import android.view.View
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.recyclerview.widget.GridLayoutManager
import coil.ImageLoader
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.feature_dapp_api.di.DAppFeatureApi
import io.novafoundation.nova.feature_dapp_impl.databinding.FragmentBrowserTabsBinding
import io.novafoundation.nova.feature_dapp_impl.di.DAppFeatureComponent
import io.novafoundation.nova.feature_dapp_impl.presentation.browser.main.DAPP_SHARED_ELEMENT_ID_IMAGE_TAB
import javax.inject.Inject

class BrowserTabsFragment : BaseFragment<BrowserTabsViewModel, FragmentBrowserTabsBinding>(), BrowserTabsAdapter.Handler {

    @Inject
    lateinit var imageLoader: ImageLoader

    private val adapter by lazy(LazyThreadSafetyMode.NONE) {
        BrowserTabsAdapter(imageLoader, this)
    }

    override fun createBinding() = FragmentBrowserTabsBinding.inflate(layoutInflater)

    override fun initViews() {
        onBackPressed { viewModel.done() }

        binder.browserTabsList.layoutManager = GridLayoutManager(requireContext(), 2)
        binder.browserTabsList.adapter = adapter

        binder.browserTabsCloseTabs.setOnClickListener { viewModel.closeAllTabs() }
        binder.browserTabsAddTab.setOnClickListener { viewModel.addTab() }
        binder.browserTabsDone.setOnClickListener { viewModel.done() }
    }

    override fun inject() {
        FeatureUtils.getFeature<DAppFeatureComponent>(this, DAppFeatureApi::class.java)
            .browserTabsComponentFactory()
            .create(this)
            .inject(this)
    }

    override fun subscribe(viewModel: BrowserTabsViewModel) {
        setupCloseAllDappTabsDialogue(viewModel.closeAllTabsConfirmation)

        viewModel.tabsFlow.observe {
            adapter.submitList(it)
            binder.browserTabsList.scrollToPosition(it.size - 1)
        }
    }

    override fun tabClicked(item: BrowserTabRvItem, view: View) {
        view.transitionName = DAPP_SHARED_ELEMENT_ID_IMAGE_TAB

        val extras = FragmentNavigatorExtras(
            view to DAPP_SHARED_ELEMENT_ID_IMAGE_TAB
        )
        viewModel.openTab(item, extras)
    }

    override fun tabCloseClicked(item: BrowserTabRvItem) {
        viewModel.closeTab(item.tabId)
    }
}
