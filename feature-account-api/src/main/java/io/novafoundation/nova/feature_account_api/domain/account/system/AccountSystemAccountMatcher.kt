package io.novafoundation.nova.feature_account_api.domain.account.system

import io.novafoundation.nova.common.address.AccountIdKey
import io.novasama.substrate_sdk_android.runtime.AccountId

class AccountSystemAccountMatcher(private val accountIdKey: AccountIdKey) : SystemAccountMatcher {

    override fun isSystemAccount(accountId: AccountId): Boolean {
        return accountIdKey.value.contentEquals(accountId)
    }
}
