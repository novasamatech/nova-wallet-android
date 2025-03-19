package io.novafoundation.nova.feature_account_api.presenatation.account.copyAddress

import io.novafoundation.nova.feature_account_api.domain.account.common.ChainWithAccountId

interface CopyAddressMixin {
    fun copyAddressOrOpenSelector(chainWithAccountId: ChainWithAccountId)

    fun copyPrimaryAddress(chainWithAccountId: ChainWithAccountId)

    fun copyLegacyAddress(chainWithAccountId: ChainWithAccountId)

    fun getBaseAddress(chainWithAccountId: ChainWithAccountId): String

    fun getLegacyAddress(chainWithAccountId: ChainWithAccountId): String?

    fun shouldShowAddressSelector(): Boolean

    fun enableAddressSelector(enable: Boolean)

    fun openAddressSelector(chainId: String, accountId: ByteArray)
}
