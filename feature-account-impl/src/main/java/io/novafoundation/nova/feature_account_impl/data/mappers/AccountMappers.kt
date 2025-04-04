package io.novafoundation.nova.feature_account_impl.data.mappers

import io.novafoundation.nova.common.utils.filterNotNull
import io.novafoundation.nova.core_db.model.chain.account.ChainAccountLocal
import io.novafoundation.nova.core_db.model.chain.account.JoinedMetaAccountInfo
import io.novafoundation.nova.core_db.model.chain.account.MetaAccountLocal
import io.novafoundation.nova.feature_account_api.domain.model.LightMetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_impl.domain.account.model.DefaultMetaAccount
import io.novafoundation.nova.feature_account_impl.domain.account.model.GenericLedgerMetaAccount
import io.novafoundation.nova.feature_account_impl.domain.account.model.LegacyLedgerMetaAccount
import io.novafoundation.nova.feature_account_impl.domain.account.model.PolkadotVaultMetaAccount
import io.novafoundation.nova.feature_account_impl.domain.account.model.ProxiedMetaAccount
import io.novafoundation.nova.feature_ledger_core.domain.LedgerMigrationTracker
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId

class AccountMappers(
    private val ledgerMigrationTracker: LedgerMigrationTracker
) {

    suspend fun mapMetaAccountsLocalToMetaAccounts(joinedMetaAccountInfo: List<JoinedMetaAccountInfo>): List<MetaAccount> {
        val supportedGenericLedgerChains = ledgerMigrationTracker.supportedChainIdsByGenericApp()

        return joinedMetaAccountInfo.map {
            mapMetaAccountLocalToMetaAccount(it) { supportedGenericLedgerChains }
        }
    }

    suspend fun mapMetaAccountLocalToMetaAccount(joinedMetaAccountInfo: JoinedMetaAccountInfo): MetaAccount {
        return mapMetaAccountLocalToMetaAccount(joinedMetaAccountInfo) {
            ledgerMigrationTracker.supportedChainIdsByGenericApp()
        }
    }

    private inline fun mapMetaAccountLocalToMetaAccount(
        joinedMetaAccountInfo: JoinedMetaAccountInfo,
        supportedGenericLedgerChains: () -> Set<ChainId>
    ): MetaAccount {
        val chainAccounts = joinedMetaAccountInfo.chainAccounts.associateBy(
            keySelector = ChainAccountLocal::chainId,
            valueTransform = {
                mapChainAccountFromLocal(it)
            }
        ).filterNotNull()

        val proxyAccount = joinedMetaAccountInfo.proxyAccountLocal?.let {
            mapProxyAccountFromLocal(it)
        }

        return with(joinedMetaAccountInfo.metaAccount) {
            when (val type = mapMetaAccountTypeFromLocal(type)) {
                LightMetaAccount.Type.SECRETS,
                LightMetaAccount.Type.WATCH_ONLY -> DefaultMetaAccount(
                    id = id,
                    globallyUniqueId = globallyUniqueId,
                    chainAccounts = chainAccounts,
                    proxy = proxyAccount,
                    substratePublicKey = substratePublicKey,
                    substrateCryptoType = substrateCryptoType,
                    substrateAccountId = substrateAccountId,
                    ethereumAddress = ethereumAddress,
                    ethereumPublicKey = ethereumPublicKey,
                    isSelected = isSelected,
                    name = name,
                    type = type,
                    status = mapMetaAccountStateFromLocal(status)
                )

                LightMetaAccount.Type.PARITY_SIGNER,
                LightMetaAccount.Type.POLKADOT_VAULT -> PolkadotVaultMetaAccount(
                    id = id,
                    globallyUniqueId = globallyUniqueId,
                    chainAccounts = chainAccounts,
                    proxy = proxyAccount,
                    substratePublicKey = substratePublicKey,
                    substrateCryptoType = substrateCryptoType,
                    substrateAccountId = substrateAccountId,
                    ethereumAddress = ethereumAddress,
                    ethereumPublicKey = ethereumPublicKey,
                    isSelected = isSelected,
                    name = name,
                    type = type,
                    status = mapMetaAccountStateFromLocal(status)
                )

                LightMetaAccount.Type.LEDGER -> GenericLedgerMetaAccount(
                    id = id,
                    globallyUniqueId = globallyUniqueId,
                    chainAccounts = chainAccounts,
                    proxy = proxyAccount,
                    substratePublicKey = substratePublicKey,
                    substrateCryptoType = substrateCryptoType,
                    substrateAccountId = substrateAccountId,
                    ethereumAddress = ethereumAddress,
                    ethereumPublicKey = ethereumPublicKey,
                    isSelected = isSelected,
                    name = name,
                    type = type,
                    status = mapMetaAccountStateFromLocal(status),
                    supportedGenericLedgerChains = supportedGenericLedgerChains()
                )

                LightMetaAccount.Type.LEDGER_LEGACY -> LegacyLedgerMetaAccount(
                    id = id,
                    globallyUniqueId = globallyUniqueId,
                    chainAccounts = chainAccounts,
                    proxy = proxyAccount,
                    substratePublicKey = substratePublicKey,
                    substrateCryptoType = substrateCryptoType,
                    substrateAccountId = substrateAccountId,
                    ethereumAddress = ethereumAddress,
                    ethereumPublicKey = ethereumPublicKey,
                    isSelected = isSelected,
                    name = name,
                    type = type,
                    status = mapMetaAccountStateFromLocal(status)
                )

                LightMetaAccount.Type.PROXIED -> ProxiedMetaAccount(
                    id = id,
                    globallyUniqueId = globallyUniqueId,
                    chainAccounts = chainAccounts,
                    proxy = proxyAccount!!,
                    substratePublicKey = substratePublicKey,
                    substrateCryptoType = substrateCryptoType,
                    substrateAccountId = substrateAccountId,
                    ethereumAddress = ethereumAddress,
                    ethereumPublicKey = ethereumPublicKey,
                    isSelected = isSelected,
                    name = name,
                    type = type,
                    status = mapMetaAccountStateFromLocal(status)
                )
            }
        }
    }

    fun mapMetaAccountLocalToLightMetaAccount(
        metaAccountLocal: MetaAccountLocal
    ): LightMetaAccount {
        return with(metaAccountLocal) {
            LightMetaAccount(
                id = id,
                substratePublicKey = substratePublicKey,
                substrateCryptoType = substrateCryptoType,
                substrateAccountId = substrateAccountId,
                ethereumAddress = ethereumAddress,
                ethereumPublicKey = ethereumPublicKey,
                isSelected = isSelected,
                name = name,
                type = mapMetaAccountTypeFromLocal(type),
                status = mapMetaAccountStateFromLocal(status),
                globallyUniqueId = globallyUniqueId
            )
        }
    }

    fun mapMetaAccountTypeFromLocal(local: MetaAccountLocal.Type): LightMetaAccount.Type {
        return when (local) {
            MetaAccountLocal.Type.SECRETS -> LightMetaAccount.Type.SECRETS
            MetaAccountLocal.Type.WATCH_ONLY -> LightMetaAccount.Type.WATCH_ONLY
            MetaAccountLocal.Type.PARITY_SIGNER -> LightMetaAccount.Type.PARITY_SIGNER
            MetaAccountLocal.Type.LEDGER -> LightMetaAccount.Type.LEDGER_LEGACY
            MetaAccountLocal.Type.LEDGER_GENERIC -> LightMetaAccount.Type.LEDGER
            MetaAccountLocal.Type.POLKADOT_VAULT -> LightMetaAccount.Type.POLKADOT_VAULT
            MetaAccountLocal.Type.PROXIED -> LightMetaAccount.Type.PROXIED
        }
    }

    private fun mapMetaAccountStateFromLocal(local: MetaAccountLocal.Status): LightMetaAccount.Status {
        return when (local) {
            MetaAccountLocal.Status.ACTIVE -> LightMetaAccount.Status.ACTIVE
            MetaAccountLocal.Status.DEACTIVATED -> LightMetaAccount.Status.DEACTIVATED
        }
    }
}
