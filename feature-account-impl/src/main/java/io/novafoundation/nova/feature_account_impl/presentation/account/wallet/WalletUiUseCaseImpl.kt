package io.novafoundation.nova.feature_account_impl.presentation.account.wallet

import android.graphics.drawable.Drawable
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.address.AddressIconGenerator.Companion.BACKGROUND_DEFAULT
import io.novafoundation.nova.common.address.AddressIconGenerator.Companion.BACKGROUND_TRANSPARENT
import io.novafoundation.nova.common.utils.ByteArrayComparator
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount.ChainAccount
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletModel
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapLatest

class WalletUiUseCaseImpl(
    private val accountRepository: AccountRepository,
    private val addressIconGenerator: AddressIconGenerator,
) : WalletUiUseCase {

    override fun selectedWalletUiFlow(
        showAddressIcon: Boolean
    ): Flow<WalletModel> {
        return accountRepository.selectedMetaAccountFlow().mapLatest { metaAccount ->
            val icon = maybeGenerateIcon(accountId = metaAccount.walletIconSeed(), shouldGenerate = showAddressIcon)

            WalletModel(
                name = metaAccount.name,
                icon = icon
            )
        }
    }

    override suspend fun selectedWalletUi(): WalletModel {
        val metaAccount = accountRepository.getSelectedMetaAccount()

        return WalletModel(
            name = metaAccount.name,
            icon = walletIcon(metaAccount)
        )
    }

    override suspend fun walletIcon(
        metaAccount: MetaAccount,
        transparentBackground: Boolean
    ): Drawable {
        val seed = metaAccount.walletIconSeed()

        return generateWalletIcon(seed, transparentBackground)
    }

    private suspend fun maybeGenerateIcon(accountId: AccountId, shouldGenerate: Boolean): Drawable? {
        return if (shouldGenerate) {
            generateWalletIcon(seed = accountId, transparentBackground = true)
        } else {
            null
        }
    }

    private suspend fun generateWalletIcon(seed: ByteArray, transparentBackground: Boolean): Drawable {
        return addressIconGenerator.createAddressIcon(
            accountId = seed,
            sizeInDp = AddressIconGenerator.SIZE_MEDIUM,
            backgroundColorRes = if (transparentBackground) BACKGROUND_TRANSPARENT else BACKGROUND_DEFAULT
        )
    }

    private fun MetaAccount.walletIconSeed(): ByteArray {
        return when {
            substrateAccountId != null -> substrateAccountId!!
            ethereumAddress != null -> ethereumAddress!!

            // if both default accounts are null there MUST be at least one chain account. Otherwise wallet is in invalid state
            else -> {
                chainAccounts.values
                    .map(ChainAccount::accountId)
                    .sortedWith(ByteArrayComparator())
                    .first()
            }
        }
    }
}
