package io.novafoundation.nova.feature_account_impl.di

import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.data.mappers.mapEncryptionToCryptoType
import io.novafoundation.nova.common.data.network.AppLinksProvider
import io.novafoundation.nova.common.data.network.rpc.SocketSingleRequestExecutor
import io.novafoundation.nova.common.data.secrets.v1.SecretStoreV1
import io.novafoundation.nova.common.data.secrets.v2.SecretStoreV2
import io.novafoundation.nova.common.data.storage.Preferences
import io.novafoundation.nova.common.data.storage.encrypt.EncryptedPreferences
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.resources.ClipboardManager
import io.novafoundation.nova.common.resources.ContextManager
import io.novafoundation.nova.common.resources.LanguagesHolder
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.sequrity.biometry.BiometricServiceFactory
import io.novafoundation.nova.common.utils.DEFAULT_DERIVATION_PATH
import io.novafoundation.nova.common.utils.systemCall.SystemCallExecutor
import io.novafoundation.nova.core.model.CryptoType
import io.novafoundation.nova.core_db.dao.AccountDao
import io.novafoundation.nova.core_db.dao.MetaAccountDao
import io.novafoundation.nova.core_db.dao.NodeDao
import io.novafoundation.nova.runtime.ethereum.gas.GasPriceProviderFactory
import io.novafoundation.nova.feature_account_api.data.ethereum.transaction.EvmTransactionService
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.data.proxy.MetaAccountsUpdatesRegistry
import io.novafoundation.nova.feature_account_api.data.proxy.ProxySyncService
import io.novafoundation.nova.feature_account_api.data.repository.OnChainIdentityRepository
import io.novafoundation.nova.feature_account_api.data.repository.ProxyRepository
import io.novafoundation.nova.feature_account_api.data.repository.addAccount.ledger.LedgerAddAccountRepository
import io.novafoundation.nova.feature_account_api.data.signer.SignerProvider
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountInteractor
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.interfaces.MetaAccountGroupingInteractor
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.domain.updaters.AccountUpdateScope
import io.novafoundation.nova.feature_account_api.presenatation.account.AddressDisplayUseCase
import io.novafoundation.nova.feature_account_api.presenatation.account.polkadotVault.config.PolkadotVaultVariantConfigProvider
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActionsProvider
import io.novafoundation.nova.feature_account_api.presenatation.language.LanguageUseCase
import io.novafoundation.nova.feature_account_api.presenatation.mixin.addressInput.AddressInputMixinFactory
import io.novafoundation.nova.feature_account_api.presenatation.mixin.identity.IdentityMixin
import io.novafoundation.nova.feature_account_api.presenatation.mixin.importType.ImportTypeChooserMixin
import io.novafoundation.nova.feature_account_api.presenatation.mixin.importType.ImportTypeChooserProvider
import io.novafoundation.nova.feature_account_api.presenatation.mixin.selectWallet.SelectWalletCommunicator
import io.novafoundation.nova.feature_account_api.presenatation.mixin.selectWallet.SelectWalletMixin
import io.novafoundation.nova.feature_account_impl.RealBiometricServiceFactory
import io.novafoundation.nova.feature_account_impl.data.ethereum.transaction.RealEvmTransactionService
import io.novafoundation.nova.feature_account_impl.data.extrinsic.RealExtrinsicService
import io.novafoundation.nova.feature_account_impl.data.network.blockchain.AccountSubstrateSource
import io.novafoundation.nova.feature_account_impl.data.network.blockchain.AccountSubstrateSourceImpl
import io.novafoundation.nova.feature_account_impl.data.proxy.RealProxySyncService
import io.novafoundation.nova.feature_account_impl.data.repository.AccountRepositoryImpl
import io.novafoundation.nova.feature_account_impl.data.repository.RealOnChainIdentityRepository
import io.novafoundation.nova.feature_account_impl.data.repository.RealProxyRepository
import io.novafoundation.nova.feature_account_impl.data.repository.datasource.AccountDataSource
import io.novafoundation.nova.feature_account_impl.data.repository.datasource.AccountDataSourceImpl
import io.novafoundation.nova.feature_account_impl.data.repository.datasource.migration.AccountDataMigration
import io.novafoundation.nova.feature_account_impl.data.secrets.AccountSecretsFactory
import io.novafoundation.nova.feature_account_impl.di.modules.AdvancedEncryptionStoreModule
import io.novafoundation.nova.feature_account_impl.di.modules.IdentityProviderModule
import io.novafoundation.nova.feature_account_impl.di.modules.ParitySignerModule
import io.novafoundation.nova.feature_account_impl.di.modules.SignersModule
import io.novafoundation.nova.feature_account_impl.di.modules.WatchOnlyModule
import io.novafoundation.nova.feature_account_impl.domain.AccountInteractorImpl
import io.novafoundation.nova.feature_account_impl.domain.MetaAccountGroupingInteractorImpl
import io.novafoundation.nova.feature_account_impl.domain.NodeHostValidator
import io.novafoundation.nova.feature_account_impl.domain.account.add.AddAccountInteractor
import io.novafoundation.nova.feature_account_impl.domain.account.advancedEncryption.AdvancedEncryptionInteractor
import io.novafoundation.nova.feature_account_api.domain.account.common.EncryptionDefaults
import io.novafoundation.nova.feature_account_api.domain.account.identity.IdentityProvider
import io.novafoundation.nova.feature_account_api.domain.account.identity.OnChainIdentity
import io.novafoundation.nova.feature_account_impl.data.proxy.RealMetaAccountsUpdatesRegistry
import io.novafoundation.nova.feature_account_impl.data.repository.addAccount.ledger.RealLedgerAddAccountRepository
import io.novafoundation.nova.feature_account_impl.data.repository.addAccount.paritySigner.ParitySignerAddAccountRepository
import io.novafoundation.nova.feature_account_impl.data.repository.addAccount.proxied.ProxiedAddAccountRepository
import io.novafoundation.nova.feature_account_impl.data.repository.addAccount.secrets.JsonAddAccountRepository
import io.novafoundation.nova.feature_account_impl.data.repository.addAccount.secrets.MnemonicAddAccountRepository
import io.novafoundation.nova.feature_account_impl.data.repository.addAccount.secrets.SeedAddAccountRepository
import io.novafoundation.nova.feature_account_impl.data.repository.addAccount.watchOnly.WatchOnlyAddAccountRepository
import io.novafoundation.nova.feature_account_impl.di.modules.ProxySigningModule
import io.novafoundation.nova.feature_account_impl.domain.account.details.WalletDetailsInteractor
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.feature_account_impl.presentation.account.common.listing.DelegatedMetaAccountUpdatesListingMixinFactory
import io.novafoundation.nova.feature_account_impl.presentation.account.common.listing.MetaAccountTypePresentationMapper
import io.novafoundation.nova.feature_account_impl.presentation.account.common.listing.MetaAccountWithBalanceListingMixinFactory
import io.novafoundation.nova.feature_account_impl.presentation.account.common.listing.ProxyFormatter
import io.novafoundation.nova.feature_account_impl.presentation.account.wallet.WalletUiUseCaseImpl
import io.novafoundation.nova.feature_account_impl.presentation.common.mixin.addAccountChooser.AddAccountLauncherMixin
import io.novafoundation.nova.feature_account_impl.presentation.common.mixin.addAccountChooser.AddAccountLauncherProvider
import io.novafoundation.nova.feature_account_impl.presentation.common.sign.notSupported.RealSigningNotSupportedPresentable
import io.novafoundation.nova.feature_account_impl.presentation.common.sign.notSupported.SigningNotSupportedPresentable
import io.novafoundation.nova.feature_account_impl.presentation.language.RealLanguageUseCase
import io.novafoundation.nova.feature_account_impl.presentation.mixin.identity.RealIdentityMixinFactory
import io.novafoundation.nova.feature_account_impl.presentation.mixin.selectWallet.RealRealSelectWalletMixinFactory
import io.novafoundation.nova.feature_account_impl.presentation.paritySigner.config.RealPolkadotVaultVariantConfigProvider
import io.novafoundation.nova.feature_currency_api.domain.interfaces.CurrencyRepository
import io.novafoundation.nova.feature_ledger_api.data.repository.LedgerRepository
import io.novafoundation.nova.runtime.di.REMOTE_STORAGE_SOURCE
import io.novafoundation.nova.runtime.extrinsic.ExtrinsicBuilderFactory
import io.novafoundation.nova.runtime.extrinsic.multi.ExtrinsicSplitter
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.qr.MultiChainQrSharingFactory
import io.novafoundation.nova.runtime.network.rpc.RpcCalls
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import io.novafoundation.nova.web3names.domain.networking.Web3NamesInteractor
import jp.co.soramitsu.fearless_utils.encrypt.json.JsonSeedDecoder
import jp.co.soramitsu.fearless_utils.encrypt.json.JsonSeedEncoder
import javax.inject.Named
import jp.co.soramitsu.fearless_utils.encrypt.MultiChainEncryption
import jp.co.soramitsu.fearless_utils.encrypt.junction.BIP32JunctionDecoder

@Module()
class AddAccountsModule {

    @Provides
    @FeatureScope
    fun provideMnemonicAddAccountRepository(
        accountDataSource: AccountDataSource,
        accountSecretsFactory: AccountSecretsFactory,
        chainRegistry: ChainRegistry,
        proxySyncService: ProxySyncService
    ) = MnemonicAddAccountRepository(
        accountDataSource,
        accountSecretsFactory,
        chainRegistry,
        proxySyncService
    )

    @Provides
    @FeatureScope
    fun provideJsonAddAccountRepository(
        accountDataSource: AccountDataSource,
        accountSecretsFactory: AccountSecretsFactory,
        jsonSeedDecoder: JsonSeedDecoder,
        chainRegistry: ChainRegistry,
        proxySyncService: ProxySyncService,
    ) = JsonAddAccountRepository(
        accountDataSource,
        accountSecretsFactory,
        jsonSeedDecoder,
        chainRegistry,
        proxySyncService
    )

    @Provides
    @FeatureScope
    fun provideSeedAddAccountRepository(
        accountDataSource: AccountDataSource,
        accountSecretsFactory: AccountSecretsFactory,
        chainRegistry: ChainRegistry,
        proxySyncService: ProxySyncService,
    ) = SeedAddAccountRepository(
        accountDataSource,
        accountSecretsFactory,
        chainRegistry,
        proxySyncService
    )

    @Provides
    @FeatureScope
    fun provideWatchOnlyAddAccountRepository(
        accountDao: MetaAccountDao,
        proxySyncService: ProxySyncService,
    ) = WatchOnlyAddAccountRepository(
        accountDao,
        proxySyncService
    )

    @Provides
    @FeatureScope
    fun provideParitySignerAddAccountRepository(
        accountDao: MetaAccountDao,
        chainRegistry: ChainRegistry,
        proxySyncService: ProxySyncService
    ) = ParitySignerAddAccountRepository(
        accountDao,
        chainRegistry,
        proxySyncService
    )

    @Provides
    @FeatureScope
    fun provideProxiedAddAccountRepository(
        accountDao: MetaAccountDao,
        chainRegistry: ChainRegistry
    ) = ProxiedAddAccountRepository(
        accountDao,
        chainRegistry
    )

    @Provides
    @FeatureScope
    fun provideLedgerAddAccountRepository(
        accountDao: MetaAccountDao,
        chainRegistry: ChainRegistry,
        secretStoreV2: SecretStoreV2,
        proxySyncService: ProxySyncService,
    ): LedgerAddAccountRepository = RealLedgerAddAccountRepository(
        accountDao,
        chainRegistry,
        secretStoreV2,
        proxySyncService
    )

}
