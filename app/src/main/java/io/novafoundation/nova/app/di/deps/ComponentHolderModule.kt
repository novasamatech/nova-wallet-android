package io.novafoundation.nova.app.di.deps

import dagger.Binds
import dagger.Module
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap
import io.novafoundation.nova.app.App
import io.novafoundation.nova.app.root.di.RootApi
import io.novafoundation.nova.app.root.di.RootFeatureHolder
import io.novafoundation.nova.common.di.FeatureApiHolder
import io.novafoundation.nova.common.di.FeatureContainer
import io.novafoundation.nova.common.di.scope.ApplicationScope
import io.novafoundation.nova.core_db.di.DbApi
import io.novafoundation.nova.core_db.di.DbHolder
import io.novafoundation.nova.feature_account_api.di.AccountFeatureApi
import io.novafoundation.nova.feature_account_impl.di.AccountFeatureHolder
import io.novafoundation.nova.feature_assets.di.AssetsFeatureApi
import io.novafoundation.nova.feature_assets.di.AssetsFeatureHolder
import io.novafoundation.nova.feature_crowdloan_api.di.CrowdloanFeatureApi
import io.novafoundation.nova.feature_crowdloan_impl.di.CrowdloanFeatureHolder
import io.novafoundation.nova.feature_currency_api.di.CurrencyFeatureApi
import io.novafoundation.nova.feature_currency_impl.di.CurrencyFeatureHolder
import io.novafoundation.nova.feature_dapp_api.di.DAppFeatureApi
import io.novafoundation.nova.feature_dapp_impl.di.DAppFeatureHolder
import io.novafoundation.nova.feature_governance_api.di.GovernanceFeatureApi
import io.novafoundation.nova.feature_governance_impl.di.GovernanceFeatureHolder
import io.novafoundation.nova.feature_ledger_api.di.LedgerFeatureApi
import io.novafoundation.nova.feature_ledger_impl.di.LedgerFeatureHolder
import io.novafoundation.nova.feature_nft_api.NftFeatureApi
import io.novafoundation.nova.feature_nft_impl.di.NftFeatureHolder
import io.novafoundation.nova.feature_onboarding_api.di.OnboardingFeatureApi
import io.novafoundation.nova.feature_onboarding_impl.di.OnboardingFeatureHolder
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureHolder
import io.novafoundation.nova.feature_versions_api.di.VersionsFeatureApi
import io.novafoundation.nova.feature_versions_impl.di.VersionsFeatureHolder
import io.novafoundation.nova.feature_vote.di.VoteFeatureApi
import io.novafoundation.nova.feature_vote.di.VoteFeatureHolder
import io.novafoundation.nova.feature_wallet_api.di.WalletFeatureApi
import io.novafoundation.nova.feature_wallet_impl.di.WalletFeatureHolder
import io.novafoundation.nova.runtime.di.RuntimeApi
import io.novafoundation.nova.runtime.di.RuntimeHolder
import io.novafoundation.nova.splash.di.SplashFeatureApi
import io.novafoundation.nova.splash.di.SplashFeatureHolder

@Module
interface ComponentHolderModule {

    @ApplicationScope
    @Binds
    fun provideFeatureContainer(application: App): FeatureContainer

    @ApplicationScope
    @Binds
    @ClassKey(SplashFeatureApi::class)
    @IntoMap
    fun provideSplashFeatureHolder(splashFeatureHolder: SplashFeatureHolder): FeatureApiHolder

    @ApplicationScope
    @Binds
    @ClassKey(DbApi::class)
    @IntoMap
    fun provideDbFeature(dbHolder: DbHolder): FeatureApiHolder

    @ApplicationScope
    @Binds
    @ClassKey(OnboardingFeatureApi::class)
    @IntoMap
    fun provideOnboardingFeature(onboardingFeatureHolder: OnboardingFeatureHolder): FeatureApiHolder

    @ApplicationScope
    @Binds
    @ClassKey(DAppFeatureApi::class)
    @IntoMap
    fun provideDAppFeature(dAppFeatureHolder: DAppFeatureHolder): FeatureApiHolder

    @ApplicationScope
    @Binds
    @ClassKey(LedgerFeatureApi::class)
    @IntoMap
    fun provideLedgerFeature(accountFeatureHolder: LedgerFeatureHolder): FeatureApiHolder

    @ApplicationScope
    @Binds
    @ClassKey(GovernanceFeatureApi::class)
    @IntoMap
    fun provideGovernanceFeature(accountFeatureHolder: GovernanceFeatureHolder): FeatureApiHolder

    @ApplicationScope
    @Binds
    @ClassKey(AccountFeatureApi::class)
    @IntoMap
    fun provideAccountFeature(accountFeatureHolder: AccountFeatureHolder): FeatureApiHolder

    @ApplicationScope
    @Binds
    @ClassKey(AssetsFeatureApi::class)
    @IntoMap
    fun provideAssetsFeature(holder: AssetsFeatureHolder): FeatureApiHolder

    @ApplicationScope
    @Binds
    @ClassKey(VoteFeatureApi::class)
    @IntoMap
    fun provideVoteFeature(holder: VoteFeatureHolder): FeatureApiHolder

    @ApplicationScope
    @Binds
    @ClassKey(WalletFeatureApi::class)
    @IntoMap
    fun provideWalletFeature(walletFeatureHolder: WalletFeatureHolder): FeatureApiHolder

    @ApplicationScope
    @Binds
    @ClassKey(CurrencyFeatureApi::class)
    @IntoMap
    fun provideCurrencyFeature(currencyFeatureHolder: CurrencyFeatureHolder): FeatureApiHolder

    @ApplicationScope
    @Binds
    @ClassKey(RootApi::class)
    @IntoMap
    fun provideMainFeature(accountFeatureHolder: RootFeatureHolder): FeatureApiHolder

    @ApplicationScope
    @Binds
    @ClassKey(StakingFeatureApi::class)
    @IntoMap
    fun provideStakingFeature(holder: StakingFeatureHolder): FeatureApiHolder

    @ApplicationScope
    @Binds
    @ClassKey(RuntimeApi::class)
    @IntoMap
    fun provideRuntimeFeature(runtimeHolder: RuntimeHolder): FeatureApiHolder

    @ApplicationScope
    @Binds
    @ClassKey(CrowdloanFeatureApi::class)
    @IntoMap
    fun provideCrowdloanFeature(holder: CrowdloanFeatureHolder): FeatureApiHolder

    @ApplicationScope
    @Binds
    @ClassKey(NftFeatureApi::class)
    @IntoMap
    fun provideNftFeature(holder: NftFeatureHolder): FeatureApiHolder

    @ApplicationScope
    @Binds
    @ClassKey(VersionsFeatureApi::class)
    @IntoMap
    fun provideVersionsFeature(holder: VersionsFeatureHolder): FeatureApiHolder
}
