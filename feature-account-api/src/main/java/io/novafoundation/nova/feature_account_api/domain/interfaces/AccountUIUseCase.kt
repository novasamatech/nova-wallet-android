package io.novafoundation.nova.feature_account_api.domain.interfaces

import android.graphics.drawable.Drawable
import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.address.AddressIconGenerator.Companion.BACKGROUND_TRANSPARENT
import io.novafoundation.nova.common.address.AddressIconGenerator.Companion.SIZE_MEDIUM
import io.novafoundation.nova.common.address.AddressModel
import io.novafoundation.nova.feature_account_api.domain.account.identity.Identity
import io.novafoundation.nova.feature_account_api.domain.account.identity.IdentityProvider
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.accountIdKeyIn
import io.novafoundation.nova.feature_account_api.presenatation.account.icon.createAddressModel
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletModel
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

sealed interface AccountModel {

    fun address(): String

    fun drawable(): Drawable?

    fun nameOrAddress(): String

    class Wallet(
        metaId: Long,
        name: String,
        icon: Drawable?,
        private val address: String
    ) : WalletModel(metaId, name, icon), AccountModel {

        constructor(walletModel: WalletModel, address: String) : this(walletModel.metaId, walletModel.name, walletModel.icon, address)

        override fun address() = address
        override fun drawable() = icon
        override fun nameOrAddress() = name
    }

    class Address(
        address: String,
        image: Drawable,
        name: String? = null
    ) : AddressModel(address, image, name), AccountModel {

        constructor(addressModel: AddressModel) : this(addressModel.address, addressModel.image, addressModel.name)

        override fun address() = address
        override fun drawable() = image
        override fun nameOrAddress() = nameOrAddress
    }
}

interface AccountUIUseCase {

    suspend fun getAccountModel(accountId: AccountIdKey, chain: Chain): AccountModel

    suspend fun getAccountModels(accountIds: Set<AccountIdKey>, chain: Chain): Map<AccountIdKey, AccountModel>
}

class RealAccountUIUseCase(
    private val accountRepository: AccountRepository,
    private val walletUiUseCase: WalletUiUseCase,
    private val addressIconGenerator: AddressIconGenerator,
    private val identityProvider: IdentityProvider
) : AccountUIUseCase {

    override suspend fun getAccountModel(accountId: AccountIdKey, chain: Chain): AccountModel {
        return getAccountModelInternal(
            accountId,
            chain,
            accountRepository.findMetaAccount(accountId.value, chain.id),
            identityProvider.identityFor(accountId.value, chain.id)
        )
    }

    override suspend fun getAccountModels(accountIds: Set<AccountIdKey>, chain: Chain): Map<AccountIdKey, AccountModel> {
        val identities = identityProvider.identitiesFor(accountIds.map { it.value }, chain.id)
        val metaAccounts = accountRepository.getActiveMetaAccounts().associateBy { it.accountIdKeyIn(chain) }

        return accountIds.associateWith { accountId ->
            val metaAccount = metaAccounts[accountId]
            val identity = identities[accountId]
            getAccountModelInternal(accountId, chain, metaAccount, identity)
        }
    }

    private suspend fun getAccountModelInternal(accountId: AccountIdKey, chain: Chain, metaAccount: MetaAccount?, identity: Identity?): AccountModel {
        return when (metaAccount) {
            null -> {
                val addressModel = addressIconGenerator.createAddressModel(
                    chain,
                    chain.addressOf(accountId),
                    SIZE_MEDIUM,
                    accountName = identity?.name,
                    background = BACKGROUND_TRANSPARENT
                )
                AccountModel.Address(addressModel)
            }

            else -> {
                val walletModel = walletUiUseCase.walletUiFor(metaAccount)
                AccountModel.Wallet(walletModel, chain.addressOf(accountId))
            }
        }
    }
}
