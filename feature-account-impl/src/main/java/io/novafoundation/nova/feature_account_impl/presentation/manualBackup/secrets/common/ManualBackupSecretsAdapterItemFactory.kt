package io.novafoundation.nova.feature_account_impl.presentation.manualBackup.secrets.common

import io.novafoundation.nova.common.data.secrets.v2.MetaAccountSecrets
import io.novafoundation.nova.common.data.secrets.v2.SecretStoreV2
import io.novafoundation.nova.common.data.secrets.v2.derivationPath
import io.novafoundation.nova.common.data.secrets.v2.ethereumDerivationPath
import io.novafoundation.nova.common.data.secrets.v2.ethereumKeypair
import io.novafoundation.nova.common.data.secrets.v2.substrateDerivationPath
import io.novafoundation.nova.common.data.secrets.v2.substrateKeypair
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.core.model.CryptoType
import io.novafoundation.nova.feature_account_api.data.mappers.mapChainToUi
import io.novafoundation.nova.feature_account_api.data.secrets.getAccountSecrets
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.cryptoTypeIn
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.data.mappers.mapCryptoTypeToCryptoTypeSubtitle
import io.novafoundation.nova.feature_account_impl.data.mappers.mapCryptoTypeToCryptoTypeTitle
import io.novafoundation.nova.feature_account_impl.domain.account.export.mnemonic.ExportMnemonicInteractor
import io.novafoundation.nova.feature_account_impl.domain.account.export.mnemonic.getMnemonicOrNull
import io.novafoundation.nova.feature_account_impl.domain.account.export.seed.ExportPrivateKeyInteractor
import io.novafoundation.nova.feature_account_impl.domain.account.export.seed.getAccountSeedOrNull
import io.novafoundation.nova.feature_account_impl.domain.account.export.seed.getEthereumPrivateKeyOrNull
import io.novafoundation.nova.feature_account_impl.presentation.manualBackup.secrets.common.adapter.viewHolders.ManualBackupChainRvItem
import io.novafoundation.nova.feature_account_impl.presentation.manualBackup.secrets.common.adapter.viewHolders.ManualBackupCryptoTypeRvItem
import io.novafoundation.nova.feature_account_impl.presentation.manualBackup.secrets.common.adapter.viewHolders.ManualBackupJsonRvItem
import io.novafoundation.nova.feature_account_impl.presentation.manualBackup.secrets.common.adapter.viewHolders.ManualBackupMnemonicRvItem
import io.novafoundation.nova.feature_account_impl.presentation.manualBackup.secrets.common.adapter.viewHolders.ManualBackupSeedRvItem
import io.novafoundation.nova.feature_account_impl.presentation.manualBackup.secrets.common.adapter.viewHolders.ManualBackupSubtitleRvItem
import io.novafoundation.nova.feature_account_impl.presentation.manualBackup.secrets.common.adapter.viewHolders.ManualBackupTitleRvItem
import io.novafoundation.nova.feature_account_impl.presentation.manualBackup.secrets.common.adapter.viewHolders.models.ManualBackupSecretsRvItem
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.scale.EncodableStruct

interface ManualBackupSecretsAdapterItemFactory {

    suspend fun createChainItem(chainId: String): ManualBackupSecretsRvItem

    suspend fun createTitle(text: String): ManualBackupSecretsRvItem

    suspend fun createSubtitle(text: String): ManualBackupSecretsRvItem

    suspend fun createMnemonic(metaId: Long, chainId: String?): ManualBackupSecretsRvItem?

    suspend fun createAdvancedSecrets(metaId: Long, chainIdOrNull: String?): Collection<ManualBackupSecretsRvItem>
}

class RealManualBackupSecretsAdapterItemFactory(
    private val resourceManager: ResourceManager,
    private val accountRepository: AccountRepository,
    private val secretsStoreV2: SecretStoreV2,
    private val chainRegistry: ChainRegistry,
    private val exportMnemonicInteractor: ExportMnemonicInteractor,
    private val exportPrivateKeyInteractor: ExportPrivateKeyInteractor
) : ManualBackupSecretsAdapterItemFactory {

    override suspend fun createChainItem(chainId: String): ManualBackupSecretsRvItem {
        val chain = chainRegistry.getChain(chainId)
        return ManualBackupChainRvItem(mapChainToUi(chain))
    }

    override suspend fun createTitle(text: String): ManualBackupSecretsRvItem {
        return ManualBackupTitleRvItem(text)
    }

    override suspend fun createSubtitle(text: String): ManualBackupSecretsRvItem {
        return ManualBackupSubtitleRvItem(text)
    }

    override suspend fun createMnemonic(metaId: Long, chainId: String?): ManualBackupSecretsRvItem? {
        val mnemonic = exportMnemonicInteractor.getMnemonicOrNull(metaId, chainId)
        return mnemonic?.let { ManualBackupMnemonicRvItem(it.wordList, isShown = false) }
    }

    override suspend fun createAdvancedSecrets(metaId: Long, chainIdOrNull: String?): Collection<ManualBackupSecretsRvItem> = buildList {
        val metaAccount = accountRepository.getMetaAccount(metaId)

        if (chainIdOrNull != null) {
            val chain = chainRegistry.getChain(chainIdOrNull)
            addChainAdditionalSecrets(metaAccount, chain)
        } else {
            val metaAccountSecrets = secretsStoreV2.getMetaAccountSecrets(metaId)

            // We are waiting that substrate metaAccount has secrets but anyway handle if it null
            if (metaAccountSecrets?.substrateKeypair != null) {
                addSubstrateAdditionalSecrets(metaAccount, metaAccountSecrets)
            }

            if (metaAccountSecrets?.ethereumKeypair != null) {
                addEthereumAdditionalSecrets(metaAccount, metaAccountSecrets)
            }
        }
    }

    private suspend fun MutableList<ManualBackupSecretsRvItem>.addChainAdditionalSecrets(metaAccount: MetaAccount, chain: Chain) {
        val privateKey = if (chain.isEthereumBased) {
            exportPrivateKeyInteractor.getEthereumPrivateKey(metaAccount.id, chain.id)
        } else {
            exportPrivateKeyInteractor.getAccountSeedOrNull(metaAccount.id, chain.id)
        }

        this += createAdditionalSecretsInternal(
            networkName = chain.name,
            isEthereumBased = chain.isEthereumBased,
            privateKey = privateKey,
            jsonExportSupported = !chain.isEthereumBased,
            cryptoType = metaAccount.cryptoTypeIn(chain),
            derivationPath = secretsStoreV2.getAccountSecrets(metaAccount, chain).rightOrNull()?.derivationPath,
            showCryptoType = privateKey != null
        )
    }

    private suspend fun MutableList<ManualBackupSecretsRvItem>.addSubstrateAdditionalSecrets(
        metaAccount: MetaAccount,
        metaAccountSecrets: EncodableStruct<MetaAccountSecrets>
    ) {
        val privateKey = exportPrivateKeyInteractor.getAccountSeedOrNull(metaAccount.id)

        this += createAdditionalSecretsInternal(
            networkName = resourceManager.getString(R.string.common_network_polkadot),
            isEthereumBased = false,
            privateKey = privateKey,
            jsonExportSupported = true,
            cryptoType = metaAccount.substrateCryptoType,
            derivationPath = metaAccountSecrets.substrateDerivationPath,
            showCryptoType = privateKey != null
        )
    }

    private suspend fun MutableList<ManualBackupSecretsRvItem>.addEthereumAdditionalSecrets(
        metaAccount: MetaAccount,
        metaAccountSecrets: EncodableStruct<MetaAccountSecrets>
    ) {
        val seed = exportPrivateKeyInteractor.getEthereumPrivateKeyOrNull(metaAccount.id)

        this += createAdditionalSecretsInternal(
            networkName = resourceManager.getString(R.string.common_network_ethereum),
            isEthereumBased = true,
            privateKey = seed,
            jsonExportSupported = true,
            cryptoType = CryptoType.ECDSA,
            derivationPath = metaAccountSecrets.ethereumDerivationPath,
            showCryptoType = seed != null
        )
    }


    private suspend fun createAdditionalSecretsInternal(
        networkName: String,
        isEthereumBased: Boolean,
        privateKey: String?,
        jsonExportSupported: Boolean,
        cryptoType: CryptoType?, // We are waiting cryptoType is not null in this case but handle it since metaAccount.substrateCryptoType can be null
        derivationPath: String?,
        showCryptoType: Boolean
    ) = buildList {
        add(createTitle(networkName))

        if (privateKey != null) {
            val label = if (isEthereumBased) {
                resourceManager.getString(R.string.account_private_key)
            } else {
                resourceManager.getString(R.string.recovery_raw_seed)
            }
            add(ManualBackupSeedRvItem(label = label, seed = privateKey, isShown = false))
        }

        if (jsonExportSupported) {
            add(ManualBackupJsonRvItem())
        }

        if (cryptoType != null && showCryptoType) {
            val cryptoTypeItem = ManualBackupCryptoTypeRvItem(
                network = networkName,
                cryptoTypeTitle = mapCryptoTypeToCryptoTypeTitle(resourceManager, cryptoType),
                cryptoTypeSubtitle = mapCryptoTypeToCryptoTypeSubtitle(resourceManager, cryptoType),
                derivationPath = derivationPath,
                hideDerivationPath = derivationPath.isNullOrEmpty()
            )

            add(cryptoTypeItem)
        }
    }
}
