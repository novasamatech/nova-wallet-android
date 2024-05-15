package io.novafoundation.nova.feature_account_impl.presentation.manualBackup.secrets.common

import io.novafoundation.nova.common.data.secrets.v2.derivationPath
import io.novafoundation.nova.common.data.secrets.v2.privateKey
import io.novafoundation.nova.common.data.secrets.v2.seed
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.core.model.CryptoType
import io.novafoundation.nova.feature_account_api.data.mappers.mapChainToUi
import io.novafoundation.nova.feature_account_api.data.secrets.derivationPath
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_account_api.domain.model.cryptoTypeIn
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.data.mappers.mapCryptoTypeToCryptoTypeSubtitle
import io.novafoundation.nova.feature_account_impl.data.mappers.mapCryptoTypeToCryptoTypeTitle
import io.novafoundation.nova.feature_account_impl.domain.account.export.CommonExportSecretsInteractor
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
    private val chainRegistry: ChainRegistry,
    private val exportSecretsInteractor: CommonExportSecretsInteractor,
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
        val metaAccount = accountRepository.getMetaAccount(metaId)
        val chain = chainId?.let { chainRegistry.getChain(it) }
        val mnemonic = if (chain == null) {
            exportSecretsInteractor.getMetaAccountMnemonic(metaAccount)
        } else {
            exportSecretsInteractor.getChainAccountMnemonic(metaAccount, chain)
        }
        return mnemonic?.let { ManualBackupMnemonicRvItem(it.wordList, isShown = false) }
    }

    override suspend fun createAdvancedSecrets(metaId: Long, chainIdOrNull: String?): Collection<ManualBackupSecretsRvItem> = buildList {
        val metaAccount = accountRepository.getMetaAccount(metaId)

        if (chainIdOrNull != null) {
            val chain = chainRegistry.getChain(chainIdOrNull)
            addChainAdditionalSecrets(metaAccount, chain)
        } else {
            addDefaultAccountSecrets(metaAccount)
        }
    }

    private suspend fun MutableList<ManualBackupSecretsRvItem>.addDefaultAccountSecrets(metaAccount: MetaAccount) {
        if (exportSecretsInteractor.hasSubstrateSecrets(metaAccount)) {
            addSubstrateAdditionalSecrets(metaAccount)
        }

        if (exportSecretsInteractor.hasEthereumSecrets(metaAccount)) {
            addEthereumAdditionalSecrets(metaAccount)
        }
    }

    private suspend fun MutableList<ManualBackupSecretsRvItem>.addChainAdditionalSecrets(metaAccount: MetaAccount, chain: Chain) {
        val privateKey = if (chain.isEthereumBased) {
            exportSecretsInteractor.getChainAccountPrivateKey(metaAccount, chain)
        } else {
            exportSecretsInteractor.getChainAccountSeed(metaAccount, chain)
        }

        this += createAdditionalSecretsInternal(
            networkName = chain.name,
            isEthereumBased = chain.isEthereumBased,
            privateKey = privateKey,
            cryptoType = metaAccount.cryptoTypeIn(chain),
            derivationPath = exportSecretsInteractor.getDerivationPath(metaAccount, chain),
            showCryptoType = privateKey != null
        )
    }

    private suspend fun MutableList<ManualBackupSecretsRvItem>.addSubstrateAdditionalSecrets(metaAccount: MetaAccount) {
        val seed = exportSecretsInteractor.getMetaAccountSeed(metaAccount)

        this += createAdditionalSecretsInternal(
            networkName = resourceManager.getString(R.string.common_network_polkadot),
            isEthereumBased = false,
            privateKey = seed,
            cryptoType = metaAccount.substrateCryptoType,
            derivationPath = exportSecretsInteractor.getDerivationPath(metaAccount, ethereum = false),
            showCryptoType = seed != null
        )
    }

    private suspend fun MutableList<ManualBackupSecretsRvItem>.addEthereumAdditionalSecrets(metaAccount: MetaAccount) {
        val privateKey = exportSecretsInteractor.getMetaAccountEthereumPrivateKey(metaAccount)

        this += createAdditionalSecretsInternal(
            networkName = resourceManager.getString(R.string.common_network_ethereum),
            isEthereumBased = true,
            privateKey = privateKey,
            cryptoType = CryptoType.ECDSA,
            derivationPath = exportSecretsInteractor.getDerivationPath(metaAccount, ethereum = true),
            showCryptoType = privateKey != null
        )
    }

    private suspend fun createAdditionalSecretsInternal(
        networkName: String,
        isEthereumBased: Boolean,
        privateKey: String?,
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

        if (!isEthereumBased) {
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
