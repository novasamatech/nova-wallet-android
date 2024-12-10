package io.novafoundation.nova.feature_assets.presentation.tokens.manage

import androidx.lifecycle.lifecycleScope

import coil.ImageLoader
import dev.chrisbanes.insetter.applyInsetter
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.utils.bindTo
import io.novafoundation.nova.common.utils.keyboard.hideSoftKeyboard
import io.novafoundation.nova.common.utils.setVisible
import io.novafoundation.nova.common.utils.keyboard.showSoftKeyboard
import io.novafoundation.nova.common.utils.submitListPreservingViewPoint
import io.novafoundation.nova.common.view.bindFromMap
import io.novafoundation.nova.feature_assets.databinding.FragmentManageTokensBinding
import io.novafoundation.nova.feature_assets.di.AssetsFeatureApi
import io.novafoundation.nova.feature_assets.di.AssetsFeatureComponent
import io.novafoundation.nova.feature_assets.domain.assets.filters.NonZeroBalanceFilter
import kotlinx.android.synthetic.main.fragment_manage_tokens.manageTokensContainer
import kotlinx.android.synthetic.main.fragment_manage_tokens.manageTokensList
import kotlinx.android.synthetic.main.fragment_manage_tokens.manageTokensPlaceholder
import kotlinx.android.synthetic.main.fragment_manage_tokens.manageTokensSearch
import kotlinx.android.synthetic.main.fragment_manage_tokens.manageTokensToolbar
import javax.inject.Inject
import kotlinx.android.synthetic.main.fragment_manage_tokens.manageTokensSwitchZeroBalances

class ManageTokensFragment :
    BaseFragment<ManageTokensViewModel, FragmentManageTokensBinding>(),
    ManageTokensAdapter.ItemHandler {

    override fun createBinding() = FragmentManageTokensBinding.inflate(layoutInflater)

    @Inject
    lateinit var imageLoader: ImageLoader

    private val tokensAdapter by lazy(LazyThreadSafetyMode.NONE) {
        ManageTokensAdapter(imageLoader, this)
    }

    override fun initViews() {
        binder.manageTokensToolbar.applyStatusBarInsets()
        binder.manageTokensContainer.applyInsetter {
            type(ime = true) {
                padding()
            }
        }

        binder.manageTokensList.setHasFixedSize(true)
        binder.manageTokensList.adapter = tokensAdapter

        binder.manageTokensList.itemAnimator = null

        binder.manageTokensToolbar.setHomeButtonListener { viewModel.closeClicked() }
        binder.manageTokensToolbar.setRightActionClickListener { viewModel.addClicked() }

        binder.manageTokensSearch.requestFocus()
        binder.manageTokensSearch.content.showSoftKeyboard()

        binder.manageTokensSwitchZeroBalances.bindFromMap(NonZeroBalanceFilter, viewModel.filtersEnabledMap, viewLifecycleOwner.lifecycleScope)
    }

    override fun inject() {
        FeatureUtils.getFeature<AssetsFeatureComponent>(this, AssetsFeatureApi::class.java)
            .manageTokensComponentFactory()
            .create(this)
            .inject(this)
    }

    override fun subscribe(viewModel: ManageTokensViewModel) {
        binder.manageTokensSearch.content.bindTo(viewModel.query, lifecycleScope)

        viewModel.searchResults.observe { data ->
            binder.manageTokensPlaceholder.setVisible(data.isEmpty())
            binder.manageTokensList.setVisible(data.isNotEmpty())

            tokensAdapter.submitListPreservingViewPoint(data = data, into = binder.manageTokensList)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        requireActivity().hideSoftKeyboard()
    }

    override fun enableSwitched(position: Int) {
        viewModel.enableTokenSwitchClicked(position)
    }

    override fun editClocked(position: Int) {
        viewModel.editClicked(position)
    }
}
