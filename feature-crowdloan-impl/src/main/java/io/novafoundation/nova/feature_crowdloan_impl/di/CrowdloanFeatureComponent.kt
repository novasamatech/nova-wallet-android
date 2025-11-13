package io.novafoundation.nova.feature_crowdloan_impl.di

import dagger.BindsInstance
import dagger.Component
import io.novafoundation.nova.common.di.CommonApi
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.core_db.di.DbApi
import io.novafoundation.nova.feature_account_api.di.AccountFeatureApi
import io.novafoundation.nova.feature_crowdloan_api.di.CrowdloanFeatureApi
import io.novafoundation.nova.feature_crowdloan_impl.di.contributions.ContributionsModule
import io.novafoundation.nova.feature_crowdloan_impl.di.validations.CrowdloansValidationsModule
import io.novafoundation.nova.feature_crowdloan_impl.presentation.CrowdloanRouter
import io.novafoundation.nova.feature_crowdloan_impl.presentation.claimControbution.di.ClaimContributionComponent
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.confirm.di.ConfirmContributeComponent
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.custom.di.CustomContributeComponent
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.custom.moonbeam.terms.di.MoonbeamCrowdloanTermsComponent
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.custom.referral.ReferralContributeView
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.select.di.CrowdloanContributeComponent
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contributions.di.UserContributionsComponent
import io.novafoundation.nova.feature_crowdloan_impl.presentation.main.di.CrowdloanComponent
import io.novafoundation.nova.feature_wallet_api.di.WalletFeatureApi
import io.novafoundation.nova.runtime.di.RuntimeApi

@Component(
    dependencies = [
        CrowdloanFeatureDependencies::class
    ],
    modules = [
        CrowdloanFeatureModule::class,
        CrowdloanUpdatersModule::class,
        CrowdloansValidationsModule::class,
        ContributionsModule::class
    ]
)
@FeatureScope
interface CrowdloanFeatureComponent : CrowdloanFeatureApi {

    fun crowdloansFactory(): CrowdloanComponent.Factory

    fun userContributionsFactory(): UserContributionsComponent.Factory

    fun selectContributeFactory(): CrowdloanContributeComponent.Factory

    fun confirmContributeFactory(): ConfirmContributeComponent.Factory

    fun customContributeFactory(): CustomContributeComponent.Factory

    fun moonbeamTermsFactory(): MoonbeamCrowdloanTermsComponent.Factory

    fun claimContributions(): ClaimContributionComponent.Factory

    fun inject(view: ReferralContributeView)

    @Component.Factory
    interface Factory {

        fun create(
            @BindsInstance router: CrowdloanRouter,
            deps: CrowdloanFeatureDependencies,
        ): CrowdloanFeatureComponent
    }

    @Component(
        dependencies = [
            CommonApi::class,
            DbApi::class,
            RuntimeApi::class,
            AccountFeatureApi::class,
            WalletFeatureApi::class
        ]
    )
    interface CrowdloanFeatureDependenciesComponent : CrowdloanFeatureDependencies
}
