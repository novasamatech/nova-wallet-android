package io.novafoundation.nova.feature_account_impl.presentation.watchOnly.create

import android.view.View
import androidx.lifecycle.lifecycleScope

import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.applySystemBarInsets
import io.novafoundation.nova.common.utils.bindTo
import io.novafoundation.nova.common.utils.scrollOnFocusTo
import io.novafoundation.nova.common.view.ChipActionsAdapter
import io.novafoundation.nova.common.view.setState
import io.novafoundation.nova.feature_account_api.di.AccountFeatureApi
import io.novafoundation.nova.feature_account_api.presenatation.mixin.addressInput.setupAddressInput
import io.novafoundation.nova.feature_account_impl.databinding.FragmentCreateWatchWalletBinding
import io.novafoundation.nova.feature_account_impl.di.AccountFeatureComponent

class CreateWatchWalletFragment : BaseFragment<CreateWatchWalletViewModel, FragmentCreateWatchWalletBinding>() {

    private val suggestionsAdapter by lazy(LazyThreadSafetyMode.NONE) {
        ChipActionsAdapter(viewModel::walletSuggestionClicked)
    }

    override fun createBinding() = FragmentCreateWatchWalletBinding.inflate(layoutInflater)

    override fun applyInsets(rootView: View) {
        binder.createWatchWalletContainer.applySystemBarInsets(imeInsets = true)
    }

    override fun initViews() {
        binder.createWatchWalletToolbar.setHomeButtonListener { viewModel.homeButtonClicked() }

        binder.createWatchWalletPresets.adapter = suggestionsAdapter
        binder.createWatchWalletPresets.setHasFixedSize(true)

        binder.createWatchWalletContinue.setOnClickListener { viewModel.nextClicked() }

        binder.createWatchWalletScrollArea.scrollOnFocusTo(
            binder.createWatchWalletName,
            binder.createWatchWalletEvmAddress,
            binder.createWatchWalletSubstrateAddress
        )
    }

    override fun inject() {
        FeatureUtils.getFeature<AccountFeatureComponent>(requireContext(), AccountFeatureApi::class.java)
            .createWatchOnlyComponentFactory()
            .create(this)
            .inject(this)
    }

    override fun subscribe(viewModel: CreateWatchWalletViewModel) {
        setupAddressInput(viewModel.substrateAddressInput, binder.createWatchWalletSubstrateAddress)
        setupAddressInput(viewModel.evmAddressInput, binder.createWatchWalletEvmAddress)

        binder.createWatchWalletName.bindTo(viewModel.nameInput, viewLifecycleOwner.lifecycleScope)

        viewModel.buttonState.observe(binder.createWatchWalletContinue::setState)

        viewModel.suggestionChipActionModels.observe(suggestionsAdapter::submitList)
    }
}
