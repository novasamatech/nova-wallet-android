package io.novafoundation.nova.feature_account_impl.presentation.watchOnly.change

import android.os.Bundle

import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.view.setState
import io.novafoundation.nova.feature_account_api.di.AccountFeatureApi
import io.novafoundation.nova.feature_account_api.presenatation.account.add.AddAccountPayload
import io.novafoundation.nova.feature_account_api.presenatation.mixin.addressInput.setupAddressInput
import io.novafoundation.nova.feature_account_impl.databinding.FragmentChangeWatchWalletBinding
import io.novafoundation.nova.feature_account_impl.di.AccountFeatureComponent

class ChangeWatchAccountFragment : BaseFragment<ChangeWatchAccountViewModel, FragmentChangeWatchWalletBinding>() {

    companion object {
        private const val KEY_ADD_ACCOUNT_PAYLOAD = "ChangeWatchAccountFragment.add_account_payload"

        fun getBundle(payload: AddAccountPayload.ChainAccount): Bundle {
            return Bundle().apply {
                putParcelable(KEY_ADD_ACCOUNT_PAYLOAD, payload)
            }
        }
    }

    override fun createBinding() = FragmentChangeWatchWalletBinding.inflate(layoutInflater)

    override fun initViews() {
        binder.changeWatchAccountToolbar.applyStatusBarInsets()
        binder.changeWatchAccountToolbar.setHomeButtonListener { viewModel.backClicked() }

        binder.changeWatchAccountContinue.setOnClickListener { viewModel.nextClicked() }
    }

    override fun inject() {
        FeatureUtils.getFeature<AccountFeatureComponent>(requireContext(), AccountFeatureApi::class.java)
            .changeWatchAccountComponentFactory()
            .create(this, argument(KEY_ADD_ACCOUNT_PAYLOAD))
            .inject(this)
    }

    override fun subscribe(viewModel: ChangeWatchAccountViewModel) {
        setupAddressInput(viewModel.chainAddressMixin, binder.changeWatchAccountChainAddress)

        viewModel.inputHint.observe(binder.changeWatchAccountChainAddressHint::setText)

        viewModel.buttonState.observe(binder.changeWatchAccountContinue::setState)
    }
}
