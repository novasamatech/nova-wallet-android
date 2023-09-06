package io.novafoundation.nova.feature_account_api.domain.account.system

import jp.co.soramitsu.fearless_utils.runtime.AccountId

interface SystemAccountMatcher {

    companion object

    fun isSystemAccount(accountId: AccountId): Boolean
}

fun SystemAccountMatcher.Companion.default(): SystemAccountMatcher {
    return CompoundSystemAccountMatcher(
        // Pallet-specific technical accounts, e.g. crowdloan-fund, nomination pool,
        PrefixSystemAccountMatcher("modl"),
        // Parachain sovereign accounts on relaychain
        PrefixSystemAccountMatcher("para"),
        // Relaychain sovereign account on parachains
        PrefixSystemAccountMatcher("Parent"),
        // Sibling parachain soveregin accounts
        PrefixSystemAccountMatcher("sibl")
    )
}
