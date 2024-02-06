package io.novafoundation.nova.feature_account_impl.di

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.data.secrets.v2.SecretStoreV2
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.core_db.dao.MetaAccountDao
import io.novafoundation.nova.feature_account_api.data.proxy.ProxySyncService
import io.novafoundation.nova.feature_account_api.data.repository.addAccount.ledger.LedgerAddAccountRepository
import io.novafoundation.nova.feature_account_api.data.repository.addAccount.proxied.ProxiedAddAccountRepository
import io.novafoundation.nova.feature_account_impl.data.repository.datasource.AccountDataSource
import io.novafoundation.nova.feature_account_impl.data.secrets.AccountSecretsFactory
import io.novafoundation.nova.feature_account_impl.data.repository.addAccount.ledger.RealLedgerAddAccountRepository
import io.novafoundation.nova.feature_account_impl.data.repository.addAccount.paritySigner.ParitySignerAddAccountRepository
import io.novafoundation.nova.feature_account_impl.data.repository.addAccount.proxied.RealProxiedAddAccountRepository
import io.novafoundation.nova.feature_account_impl.data.repository.addAccount.secrets.JsonAddAccountRepository
import io.novafoundation.nova.feature_account_impl.data.repository.addAccount.secrets.MnemonicAddAccountRepository
import io.novafoundation.nova.feature_account_impl.data.repository.addAccount.secrets.SeedAddAccountRepository
import io.novafoundation.nova.feature_account_impl.data.repository.addAccount.watchOnly.WatchOnlyAddAccountRepository
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import jp.co.soramitsu.fearless_utils.encrypt.json.JsonSeedDecoder

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
    ): ProxiedAddAccountRepository = RealProxiedAddAccountRepository(
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
