package io.novafoundation.nova.feature_account_impl.presentation.common.createName

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.utils.bindTo
import io.novafoundation.nova.common.utils.hideSoftKeyboard
import io.novafoundation.nova.common.view.setState
import io.novafoundation.nova.feature_account_impl.R
import kotlinx.android.synthetic.main.fragment_create_wallet_name.createWalletNameContinue
import kotlinx.android.synthetic.main.fragment_create_wallet_name.createWalletNameInput
import kotlinx.android.synthetic.main.fragment_create_wallet_name.createWalletNameToolbar

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
