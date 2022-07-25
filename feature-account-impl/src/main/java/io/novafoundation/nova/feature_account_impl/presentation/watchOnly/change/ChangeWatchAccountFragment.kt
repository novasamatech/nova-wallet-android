package io.novafoundation.nova.feature_account_impl.presentation.watchOnly.change

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.applyStatusBarInsets
import io.novafoundation.nova.common.view.setState
import io.novafoundation.nova.feature_account_api.di.AccountFeatureApi
import io.novafoundation.nova.feature_account_api.presenatation.account.add.AddAccountPayload
import io.novafoundation.nova.feature_account_api.presenatation.mixin.addressInput.setupAddressInput
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.di.AccountFeatureComponent
import kotlinx.android.synthetic.main.fragment_change_watch_wallet.changeWatchAccountChainAddress
import kotlinx.android.synthetic.main.fragment_change_watch_wallet.changeWatchAccountChainAddressHint
import kotlinx.android.synthetic.main.fragment_change_watch_wallet.changeWatchAccountContinue
import kotlinx.android.synthetic.main.fragment_change_watch_wallet.changeWatchAccountToolbar

class ChangeWatchAccountFragment : BaseFragment<ChangeWatchAccountViewModel>() {

    companion object {
        private const val KEY_ADD_ACCOUNT_PAYLOAD = "ChangeWatchAccountFragment.add_account_payload"

        fun getBundle(payload: AddAccountPayload.ChainAccount): Bundle {
            return Bundle().apply {
                putParcelable(KEY_ADD_ACCOUNT_PAYLOAD, payload)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_change_watch_wallet, container, false)
    }

    override fun initViews() {
        changeWatchAccountToolbar.applyStatusBarInsets()
        changeWatchAccountToolbar.setHomeButtonListener { viewModel.backClicked() }

        changeWatchAccountContinue.setOnClickListener { viewModel.nextClicked() }
    }

    override fun inject() {
        FeatureUtils.getFeature<AccountFeatureComponent>(requireContext(), AccountFeatureApi::class.java)
            .changeWatchAccountComponentFactory()
            .create(this, argument(KEY_ADD_ACCOUNT_PAYLOAD))
            .inject(this)
    }

    override fun subscribe(viewModel: ChangeWatchAccountViewModel) {
        setupAddressInput(viewModel.chainAddressMixin, changeWatchAccountChainAddress)

        viewModel.inputHint.observe(changeWatchAccountChainAddressHint::setText)

        viewModel.buttonState.observe(changeWatchAccountContinue::setState)
    }
}
