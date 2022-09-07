package io.novafoundation.nova.feature_account_impl.presentation.account.create

import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.feature_account_api.di.AccountFeatureApi
import io.novafoundation.nova.feature_account_api.presenatation.account.createName.CreateWalletNameFragment
import io.novafoundation.nova.feature_account_impl.di.AccountFeatureComponent

class CreateAccountFragment : CreateWalletNameFragment<CreateAccountViewModel>() {

    override fun inject() {
        FeatureUtils.getFeature<AccountFeatureComponent>(requireContext(), AccountFeatureApi::class.java)
            .createAccountComponentFactory()
            .create(this)
            .inject(this)
    }
}
