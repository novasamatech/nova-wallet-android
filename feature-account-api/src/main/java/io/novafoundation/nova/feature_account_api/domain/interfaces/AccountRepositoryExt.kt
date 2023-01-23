package io.novafoundation.nova.feature_account_api.domain.interfaces

import io.novafoundation.nova.feature_account_api.domain.model.requireAccountIdIn
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import jp.co.soramitsu.fearless_utils.extensions.toHexString
import jp.co.soramitsu.fearless_utils.runtime.AccountId

suspend fun AccountRepository.findMetaAccountOrThrow(accountId: AccountId) = findMetaAccount(accountId)
    ?: error("No meta account found for accountId: ${accountId.toHexString()}")

suspend fun AccountRepository.requireIdOfSelectedMetaAccountIn(chain: Chain): AccountId {
    val metaAccount = getSelectedMetaAccount()

    return metaAccount.requireAccountIdIn(chain)
}
