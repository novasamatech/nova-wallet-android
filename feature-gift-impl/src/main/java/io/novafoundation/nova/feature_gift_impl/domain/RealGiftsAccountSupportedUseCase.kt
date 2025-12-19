package io.novafoundation.nova.feature_gift_impl.domain

import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.domain.model.LightMetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.asMultisig
import io.novafoundation.nova.feature_account_api.domain.model.isThreshold1
import io.novafoundation.nova.feature_gift_api.domain.GiftsAccountSupportedUseCase
import io.novafoundation.nova.feature_gift_api.domain.GiftsSupportedState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RealGiftsAccountSupportedUseCase(
    private val selectedAccountUseCase: SelectedAccountUseCase,
) : GiftsAccountSupportedUseCase {

    override suspend fun supportedState(): GiftsSupportedState {
        val selectedAccount = selectedAccountUseCase.getSelectedMetaAccount()
        return selectedAccount.supportsGifts()
    }

    override fun areGiftsSupportedFlow(): Flow<GiftsSupportedState> {
        return selectedAccountUseCase.selectedMetaAccountFlow()
            .map { it.supportsGifts() }
    }

    private fun MetaAccount.supportsGifts(): GiftsSupportedState {
        return when (type) {
            LightMetaAccount.Type.SECRETS,
            LightMetaAccount.Type.WATCH_ONLY,
            LightMetaAccount.Type.PARITY_SIGNER,
            LightMetaAccount.Type.LEDGER_LEGACY,
            LightMetaAccount.Type.LEDGER,
            LightMetaAccount.Type.PROXIED,
            LightMetaAccount.Type.POLKADOT_VAULT -> GiftsSupportedState.SUPPORTED

            LightMetaAccount.Type.MULTISIG -> if (asMultisig().isThreshold1()) {
                GiftsSupportedState.SUPPORTED
            } else {
                GiftsSupportedState.UNSUPPORTED_MULTISIG_ACCOUNTS
            }
        }
    }
}
