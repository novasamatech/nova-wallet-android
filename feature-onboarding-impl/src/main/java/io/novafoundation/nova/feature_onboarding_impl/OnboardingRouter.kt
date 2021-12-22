package io.novafoundation.nova.feature_onboarding_impl

import io.novafoundation.nova.feature_account_api.presenatation.account.add.AddAccountPayload
import io.novafoundation.nova.feature_account_api.presenatation.account.add.ImportAccountPayload

interface OnboardingRouter {

    fun openCreateAccount(addAccountPayload: AddAccountPayload.MetaAccount)

    fun openMnemonicScreen(accountName: String?, payload: AddAccountPayload)

    fun openImportAccountScreen(payload: ImportAccountPayload)

    fun back()
}
