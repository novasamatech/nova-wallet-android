package io.novafoundation.nova.feature_account_impl.presentation.common.address

import io.novafoundation.nova.common.data.storage.Preferences
import io.novafoundation.nova.common.utils.CopyValueMixin
import io.novafoundation.nova.feature_account_api.domain.account.common.ChainWithAccountId
import io.novafoundation.nova.feature_account_api.presenatation.account.copyAddress.CopyAddressMixin
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.runtime.ext.hasOnlyOneAddressFormat
import io.novafoundation.nova.runtime.ext.legacyAddressOfOrNull

private const val SHOW_DIALOG_KEY = "SHOW_DIALOG_KEY"

class RealCopyAddressMixin(
    private val copyValueMixin: CopyValueMixin,
    private val preferences: Preferences,
    private val router: AccountRouter
) : CopyAddressMixin {

    override fun copyAddressOrOpenSelector(chainWithAccountId: ChainWithAccountId) {
        val chain = chainWithAccountId.chain

        if (chain.hasOnlyOneAddressFormat() || addressSelectorDisabled()) {
            copyPrimaryAddress(chainWithAccountId)
        } else {
            val accountId = chainWithAccountId.accountId
            openAddressSelector(chain.id, accountId)
        }
    }

    override fun copyPrimaryAddress(chainWithAccountId: ChainWithAccountId) {
        copyAddress(getPrimaryAddress(chainWithAccountId))
    }

    override fun copyLegacyAddress(chainWithAccountId: ChainWithAccountId) {
        copyAddress(getLegacyAddress(chainWithAccountId))
    }

    override fun getPrimaryAddress(chainWithAccountId: ChainWithAccountId): String {
        return chainWithAccountId.chain.addressOf(chainWithAccountId.accountId)
    }

    override fun getLegacyAddress(chainWithAccountId: ChainWithAccountId): String? {
        return chainWithAccountId.chain.legacyAddressOfOrNull(chainWithAccountId.accountId)
    }

    private fun addressSelectorDisabled() = !shouldShowAddressSelector()

    override fun shouldShowAddressSelector(): Boolean {
        return preferences.getBoolean(SHOW_DIALOG_KEY, true)
    }

    override fun enableAddressSelector(enable: Boolean) {
        preferences.putBoolean(SHOW_DIALOG_KEY, enable)
    }

    override fun openAddressSelector(chainId: String, accountId: ByteArray) {
        router.openChainAddressSelector(chainId, accountId)
    }

    private fun copyAddress(address: String?) {
        if (address == null) return

        copyValueMixin.copyValue(address)
    }
}
