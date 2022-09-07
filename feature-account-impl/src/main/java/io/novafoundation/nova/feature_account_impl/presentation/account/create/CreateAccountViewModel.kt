package io.novafoundation.nova.feature_account_impl.presentation.account.create

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_account_api.presenatation.account.add.AddAccountPayload
import io.novafoundation.nova.feature_account_api.presenatation.account.createName.CreateWalletNameViewModel
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter

class CreateAccountViewModel(
    private val router: AccountRouter,
    private val resourceManager: ResourceManager,
) : CreateWalletNameViewModel(router, resourceManager) {

    override fun proceed(name: String) {
        router.openMnemonicScreen(name, AddAccountPayload.MetaAccount)
    }
}
