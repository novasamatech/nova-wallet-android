package io.novafoundation.nova.core_db.di

import io.novafoundation.nova.core_db.AppDatabase
import io.novafoundation.nova.core_db.dao.AccountDao
import io.novafoundation.nova.core_db.dao.AccountStakingDao
import io.novafoundation.nova.core_db.dao.AssetDao
import io.novafoundation.nova.core_db.dao.BrowserHostSettingsDao
import io.novafoundation.nova.core_db.dao.ChainAssetDao
import io.novafoundation.nova.core_db.dao.ChainDao
import io.novafoundation.nova.core_db.dao.ContributionDao
import io.novafoundation.nova.core_db.dao.CurrencyDao
import io.novafoundation.nova.core_db.dao.DappAuthorizationDao
import io.novafoundation.nova.core_db.dao.FavouriteDAppsDao
import io.novafoundation.nova.core_db.dao.GovernanceDAppsDao
import io.novafoundation.nova.core_db.dao.LockDao
import io.novafoundation.nova.core_db.dao.MetaAccountDao
import io.novafoundation.nova.core_db.dao.NftDao
import io.novafoundation.nova.core_db.dao.NodeDao
import io.novafoundation.nova.core_db.dao.OperationDao
import io.novafoundation.nova.core_db.dao.PhishingAddressDao
import io.novafoundation.nova.core_db.dao.PhishingSitesDao
import io.novafoundation.nova.core_db.dao.StakingTotalRewardDao
import io.novafoundation.nova.core_db.dao.StorageDao
import io.novafoundation.nova.core_db.dao.TokenDao
import io.novafoundation.nova.core_db.dao.WalletConnectSessionsDao

interface DbApi {

    fun provideDatabase(): AppDatabase

    fun provideLockDao(): LockDao

    fun provideAccountDao(): AccountDao

    fun contributionDao(): ContributionDao

    fun provideNodeDao(): NodeDao

    fun provideAssetDao(): AssetDao

    fun provideOperationDao(): OperationDao

    fun providePhishingAddressDao(): PhishingAddressDao

    fun storageDao(): StorageDao

    fun tokenDao(): TokenDao

    fun accountStakingDao(): AccountStakingDao

    fun stakingTotalRewardDao(): StakingTotalRewardDao

    fun chainDao(): ChainDao

    fun chainAssetDao(): ChainAssetDao

    fun metaAccountDao(): MetaAccountDao

    fun dappAuthorizationDao(): DappAuthorizationDao

    fun nftDao(): NftDao

    fun governanceDAppsDao(): GovernanceDAppsDao

    fun browserHostSettingsDao(): BrowserHostSettingsDao

    val phishingSitesDao: PhishingSitesDao

    val favouritesDAppsDao: FavouriteDAppsDao

    val currencyDao: CurrencyDao

    val walletConnectSessionsDao: WalletConnectSessionsDao
}
