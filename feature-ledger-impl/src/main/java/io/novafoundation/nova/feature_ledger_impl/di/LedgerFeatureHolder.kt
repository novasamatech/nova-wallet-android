package io.novafoundation.nova.feature_ledger_impl.di

import io.novafoundation.nova.common.di.FeatureApiHolder
import io.novafoundation.nova.common.di.FeatureContainer
import io.novafoundation.nova.common.di.scope.ApplicationScope
import io.novafoundation.nova.core_db.di.DbApi
import io.novafoundation.nova.feature_account_api.di.AccountFeatureApi
import io.novafoundation.nova.feature_account_api.presenatation.sign.LedgerSignCommunicator
import io.novafoundation.nova.feature_ledger_core.di.LedgerCoreApi
import io.novafoundation.nova.feature_ledger_impl.presentation.LedgerRouter
import io.novafoundation.nova.feature_ledger_impl.presentation.account.connect.legacy.SelectLedgerAddressInterScreenCommunicator
import io.novafoundation.nova.feature_wallet_api.di.WalletFeatureApi
import io.novafoundation.nova.runtime.di.RuntimeApi
import javax.inject.Inject

@ApplicationScope
class LedgerFeatureHolder @Inject constructor(
    featureContainer: FeatureContainer,
    private val router: LedgerRouter,
    private val selectLedgerAddressInterScreenCommunicator: SelectLedgerAddressInterScreenCommunicator,
    private val signInterScreenCommunicator: LedgerSignCommunicator,
) : FeatureApiHolder(featureContainer) {

    override fun initializeDependencies(): Any {
        val accountFeatureDependencies = DaggerLedgerFeatureComponent_LedgerFeatureDependenciesComponent.builder()
            .commonApi(commonApi())
            .dbApi(getFeature(DbApi::class.java))
            .runtimeApi(getFeature(RuntimeApi::class.java))
            .walletFeatureApi(getFeature(WalletFeatureApi::class.java))
            .accountFeatureApi(getFeature(AccountFeatureApi::class.java))
            .ledgerCoreApi(getFeature(LedgerCoreApi::class.java))
            .build()

        return DaggerLedgerFeatureComponent.factory()
            .create(
                accountFeatureDependencies,
                router,
                selectLedgerAddressInterScreenCommunicator,
                signInterScreenCommunicator
            )
    }
}
