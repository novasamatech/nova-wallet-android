package io.novafoundation.nova.feature_onboarding_impl

import io.novafoundation.nova.feature_account_api.presenatation.account.add.AddAccountPayload

interface OnboardingRouter {

    fun openCreateAccount(addAccountPayload: AddAccountPayload.MetaAccount)

    fun openMnemonicScreen(accountName: String?, payload: AddAccountPayload)

    fun openImportAccountScreen(addAccountPayload: AddAccountPayload)

    fun back()
}
