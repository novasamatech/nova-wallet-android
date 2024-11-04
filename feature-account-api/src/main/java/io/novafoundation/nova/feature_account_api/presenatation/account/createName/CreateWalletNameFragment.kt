package io.novafoundation.nova.feature_account_api.presenatation.account.createName

import androidx.lifecycle.lifecycleScope
import by.kirich1409.viewbindingdelegate.viewBinding
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.utils.bindTo
import io.novafoundation.nova.common.utils.keyboard.hideSoftKeyboard
import io.novafoundation.nova.common.view.setState
import io.novafoundation.nova.feature_account_api.databinding.FragmentCreateWalletNameBinding

abstract class CreateWalletNameFragment<V : CreateWalletNameViewModel> : BaseFragment<V, FragmentCreateWalletNameBinding>() {

    override fun createBinding() = FragmentCreateWalletNameBinding.inflate(layoutInflater)

    override fun initViews() {
        binder.createWalletNameToolbar.setHomeButtonListener { viewModel.homeButtonClicked() }

        binder.createWalletNameContinue.setOnClickListener {
            binder.createWalletNameInput.hideSoftKeyboard()
            viewModel.nextClicked()
        }
    }

    override fun subscribe(viewModel: V) {
        viewModel.continueState.observe(binder.createWalletNameContinue::setState)

        binder.createWalletNameInput.bindTo(viewModel.name, viewLifecycleOwner.lifecycleScope)
    }
}
