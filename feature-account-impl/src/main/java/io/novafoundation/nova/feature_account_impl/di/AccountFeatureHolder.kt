package io.novafoundation.nova.feature_account_impl.di

import io.novafoundation.nova.common.di.FeatureApiHolder
import io.novafoundation.nova.common.di.FeatureContainer
import io.novafoundation.nova.common.di.scope.ApplicationScope
import io.novafoundation.nova.core_db.di.DbApi
import io.novafoundation.nova.feature_account_api.presenatation.account.wallet.list.SelectAddressCommunicator
import io.novafoundation.nova.feature_account_api.presenatation.sign.LedgerSignCommunicator
import io.novafoundation.nova.feature_account_impl.data.signer.paritySigner.ParitySignerSignCommunicator
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.feature_account_impl.presentation.AdvancedEncryptionCommunicator
import io.novafoundation.nova.feature_currency_api.di.CurrencyFeatureApi
import io.novafoundation.nova.feature_versions_api.di.VersionsFeatureApi
import io.novafoundation.nova.runtime.di.RuntimeApi
import javax.inject.Inject

@ApplicationScope
class AccountFeatureHolder @Inject constructor(
    featureContainer: FeatureContainer,
    private val accountRouter: AccountRouter,
    private val advancedEncryptionCommunicator: AdvancedEncryptionCommunicator,
    private val paritySignerSignCommunicator: ParitySignerSignCommunicator,
    private val ledgerSignCommunicator: LedgerSignCommunicator,
    private val selectAddressCommunicator: SelectAddressCommunicator
) : FeatureApiHolder(featureContainer) {

    override fun initializeDependencies(): Any {
        val accountFeatureDependencies = DaggerAccountFeatureComponent_AccountFeatureDependenciesComponent.builder()
            .commonApi(commonApi())
            .dbApi(getFeature(DbApi::class.java))
            .runtimeApi(getFeature(RuntimeApi::class.java))
            .currencyFeatureApi(getFeature(CurrencyFeatureApi::class.java))
            .versionsFeatureApi(getFeature(VersionsFeatureApi::class.java))
            .build()

        return DaggerAccountFeatureComponent.factory()
            .create(
                accountRouter = accountRouter,
                advancedEncryptionCommunicator = advancedEncryptionCommunicator,
                paritySignerSignInterScreenCommunicator = paritySignerSignCommunicator,
                ledgerSignInterScreenCommunicator = ledgerSignCommunicator,
                selectAddressCommunicator = selectAddressCommunicator,
                deps = accountFeatureDependencies
            )
    }
}
