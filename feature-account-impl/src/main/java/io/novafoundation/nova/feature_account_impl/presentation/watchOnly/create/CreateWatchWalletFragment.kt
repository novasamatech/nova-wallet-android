package io.novafoundation.nova.feature_account_impl.presentation.watchOnly.create

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import dev.chrisbanes.insetter.applyInsetter
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.utils.bindTo
import io.novafoundation.nova.common.utils.scrollOnFocusTo
import io.novafoundation.nova.common.view.ChipActionsAdapter
import io.novafoundation.nova.common.view.setState
import io.novafoundation.nova.feature_account_api.di.AccountFeatureApi
import io.novafoundation.nova.feature_account_api.presenatation.mixin.addressInput.setupAddressInput
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.di.AccountFeatureComponent

class CreateWatchWalletFragment : BaseFragment<CreateWatchWalletViewModel, FragmentCreateWatchWalletBinding>() {

    private val suggestionsAdapter by lazy(LazyThreadSafetyMode.NONE) {
        ChipActionsAdapter(viewModel::walletSuggestionClicked)
    }

    override val binder by viewBinding(FragmentCreateWatchWalletBinding::bind)

    override fun initViews() {
        binder.createWatchWalletToolbar.applyStatusBarInsets()
        binder.createWatchWalletToolbar.setHomeButtonListener { viewModel.homeButtonClicked() }

        binder.createWatchWalletPresets.adapter = suggestionsAdapter
        binder.createWatchWalletPresets.setHasFixedSize(true)

        binder.createWatchWalletContinue.setOnClickListener { viewModel.nextClicked() }

        binder.createWatchWalletContainer.applyInsetter {
            type(ime = true) {
                padding()
            }
        }

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
