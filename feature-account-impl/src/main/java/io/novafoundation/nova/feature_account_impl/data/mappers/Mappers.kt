package io.novafoundation.nova.feature_account_impl.data.mappers

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.filterNotNull
import io.novafoundation.nova.core.model.CryptoType
import io.novafoundation.nova.core.model.Node
import io.novafoundation.nova.core.model.Node.NetworkType
import io.novafoundation.nova.core_db.model.NodeLocal
import io.novafoundation.nova.core_db.model.chain.ChainAccountLocal
import io.novafoundation.nova.core_db.model.chain.JoinedMetaAccountInfo
import io.novafoundation.nova.core_db.model.chain.MetaAccountLocal
import io.novafoundation.nova.feature_account_api.data.mappers.stubNetwork
import io.novafoundation.nova.feature_account_api.domain.model.Account
import io.novafoundation.nova.feature_account_api.domain.model.AddAccountType
import io.novafoundation.nova.feature_account_api.domain.model.LightMetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.addressIn
import io.novafoundation.nova.feature_account_api.presenatation.account.add.AddAccountPayload
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.presentation.common.mixin.api.AccountNameChooserMixin
import io.novafoundation.nova.feature_account_impl.presentation.node.model.NodeModel
import io.novafoundation.nova.feature_account_impl.presentation.view.advanced.encryption.model.CryptoTypeModel
import io.novafoundation.nova.feature_account_impl.presentation.view.advanced.network.model.NetworkModel
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.runtime.ext.hexAccountIdOf
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import jp.co.soramitsu.fearless_utils.extensions.toHexString

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

    val name = when (encryptionType) {
        CryptoType.SR25519 -> "${resourceManager.getString(R.string.sr25519_selection_title)} ${resourceManager.getString(
            R.string.sr25519_selection_subtitle
        )}"
        CryptoType.ED25519 -> "${resourceManager.getString(R.string.ed25519_selection_title)} ${resourceManager.getString(
            R.string.ed25519_selection_subtitle
        )}"
        CryptoType.ECDSA -> "${resourceManager.getString(R.string.ecdsa_selection_title)} ${resourceManager.getString(
            R.string.ecdsa_selection_subtitle
        )}"
    }

    return CryptoTypeModel(name, encryptionType)
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

private fun mapMetaAccountTypeFromLocal(local: MetaAccountLocal.Type): LightMetaAccount.Type {
    return when(local) {
        MetaAccountLocal.Type.SECRETS -> LightMetaAccount.Type.SECRETS
    }
}

fun mapMetaAccountLocalToLightMetaAccount(
    metaAccountLocal: MetaAccountLocal
): LightMetaAccount = with(metaAccountLocal) {
    LightMetaAccount(
        id = id,
        substratePublicKey = substratePublicKey,
        substrateCryptoType = substrateCryptoType,
        substrateAccountId = substrateAccountId,
        ethereumAddress = ethereumAddress,
        ethereumPublicKey = ethereumPublicKey,
        isSelected = isSelected,
        name = name,
        type = mapMetaAccountTypeFromLocal(type)
    )
}

fun mapMetaAccountLocalToMetaAccount(
    chainsById: Map<ChainId, Chain>,
    joinedMetaAccountInfo: JoinedMetaAccountInfo
): MetaAccount {
    val chainAccounts = joinedMetaAccountInfo.chainAccounts.associateBy(
        keySelector = ChainAccountLocal::chainId,
        valueTransform = {
            // ignore chainAccounts with unknown chainId
            val chain = chainsById[it.chainId] ?: return@associateBy null

            MetaAccount.ChainAccount(
                metaId = joinedMetaAccountInfo.metaAccount.id,
                chain = chain,
                publicKey = it.publicKey,
                accountId = it.accountId,
                cryptoType = it.cryptoType
            )
        }
    ).filterNotNull()

    return with(joinedMetaAccountInfo.metaAccount) {
        MetaAccount(
            id = id,
            chainAccounts = chainAccounts,
            substratePublicKey = substratePublicKey,
            substrateCryptoType = substrateCryptoType,
            substrateAccountId = substrateAccountId,
            ethereumAddress = ethereumAddress,
            ethereumPublicKey = ethereumPublicKey,
            isSelected = isSelected,
            name = name,
            type = mapMetaAccountTypeFromLocal(type)
        )
    }
}

fun mapMetaAccountToAccount(chain: Chain, metaAccount: MetaAccount): Account? {
    return metaAccount.addressIn(chain)?.let { address ->

        val accountId = chain.hexAccountIdOf(address)

        Account(
            address = address,
            name = metaAccount.name,
            accountIdHex = accountId,
            cryptoType = metaAccount.substrateCryptoType,
            position = 0,
            network = stubNetwork(chain.id),
        )
    }
}

fun mapChainAccountToAccount(
    parent: MetaAccount,
    chainAccount: MetaAccount.ChainAccount,
): Account {
    val chain = chainAccount.chain

    return Account(
        address = chain.addressOf(chainAccount.accountId),
        name = parent.name,
        accountIdHex = chainAccount.accountId.toHexString(),
        cryptoType = chainAccount.cryptoType,
        position = 0,
        network = stubNetwork(chain.id),
    )
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

fun mapNameChooserStateToOptionalName(state: AccountNameChooserMixin.State) = (state as? AccountNameChooserMixin.State.Input)?.value

fun mapOptionalNameToNameChooserState(name: String?) = when (name) {
    null -> AccountNameChooserMixin.State.NoInput
    else -> AccountNameChooserMixin.State.Input(name)
}
