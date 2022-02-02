package io.novafoundation.nova.feature_wallet_impl.di

import dagger.BindsInstance
import dagger.Component
import io.novafoundation.nova.common.di.CommonApi
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.core_db.di.DbApi
import io.novafoundation.nova.feature_account_api.di.AccountFeatureApi
import io.novafoundation.nova.feature_wallet_api.di.WalletFeatureApi
import io.novafoundation.nova.feature_wallet_impl.di.modules.AssetsModule
import io.novafoundation.nova.feature_wallet_impl.di.modules.SendModule
import io.novafoundation.nova.feature_wallet_impl.presentation.WalletRouter
import io.novafoundation.nova.feature_wallet_impl.presentation.balance.detail.di.BalanceDetailComponent
import io.novafoundation.nova.feature_wallet_impl.presentation.balance.filters.di.AssetFiltersComponent
import io.novafoundation.nova.feature_wallet_impl.presentation.balance.list.di.BalanceListComponent
import io.novafoundation.nova.feature_wallet_impl.presentation.receive.di.ReceiveComponent
import io.novafoundation.nova.feature_wallet_impl.presentation.send.amount.di.ChooseAmountComponent
import io.novafoundation.nova.feature_wallet_impl.presentation.send.confirm.di.ConfirmTransferComponent
import io.novafoundation.nova.feature_wallet_impl.presentation.send.recipient.di.ChooseRecipientComponent
import io.novafoundation.nova.feature_wallet_impl.presentation.transaction.detail.di.ExtrinsicDetailComponent
import io.novafoundation.nova.feature_wallet_impl.presentation.transaction.detail.di.RewardDetailComponent
import io.novafoundation.nova.feature_wallet_impl.presentation.transaction.detail.di.TransactionDetailComponent
import io.novafoundation.nova.feature_wallet_impl.presentation.transaction.filter.di.TransactionHistoryFilterComponent
import io.novafoundation.nova.feature_wallet_impl.presentation.transaction.filter.di.TransactionHistoryFilterModule
import io.novafoundation.nova.runtime.di.RuntimeApi

@Component(
    dependencies = [
        WalletFeatureDependencies::class
    ],
    modules = [
        WalletFeatureModule::class,
        AssetsModule::class,
        SendModule::class,
        TransactionHistoryFilterModule::class
    ]
)
@FeatureScope
interface WalletFeatureComponent : WalletFeatureApi {

    fun balanceListComponentFactory(): BalanceListComponent.Factory

    fun balanceDetailComponentFactory(): BalanceDetailComponent.Factory

    fun chooseRecipientComponentFactory(): ChooseRecipientComponent.Factory

    fun chooseAmountComponentFactory(): ChooseAmountComponent.Factory

    fun confirmTransferComponentFactory(): ConfirmTransferComponent.Factory

    fun transactionDetailComponentFactory(): TransactionDetailComponent.Factory

    fun transactionHistoryComponentFactory(): TransactionHistoryFilterComponent.Factory

    fun rewardDetailComponentFactory(): RewardDetailComponent.Factory

    fun extrinsicDetailComponentFactory(): ExtrinsicDetailComponent.Factory

    fun receiveComponentFactory(): ReceiveComponent.Factory

    fun assetFiltersComponentFactory(): AssetFiltersComponent.Factory

    @Component.Factory
    interface Factory {

        fun create(
            @BindsInstance accountRouter: WalletRouter,
            deps: WalletFeatureDependencies
        ): WalletFeatureComponent
    }

    @Component(
        dependencies = [
            CommonApi::class,
            DbApi::class,
            RuntimeApi::class,
            AccountFeatureApi::class
        ]
    )
    interface WalletFeatureDependenciesComponent : WalletFeatureDependencies
}
