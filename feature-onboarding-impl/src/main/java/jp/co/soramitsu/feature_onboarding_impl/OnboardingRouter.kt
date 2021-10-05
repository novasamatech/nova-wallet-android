package jp.co.soramitsu.feature_onboarding_impl

import jp.co.soramitsu.feature_account_api.presenatation.account.add.AddAccountPayload

interface OnboardingRouter {

    fun openCreateAccount(addAccountPayload: AddAccountPayload)

    fun openImportAccountScreen(addAccountPayload: AddAccountPayload)

    fun back()
}
