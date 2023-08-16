package io.novafoundation.nova.feature_account_api.domain.account.system

import jp.co.soramitsu.fearless_utils.runtime.AccountId

class CompoundSystemAccountMatcher(
    private val delegates: List<SystemAccountMatcher>
): SystemAccountMatcher {

    constructor(vararg delegates: SystemAccountMatcher): this(delegates.toList())

    override fun isSystemAccount(accountId: AccountId): Boolean {
        return delegates.any { it.isSystemAccount(accountId) }
    }
}
