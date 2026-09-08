package io.novafoundation.nova.feature_assets.presentation.tokens.manage

import android.view.View
import androidx.lifecycle.lifecycleScope
import coil.ImageLoader
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.bindTo
import io.novafoundation.nova.common.utils.insets.ImeInsetsState
import io.novafoundation.nova.common.utils.insets.applyNavigationBarInsets
import io.novafoundation.nova.common.utils.insets.applyStatusBarInsets
import io.novafoundation.nova.common.utils.keyboard.hideSoftKeyboard
import io.novafoundation.nova.common.utils.keyboard.showSoftKeyboard
import io.novafoundation.nova.common.utils.setVisible
import io.novafoundation.nova.common.utils.recyclerView.expandable.ExpandableAnimationSettings
import io.novafoundation.nova.common.utils.recyclerView.expandable.animator.ExpandableAnimator
import io.novafoundation.nova.common.utils.submitListPreservingViewPoint
import io.novafoundation.nova.feature_assets.presentation.balance.common.AssetTokensItemAnimator
import io.novafoundation.nova.feature_assets.presentation.balance.common.createForAssets
import io.novafoundation.nova.feature_assets.databinding.FragmentManageTokensBinding
import io.novafoundation.nova.feature_assets.di.AssetsFeatureApi
import io.novafoundation.nova.feature_assets.di.AssetsFeatureComponent
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

    private var expansionPending = false

    override fun applyInsets(rootView: View) {
        binder.manageTokensToolbarContainer.applyStatusBarInsets()
        binder.manageTokensList.applyNavigationBarInsets(consume = false, imeInsets = ImeInsetsState.ENABLE_IF_SUPPORTED)
    }

    override fun initViews() {
        binder.manageTokensList.setHasFixedSize(true)
        binder.manageTokensList.adapter = tokensAdapter

        // Same animator the balance list uses for its token groups, so expanding reads the same way
        val animationSettings = ExpandableAnimationSettings.createForAssets()
        val expandableAnimator = ExpandableAnimator(binder.manageTokensList, animationSettings, tokensAdapter)
        binder.manageTokensList.itemAnimator = AssetTokensItemAnimator(animationSettings, expandableAnimator)

        binder.manageTokensToolbar.setHomeButtonListener { viewModel.closeClicked() }
        binder.manageTokensToolbar.setRightActionClickListener { viewModel.addClicked() }

        binder.manageTokensSearch.requestFocus()
        binder.manageTokensSearch.content.showSoftKeyboard()

        binder.manageTokensSwitchAutoEnable.setOnCheckedChangeListener { _, isChecked ->
            viewModel.autoEnableTokensChanged(isChecked)
        }
    }

    override fun inject() {
        FeatureUtils.getFeature<AssetsFeatureComponent>(this, AssetsFeatureApi::class.java)
            .manageTokensComponentFactory()
            .create(this)
            .inject(this)
    }

    override fun subscribe(viewModel: ManageTokensViewModel) {
        binder.manageTokensSearch.content.bindTo(viewModel.query, lifecycleScope)

        viewModel.autoEnableTokens.observe { binder.manageTokensSwitchAutoEnable.isChecked = it }

        viewModel.listItems.observe { data ->
            binder.manageTokensPlaceholder.setVisible(data.isEmpty())
            binder.manageTokensList.setVisible(data.isNotEmpty())

            if (expansionPending) {
                // Restoring the scroll position calls requestLayout right as the animations start,
                // which cancels them. Expanding does not move the list anyway, so skip it here.
                expansionPending = false

                tokensAdapter.submitList(data)
            } else {
                tokensAdapter.submitListPreservingViewPoint(data = data, into = binder.manageTokensList)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        requireActivity().hideSoftKeyboard()
    }

    override fun groupClicked(position: Int) {
        // Must run before the list changes, otherwise the animator skips the item animations
        (binder.manageTokensList.itemAnimator as AssetTokensItemAnimator).prepareForAnimation()
        expansionPending = true

        viewModel.groupClicked(position)
    }

    override fun groupSwitched(position: Int) {
        viewModel.groupSwitched(position)
    }

    override fun childSwitched(position: Int) {
        viewModel.childSwitched(position)
    }
}
