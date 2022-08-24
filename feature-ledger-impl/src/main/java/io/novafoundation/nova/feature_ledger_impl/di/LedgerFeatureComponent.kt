package io.novafoundation.nova.feature_ledger_impl.di

import dagger.BindsInstance
import dagger.Component
import io.novafoundation.nova.common.di.CommonApi
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_ledger_api.di.LedgerFeatureApi
import io.novafoundation.nova.feature_ledger_impl.presentation.LedgerRouter
import io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.fillWallet.di.FillWalletImportLedgerComponent
import io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.selectAddress.di.SelectAddressImportLedgerComponent
import io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.selectLedger.di.SelectLedgerImportLedgerComponent
import io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.start.di.StartImportLedgerComponent
import io.novafoundation.nova.feature_wallet_api.di.WalletFeatureApi
import io.novafoundation.nova.runtime.di.RuntimeApi

@Component(
    dependencies = [
        LedgerFeatureDependencies::class,
    ],
    modules = [
        LedgerFeatureModule::class,
    ]
)
@FeatureScope
interface LedgerFeatureComponent : LedgerFeatureApi {

    @Component.Factory
    interface Factory {

        fun create(
            deps: LedgerFeatureDependencies,
            @BindsInstance router: LedgerRouter
        ): LedgerFeatureComponent
    }

    fun startImportLedgerComponentFactory(): StartImportLedgerComponent.Factory
    fun fillWalletImportLedgerComponentFactory(): FillWalletImportLedgerComponent.Factory

    fun selectLedgerImportComponentFactory(): SelectLedgerImportLedgerComponent.Factory
    fun selectAddressImportLedgerComponentFactory(): SelectAddressImportLedgerComponent.Factory

    @Component(
        dependencies = [
            CommonApi::class,
            RuntimeApi::class,
            WalletFeatureApi::class,
        ]
    )
    interface LedgerFeatureDependenciesComponent : LedgerFeatureDependencies
}
