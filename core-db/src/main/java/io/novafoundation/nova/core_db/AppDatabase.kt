package io.novafoundation.nova.core_db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import io.novafoundation.nova.core_db.converters.AssetConverters
import io.novafoundation.nova.core_db.converters.ChainConverters
import io.novafoundation.nova.core_db.converters.CryptoTypeConverters
import io.novafoundation.nova.core_db.converters.CurrencyConverters
import io.novafoundation.nova.core_db.converters.ExternalApiConverters
import io.novafoundation.nova.core_db.converters.ExternalBalanceTypeConverters
import io.novafoundation.nova.core_db.converters.LongMathConverters
import io.novafoundation.nova.core_db.converters.MetaAccountTypeConverters
import io.novafoundation.nova.core_db.converters.NetworkTypeConverters
import io.novafoundation.nova.core_db.converters.NftConverters
import io.novafoundation.nova.core_db.converters.OperationConverters
import io.novafoundation.nova.core_db.converters.ProxyAccountConverters
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
import io.novafoundation.nova.core_db.dao.TokenDao
import io.novafoundation.nova.core_db.dao.WalletConnectSessionsDao
import io.novafoundation.nova.core_db.migrations.AddAdditionalFieldToChains_12_13
import io.novafoundation.nova.core_db.migrations.AddBalanceModesToAssets_51_52
import io.novafoundation.nova.core_db.migrations.AddBrowserHostSettings_34_35
import io.novafoundation.nova.core_db.migrations.AddBuyProviders_7_8
import io.novafoundation.nova.core_db.migrations.AddChainColor_4_5
import io.novafoundation.nova.core_db.migrations.AddConnectionStateToChains_53_54
import io.novafoundation.nova.core_db.migrations.AddContributions_23_24
import io.novafoundation.nova.core_db.migrations.AddCurrencies_18_19
import io.novafoundation.nova.core_db.migrations.AddDAppAuthorizations_1_2
import io.novafoundation.nova.core_db.migrations.AddEnabledColumnToChainAssets_30_31
import io.novafoundation.nova.core_db.migrations.AddEventIdToOperation_47_48
import io.novafoundation.nova.core_db.migrations.AddExternalBalances_45_46
import io.novafoundation.nova.core_db.migrations.AddExtrinsicContentField_37_38
import io.novafoundation.nova.core_db.migrations.AddFavouriteDApps_9_10
import io.novafoundation.nova.core_db.migrations.AddFungibleNfts_55_56
import io.novafoundation.nova.core_db.migrations.AddGloballyUniqueIdToMetaAccounts_57_58
import io.novafoundation.nova.core_db.migrations.AddGloballyUniqueIdToMetaAccounts_58_59
import io.novafoundation.nova.core_db.migrations.AddGovernanceDapps_25_26
import io.novafoundation.nova.core_db.migrations.AddGovernanceExternalApiToChain_27_28
import io.novafoundation.nova.core_db.migrations.AddGovernanceFlagToChains_24_25
import io.novafoundation.nova.core_db.migrations.AddGovernanceNetworkToExternalApi_33_34
import io.novafoundation.nova.core_db.migrations.AddLocks_22_23
import io.novafoundation.nova.core_db.migrations.AddMetaAccountType_14_15
import io.novafoundation.nova.core_db.migrations.AddNfts_5_6
import io.novafoundation.nova.core_db.migrations.AddNodeSelectionStrategyField_38_39
import io.novafoundation.nova.core_db.migrations.AddPoolIdToOperations_46_47
import io.novafoundation.nova.core_db.migrations.AddProxyAccount_54_55
import io.novafoundation.nova.core_db.migrations.AddRewardAccountToStakingDashboard_43_44
import io.novafoundation.nova.core_db.migrations.AddRuntimeFlagToChains_36_37
import io.novafoundation.nova.core_db.migrations.AddSitePhishing_6_7
import io.novafoundation.nova.core_db.migrations.AddSourceToLocalAsset_28_29
import io.novafoundation.nova.core_db.migrations.AddStakingDashboardItems_41_42
import io.novafoundation.nova.core_db.migrations.AddStakingTypeToTotalRewards_44_45
import io.novafoundation.nova.core_db.migrations.AddSwapOption_48_49
import io.novafoundation.nova.core_db.migrations.AddTransactionVersionToRuntime_50_51
import io.novafoundation.nova.core_db.migrations.AddTransferApisTable_29_30
import io.novafoundation.nova.core_db.migrations.AddVersioningToGovernanceDapps_32_33
import io.novafoundation.nova.core_db.migrations.AddWalletConnectSessions_39_40
import io.novafoundation.nova.core_db.migrations.AssetTypes_2_3
import io.novafoundation.nova.core_db.migrations.BetterChainDiffing_8_9
import io.novafoundation.nova.core_db.migrations.ChainPushSupport_56_57
import io.novafoundation.nova.core_db.migrations.ChangeAsset_3_4
import io.novafoundation.nova.core_db.migrations.ChangeChainNodes_20_21
import io.novafoundation.nova.core_db.migrations.ChangeDAppAuthorization_10_11
import io.novafoundation.nova.core_db.migrations.ChangeSessionTopicToParing_52_53
import io.novafoundation.nova.core_db.migrations.ChangeTokens_19_20
import io.novafoundation.nova.core_db.migrations.ExtractExternalApiToSeparateTable_35_36
import io.novafoundation.nova.core_db.migrations.FixBrokenForeignKeys_31_32
import io.novafoundation.nova.core_db.migrations.FixMigrationConflicts_13_14
import io.novafoundation.nova.core_db.migrations.GovernanceFlagToEnum_26_27
import io.novafoundation.nova.core_db.migrations.NullableSubstrateAccountId_21_22
import io.novafoundation.nova.core_db.migrations.NullableSubstratePublicKey_15_16
import io.novafoundation.nova.core_db.migrations.RefactorOperations_49_50
import io.novafoundation.nova.core_db.migrations.RemoveChainForeignKeyFromChainAccount_11_12
import io.novafoundation.nova.core_db.migrations.RemoveColorFromChains_17_18
import io.novafoundation.nova.core_db.migrations.StakingRewardPeriods_42_43
import io.novafoundation.nova.core_db.migrations.TransferFiatAmount_40_41
import io.novafoundation.nova.core_db.migrations.WatchOnlyChainAccounts_16_17
import io.novafoundation.nova.core_db.model.AccountLocal
import io.novafoundation.nova.core_db.model.AccountStakingLocal
import io.novafoundation.nova.core_db.model.AssetLocal
import io.novafoundation.nova.core_db.model.BalanceLockLocal
import io.novafoundation.nova.core_db.model.BrowserHostSettingsLocal
import io.novafoundation.nova.core_db.model.CoinPriceLocal
import io.novafoundation.nova.core_db.model.ContributionLocal
import io.novafoundation.nova.core_db.model.CurrencyLocal
import io.novafoundation.nova.core_db.model.DappAuthorizationLocal
import io.novafoundation.nova.core_db.model.ExternalBalanceLocal
import io.novafoundation.nova.core_db.model.FavouriteDAppLocal
import io.novafoundation.nova.core_db.model.GovernanceDAppLocal
import io.novafoundation.nova.core_db.model.NftLocal
import io.novafoundation.nova.core_db.model.NodeLocal
import io.novafoundation.nova.core_db.model.PhishingAddressLocal
import io.novafoundation.nova.core_db.model.PhishingSiteLocal
import io.novafoundation.nova.core_db.model.StakingDashboardItemLocal
import io.novafoundation.nova.core_db.model.StakingRewardPeriodLocal
import io.novafoundation.nova.core_db.model.StorageEntryLocal
import io.novafoundation.nova.core_db.model.TokenLocal
import io.novafoundation.nova.core_db.model.TotalRewardLocal
import io.novafoundation.nova.core_db.model.WalletConnectPairingLocal
import io.novafoundation.nova.core_db.model.chain.ChainAssetLocal
import io.novafoundation.nova.core_db.model.chain.ChainExplorerLocal
import io.novafoundation.nova.core_db.model.chain.ChainExternalApiLocal
import io.novafoundation.nova.core_db.model.chain.ChainLocal
import io.novafoundation.nova.core_db.model.chain.ChainNodeLocal
import io.novafoundation.nova.core_db.model.chain.ChainRuntimeInfoLocal
import io.novafoundation.nova.core_db.model.chain.account.ChainAccountLocal
import io.novafoundation.nova.core_db.model.chain.account.MetaAccountLocal
import io.novafoundation.nova.core_db.model.chain.account.ProxyAccountLocal
import io.novafoundation.nova.core_db.model.operation.DirectRewardTypeLocal
import io.novafoundation.nova.core_db.model.operation.ExtrinsicTypeLocal
import io.novafoundation.nova.core_db.model.operation.OperationBaseLocal
import io.novafoundation.nova.core_db.model.operation.PoolRewardTypeLocal
import io.novafoundation.nova.core_db.model.operation.SwapTypeLocal
import io.novafoundation.nova.core_db.model.operation.TransferTypeLocal

@Database(
    version = 59,
    entities = [
        AccountLocal::class,
        NodeLocal::class,
        AssetLocal::class,
        TokenLocal::class,
        PhishingAddressLocal::class,
        StorageEntryLocal::class,
        AccountStakingLocal::class,
        TotalRewardLocal::class,
        OperationBaseLocal::class,
        TransferTypeLocal::class,
        DirectRewardTypeLocal::class,
        PoolRewardTypeLocal::class,
        ExtrinsicTypeLocal::class,
        SwapTypeLocal::class,
        ChainLocal::class,
        ChainNodeLocal::class,
        ChainAssetLocal::class,
        ChainRuntimeInfoLocal::class,
        ChainExplorerLocal::class,
        ChainExternalApiLocal::class,
        MetaAccountLocal::class,
        ChainAccountLocal::class,
        DappAuthorizationLocal::class,
        NftLocal::class,
        PhishingSiteLocal::class,
        FavouriteDAppLocal::class,
        CurrencyLocal::class,
        BalanceLockLocal::class,
        ContributionLocal::class,
        GovernanceDAppLocal::class,
        BrowserHostSettingsLocal::class,
        WalletConnectPairingLocal::class,
        CoinPriceLocal::class,
        StakingDashboardItemLocal::class,
        StakingRewardPeriodLocal::class,
        ExternalBalanceLocal::class,
        ProxyAccountLocal::class
    ],
)
@TypeConverters(
    LongMathConverters::class,
    NetworkTypeConverters::class,
    OperationConverters::class,
    CryptoTypeConverters::class,
    NftConverters::class,
    MetaAccountTypeConverters::class,
    CurrencyConverters::class,
    AssetConverters::class,
    ExternalApiConverters::class,
    ChainConverters::class,
    ExternalBalanceTypeConverters::class,
    ProxyAccountConverters::class
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
                    AppDatabase::class.java,
                    "app.db"
                )
                    .addMigrations(AddDAppAuthorizations_1_2, AssetTypes_2_3, ChangeAsset_3_4)
                    .addMigrations(AddChainColor_4_5, AddNfts_5_6, AddSitePhishing_6_7, AddBuyProviders_7_8, BetterChainDiffing_8_9)
                    .addMigrations(AddFavouriteDApps_9_10, ChangeDAppAuthorization_10_11, RemoveChainForeignKeyFromChainAccount_11_12)
                    .addMigrations(AddAdditionalFieldToChains_12_13, FixMigrationConflicts_13_14, AddMetaAccountType_14_15)
                    .addMigrations(NullableSubstratePublicKey_15_16, WatchOnlyChainAccounts_16_17, RemoveColorFromChains_17_18)
                    .addMigrations(AddCurrencies_18_19, ChangeTokens_19_20, ChangeChainNodes_20_21)
                    .addMigrations(NullableSubstrateAccountId_21_22, AddLocks_22_23, AddContributions_23_24)
                    .addMigrations(AddGovernanceFlagToChains_24_25, AddGovernanceDapps_25_26, GovernanceFlagToEnum_26_27)
                    .addMigrations(AddGovernanceExternalApiToChain_27_28)
                    .addMigrations(AddSourceToLocalAsset_28_29, AddTransferApisTable_29_30, AddEnabledColumnToChainAssets_30_31)
                    .addMigrations(FixBrokenForeignKeys_31_32, AddVersioningToGovernanceDapps_32_33)
                    .addMigrations(AddGovernanceNetworkToExternalApi_33_34, AddBrowserHostSettings_34_35)
                    .addMigrations(ExtractExternalApiToSeparateTable_35_36, AddRuntimeFlagToChains_36_37)
                    .addMigrations(AddExtrinsicContentField_37_38, AddNodeSelectionStrategyField_38_39)
                    .addMigrations(AddWalletConnectSessions_39_40, TransferFiatAmount_40_41)
                    .addMigrations(AddStakingDashboardItems_41_42, StakingRewardPeriods_42_43)
                    .addMigrations(AddRewardAccountToStakingDashboard_43_44, AddStakingTypeToTotalRewards_44_45, AddExternalBalances_45_46)
                    .addMigrations(AddPoolIdToOperations_46_47, AddEventIdToOperation_47_48, AddSwapOption_48_49)
                    .addMigrations(RefactorOperations_49_50, AddTransactionVersionToRuntime_50_51, AddBalanceModesToAssets_51_52)
                    .addMigrations(ChangeSessionTopicToParing_52_53, AddConnectionStateToChains_53_54, AddProxyAccount_54_55)
                    .addMigrations(AddFungibleNfts_55_56, ChainPushSupport_56_57)
                    .addMigrations(AddGloballyUniqueIdToMetaAccounts_57_58, AddGloballyUniqueIdToMetaAccounts_58_59)
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

    abstract fun chainAssetDao(): ChainAssetDao

    abstract fun metaAccountDao(): MetaAccountDao

    abstract fun dAppAuthorizationDao(): DappAuthorizationDao

    abstract fun nftDao(): NftDao

    abstract fun phishingSitesDao(): PhishingSitesDao

    abstract fun favouriteDAppsDao(): FavouriteDAppsDao

    abstract fun currencyDao(): CurrencyDao

    abstract fun lockDao(): LockDao

    abstract fun contributionDao(): ContributionDao

    abstract fun governanceDAppsDao(): GovernanceDAppsDao

    abstract fun browserHostSettingsDao(): BrowserHostSettingsDao

    abstract fun walletConnectSessionsDao(): WalletConnectSessionsDao

    abstract fun stakingDashboardDao(): StakingDashboardDao

    abstract fun coinPriceDao(): CoinPriceDao

    abstract fun stakingRewardPeriodDao(): StakingRewardPeriodDao

    abstract fun externalBalanceDao(): ExternalBalanceDao
}
