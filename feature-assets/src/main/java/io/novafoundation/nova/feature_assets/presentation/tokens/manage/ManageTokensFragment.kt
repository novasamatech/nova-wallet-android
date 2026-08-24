package io.novafoundation.nova.feature_assets.presentation.tokens.manage

import android.view.View
import androidx.lifecycle.lifecycleScope

import coil.ImageLoader
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.mixin.actionAwaitable.setupConfirmationDialog
import io.novafoundation.nova.common.utils.insets.applyNavigationBarInsets
import io.novafoundation.nova.common.utils.insets.applyStatusBarInsets
import io.novafoundation.nova.common.utils.bindTo
import io.novafoundation.nova.common.utils.insets.ImeInsetsState
import io.novafoundation.nova.common.utils.keyboard.hideSoftKeyboard
import io.novafoundation.nova.common.utils.setVisible
import io.novafoundation.nova.common.utils.keyboard.showSoftKeyboard
import io.novafoundation.nova.common.utils.setTabSelectedListener
import io.novafoundation.nova.common.utils.submitListPreservingViewPoint
import io.novafoundation.nova.common.view.bindFromMap
import io.novafoundation.nova.feature_assets.R
import io.novafoundation.nova.feature_assets.databinding.FragmentManageTokensBinding
import io.novafoundation.nova.feature_assets.di.AssetsFeatureApi
import io.novafoundation.nova.feature_assets.di.AssetsFeatureComponent
import io.novafoundation.nova.feature_assets.domain.assets.filters.NonZeroBalanceFilter
import io.novafoundation.nova.feature_assets.presentation.tokens.manage.model.ManageTokensRvItem
import io.novafoundation.nova.feature_assets.presentation.tokens.manage.model.ManageTokensTab
import java.math.BigDecimal
import javax.inject.Inject

class ManageTokensFragment :
    BaseFragment<ManageTokensViewModel, FragmentManageTokensBinding>(),
    ManageTokensAdapter.ItemHandler {

    override fun createBinding() = FragmentManageTokensBinding.inflate(layoutInflater)

    @Inject
    lateinit var imageLoader: ImageLoader

    private val tokensAdapter by lazy(LazyThreadSafetyMode.NONE) {
        ManageTokensAdapter(imageLoader, this)
    }

    override fun applyInsets(rootView: View) {
        binder.manageTokensToolbarContainer.applyStatusBarInsets()
        binder.manageTokensList.applyNavigationBarInsets(consume = false, imeInsets = ImeInsetsState.ENABLE_IF_SUPPORTED)
    }

    override fun initViews() {
        binder.manageTokensList.setHasFixedSize(false)
        binder.manageTokensList.adapter = tokensAdapter

        binder.manageTokensList.itemAnimator = null

        binder.manageTokensToolbar.setHomeButtonListener { viewModel.closeClicked() }
        binder.manageTokensToolbar.setRightActionClickListener { viewModel.addClicked() }

        binder.manageTokensSearch.requestFocus()
        binder.manageTokensSearch.content.showSoftKeyboard()

        binder.manageTokensSwitchZeroBalances.bindFromMap(NonZeroBalanceFilter, viewModel.filtersEnabledMap, viewLifecycleOwner.lifecycleScope)

        setupDustFilter()
        setupTabs()

        binder.manageTokensSelectAll.setOnClickListener { viewModel.selectAllClicked() }
    }

    private fun setupDustFilter() {
        binder.manageTokensSwitchDustFilter.setOnCheckedChangeListener { _, isChecked ->
            viewModel.dustFilterToggled(isChecked)
        }

        binder.manageTokensDustThresholdGroup.setOnCheckedChangeListener { _, checkedId ->
            val threshold = when (checkedId) {
                R.id.manageTokensDustThreshold1 -> BigDecimal.ONE
                R.id.manageTokensDustThreshold5 -> BigDecimal(5)
                R.id.manageTokensDustThreshold10 -> BigDecimal.TEN
                R.id.manageTokensDustThreshold50 -> BigDecimal(50)
                else -> BigDecimal.ONE
            }
            viewModel.dustThresholdSelected(threshold)
        }
    }

    private fun setupTabs() {
        val tokensTab = binder.manageTokensTabs.newTab().setText(R.string.manage_tokens_tab_tokens)
        val networksTab = binder.manageTokensTabs.newTab().setText(R.string.manage_tokens_tab_networks)

        binder.manageTokensTabs.addTab(tokensTab)
        binder.manageTokensTabs.addTab(networksTab)

        binder.manageTokensTabs.setTabSelectedListener { tab ->
            val selectedTab = when (tab.position) {
                0 -> ManageTokensTab.TOKENS
                1 -> ManageTokensTab.NETWORKS
                else -> ManageTokensTab.TOKENS
            }
            viewModel.tabSelected(selectedTab)
        }

        val currentTabIndex = if (viewModel.currentTab.value == ManageTokensTab.NETWORKS) 1 else 0
        binder.manageTokensTabs.getTabAt(currentTabIndex)?.select()
    }

    override fun inject() {
        FeatureUtils.getFeature<AssetsFeatureComponent>(this, AssetsFeatureApi::class.java)
            .manageTokensComponentFactory()
            .create(this)
            .inject(this)
    }

    override fun subscribe(viewModel: ManageTokensViewModel) {
        setupConfirmationDialog(R.style.AccentAlertDialogTheme, viewModel.confirmationAwaitableAction)

        binder.manageTokensSearch.content.bindTo(viewModel.query, lifecycleScope)

        viewModel.listItems.observe { data ->
            binder.manageTokensPlaceholder.setVisible(data.isEmpty())
            binder.manageTokensList.setVisible(data.isNotEmpty())

            tokensAdapter.submitListPreservingViewPoint(data = data, into = binder.manageTokensList)
        }

        viewModel.selectAllButtonTextRes.observe { textRes ->
            binder.manageTokensSelectAll.text = getString(textRes)
        }

        viewModel.zeroBalanceFilterEnabled.observe { zeroBalanceOn ->
            binder.manageTokensDustFilterContainer.setVisible(zeroBalanceOn)
            if (!zeroBalanceOn) {
                viewModel.dustFilterToggled(false)
            }
        }

        viewModel.dustFilterEnabled.observe { dustOn ->
            binder.manageTokensSwitchDustFilter.isChecked = dustOn
            binder.manageTokensDustThresholdGroup.setVisible(dustOn)
        }

        viewModel.dustFilterThreshold.observe { threshold ->
            val chipId = when {
                threshold.compareTo(BigDecimal(50)) == 0 -> R.id.manageTokensDustThreshold50
                threshold.compareTo(BigDecimal.TEN) == 0 -> R.id.manageTokensDustThreshold10
                threshold.compareTo(BigDecimal(5)) == 0 -> R.id.manageTokensDustThreshold5
                else -> R.id.manageTokensDustThreshold1
            }
            binder.manageTokensDustThresholdGroup.check(chipId)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        requireActivity().hideSoftKeyboard()
    }

    override fun headerClicked(headerId: String) {
        viewModel.headerClicked(headerId)
    }

    override fun childToggled(item: ManageTokensRvItem.Child) {
        viewModel.childToggled(item)
    }
}
