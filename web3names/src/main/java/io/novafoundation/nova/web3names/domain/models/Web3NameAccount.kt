package io.novafoundation.nova.web3names.domain.models

import jp.co.soramitsu.fearless_utils.runtime.AccountId

class Web3NameAccount(
    val accountId: AccountId?,
    val address: String,
    val description: String?,
)

val Web3NameAccount.isValid: Boolean
    get() = accountId != null
