package io.novafoundation.nova.web3names.domain.models

import jp.co.soramitsu.fearless_utils.runtime.AccountId

class Web3NameAccount(
    val accountId: AccountId?,
    val address: String,
    val isValid: Boolean,
    val description: String?,
)
