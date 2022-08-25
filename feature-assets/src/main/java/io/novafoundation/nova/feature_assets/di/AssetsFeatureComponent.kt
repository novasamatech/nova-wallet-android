package io.novafoundation.nova.feature_assets.di

import dagger.BindsInstance
import dagger.Component
import io.novafoundation.nova.common.di.CommonApi
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.core_db.di.DbApi
import io.novafoundation.nova.feature_account_api.di.AccountFeatureApi
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.SelectAddressCommunicator
import io.novafoundation.nova.feature_assets.presentation.AssetsRouter
import io.novafoundation.nova.feature_assets.presentation.balance.detail.di.BalanceDetailComponent
import io.novafoundation.nova.feature_assets.presentation.balance.filters.di.AssetFiltersComponent
import io.novafoundation.nova.feature_assets.presentation.balance.list.di.BalanceListComponent
import io.novafoundation.nova.feature_assets.presentation.balance.list.view.GoToNftsView
import io.novafoundation.nova.feature_assets.presentation.balance.search.di.AssetSearchComponent
import io.novafoundation.nova.feature_assets.presentation.receive.di.ReceiveComponent
import io.novafoundation.nova.feature_assets.presentation.send.amount.di.SelectSendComponent
import io.novafoundation.nova.feature_assets.presentation.send.confirm.di.ConfirmSendComponent
import io.novafoundation.nova.feature_assets.presentation.transaction.detail.di.ExtrinsicDetailComponent
import io.novafoundation.nova.feature_assets.presentation.transaction.detail.di.RewardDetailComponent
import io.novafoundation.nova.feature_assets.presentation.transaction.detail.di.TransactionDetailComponent
import io.novafoundation.nova.feature_assets.presentation.transaction.filter.di.TransactionHistoryFilterComponent
import io.novafoundation.nova.feature_currency_api.di.CurrencyFeatureApi
import io.novafoundation.nova.feature_nft_api.NftFeatureApi
import io.novafoundation.nova.feature_wallet_api.di.WalletFeatureApi
import io.novafoundation.nova.runtime.di.RuntimeApi

@Component(
    dependencies = [
        AssetsFeatureDependencies::class
    ],
    modules = [
        AssetsFeatureModule::class,
    ]
)
@FeatureScope
interface AssetsFeatureComponent : AssetsFeatureApi {

    fun balanceListComponentFactory(): BalanceListComponent.Factory

    fun balanceDetailComponentFactory(): BalanceDetailComponent.Factory

    fun chooseAmountComponentFactory(): SelectSendComponent.Factory

    fun confirmTransferComponentFactory(): ConfirmSendComponent.Factory

    fun transactionDetailComponentFactory(): TransactionDetailComponent.Factory

    fun transactionHistoryComponentFactory(): TransactionHistoryFilterComponent.Factory

    fun rewardDetailComponentFactory(): RewardDetailComponent.Factory

    fun extrinsicDetailComponentFactory(): ExtrinsicDetailComponent.Factory

    fun receiveComponentFactory(): ReceiveComponent.Factory

    fun assetFiltersComponentFactory(): AssetFiltersComponent.Factory

    fun assetSearchComponentFactory(): AssetSearchComponent.Factory

    fun inject(view: GoToNftsView)

    @Component.Factory
    interface Factory {

        fun create(
            @BindsInstance accountRouter: AssetsRouter,
            @BindsInstance selectAddressCommunicator: SelectAddressCommunicator,
            deps: AssetsFeatureDependencies
        ): AssetsFeatureComponent
    }

    @Component(
        dependencies = [
            CommonApi::class,
            DbApi::class,
            RuntimeApi::class,
            NftFeatureApi::class,
            WalletFeatureApi::class,
            AccountFeatureApi::class,
            CurrencyFeatureApi::class
        ]
    )
    interface AssetsFeatureDependenciesComponent : AssetsFeatureDependencies
}
