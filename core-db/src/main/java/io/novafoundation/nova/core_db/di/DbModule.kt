package io.novafoundation.nova.core_db.di

import android.content.Context
import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.ApplicationScope
import io.novafoundation.nova.core_db.AppDatabase
import io.novafoundation.nova.core_db.dao.AccountDao
import io.novafoundation.nova.core_db.dao.AccountStakingDao
import io.novafoundation.nova.core_db.dao.AssetDao
import io.novafoundation.nova.core_db.dao.BrowserHostSettingsDao
import io.novafoundation.nova.core_db.dao.ChainAssetDao
import io.novafoundation.nova.core_db.dao.ChainDao
import io.novafoundation.nova.core_db.dao.CoinPriceDao
import io.novafoundation.nova.core_db.dao.ContributionDao
import io.novafoundation.nova.core_db.dao.CurrencyDao
import io.novafoundation.nova.core_db.dao.DappAuthorizationDao
import io.novafoundation.nova.core_db.dao.ExternalBalanceDao
import io.novafoundation.nova.core_db.dao.FavouriteDAppsDao
import io.novafoundation.nova.core_db.dao.GovernanceDAppsDao
import io.novafoundation.nova.core_db.dao.HoldsDao
import io.novafoundation.nova.core_db.dao.LockDao
import io.novafoundation.nova.core_db.dao.MetaAccountDao
import io.novafoundation.nova.core_db.dao.NftDao
import io.novafoundation.nova.core_db.dao.NodeDao
import io.novafoundation.nova.core_db.dao.OperationDao
import io.novafoundation.nova.core_db.dao.PhishingAddressDao
import io.novafoundation.nova.core_db.dao.PhishingSitesDao
import io.novafoundation.nova.core_db.dao.StakingDashboardDao
import io.novafoundation.nova.core_db.dao.StakingRewardPeriodDao
import io.novafoundation.nova.core_db.dao.StakingTotalRewardDao
import io.novafoundation.nova.core_db.dao.StorageDao
import io.novafoundation.nova.core_db.dao.TinderGovDao
import io.novafoundation.nova.core_db.dao.TokenDao
import io.novafoundation.nova.core_db.dao.WalletConnectSessionsDao

@Module
class DbModule {

    @Provides
    @ApplicationScope
    fun provideAppDatabase(
        context: Context
    ): AppDatabase {
        return AppDatabase.get(context)
    }

    @Provides
    @ApplicationScope
    fun provideUserDao(appDatabase: AppDatabase): AccountDao {
        return appDatabase.userDao()
    }

    @Provides
    @ApplicationScope
    fun provideNodeDao(appDatabase: AppDatabase): NodeDao {
        return appDatabase.nodeDao()
    }

    @Provides
    @ApplicationScope
    fun provideAssetDao(appDatabase: AppDatabase): AssetDao {
        return appDatabase.assetDao()
    }

    @Provides
    @ApplicationScope
    fun provideLockDao(appDatabase: AppDatabase): LockDao {
        return appDatabase.lockDao()
    }

    @Provides
    @ApplicationScope
    fun provideContributionDao(appDatabase: AppDatabase): ContributionDao {
        return appDatabase.contributionDao()
    }

    @Provides
    @ApplicationScope
    fun provideOperationHistoryDao(appDatabase: AppDatabase): OperationDao {
        return appDatabase.operationDao()
    }

    @Provides
    @ApplicationScope
    fun providePhishingAddressDao(appDatabase: AppDatabase): PhishingAddressDao {
        return appDatabase.phishingAddressesDao()
    }

    @Provides
    @ApplicationScope
    fun provideStorageDao(appDatabase: AppDatabase): StorageDao {
        return appDatabase.storageDao()
    }

    @Provides
    @ApplicationScope
    fun provideTokenDao(appDatabase: AppDatabase): TokenDao {
        return appDatabase.tokenDao()
    }

    @Provides
    @ApplicationScope
    fun provideAccountStakingDao(appDatabase: AppDatabase): AccountStakingDao {
        return appDatabase.accountStakingDao()
    }

    @Provides
    @ApplicationScope
    fun provideStakingTotalRewardDao(appDatabase: AppDatabase): StakingTotalRewardDao {
        return appDatabase.stakingTotalRewardDao()
    }

    @Provides
    @ApplicationScope
    fun provideChainDao(appDatabase: AppDatabase): ChainDao {
        return appDatabase.chainDao()
    }

    @Provides
    @ApplicationScope
    fun provideChainAssetDao(appDatabase: AppDatabase): ChainAssetDao {
        return appDatabase.chainAssetDao()
    }

    @Provides
    @ApplicationScope
    fun provideMetaAccountDao(appDatabase: AppDatabase): MetaAccountDao {
        return appDatabase.metaAccountDao()
    }

    @Provides
    @ApplicationScope
    fun provideDappAuthorizationDao(appDatabase: AppDatabase): DappAuthorizationDao {
        return appDatabase.dAppAuthorizationDao()
    }

    @Provides
    @ApplicationScope
    fun provideNftDao(appDatabase: AppDatabase): NftDao {
        return appDatabase.nftDao()
    }

    @Provides
    @ApplicationScope
    fun providePhishingSitesDao(appDatabase: AppDatabase): PhishingSitesDao {
        return appDatabase.phishingSitesDao()
    }

    @Provides
    @ApplicationScope
    fun provideFavouriteDappsDao(appDatabase: AppDatabase): FavouriteDAppsDao {
        return appDatabase.favouriteDAppsDao()
    }

    @Provides
    @ApplicationScope
    fun provideCurrencyDao(appDatabase: AppDatabase): CurrencyDao {
        return appDatabase.currencyDao()
    }

    @Provides
    @ApplicationScope
    fun provideGovernanceDAppDao(appDatabase: AppDatabase): GovernanceDAppsDao {
        return appDatabase.governanceDAppsDao()
    }

    @Provides
    @ApplicationScope
    fun provideBrowserHostSettingsDao(appDatabase: AppDatabase): BrowserHostSettingsDao {
        return appDatabase.browserHostSettingsDao()
    }

    @Provides
    @ApplicationScope
    fun provideWalletConnectSessionsDao(appDatabase: AppDatabase): WalletConnectSessionsDao {
        return appDatabase.walletConnectSessionsDao()
    }

    @Provides
    @ApplicationScope
    fun provideStakingDashboardDao(appDatabase: AppDatabase): StakingDashboardDao {
        return appDatabase.stakingDashboardDao()
    }

    @Provides
    @ApplicationScope
    fun provideCoinPriceDao(appDatabase: AppDatabase): CoinPriceDao {
        return appDatabase.coinPriceDao()
    }

    @Provides
    @ApplicationScope
    fun provideStakingRewardPeriodDao(appDatabase: AppDatabase): StakingRewardPeriodDao {
        return appDatabase.stakingRewardPeriodDao()
    }

    @Provides
    @ApplicationScope
    fun provideExternalBalanceDao(appDatabase: AppDatabase): ExternalBalanceDao {
        return appDatabase.externalBalanceDao()
    }

    @Provides
    @ApplicationScope
    fun provideHoldsDao(appDatabase: AppDatabase): HoldsDao {
        return appDatabase.holdsDao()
    }

    @Provides
    @ApplicationScope
    fun provideTinderGovDao(appDatabase: AppDatabase): TinderGovDao {
        return appDatabase.tinderGovDao()
    }
}
