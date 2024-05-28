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
import io.novafoundation.nova.common.utils.coroutines.RootScope
import io.novafoundation.nova.common.utils.systemCall.SystemCallExecutor
import io.novafoundation.nova.core.model.CryptoType
import io.novafoundation.nova.core_db.dao.AccountDao
import io.novafoundation.nova.core_db.dao.MetaAccountDao
import io.novafoundation.nova.core_db.dao.NodeDao
import io.novafoundation.nova.feature_account_api.data.ethereum.transaction.EvmTransactionService
import io.novafoundation.nova.feature_account_api.data.events.MetaAccountChangesEventBus
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.data.proxy.MetaAccountsUpdatesRegistry
import io.novafoundation.nova.feature_account_api.data.proxy.ProxySyncService
import io.novafoundation.nova.feature_account_api.data.repository.OnChainIdentityRepository
import io.novafoundation.nova.feature_account_api.data.repository.addAccount.proxied.ProxiedAddAccountRepository
import io.novafoundation.nova.feature_account_api.data.signer.SignerProvider
import io.novafoundation.nova.feature_account_api.domain.account.common.EncryptionDefaults
import io.novafoundation.nova.feature_account_api.domain.account.identity.IdentityProvider
import io.novafoundation.nova.feature_account_api.domain.account.identity.OnChainIdentity
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
import io.novafoundation.nova.feature_account_api.presenatation.mixin.selectAddress.SelectAddressCommunicator
import io.novafoundation.nova.feature_account_api.presenatation.mixin.selectAddress.SelectAddressMixin
import io.novafoundation.nova.feature_account_api.presenatation.mixin.selectWallet.SelectWalletCommunicator
import io.novafoundation.nova.feature_account_api.presenatation.mixin.selectWallet.SelectWalletMixin
import io.novafoundation.nova.feature_account_impl.BuildConfig
import io.novafoundation.nova.feature_account_impl.RealBiometricServiceFactory
import io.novafoundation.nova.feature_account_impl.data.cloudBackup.CloudBackupAccountsModificationsTracker
import io.novafoundation.nova.feature_account_api.data.cloudBackup.LocalAccountsCloudBackupFacade
import io.novafoundation.nova.feature_account_api.domain.cloudBackup.ApplyLocalSnapshotToCloudBackupUseCase
import io.novafoundation.nova.feature_account_impl.data.ethereum.transaction.RealEvmTransactionService
import io.novafoundation.nova.feature_account_impl.data.events.RealMetaAccountChangesEventBus
import io.novafoundation.nova.feature_account_impl.data.extrinsic.RealExtrinsicService
import io.novafoundation.nova.feature_account_impl.data.network.blockchain.AccountSubstrateSource
import io.novafoundation.nova.feature_account_impl.data.network.blockchain.AccountSubstrateSourceImpl
import io.novafoundation.nova.feature_account_impl.data.proxy.RealMetaAccountsUpdatesRegistry
import io.novafoundation.nova.feature_account_impl.data.proxy.RealProxySyncService
import io.novafoundation.nova.feature_account_impl.data.repository.AccountRepositoryImpl
import io.novafoundation.nova.feature_account_impl.data.repository.RealOnChainIdentityRepository
import io.novafoundation.nova.feature_account_impl.data.repository.addAccount.secrets.JsonAddAccountRepository
import io.novafoundation.nova.feature_account_impl.data.repository.addAccount.secrets.MnemonicAddAccountRepository
import io.novafoundation.nova.feature_account_impl.data.repository.addAccount.secrets.SeedAddAccountRepository
import io.novafoundation.nova.feature_account_impl.data.repository.datasource.AccountDataSource
import io.novafoundation.nova.feature_account_impl.data.repository.datasource.AccountDataSourceImpl
import io.novafoundation.nova.feature_account_impl.data.repository.datasource.RealSecretsMetaAccountLocalFactory
import io.novafoundation.nova.feature_account_impl.data.repository.datasource.SecretsMetaAccountLocalFactory
import io.novafoundation.nova.feature_account_impl.data.repository.datasource.migration.AccountDataMigration
import io.novafoundation.nova.feature_account_impl.data.secrets.AccountSecretsFactory
import io.novafoundation.nova.feature_account_impl.di.modules.AdvancedEncryptionStoreModule
import io.novafoundation.nova.feature_account_impl.di.modules.CloudBackupModule
import io.novafoundation.nova.feature_account_impl.di.modules.IdentityProviderModule
import io.novafoundation.nova.feature_account_impl.di.modules.ParitySignerModule
import io.novafoundation.nova.feature_account_impl.di.modules.ProxySigningModule
import io.novafoundation.nova.feature_account_impl.di.modules.WatchOnlyModule
import io.novafoundation.nova.feature_account_impl.di.modules.signers.SignersModule
import io.novafoundation.nova.feature_account_impl.domain.AccountInteractorImpl
import io.novafoundation.nova.feature_account_impl.domain.MetaAccountGroupingInteractorImpl
import io.novafoundation.nova.feature_account_impl.domain.NodeHostValidator
import io.novafoundation.nova.feature_account_impl.domain.account.add.AddAccountInteractor
import io.novafoundation.nova.feature_account_impl.domain.account.advancedEncryption.AdvancedEncryptionInteractor
import io.novafoundation.nova.feature_account_impl.domain.account.details.WalletDetailsInteractor
import io.novafoundation.nova.feature_account_impl.domain.cloudBackup.createPassword.CreateCloudBackupPasswordInteractor
import io.novafoundation.nova.feature_account_impl.domain.cloudBackup.createPassword.RealCreateCloudBackupPasswordInteractor
import io.novafoundation.nova.feature_account_impl.domain.cloudBackup.enterPassword.RealEnterCloudBackupInteractor
import io.novafoundation.nova.feature_account_impl.domain.cloudBackup.enterPassword.EnterCloudBackupInteractor
import io.novafoundation.nova.feature_account_impl.domain.startCreateWallet.RealStartCreateWalletInteractor
import io.novafoundation.nova.feature_account_impl.domain.startCreateWallet.StartCreateWalletInteractor
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.feature_account_impl.presentation.account.common.listing.DelegatedMetaAccountUpdatesListingMixinFactory
import io.novafoundation.nova.feature_account_api.presenatation.account.common.listing.MetaAccountTypePresentationMapper
import io.novafoundation.nova.feature_account_impl.domain.account.cloudBackup.RealApplyLocalSnapshotToCloudBackupUseCase
import io.novafoundation.nova.feature_account_impl.domain.account.export.CommonExportSecretsInteractor
import io.novafoundation.nova.feature_account_impl.domain.account.export.RealCommonExportSecretsInteractor
import io.novafoundation.nova.feature_account_impl.domain.manualBackup.ManualBackupSelectAccountInteractor
import io.novafoundation.nova.feature_account_impl.domain.manualBackup.ManualBackupSelectWalletInteractor
import io.novafoundation.nova.feature_account_impl.domain.manualBackup.RealManualBackupSelectAccountInteractor
import io.novafoundation.nova.feature_account_impl.domain.manualBackup.RealManualBackupSelectWalletInteractor
import io.novafoundation.nova.feature_account_impl.presentation.account.common.listing.MetaAccountWithBalanceListingMixinFactory
import io.novafoundation.nova.feature_account_impl.presentation.account.common.listing.ProxyFormatter
import io.novafoundation.nova.feature_account_impl.presentation.account.common.listing.RealMetaAccountTypePresentationMapper
import io.novafoundation.nova.feature_account_impl.presentation.account.mixin.SelectAddressMixinFactory
import io.novafoundation.nova.feature_account_impl.presentation.account.wallet.WalletUiUseCaseImpl
import io.novafoundation.nova.feature_account_impl.presentation.common.mixin.addAccountChooser.AddAccountLauncherPresentationFactory
import io.novafoundation.nova.feature_account_impl.presentation.common.mixin.addAccountChooser.RealAddAccountLauncherPresentationFactory
import io.novafoundation.nova.feature_account_impl.presentation.common.sign.notSupported.RealSigningNotSupportedPresentable
import io.novafoundation.nova.feature_account_impl.presentation.common.sign.notSupported.SigningNotSupportedPresentable
import io.novafoundation.nova.feature_account_impl.presentation.language.RealLanguageUseCase
import io.novafoundation.nova.feature_account_impl.presentation.manualBackup.secrets.common.ManualBackupSecretsAdapterItemFactory
import io.novafoundation.nova.feature_account_impl.presentation.manualBackup.secrets.common.RealManualBackupSecretsAdapterItemFactory
import io.novafoundation.nova.feature_account_impl.presentation.mixin.identity.RealIdentityMixinFactory
import io.novafoundation.nova.feature_account_impl.presentation.mixin.selectWallet.RealRealSelectWalletMixinFactory
import io.novafoundation.nova.feature_account_impl.presentation.paritySigner.config.RealPolkadotVaultVariantConfigProvider
import io.novafoundation.nova.feature_cloud_backup_api.domain.CloudBackupService
import io.novafoundation.nova.feature_cloud_backup_api.presenter.mixin.CloudBackupChangingWarningMixinFactory
import io.novafoundation.nova.feature_currency_api.domain.interfaces.CurrencyRepository
import io.novafoundation.nova.feature_proxy_api.data.repository.GetProxyRepository
import io.novafoundation.nova.runtime.di.REMOTE_STORAGE_SOURCE
import io.novafoundation.nova.runtime.ethereum.gas.GasPriceProviderFactory
import io.novafoundation.nova.runtime.extrinsic.ExtrinsicBuilderFactory
import io.novafoundation.nova.runtime.extrinsic.multi.ExtrinsicSplitter
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.qr.MultiChainQrSharingFactory
import io.novafoundation.nova.runtime.network.rpc.RpcCalls
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import io.novafoundation.nova.web3names.domain.networking.Web3NamesInteractor
import io.novasama.substrate_sdk_android.encrypt.MultiChainEncryption
import io.novasama.substrate_sdk_android.encrypt.json.JsonSeedDecoder
import io.novasama.substrate_sdk_android.encrypt.json.JsonSeedEncoder
import io.novasama.substrate_sdk_android.encrypt.junction.BIP32JunctionDecoder
import javax.inject.Named

@Module(
    includes = [
        SignersModule::class,
        WatchOnlyModule::class,
        ProxySigningModule::class,
        ParitySignerModule::class,
        IdentityProviderModule::class,
        AdvancedEncryptionStoreModule::class,
        AddAccountsModule::class,
        CloudBackupModule::class
    ]
)
class AccountFeatureModule {

    @Provides
    @FeatureScope
    fun provideMetaAccountsUpdatesRegistry(
        preferences: Preferences
    ): MetaAccountsUpdatesRegistry = RealMetaAccountsUpdatesRegistry(preferences)

    @Provides
    @FeatureScope
    fun provideEncryptionDefaults(): EncryptionDefaults = EncryptionDefaults(
        substrateCryptoType = CryptoType.SR25519,
        substrateDerivationPath = "",
        ethereumCryptoType = mapEncryptionToCryptoType(MultiChainEncryption.Ethereum.encryptionType),
        ethereumDerivationPath = BIP32JunctionDecoder.DEFAULT_DERIVATION_PATH
    )

    @Provides
    @FeatureScope
    fun provideExtrinsicService(
        accountRepository: AccountRepository,
        rpcCalls: RpcCalls,
        extrinsicBuilderFactory: ExtrinsicBuilderFactory,
        chainRegistry: ChainRegistry,
        signerProvider: SignerProvider,
        extrinsicSplitter: ExtrinsicSplitter
    ): ExtrinsicService = RealExtrinsicService(
        rpcCalls,
        chainRegistry,
        accountRepository,
        extrinsicBuilderFactory,
        signerProvider,
        extrinsicSplitter,
    )

    @Provides
    @FeatureScope
    fun provideJsonDecoder(jsonMapper: Gson) = JsonSeedDecoder(jsonMapper)

    @Provides
    @FeatureScope
    fun provideJsonEncoder(
        jsonMapper: Gson,
    ) = JsonSeedEncoder(jsonMapper)

    @Provides
    @FeatureScope
    fun provideMetaAccountChangesRequestBus(
        proxySyncService: dagger.Lazy<ProxySyncService>,
        cloudBackupAccountsModificationsTracker: CloudBackupAccountsModificationsTracker,
    ): MetaAccountChangesEventBus = RealMetaAccountChangesEventBus(
        proxySyncService = proxySyncService,
        cloudBackupAccountsModificationsTracker = cloudBackupAccountsModificationsTracker
    )

    @Provides
    @FeatureScope
    fun provideAccountRepository(
        accountDataSource: AccountDataSource,
        accountDao: AccountDao,
        nodeDao: NodeDao,
        jsonSeedEncoder: JsonSeedEncoder,
        accountSubstrateSource: AccountSubstrateSource,
        languagesHolder: LanguagesHolder,
        secretStoreV2: SecretStoreV2,
        metaAccountChangesEventBus: MetaAccountChangesEventBus,
    ): AccountRepository {
        return AccountRepositoryImpl(
            accountDataSource,
            accountDao,
            nodeDao,
            jsonSeedEncoder,
            languagesHolder,
            accountSubstrateSource,
            secretStoreV2,
            metaAccountChangesEventBus
        )
    }

    @Provides
    @FeatureScope
    fun provideAccountInteractor(
        chainRegistry: ChainRegistry,
        accountRepository: AccountRepository,
    ): AccountInteractor {
        return AccountInteractorImpl(chainRegistry, accountRepository)
    }

    @Provides
    @FeatureScope
    fun provideCreateCloudBackupPasswordInteractor(
        cloudBackupService: CloudBackupService,
        accountRepository: AccountRepository,
        encryptionDefaults: EncryptionDefaults,
        accountSecretsFactory: AccountSecretsFactory,
        secretsMetaAccountLocalFactory: SecretsMetaAccountLocalFactory,
        metaAccountDao: MetaAccountDao,
        cloudBackupFacade: LocalAccountsCloudBackupFacade
    ): CreateCloudBackupPasswordInteractor {
        return RealCreateCloudBackupPasswordInteractor(
            cloudBackupService,
            accountRepository,
            encryptionDefaults,
            accountSecretsFactory,
            secretsMetaAccountLocalFactory,
            metaAccountDao,
            cloudBackupFacade
        )
    }

    @Provides
    @FeatureScope
    fun provideRestoreCloudBackupInteractor(
        cloudBackupService: CloudBackupService,
        cloudBackupFacade: LocalAccountsCloudBackupFacade,
        accountRepository: AccountRepository
    ): EnterCloudBackupInteractor {
        return RealEnterCloudBackupInteractor(
            cloudBackupService = cloudBackupService,
            cloudBackupFacade = cloudBackupFacade,
            accountRepository = accountRepository
        )
    }

    @Provides
    @FeatureScope
    fun provideSecretsMetaAccountLocalFactory(): SecretsMetaAccountLocalFactory {
        return RealSecretsMetaAccountLocalFactory()
    }

    @Provides
    @FeatureScope
    fun provideAccountDataSource(
        preferences: Preferences,
        encryptedPreferences: EncryptedPreferences,
        nodeDao: NodeDao,
        secretStoreV1: SecretStoreV1,
        accountDataMigration: AccountDataMigration,
        metaAccountDao: MetaAccountDao,
        chainRegistry: ChainRegistry,
        secretsMetaAccountLocalFactory: SecretsMetaAccountLocalFactory,
        secretStoreV2: SecretStoreV2,
    ): AccountDataSource {
        return AccountDataSourceImpl(
            preferences,
            encryptedPreferences,
            nodeDao,
            metaAccountDao,
            chainRegistry,
            secretStoreV2,
            secretsMetaAccountLocalFactory,
            secretStoreV1,
            accountDataMigration
        )
    }

    @Provides
    fun provideNodeHostValidator() = NodeHostValidator()

    @Provides
    @FeatureScope
    fun provideAccountSubstrateSource(socketRequestExecutor: SocketSingleRequestExecutor): AccountSubstrateSource {
        return AccountSubstrateSourceImpl(socketRequestExecutor)
    }

    @Provides
    @FeatureScope
    fun provideAccountDataMigration(
        preferences: Preferences,
        encryptedPreferences: EncryptedPreferences,
        accountDao: AccountDao,
    ): AccountDataMigration {
        return AccountDataMigration(preferences, encryptedPreferences, accountDao)
    }

    @Provides
    @FeatureScope
    fun provideExternalAccountActions(
        clipboardManager: ClipboardManager,
        resourceManager: ResourceManager,
        addressIconGenerator: AddressIconGenerator
    ): ExternalActions.Presentation {
        return ExternalActionsProvider(clipboardManager, resourceManager, addressIconGenerator)
    }

    @Provides
    @FeatureScope
    fun provideAccountUpdateScope(
        accountRepository: AccountRepository,
    ) = AccountUpdateScope(accountRepository)

    @Provides
    @FeatureScope
    fun provideAddressDisplayUseCase(
        accountRepository: AccountRepository,
    ) = AddressDisplayUseCase(accountRepository)

    @Provides
    @FeatureScope
    fun provideAccountUseCase(
        accountRepository: AccountRepository,
        addressIconGenerator: AddressIconGenerator,
        walletUiUseCase: WalletUiUseCase,
        polkadotVaultVariantConfigProvider: PolkadotVaultVariantConfigProvider,
        metaAccountsUpdatesRegistry: MetaAccountsUpdatesRegistry
    ) = SelectedAccountUseCase(
        accountRepository = accountRepository,
        walletUiUseCase = walletUiUseCase,
        addressIconGenerator = addressIconGenerator,
        polkadotVaultVariantConfigProvider = polkadotVaultVariantConfigProvider,
        metaAccountsUpdatesRegistry = metaAccountsUpdatesRegistry
    )

    @Provides
    @FeatureScope
    fun providePolkadotVaultVariantConfigProvider(
        resourceManager: ResourceManager,
        appLinksProvider: AppLinksProvider
    ): PolkadotVaultVariantConfigProvider = RealPolkadotVaultVariantConfigProvider(resourceManager, appLinksProvider)

    @Provides
    @FeatureScope
    fun provideAccountDetailsInteractor(
        accountRepository: AccountRepository,
        secretStoreV2: SecretStoreV2,
        chainRegistry: ChainRegistry,
    ) = WalletDetailsInteractor(
        accountRepository,
        secretStoreV2,
        chainRegistry
    )

    @Provides
    @FeatureScope
    fun provideAccountSecretsFactory(
        jsonSeedDecoder: JsonSeedDecoder
    ) = AccountSecretsFactory(jsonSeedDecoder)

    @Provides
    @FeatureScope
    fun provideAddAccountInteractor(
        mnemonicAddAccountRepository: MnemonicAddAccountRepository,
        jsonAddAccountRepository: JsonAddAccountRepository,
        seedAddAccountRepository: SeedAddAccountRepository,
        accountRepository: AccountRepository,
        advancedEncryptionInteractor: AdvancedEncryptionInteractor
    ) = AddAccountInteractor(
        mnemonicAddAccountRepository,
        jsonAddAccountRepository,
        seedAddAccountRepository,
        accountRepository,
        advancedEncryptionInteractor
    )

    @Provides
    @FeatureScope
    fun provideInteractor(
        accountRepository: AccountRepository,
        secretStoreV2: SecretStoreV2,
        chainRegistry: ChainRegistry,
        encryptionDefaults: EncryptionDefaults
    ) = AdvancedEncryptionInteractor(accountRepository, secretStoreV2, chainRegistry, encryptionDefaults)

    @Provides
    fun provideImportTypeChooserMixin(): ImportTypeChooserMixin.Presentation = ImportTypeChooserProvider()

    @Provides
    fun provideAddAccountLauncherPresentationFactory(
        cloudBackupService: CloudBackupService,
        importTypeChooserMixin: ImportTypeChooserMixin.Presentation,
        resourceManager: ResourceManager,
        router: AccountRouter,
        addAccountInteractor: AddAccountInteractor,
        cloudBackupChangingWarningMixinFactory: CloudBackupChangingWarningMixinFactory
    ): AddAccountLauncherPresentationFactory = RealAddAccountLauncherPresentationFactory(
        cloudBackupService = cloudBackupService,
        importTypeChooserMixin = importTypeChooserMixin,
        resourceManager = resourceManager,
        router = router,
        addAccountInteractor = addAccountInteractor,
        cloudBackupChangingWarningMixinFactory = cloudBackupChangingWarningMixinFactory
    )

    @Provides
    @FeatureScope
    fun provideAddressInputMixinFactory(
        addressIconGenerator: AddressIconGenerator,
        systemCallExecutor: SystemCallExecutor,
        clipboardManager: ClipboardManager,
        multiChainQrSharingFactory: MultiChainQrSharingFactory,
        resourceManager: ResourceManager,
        accountUseCase: SelectedAccountUseCase,
        web3NamesInteractor: Web3NamesInteractor
    ) = AddressInputMixinFactory(
        addressIconGenerator = addressIconGenerator,
        systemCallExecutor = systemCallExecutor,
        clipboardManager = clipboardManager,
        qrSharingFactory = multiChainQrSharingFactory,
        resourceManager = resourceManager,
        accountUseCase = accountUseCase,
        web3NamesInteractor = web3NamesInteractor
    )

    @Provides
    @FeatureScope
    fun provideWalletUiUseCase(
        accountRepository: AccountRepository,
        addressIconGenerator: AddressIconGenerator,
        chainRegistry: ChainRegistry
    ): WalletUiUseCase {
        return WalletUiUseCaseImpl(accountRepository, addressIconGenerator, chainRegistry)
    }

    @Provides
    @FeatureScope
    fun provideMetaAccountGroupingInteractor(
        chainRegistry: ChainRegistry,
        accountRepository: AccountRepository,
        currencyRepository: CurrencyRepository,
        metaAccountsUpdatesRegistry: MetaAccountsUpdatesRegistry
    ): MetaAccountGroupingInteractor {
        return MetaAccountGroupingInteractorImpl(chainRegistry, accountRepository, currencyRepository, metaAccountsUpdatesRegistry)
    }

    @Provides
    @FeatureScope
    fun provideProxyFormatter(
        walletUseCase: WalletUiUseCase,
        resourceManager: ResourceManager
    ) = ProxyFormatter(walletUseCase, resourceManager)

    @Provides
    @FeatureScope
    fun provideDelegatedMetaAccountUpdatesListingMixinFactory(
        walletUseCase: WalletUiUseCase,
        metaAccountGroupingInteractor: MetaAccountGroupingInteractor,
        proxyFormatter: ProxyFormatter,
        resourceManager: ResourceManager
    ) = DelegatedMetaAccountUpdatesListingMixinFactory(walletUseCase, metaAccountGroupingInteractor, proxyFormatter, resourceManager)

    @Provides
    @FeatureScope
    fun provideAccountListingMixinFactory(
        walletUseCase: WalletUiUseCase,
        metaAccountGroupingInteractor: MetaAccountGroupingInteractor,
        proxyFormatter: ProxyFormatter,
        accountTypePresentationMapper: MetaAccountTypePresentationMapper,
        resourceManager: ResourceManager
    ) = MetaAccountWithBalanceListingMixinFactory(walletUseCase, metaAccountGroupingInteractor, accountTypePresentationMapper, proxyFormatter, resourceManager)

    @Provides
    @FeatureScope
    fun provideAccountTypePresentationMapper(
        resourceManager: ResourceManager,
        polkadotVaultVariantConfigProvider: PolkadotVaultVariantConfigProvider,
    ): MetaAccountTypePresentationMapper = RealMetaAccountTypePresentationMapper(resourceManager, polkadotVaultVariantConfigProvider)

    @Provides
    @FeatureScope
    fun provideIdentityRepository(
        @Named(REMOTE_STORAGE_SOURCE) remoteStorageSource: StorageDataSource,
        chainRegistry: ChainRegistry
    ): OnChainIdentityRepository = RealOnChainIdentityRepository(remoteStorageSource, chainRegistry)

    @Provides
    @FeatureScope
    fun provideEvmTransactionService(
        accountRepository: AccountRepository,
        chainRegistry: ChainRegistry,
        signerProvider: SignerProvider,
        gasPriceProviderFactory: GasPriceProviderFactory,
    ): EvmTransactionService = RealEvmTransactionService(
        accountRepository = accountRepository,
        chainRegistry = chainRegistry,
        signerProvider = signerProvider,
        gasPriceProviderFactory = gasPriceProviderFactory
    )

    @Provides
    @FeatureScope
    fun provideIdentityMixinFactory(
        appLinksProvider: AppLinksProvider
    ): IdentityMixin.Factory {
        return RealIdentityMixinFactory(appLinksProvider)
    }

    @Provides
    @FeatureScope
    fun provideLanguageUseCase(accountInteractor: AccountInteractor): LanguageUseCase {
        return RealLanguageUseCase(accountInteractor)
    }

    @Provides
    @FeatureScope
    fun provideSelectWalletMixinFactory(
        accountRepository: AccountRepository,
        accountGroupingInteractor: MetaAccountGroupingInteractor,
        walletUiUseCase: WalletUiUseCase,
        communicator: SelectWalletCommunicator,
    ): SelectWalletMixin.Factory {
        return RealRealSelectWalletMixinFactory(
            accountRepository = accountRepository,
            accountGroupingInteractor = accountGroupingInteractor,
            walletUiUseCase = walletUiUseCase,
            requester = communicator
        )
    }

    @Provides
    @FeatureScope
    fun provideBiometricServiceFactory(accountRepository: AccountRepository): BiometricServiceFactory {
        return RealBiometricServiceFactory(accountRepository)
    }

    @Provides
    @FeatureScope
    fun provideSigningNotSupportedPresentable(
        contextManager: ContextManager
    ): SigningNotSupportedPresentable = RealSigningNotSupportedPresentable(contextManager)

    @Provides
    @FeatureScope
    fun provideProxySyncService(
        chainRegistry: ChainRegistry,
        getProxyRepository: GetProxyRepository,
        accounRepository: AccountRepository,
        metaAccountDao: MetaAccountDao,
        @OnChainIdentity identityProvider: IdentityProvider,
        metaAccountsUpdatesRegistry: MetaAccountsUpdatesRegistry,
        proxiedAddAccountRepository: ProxiedAddAccountRepository,
        rootScope: RootScope
    ): ProxySyncService = RealProxySyncService(
        chainRegistry,
        getProxyRepository,
        accounRepository,
        metaAccountDao,
        identityProvider,
        metaAccountsUpdatesRegistry,
        proxiedAddAccountRepository,
        rootScope,
        shouldSyncWatchOnlyProxies = BuildConfig.DEBUG
    )

    @Provides
    @FeatureScope
    fun provideSelectAddressMixinFactory(
        selectAddressCommunicator: SelectAddressCommunicator,
        metaAccountGroupingInteractor: MetaAccountGroupingInteractor,
    ): SelectAddressMixin.Factory {
        return SelectAddressMixinFactory(
            selectAddressCommunicator,
            metaAccountGroupingInteractor
        )
    }

    @Provides
    @FeatureScope
    fun provideStartCreateWalletInteractor(
        cloudBackupService: CloudBackupService,
        addAccountInteractor: AddAccountInteractor,
        accountRepository: AccountRepository
    ): StartCreateWalletInteractor {
        return RealStartCreateWalletInteractor(cloudBackupService, addAccountInteractor, accountRepository)
    }

    @Provides
    @FeatureScope
    fun provideApplyLocalSnapshotToCloudBackupUseCase(
        localAccountsCloudBackupFacade: LocalAccountsCloudBackupFacade,
        cloudBackupService: CloudBackupService
    ): ApplyLocalSnapshotToCloudBackupUseCase {
        return RealApplyLocalSnapshotToCloudBackupUseCase(
            localAccountsCloudBackupFacade,
            cloudBackupService
        )
    }

    @Provides
    @FeatureScope
    fun provideManualBackupSelectAccountInteractor(
        accountRepository: AccountRepository,
        chainRegistry: ChainRegistry
    ): ManualBackupSelectAccountInteractor {
        return RealManualBackupSelectAccountInteractor(accountRepository, chainRegistry)
    }

    @Provides
    @FeatureScope
    fun provideManualBackupSelectWalletInteractor(
        accountRepository: AccountRepository
    ): ManualBackupSelectWalletInteractor {
        return RealManualBackupSelectWalletInteractor(accountRepository)
    }

    @Provides
    @FeatureScope
    fun provideCommonExportSecretsInteractor(
        accountRepository: AccountRepository,
        chainRegistry: ChainRegistry,
        secretStoreV2: SecretStoreV2
    ): CommonExportSecretsInteractor {
        return RealCommonExportSecretsInteractor(
            accountRepository,
            chainRegistry,
            secretStoreV2
        )
    }

    @Provides
    @FeatureScope
    fun provideManualBackupSecretsAdapterItemFactory(
        resourceManager: ResourceManager,
        accountRepository: AccountRepository,
        chainRegistry: ChainRegistry,
        commonExportSecretsInteractor: CommonExportSecretsInteractor
    ): ManualBackupSecretsAdapterItemFactory {
        return RealManualBackupSecretsAdapterItemFactory(
            resourceManager,
            accountRepository,
            chainRegistry,
            commonExportSecretsInteractor
        )
    }
}
