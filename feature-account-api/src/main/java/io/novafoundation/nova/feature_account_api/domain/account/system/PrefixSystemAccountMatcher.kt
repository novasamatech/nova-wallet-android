package io.novafoundation.nova.feature_account_api.domain.account.system

import io.novafoundation.nova.common.utils.startsWith
import io.novasama.substrate_sdk_android.runtime.AccountId

class PrefixSystemAccountMatcher(prefix: String) : SystemAccountMatcher {

    private val prefixBytes = prefix.encodeToByteArray()

    override fun isSystemAccount(accountId: AccountId): Boolean {
        return accountId.startsWith(prefixBytes)
    }
}
