package io.novafoundation.nova.feature_account_impl.presentation.watchOnly.create

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.utils.bindTo
import io.novafoundation.nova.common.view.ChipActionsAdapter
import io.novafoundation.nova.common.view.setState
import io.novafoundation.nova.feature_account_api.di.AccountFeatureApi
import io.novafoundation.nova.feature_account_api.presenatation.mixin.addressInput.setupAddressInput
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.di.AccountFeatureComponent
import kotlinx.android.synthetic.main.fragment_create_watch_wallet.createWatchWalletContinue
import kotlinx.android.synthetic.main.fragment_create_watch_wallet.createWatchWalletEvmAddress
import kotlinx.android.synthetic.main.fragment_create_watch_wallet.createWatchWalletName
import kotlinx.android.synthetic.main.fragment_create_watch_wallet.createWatchWalletPresets
import kotlinx.android.synthetic.main.fragment_create_watch_wallet.createWatchWalletSubstrateAddress
import kotlinx.android.synthetic.main.fragment_create_watch_wallet.createWatchWalletToolbar

class CreateWatchWalletFragment : BaseFragment<CreateWatchWalletViewModel>() {

    private val suggestionsAdapter by lazy(LazyThreadSafetyMode.NONE) {
        ChipActionsAdapter(viewModel::walletSuggestionClicked)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_create_watch_wallet, container, false)
    }

    override fun initViews() {
        createWatchWalletToolbar.applyStatusBarInsets()
        createWatchWalletToolbar.setHomeButtonListener { viewModel.homeButtonClicked() }

        createWatchWalletPresets.adapter = suggestionsAdapter
        createWatchWalletPresets.setHasFixedSize(true)

        createWatchWalletContinue.setOnClickListener { viewModel.nextClicked() }
    }

    override fun inject() {
        FeatureUtils.getFeature<AccountFeatureComponent>(requireContext(), AccountFeatureApi::class.java)
            .createWatchOnlyComponentFactory()
            .create(this)
            .inject(this)
    }

    override fun subscribe(viewModel: CreateWatchWalletViewModel) {
        setupAddressInput(viewModel.substrateAddressInput, createWatchWalletSubstrateAddress)
        setupAddressInput(viewModel.evmAddressInput, createWatchWalletEvmAddress)

        createWatchWalletName.bindTo(viewModel.nameInput, viewLifecycleOwner.lifecycleScope)

        viewModel.buttonState.observe(createWatchWalletContinue::setState)

        viewModel.suggestionChipActionModels.observe(suggestionsAdapter::submitList)
    }
}
