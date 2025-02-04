package io.novafoundation.nova.feature_dapp_impl.presentation.tab

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.recyclerview.widget.GridLayoutManager
import coil.ImageLoader
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.feature_dapp_api.di.DAppFeatureApi
import io.novafoundation.nova.feature_dapp_impl.R
import io.novafoundation.nova.feature_dapp_impl.di.DAppFeatureComponent
import io.novafoundation.nova.feature_dapp_impl.presentation.browser.main.DAPP_SHARED_ELEMENT_ID_IMAGE_TAB
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

        onBackPressed { viewModel.done() }

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
        setupCloseAllDappTabsDialogue(viewModel.closeAllTabsConfirmation)

        viewModel.tabsFlow.observe {
            adapter.submitList(it)
            browserTabsList.scrollToPosition(it.size - 1)
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
