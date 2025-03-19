package io.novafoundation.nova.feature_account_impl.presentation.legacyAddress

import io.novafoundation.nova.common.base.BaseViewModel
import io.novafoundation.nova.common.data.network.AppLinksProvider
import io.novafoundation.nova.common.mixin.api.Browserable
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountInteractor
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.feature_account_api.domain.account.common.ChainWithAccountId
import io.novafoundation.nova.feature_account_api.presenatation.account.copyAddress.CopyAddressMixin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class ChainAddressSelectorViewModel(
    private val router: AccountRouter,
    private val accountInteractor: AccountInteractor,
    private val payload: ChainAddressSelectorPayload,
    private val copyAddressMixin: CopyAddressMixin,
    private val appLinksProvider: AppLinksProvider
) : BaseViewModel(), Browserable.Presentation by Browserable() {

    private val chainWithAccountIdFlow = accountInteractor.chainFlow(payload.chainId)
        .map { ChainWithAccountId(it, payload.accountId) }
        .shareInBackground()

    val newAddressFlow = chainWithAccountIdFlow.map { copyAddressMixin.getPrimaryAddress(it) }

    val legacyAddressFlow = chainWithAccountIdFlow.map { copyAddressMixin.getLegacyAddress(it) }

    fun back() {
        router.back()
    }

    fun copyNewAddress() {
        launch {
            copyAddressMixin.copyPrimaryAddress(chainWithAccountIdFlow.first())
            back()
        }
    }

    fun copyLegacyAddress() {
        launch {
            copyAddressMixin.copyLegacyAddress(chainWithAccountIdFlow.first())
            back()
        }
    }

    fun disableAddressSelector(disable: Boolean) {
        enableAddressSelector(!disable)
    }

    fun addressSelectorDisabled(): Boolean {
        return !copyAddressMixin.shouldShowAddressSelector()
    }

    fun openLearnMore() {
        showBrowser(appLinksProvider.unifiedAddressArticle)
    }

    private fun enableAddressSelector(enable: Boolean) {
        copyAddressMixin.enableAddressSelector(enable)
    }
}
