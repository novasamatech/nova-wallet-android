package io.novafoundation.nova.feature_account_impl.di

import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.data.network.AppLinksProvider
import io.novafoundation.nova.common.data.network.rpc.SocketSingleRequestExecutor
import io.novafoundation.nova.common.data.secrets.v1.SecretStoreV1
import io.novafoundation.nova.common.data.secrets.v2.SecretStoreV2
import io.novafoundation.nova.common.data.storage.Preferences
import io.novafoundation.nova.common.data.storage.encrypt.EncryptedPreferences
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.resources.ClipboardManager
import io.novafoundation.nova.common.resources.LanguagesHolder
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.systemCall.SystemCallExecutor
import io.novafoundation.nova.core_db.dao.AccountDao
import io.novafoundation.nova.core_db.dao.MetaAccountDao
import io.novafoundation.nova.core_db.dao.NodeDao
import io.novafoundation.nova.feature_account_api.data.ethereum.transaction.EvmTransactionService
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.data.repository.OnChainIdentityRepository
import io.novafoundation.nova.feature_account_api.data.signer.SignerProvider
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountInteractor
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_account_api.domain.interfaces.MetaAccountGroupingInteractor
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_account_api.domain.updaters.AccountUpdateScope
import io.novafoundation.nova.feature_account_api.presenatation.account.AddressDisplayUseCase
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.WalletUiUseCase
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActionsProvider
import io.novafoundation.nova.feature_account_api.presenatation.mixin.addressInput.AddressInputMixinFactory
import io.novafoundation.nova.feature_account_api.presenatation.mixin.identity.IdentityMixin
import io.novafoundation.nova.feature_account_api.presenatation.mixin.importType.ImportTypeChooserMixin
import io.novafoundation.nova.feature_account_api.presenatation.mixin.importType.ImportTypeChooserProvider
import io.novafoundation.nova.feature_account_impl.data.ethereum.transaction.RealEvmTransactionService
import io.novafoundation.nova.feature_account_impl.data.extrinsic.RealExtrinsicService
import io.novafoundation.nova.feature_account_impl.data.network.blockchain.AccountSubstrateSource
import io.novafoundation.nova.feature_account_impl.data.network.blockchain.AccountSubstrateSourceImpl
import io.novafoundation.nova.feature_account_impl.data.repository.AccountRepositoryImpl
import io.novafoundation.nova.feature_account_impl.data.repository.AddAccountRepository
import io.novafoundation.nova.feature_account_impl.data.repository.RealOnChainIdentityRepository
import io.novafoundation.nova.feature_account_impl.data.repository.datasource.AccountDataSource
import io.novafoundation.nova.feature_account_impl.data.repository.datasource.AccountDataSourceImpl
import io.novafoundation.nova.feature_account_impl.data.repository.datasource.migration.AccountDataMigration
import io.novafoundation.nova.feature_account_impl.data.secrets.AccountSecretsFactory
import io.novafoundation.nova.feature_account_impl.di.modules.IdentityProviderModule
import io.novafoundation.nova.feature_account_impl.di.modules.ParitySignerModule
import io.novafoundation.nova.feature_account_impl.di.modules.SignersModule
import io.novafoundation.nova.feature_account_impl.di.modules.WatchOnlyModule
import io.novafoundation.nova.feature_account_impl.domain.AccountInteractorImpl
import io.novafoundation.nova.feature_account_impl.domain.MetaAccountGroupingInteractorImpl
import io.novafoundation.nova.feature_account_impl.domain.NodeHostValidator
import io.novafoundation.nova.feature_account_impl.domain.account.add.AddAccountInteractor
import io.novafoundation.nova.feature_account_impl.domain.account.advancedEncryption.AdvancedEncryptionInteractor
import io.novafoundation.nova.feature_account_impl.domain.account.details.AccountDetailsInteractor
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.feature_account_impl.presentation.account.common.listing.MetaAccountWithBalanceListingMixinFactory
import io.novafoundation.nova.feature_account_impl.presentation.account.wallet.WalletUiUseCaseImpl
import io.novafoundation.nova.feature_account_impl.presentation.common.mixin.addAccountChooser.AddAccountLauncherMixin
import io.novafoundation.nova.feature_account_impl.presentation.common.mixin.addAccountChooser.AddAccountLauncherProvider
import io.novafoundation.nova.feature_account_impl.presentation.mixin.identity.RealIdentityMixinFactory
import io.novafoundation.nova.feature_currency_api.domain.interfaces.CurrencyRepository
import io.novafoundation.nova.runtime.di.REMOTE_STORAGE_SOURCE
import io.novafoundation.nova.runtime.extrinsic.ExtrinsicBuilderFactory
import io.novafoundation.nova.runtime.extrinsic.multi.ExtrinsicSplitter
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.qr.MultiChainQrSharingFactory
import io.novafoundation.nova.runtime.network.rpc.RpcCalls
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import jp.co.soramitsu.fearless_utils.encrypt.json.JsonSeedDecoder
import jp.co.soramitsu.fearless_utils.encrypt.json.JsonSeedEncoder
import javax.inject.Named

@Module(includes = [SignersModule::class, WatchOnlyModule::class, ParitySignerModule::class, IdentityProviderModule::class])
class AccountFeatureModule {

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
    fun provideAccountRepository(
        accountDataSource: AccountDataSource,
        accountDao: AccountDao,
        nodeDao: NodeDao,
        jsonSeedEncoder: JsonSeedEncoder,
        accountSubstrateSource: AccountSubstrateSource,
        languagesHolder: LanguagesHolder,
        secretStoreV2: SecretStoreV2,
        multiChainQrSharingFactory: MultiChainQrSharingFactory,
    ): AccountRepository {
        return AccountRepositoryImpl(
            accountDataSource,
            accountDao,
            nodeDao,
            jsonSeedEncoder,
            languagesHolder,
            accountSubstrateSource,
            secretStoreV2,
            multiChainQrSharingFactory
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
    fun provideAccountDataSource(
        preferences: Preferences,
        encryptedPreferences: EncryptedPreferences,
        nodeDao: NodeDao,
        secretStoreV1: SecretStoreV1,
        accountDataMigration: AccountDataMigration,
        metaAccountDao: MetaAccountDao,
        chainRegistry: ChainRegistry,
        secretStoreV2: SecretStoreV2,
    ): AccountDataSource {
        return AccountDataSourceImpl(
            preferences,
            encryptedPreferences,
            nodeDao,
            metaAccountDao,
            chainRegistry,
            secretStoreV2,
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
    ) = SelectedAccountUseCase(accountRepository, walletUiUseCase, addressIconGenerator)

    @Provides
    @FeatureScope
    fun provideAccountDetailsInteractor(
        accountRepository: AccountRepository,
        secretStoreV2: SecretStoreV2,
        chainRegistry: ChainRegistry,
    ) = AccountDetailsInteractor(
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
    fun provideAddAccountRepository(
        accountDataSource: AccountDataSource,
        accountSecretsFactory: AccountSecretsFactory,
        jsonSeedDecoder: JsonSeedDecoder,
        chainRegistry: ChainRegistry,
    ) = AddAccountRepository(
        accountDataSource,
        accountSecretsFactory,
        jsonSeedDecoder,
        chainRegistry
    )

    @Provides
    @FeatureScope
    fun provideAddAccountInteractor(
        addAccountRepository: AddAccountRepository,
        accountRepository: AccountRepository,
    ) = AddAccountInteractor(addAccountRepository, accountRepository)

    @Provides
    @FeatureScope
    fun provideInteractor(
        accountRepository: AccountRepository,
        secretStoreV2: SecretStoreV2,
        chainRegistry: ChainRegistry,
    ) = AdvancedEncryptionInteractor(accountRepository, secretStoreV2, chainRegistry)

    @Provides
    fun provideImportTypeChooserMixin(): ImportTypeChooserMixin.Presentation = ImportTypeChooserProvider()

    @Provides
    fun provideAddAccountLauncherMixin(
        importTypeChooserMixin: ImportTypeChooserMixin.Presentation,
        resourceManager: ResourceManager,
        router: AccountRouter,
    ): AddAccountLauncherMixin.Presentation = AddAccountLauncherProvider(
        importTypeChooserMixin = importTypeChooserMixin,
        resourceManager = resourceManager,
        router = router
    )

    @Provides
    @FeatureScope
    fun provideAddressInputMixinFactory(
        addressIconGenerator: AddressIconGenerator,
        systemCallExecutor: SystemCallExecutor,
        clipboardManager: ClipboardManager,
        multiChainQrSharingFactory: MultiChainQrSharingFactory,
        resourceManager: ResourceManager,
        accountUseCase: SelectedAccountUseCase
    ) = AddressInputMixinFactory(
        addressIconGenerator = addressIconGenerator,
        systemCallExecutor = systemCallExecutor,
        clipboardManager = clipboardManager,
        qrSharingFactory = multiChainQrSharingFactory,
        resourceManager = resourceManager,
        accountUseCase = accountUseCase
    )

    @Provides
    @FeatureScope
    fun provideWalletUiUseCase(
        accountRepository: AccountRepository,
        addressIconGenerator: AddressIconGenerator
    ): WalletUiUseCase {
        return WalletUiUseCaseImpl(accountRepository, addressIconGenerator)
    }

    @Provides
    @FeatureScope
    fun provideMetaAccountGroupingInteractor(
        chainRegistry: ChainRegistry,
        accountRepository: AccountRepository,
        currencyRepository: CurrencyRepository,
    ): MetaAccountGroupingInteractor {
        return MetaAccountGroupingInteractorImpl(chainRegistry, accountRepository, currencyRepository)
    }

    @Provides
    @FeatureScope
    fun provideAccountListingMixinFactory(
        walletUseCase: WalletUiUseCase,
        resourceManager: ResourceManager,
        metaAccountGroupingInteractor: MetaAccountGroupingInteractor,
    ) = MetaAccountWithBalanceListingMixinFactory(walletUseCase, resourceManager, metaAccountGroupingInteractor)

    @Provides
    @FeatureScope
    fun provideIdentityRepository(
        @Named(REMOTE_STORAGE_SOURCE) remoteStorageSource: StorageDataSource
    ): OnChainIdentityRepository = RealOnChainIdentityRepository(remoteStorageSource)

    @Provides
    @FeatureScope
    fun provideEvmTransactionService(
        accountRepository: AccountRepository,
        chainRegistry: ChainRegistry,
        signerProvider: SignerProvider
    ): EvmTransactionService = RealEvmTransactionService(
        accountRepository = accountRepository,
        chainRegistry = chainRegistry,
        signerProvider = signerProvider
    )

    @Provides
    @FeatureScope
    fun provideIdentityMixinFactory(
        appLinksProvider: AppLinksProvider
    ): IdentityMixin.Factory {
        return RealIdentityMixinFactory(appLinksProvider)
    }
}
