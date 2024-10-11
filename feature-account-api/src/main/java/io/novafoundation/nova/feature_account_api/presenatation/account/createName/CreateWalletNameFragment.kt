package io.novafoundation.nova.feature_account_api.presenatation.account.createName

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.utils.bindTo
import io.novafoundation.nova.common.utils.keyboard.hideSoftKeyboard
import io.novafoundation.nova.common.view.setState
import io.novafoundation.nova.feature_account_api.R

abstract class CreateWalletNameFragment<V : CreateWalletNameViewModel> : BaseFragment<V>() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_create_wallet_name, container, false)
    }

    override fun initViews() {
        createWalletNameToolbar.setHomeButtonListener { viewModel.homeButtonClicked() }

        createWalletNameContinue.setOnClickListener {
            createWalletNameInput.hideSoftKeyboard()
            viewModel.nextClicked()
        }
    }

    override fun subscribe(viewModel: V) {
        viewModel.continueState.observe(createWalletNameContinue::setState)

        createWalletNameInput.bindTo(viewModel.name, viewLifecycleOwner.lifecycleScope)
    }
}
