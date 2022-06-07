package io.novafoundation.nova.core_db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import io.novafoundation.nova.core_db.converters.CryptoTypeConverters
import io.novafoundation.nova.core_db.converters.LongMathConverters
import io.novafoundation.nova.core_db.converters.NetworkTypeConverters
import io.novafoundation.nova.core_db.converters.NftTypeConverters
import io.novafoundation.nova.core_db.converters.OperationConverters
import io.novafoundation.nova.core_db.converters.TokenConverters
import io.novafoundation.nova.core_db.dao.AccountDao
import io.novafoundation.nova.core_db.dao.AccountStakingDao
import io.novafoundation.nova.core_db.dao.AssetDao
import io.novafoundation.nova.core_db.dao.ChainDao
import io.novafoundation.nova.core_db.dao.DappAuthorizationDao
import io.novafoundation.nova.core_db.dao.FavouriteDAppsDao
import io.novafoundation.nova.core_db.dao.MetaAccountDao
import io.novafoundation.nova.core_db.dao.NftDao
import io.novafoundation.nova.core_db.dao.NodeDao
import io.novafoundation.nova.core_db.dao.OperationDao
import io.novafoundation.nova.core_db.dao.PhishingAddressDao
import io.novafoundation.nova.core_db.dao.PhishingSitesDao
import io.novafoundation.nova.core_db.dao.StakingTotalRewardDao
import io.novafoundation.nova.core_db.dao.StorageDao
import io.novafoundation.nova.core_db.dao.TokenDao
import io.novafoundation.nova.core_db.migrations.AddAdditionalFieldToChains_12_13
import io.novafoundation.nova.core_db.migrations.AddBuyProviders_7_8
import io.novafoundation.nova.core_db.migrations.AddChainColor_4_5
import io.novafoundation.nova.core_db.migrations.AddChainToTotalRewards_12_13
import io.novafoundation.nova.core_db.migrations.AddDAppAuthorizations_1_2
import io.novafoundation.nova.core_db.migrations.AddFavouriteDApps_9_10
import io.novafoundation.nova.core_db.migrations.AddNfts_5_6
import io.novafoundation.nova.core_db.migrations.AddSitePhishing_6_7
import io.novafoundation.nova.core_db.migrations.AssetTypes_2_3
import io.novafoundation.nova.core_db.migrations.BetterChainDiffing_8_9
import io.novafoundation.nova.core_db.migrations.ChangeAsset_3_4
import io.novafoundation.nova.core_db.migrations.ChangeDAppAuthorization_10_11
import io.novafoundation.nova.core_db.migrations.RemoveChainForeignKeyFromChainAccount_11_12
import io.novafoundation.nova.core_db.model.AccountLocal
import io.novafoundation.nova.core_db.model.AccountStakingLocal
import io.novafoundation.nova.core_db.model.AssetLocal
import io.novafoundation.nova.core_db.model.DappAuthorizationLocal
import io.novafoundation.nova.core_db.model.FavouriteDAppLocal
import io.novafoundation.nova.core_db.model.NftLocal
import io.novafoundation.nova.core_db.model.NodeLocal
import io.novafoundation.nova.core_db.model.OperationLocal
import io.novafoundation.nova.core_db.model.PhishingAddressLocal
import io.novafoundation.nova.core_db.model.PhishingSiteLocal
import io.novafoundation.nova.core_db.model.StorageEntryLocal
import io.novafoundation.nova.core_db.model.TokenLocal
import io.novafoundation.nova.core_db.model.TotalRewardLocal
import io.novafoundation.nova.core_db.model.chain.ChainAccountLocal
import io.novafoundation.nova.core_db.model.chain.ChainAssetLocal
import io.novafoundation.nova.core_db.model.chain.ChainExplorerLocal
import io.novafoundation.nova.core_db.model.chain.ChainLocal
import io.novafoundation.nova.core_db.model.chain.ChainNodeLocal
import io.novafoundation.nova.core_db.model.chain.ChainRuntimeInfoLocal
import io.novafoundation.nova.core_db.model.chain.MetaAccountLocal

@Database(
    version = 13,
    entities = [
        AccountLocal::class,
        NodeLocal::class,
        AssetLocal::class,
        TokenLocal::class,
        PhishingAddressLocal::class,
        StorageEntryLocal::class,
        AccountStakingLocal::class,
        TotalRewardLocal::class,
        OperationLocal::class,

        ChainLocal::class,
        ChainNodeLocal::class,
        ChainAssetLocal::class,
        ChainRuntimeInfoLocal::class,
        ChainExplorerLocal::class,
        MetaAccountLocal::class,
        ChainAccountLocal::class,

        DappAuthorizationLocal::class,
        NftLocal::class,

        PhishingSiteLocal::class,

        FavouriteDAppLocal::class
    ],
)
@TypeConverters(
    LongMathConverters::class,
    NetworkTypeConverters::class,
    TokenConverters::class,
    OperationConverters::class,
    CryptoTypeConverters::class,
    NftTypeConverters::class
)

abstract class AppDatabase : RoomDatabase() {

    companion object {

        private var instance: AppDatabase? = null

        @Synchronized
        fun get(
            context: Context
        ): AppDatabase {
            if (instance == null) {
                instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java, "app.db"
                )
                    .addMigrations(AddDAppAuthorizations_1_2, AssetTypes_2_3, ChangeAsset_3_4)
                    .addMigrations(AddChainColor_4_5, AddNfts_5_6, AddSitePhishing_6_7, AddBuyProviders_7_8, BetterChainDiffing_8_9)
                    .addMigrations(AddFavouriteDApps_9_10, ChangeDAppAuthorization_10_11, RemoveChainForeignKeyFromChainAccount_11_12)
                    .addMigrations(AddAdditionalFieldToChains_12_13)
                    .fallbackToDestructiveMigration()
                    .build()
            }
            return instance!!
        }
    }

    abstract fun nodeDao(): NodeDao

    abstract fun userDao(): AccountDao

    abstract fun assetDao(): AssetDao

    abstract fun operationDao(): OperationDao

    abstract fun phishingAddressesDao(): PhishingAddressDao

    abstract fun storageDao(): StorageDao

    abstract fun tokenDao(): TokenDao

    abstract fun accountStakingDao(): AccountStakingDao

    abstract fun stakingTotalRewardDao(): StakingTotalRewardDao

    abstract fun chainDao(): ChainDao

    abstract fun metaAccountDao(): MetaAccountDao

    abstract fun dAppAuthorizationDao(): DappAuthorizationDao

    abstract fun nftDao(): NftDao

    abstract fun phishingSitesDao(): PhishingSitesDao

    abstract fun favouriteDAppsDao(): FavouriteDAppsDao
}
