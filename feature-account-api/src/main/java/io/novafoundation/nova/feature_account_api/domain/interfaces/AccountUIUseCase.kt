package io.novafoundation.nova.feature_account_api.domain.interfaces

import android.graphics.drawable.Drawable
import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.address.AddressIconGenerator.Companion.SIZE_MEDIUM
import io.novafoundation.nova.common.address.AddressModel
import io.novafoundation.nova.common.address.intoKey
import io.novafoundation.nova.feature_account_api.domain.account.identity.Identity
import io.novafoundation.nova.feature_account_api.domain.account.identity.IdentityProvider
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.accountIdKeyIn
import io.novafoundation.nova.feature_account_api.presenatation.account.icon.createAddressModel
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletModel
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.runtime.AccountId

sealed interface AccountModel {

    fun getTitle(): String

    fun getIcon(): Drawable?

    fun getAddress(): String

    class Wallet(
        metaId: Long,
        name: String,
        icon: Drawable?,
        private val address: String
    ) : WalletModel(metaId, name, icon), AccountModel {

        constructor(walletModel: WalletModel, address: String) : this(walletModel.metaId, walletModel.name, walletModel.icon, address)

        override fun getTitle() = name
        override fun getIcon() = icon
        override fun getAddress() = address
    }

    class Address(
        address: String,
        image: Drawable,
        name: String? = null
    ) : AddressModel(address, image, name), AccountModel {

        constructor(addressModel: AddressModel) : this(addressModel.address, addressModel.image, addressModel.name)

        override fun getTitle() = nameOrAddress
        override fun getIcon() = image
        override fun getAddress() = address
    }
}

interface AccountUIUseCase {

    suspend fun getAccountModel(accountId: AccountId, chain: Chain): AccountModel

    suspend fun getAccountModels(accountIds: Set<AccountId>, chain: Chain): Map<AccountIdKey, AccountModel>
}

class RealAccountUIUseCase(
    private val accountRepository: AccountRepository,
    private val walletUiUseCase: WalletUiUseCase,
    private val addressIconGenerator: AddressIconGenerator,
    private val identityProvider: IdentityProvider
) : AccountUIUseCase {

    override suspend fun getAccountModel(accountId: AccountId, chain: Chain): AccountModel {
        return getAccountModelInternal(
            accountId,
            chain,
            accountRepository.findMetaAccount(accountId, chain.id),
            identityProvider.identityFor(accountId, chain.id)
        )
    }

    override suspend fun getAccountModels(accountIds: Set<AccountId>, chain: Chain): Map<AccountIdKey, AccountModel> {
        val identities = identityProvider.identitiesFor(accountIds, chain.id)
        val metaAccounts = accountRepository.getActiveMetaAccounts().associateBy { it.accountIdKeyIn(chain) }

        return accountIds.map { it.intoKey() }.associateWith { accountId ->
            val metaAccount = metaAccounts[accountId]
            val identity = identities[accountId]
            getAccountModelInternal(accountId.value, chain, metaAccount, identity)
        }
    }

    private suspend fun getAccountModelInternal(accountId: AccountId, chain: Chain, metaAccount: MetaAccount?, identity: Identity?): AccountModel {
        return when (metaAccount) {
            null -> {
                val addressModel = addressIconGenerator.createAddressModel(
                    chain,
                    chain.addressOf(accountId),
                    SIZE_MEDIUM,
                    accountName = identity?.name
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
