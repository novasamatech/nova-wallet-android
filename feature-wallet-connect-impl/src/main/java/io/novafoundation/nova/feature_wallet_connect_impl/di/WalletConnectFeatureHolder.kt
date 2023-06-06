package io.novafoundation.nova.feature_wallet_connect_impl.di

import io.novafoundation.nova.caip.di.CaipApi
import io.novafoundation.nova.common.di.FeatureApiHolder
import io.novafoundation.nova.common.di.FeatureContainer
import io.novafoundation.nova.common.di.scope.ApplicationScope
import io.novafoundation.nova.core_db.di.DbApi
import io.novafoundation.nova.feature_account_api.di.AccountFeatureApi
import io.novafoundation.nova.feature_external_sign_api.di.ExternalSignFeatureApi
import io.novafoundation.nova.feature_external_sign_api.model.ExternalSignCommunicator
import io.novafoundation.nova.feature_wallet_connect_impl.WalletConnectRouter
import io.novafoundation.nova.feature_wallet_connect_impl.presentation.sessions.approve.ApproveSessionCommunicator
import io.novafoundation.nova.runtime.di.RuntimeApi
import javax.inject.Inject

@ApplicationScope
class WalletConnectFeatureHolder @Inject constructor(
    featureContainer: FeatureContainer,
    private val router: WalletConnectRouter,
    private val signCommunicator: ExternalSignCommunicator,
    private val approveSessionCommunicator: ApproveSessionCommunicator,
) : FeatureApiHolder(featureContainer) {

    override fun initializeDependencies(): Any {
        val deps = DaggerWalletConnectFeatureComponent_WalletConnectFeatureDependenciesComponent.builder()
            .commonApi(commonApi())
            .dbApi(getFeature(DbApi::class.java))
            .accountFeatureApi(getFeature(AccountFeatureApi::class.java))
            .runtimeApi(getFeature(RuntimeApi::class.java))
            .caipApi(getFeature(CaipApi::class.java))
            .externalSignFeatureApi(getFeature(ExternalSignFeatureApi::class.java))
            .build()

        return DaggerWalletConnectFeatureComponent.factory()
            .create(router, signCommunicator, approveSessionCommunicator, deps)
    }
}
