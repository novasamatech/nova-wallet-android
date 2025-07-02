package io.novafoundation.nova.feature_account_impl.presentation.account.wallet

import android.graphics.drawable.Drawable
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.address.AddressIconGenerator.Companion.BACKGROUND_DEFAULT
import io.novafoundation.nova.common.address.AddressIconGenerator.Companion.BACKGROUND_TRANSPARENT
import io.novafoundation.nova.common.address.AddressIconGenerator.Companion.SIZE_MEDIUM
import io.novafoundation.nova.common.address.AddressModel
import io.novafoundation.nova.common.utils.ByteArrayComparator
import io.novafoundation.nova.common.utils.flowOf
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount.ChainAccount
import io.novafoundation.nova.feature_account_api.presenatation.account.icon.createAccountAddressModel
import io.novafoundation.nova.feature_account_api.presenatation.account.icon.createAddressIcon
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletModel
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.runtime.AccountId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapLatest

class WalletUiUseCaseImpl(
    private val accountRepository: AccountRepository,
    private val addressIconGenerator: AddressIconGenerator,
    private val chainRegistry: ChainRegistry
) : WalletUiUseCase {

    override fun selectedWalletUiFlow(
        showAddressIcon: Boolean
    ): Flow<WalletModel> {
        return accountRepository.selectedMetaAccountFlow().mapLatest { metaAccount ->
            val icon = maybeGenerateIcon(accountId = metaAccount.walletIconSeed(), shouldGenerate = showAddressIcon)

            WalletModel(
                metaId = metaAccount.id,
                name = metaAccount.name,
                icon = icon
            )
        }
    }

    override fun walletUiFlow(metaId: Long, showAddressIcon: Boolean): Flow<WalletModel> {
        return flowOf {
            val metaAccount = accountRepository.getMetaAccount(metaId)
            val icon = maybeGenerateIcon(accountId = metaAccount.walletIconSeed(), shouldGenerate = showAddressIcon)

            WalletModel(
                metaId = metaId,
                name = metaAccount.name,
                icon = icon
            )
        }
    }

    override fun walletUiFlow(metaId: Long, chainId: String, showAddressIcon: Boolean): Flow<WalletModel> {
        return flowOf {
            val metaAccount = accountRepository.getMetaAccount(metaId)
            val chain = chainRegistry.getChain(chainId)
            val icon = maybeGenerateIcon(accountId = metaAccount.accountIdIn(chain)!!, shouldGenerate = showAddressIcon)

            WalletModel(
                metaId = metaId,
                name = metaAccount.name,
                icon = icon
            )
        }
    }

    override suspend fun selectedWalletUi(): WalletModel {
        val metaAccount = accountRepository.getSelectedMetaAccount()

        return WalletModel(
            metaId = metaAccount.id,
            name = metaAccount.name,
            icon = walletIcon(metaAccount, SIZE_MEDIUM)
        )
    }

    override suspend fun walletIcon(
        substrateAccountId: AccountId?,
        ethereumAccountId: AccountId?,
        chainAccountIds: List<AccountId>,
        iconSize: Int,
        transparentBackground: Boolean
    ): Drawable {
        val seed = walletSeed(substrateAccountId, ethereumAccountId, chainAccountIds)

        return generateWalletIcon(seed, iconSize, transparentBackground)
    }

    override suspend fun walletIcon(
        metaAccount: MetaAccount,
        iconSize: Int,
        transparentBackground: Boolean
    ): Drawable {
        val seed = metaAccount.walletIconSeed()

        return generateWalletIcon(seed, iconSize, transparentBackground)
    }

    override suspend fun walletUiFor(metaAccount: MetaAccount): WalletModel {
        return WalletModel(
            metaId = metaAccount.id,
            name = metaAccount.name,
            icon = walletIcon(metaAccount, SIZE_MEDIUM, transparentBackground = true)
        )
    }

    private suspend fun maybeGenerateIcon(accountId: AccountId, shouldGenerate: Boolean): Drawable? {
        return if (shouldGenerate) {
            generateWalletIcon(seed = accountId, iconSize = SIZE_MEDIUM, transparentBackground = true)
        } else {
            null
        }
    }

    private suspend fun generateWalletIcon(seed: ByteArray, iconSize: Int, transparentBackground: Boolean): Drawable {
        return addressIconGenerator.createAddressIcon(
            accountId = seed,
            sizeInDp = iconSize,
            backgroundColorRes = if (transparentBackground) BACKGROUND_TRANSPARENT else BACKGROUND_DEFAULT
        )
    }

    private fun walletSeed(substrateAccountId: AccountId?, ethereumAccountId: AccountId?, chainAccountIds: List<AccountId>): AccountId {
        return when {
            substrateAccountId != null -> substrateAccountId
            ethereumAccountId != null -> ethereumAccountId

            // if both default accounts are null there MUST be at least one chain account. Otherwise it's an invalid state
            else -> {
                chainAccountIds
                    .sortedWith(ByteArrayComparator())
                    .first()
            }
        }
    }

    private fun MetaAccount.walletIconSeed(): ByteArray {
        return walletSeed(substrateAccountId, ethereumAddress, chainAccounts.values.map(ChainAccount::accountId))
    }

    override suspend fun walletAddressModel(metaAccount: MetaAccount, chain: Chain, iconSize: Int): AddressModel {
        return addressIconGenerator.createAccountAddressModel(
            chain = chain,
            account = metaAccount,
            name = metaAccount.name
        )
    }
}
