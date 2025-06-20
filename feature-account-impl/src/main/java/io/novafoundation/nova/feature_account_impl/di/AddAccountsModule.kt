package io.novafoundation.nova.feature_account_impl.di

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.data.secrets.v2.SecretStoreV2
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.core_db.dao.MetaAccountDao
import io.novafoundation.nova.feature_account_api.data.events.MetaAccountChangesEventBus
import io.novafoundation.nova.feature_account_api.data.repository.addAccount.ledger.GenericLedgerAddAccountRepository
import io.novafoundation.nova.feature_account_api.data.repository.addAccount.ledger.LegacyLedgerAddAccountRepository
import io.novafoundation.nova.feature_account_api.data.repository.addAccount.secrets.MnemonicAddAccountRepository
import io.novafoundation.nova.feature_account_impl.data.mappers.AccountMappers
import io.novafoundation.nova.feature_account_impl.data.repository.addAccount.LocalAddMetaAccountRepository
import io.novafoundation.nova.feature_account_impl.data.repository.addAccount.ledger.RealGenericLedgerAddAccountRepository
import io.novafoundation.nova.feature_account_impl.data.repository.addAccount.ledger.RealLegacyLedgerAddAccountRepository
import io.novafoundation.nova.feature_account_impl.data.repository.addAccount.paritySigner.ParitySignerAddAccountRepository
import io.novafoundation.nova.feature_account_impl.data.repository.addAccount.secrets.JsonAddAccountRepository
import io.novafoundation.nova.feature_account_impl.data.repository.addAccount.secrets.RealMnemonicAddAccountRepository
import io.novafoundation.nova.feature_account_impl.data.repository.addAccount.secrets.SeedAddAccountRepository
import io.novafoundation.nova.feature_account_impl.data.repository.addAccount.watchOnly.WatchOnlyAddAccountRepository
import io.novafoundation.nova.feature_account_impl.data.repository.datasource.AccountDataSource
import io.novafoundation.nova.feature_account_impl.data.secrets.AccountSecretsFactory
import io.novafoundation.nova.feature_account_impl.di.AddAccountsModule.BindsModule
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novasama.substrate_sdk_android.encrypt.json.JsonSeedDecoder

@Module(includes = [BindsModule::class])
class AddAccountsModule {

    @Module
    interface BindsModule

    @Provides
    @FeatureScope
    fun provideLocalAddMetaAccountRepository(
        metaAccountChangesEventBus: MetaAccountChangesEventBus,
        metaAccountDao: MetaAccountDao,
        secretStoreV2: SecretStoreV2
    ) = LocalAddMetaAccountRepository(
        metaAccountChangesEventBus,
        metaAccountDao,
        secretStoreV2
    )

    @Provides
    @FeatureScope
    fun provideMnemonicAddAccountRepository(
        accountDataSource: AccountDataSource,
        accountSecretsFactory: AccountSecretsFactory,
        chainRegistry: ChainRegistry,
        metaAccountChangesEventBus: MetaAccountChangesEventBus
    ): MnemonicAddAccountRepository = RealMnemonicAddAccountRepository(
        accountDataSource,
        accountSecretsFactory,
        chainRegistry,
        metaAccountChangesEventBus
    )

    @Provides
    @FeatureScope
    fun provideJsonAddAccountRepository(
        accountDataSource: AccountDataSource,
        accountSecretsFactory: AccountSecretsFactory,
        jsonSeedDecoder: JsonSeedDecoder,
        chainRegistry: ChainRegistry,
        metaAccountChangesEventBus: MetaAccountChangesEventBus
    ) = JsonAddAccountRepository(
        accountDataSource,
        accountSecretsFactory,
        jsonSeedDecoder,
        chainRegistry,
        metaAccountChangesEventBus
    )

    @Provides
    @FeatureScope
    fun provideSeedAddAccountRepository(
        accountDataSource: AccountDataSource,
        accountSecretsFactory: AccountSecretsFactory,
        chainRegistry: ChainRegistry,
        metaAccountChangesEventBus: MetaAccountChangesEventBus
    ) = SeedAddAccountRepository(
        accountDataSource,
        accountSecretsFactory,
        chainRegistry,
        metaAccountChangesEventBus
    )

    @Provides
    @FeatureScope
    fun provideWatchOnlyAddAccountRepository(
        accountDao: MetaAccountDao,
        metaAccountChangesEventBus: MetaAccountChangesEventBus
    ) = WatchOnlyAddAccountRepository(
        accountDao,
        metaAccountChangesEventBus
    )

    @Provides
    @FeatureScope
    fun provideParitySignerAddAccountRepository(
        accountDao: MetaAccountDao,
        metaAccountChangesEventBus: MetaAccountChangesEventBus
    ) = ParitySignerAddAccountRepository(
        accountDao,
        metaAccountChangesEventBus
    )

    @Provides
    @FeatureScope
    fun provideLegacyLedgerAddAccountRepository(
        accountDao: MetaAccountDao,
        chainRegistry: ChainRegistry,
        secretStoreV2: SecretStoreV2,
        metaAccountChangesEventBus: MetaAccountChangesEventBus
    ): LegacyLedgerAddAccountRepository = RealLegacyLedgerAddAccountRepository(
        accountDao,
        chainRegistry,
        secretStoreV2,
        metaAccountChangesEventBus
    )

    @Provides
    @FeatureScope
    fun provideGenericLedgerAddAccountRepository(
        accountDao: MetaAccountDao,
        secretStoreV2: SecretStoreV2,
        metaAccountChangesEventBus: MetaAccountChangesEventBus,
        accountMappers: AccountMappers,
    ): GenericLedgerAddAccountRepository = RealGenericLedgerAddAccountRepository(
        accountDao,
        secretStoreV2,
        accountMappers,
        metaAccountChangesEventBus,
    )
}
