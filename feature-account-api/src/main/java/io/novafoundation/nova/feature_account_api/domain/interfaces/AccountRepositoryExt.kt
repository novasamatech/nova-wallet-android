package io.novafoundation.nova.feature_account_api.domain.interfaces

import jp.co.soramitsu.fearless_utils.extensions.toHexString
import jp.co.soramitsu.fearless_utils.runtime.AccountId

suspend fun AccountRepository.findMetaAccountOrThrow(accountId: AccountId) = findMetaAccount(accountId)
    ?: error("No meta account found for accountId: ${accountId.toHexString()}")
