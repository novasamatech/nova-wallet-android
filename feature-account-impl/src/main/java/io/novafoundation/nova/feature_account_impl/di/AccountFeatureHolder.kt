package io.novafoundation.nova.feature_account_impl.di

import io.novafoundation.nova.common.di.FeatureApiHolder
import io.novafoundation.nova.common.di.FeatureContainer
import io.novafoundation.nova.common.di.scope.ApplicationScope
import io.novafoundation.nova.core_db.di.DbApi
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.SelectAddressCommunicator
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.feature_account_impl.presentation.AdvancedEncryptionCommunicator
import io.novafoundation.nova.feature_account_impl.presentation.paritySigner.ParitySignerSignInterScreenCommunicator
import io.novafoundation.nova.runtime.di.RuntimeApi
import javax.inject.Inject

@ApplicationScope
class AccountFeatureHolder @Inject constructor(
    featureContainer: FeatureContainer,
    private val accountRouter: AccountRouter,
    private val advancedEncryptionCommunicator: AdvancedEncryptionCommunicator,
    private val paritySignerSignCommunicator: ParitySignerSignInterScreenCommunicator,
    private val selectAddressCommunicator: SelectAddressCommunicator
) : FeatureApiHolder(featureContainer) {

    override fun initializeDependencies(): Any {
        val accountFeatureDependencies = DaggerAccountFeatureComponent_AccountFeatureDependenciesComponent.builder()
            .commonApi(commonApi())
            .dbApi(getFeature(DbApi::class.java))
            .runtimeApi(getFeature(RuntimeApi::class.java))
            .build()

        return DaggerAccountFeatureComponent.factory()
            .create(
                accountRouter = accountRouter,
                advancedEncryptionCommunicator = advancedEncryptionCommunicator,
                paritySignerSignInterScreenCommunicator = paritySignerSignCommunicator,
                selectAddressCommunicator = selectAddressCommunicator,
                deps = accountFeatureDependencies
            )
    }
}
