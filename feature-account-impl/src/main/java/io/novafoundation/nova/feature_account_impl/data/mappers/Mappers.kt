package io.novafoundation.nova.feature_account_impl.data.mappers

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.asPrecision
import io.novafoundation.nova.core.model.CryptoType
import io.novafoundation.nova.core.model.Node
import io.novafoundation.nova.core.model.Node.NetworkType
import io.novafoundation.nova.core_db.dao.MetaAccountWithBalanceLocal
import io.novafoundation.nova.core_db.model.NodeLocal
import io.novafoundation.nova.core_db.model.chain.account.ChainAccountLocal
import io.novafoundation.nova.core_db.model.chain.account.MetaAccountLocal
import io.novafoundation.nova.core_db.model.chain.account.ProxyAccountLocal
import io.novafoundation.nova.feature_account_api.domain.model.AddAccountType
import io.novafoundation.nova.feature_account_api.domain.model.LightMetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccountAssetBalance
import io.novafoundation.nova.feature_account_api.domain.model.ProxyAccount
import io.novafoundation.nova.feature_account_api.presenatation.account.add.AddAccountPayload
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.presentation.common.mixin.api.AccountNameChooserMixin
import io.novafoundation.nova.feature_account_impl.presentation.node.model.NodeModel
import io.novafoundation.nova.feature_account_impl.presentation.view.advanced.encryption.model.CryptoTypeModel
import io.novafoundation.nova.feature_account_impl.presentation.view.advanced.network.model.NetworkModel
import io.novafoundation.nova.feature_proxy_api.domain.model.ProxyType
import io.novafoundation.nova.feature_proxy_api.domain.model.fromString

fun mapNetworkTypeToNetworkModel(networkType: NetworkType): NetworkModel {
    val type = when (networkType) {
        NetworkType.KUSAMA -> NetworkModel.NetworkTypeUI.Kusama
        NetworkType.POLKADOT -> NetworkModel.NetworkTypeUI.Polkadot
        NetworkType.WESTEND -> NetworkModel.NetworkTypeUI.Westend
        NetworkType.ROCOCO -> NetworkModel.NetworkTypeUI.Rococo
    }

    return NetworkModel(networkType.readableName, type)
}

fun mapCryptoTypeToCryptoTypeModel(
    resourceManager: ResourceManager,
    encryptionType: CryptoType
): CryptoTypeModel {
    val title = mapCryptoTypeToCryptoTypeTitle(resourceManager, encryptionType)
    val subtitle = mapCryptoTypeToCryptoTypeSubtitle(resourceManager, encryptionType)

    return CryptoTypeModel("$title $subtitle", encryptionType)
}

fun mapCryptoTypeToCryptoTypeTitle(
    resourceManager: ResourceManager,
    encryptionType: CryptoType
): String {
    return when (encryptionType) {
        CryptoType.SR25519 -> resourceManager.getString(R.string.sr25519_selection_title)

        CryptoType.ED25519 -> resourceManager.getString(R.string.ed25519_selection_title)

        CryptoType.ECDSA -> resourceManager.getString(R.string.ecdsa_selection_title)
    }
}

fun mapCryptoTypeToCryptoTypeSubtitle(
    resourceManager: ResourceManager,
    encryptionType: CryptoType
): String {
    return when (encryptionType) {
        CryptoType.SR25519 -> resourceManager.getString(R.string.sr25519_selection_subtitle)

        CryptoType.ED25519 -> resourceManager.getString(R.string.ed25519_selection_subtitle)

        CryptoType.ECDSA -> resourceManager.getString(R.string.ecdsa_selection_subtitle)
    }
}

fun mapNodeToNodeModel(node: Node): NodeModel {
    val networkModelType = mapNetworkTypeToNetworkModel(node.networkType)

    return with(node) {
        NodeModel(
            id = id,
            name = name,
            link = link,
            networkModelType = networkModelType.networkTypeUI,
            isDefault = isDefault,
            isActive = isActive
        )
    }
}

fun mapNodeLocalToNode(nodeLocal: NodeLocal): Node {
    return with(nodeLocal) {
        Node(
            id = id,
            name = name,
            networkType = NetworkType.values()[nodeLocal.networkType],
            link = link,
            isActive = isActive,
            isDefault = isDefault
        )
    }
}

fun mapMetaAccountTypeToLocal(local: LightMetaAccount.Type): MetaAccountLocal.Type {
    return when (local) {
        LightMetaAccount.Type.SECRETS -> MetaAccountLocal.Type.SECRETS
        LightMetaAccount.Type.WATCH_ONLY -> MetaAccountLocal.Type.WATCH_ONLY
        LightMetaAccount.Type.PARITY_SIGNER -> MetaAccountLocal.Type.PARITY_SIGNER
        LightMetaAccount.Type.LEDGER_LEGACY -> MetaAccountLocal.Type.LEDGER
        LightMetaAccount.Type.LEDGER -> MetaAccountLocal.Type.LEDGER_GENERIC
        LightMetaAccount.Type.POLKADOT_VAULT -> MetaAccountLocal.Type.POLKADOT_VAULT
        LightMetaAccount.Type.PROXIED -> MetaAccountLocal.Type.PROXIED
    }
}

fun mapMetaAccountWithBalanceFromLocal(local: MetaAccountWithBalanceLocal): MetaAccountAssetBalance {
    return with(local) {
        MetaAccountAssetBalance(
            metaId = id,
            freeInPlanks = freeInPlanks,
            reservedInPlanks = reservedInPlanks,
            offChainBalance = offChainBalance,
            precision = precision.asPrecision(),
            rate = rate,
        )
    }
}

fun mapChainAccountFromLocal(chainAccountLocal: ChainAccountLocal): MetaAccount.ChainAccount {
    return with(chainAccountLocal) {
        MetaAccount.ChainAccount(
            metaId = metaId,
            publicKey = publicKey,
            chainId = chainId,
            accountId = accountId,
            cryptoType = cryptoType
        )
    }
}

fun mapProxyAccountFromLocal(proxyAccountLocal: ProxyAccountLocal): ProxyAccount {
    return with(proxyAccountLocal) {
        ProxyAccount(
            metaId = proxyMetaId,
            chainId = chainId,
            proxiedAccountId = proxiedAccountId,
            proxyType = ProxyType.fromString(proxyType)
        )
    }
}

fun mapAddAccountPayloadToAddAccountType(
    payload: AddAccountPayload,
    accountNameState: AccountNameChooserMixin.State,
): AddAccountType {
    return when (payload) {
        AddAccountPayload.MetaAccount -> {
            require(accountNameState is AccountNameChooserMixin.State.Input) { "Name input should be present for meta account" }

            AddAccountType.MetaAccount(accountNameState.value)
        }

        is AddAccountPayload.ChainAccount -> AddAccountType.ChainAccount(payload.chainId, payload.metaId)
    }
}

fun mapOptionalNameToNameChooserState(name: String?) = when (name) {
    null -> AccountNameChooserMixin.State.NoInput
    else -> AccountNameChooserMixin.State.Input(name)
}
