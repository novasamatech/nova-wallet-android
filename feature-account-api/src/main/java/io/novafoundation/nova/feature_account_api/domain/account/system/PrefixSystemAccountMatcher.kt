package io.novafoundation.nova.feature_account_api.domain.account.system

import io.novafoundation.nova.common.utils.startsWith
import io.novasama.substrate_sdk_android.runtime.AccountId

class PrefixSystemAccountMatcher(private val prefix: ByteArray) : SystemAccountMatcher {

    constructor(utf8Prefix: String): this(utf8Prefix.encodeToByteArray())

    override fun isSystemAccount(accountId: AccountId): Boolean {
        return accountId.startsWith(prefix)
    }
}
