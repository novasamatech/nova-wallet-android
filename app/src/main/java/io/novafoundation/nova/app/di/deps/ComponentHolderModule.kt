package io.novafoundation.nova.app.di.deps

import dagger.Binds
import dagger.Module
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap
import io.novafoundation.nova.app.App
import io.novafoundation.nova.app.root.di.RootApi
import io.novafoundation.nova.app.root.di.RootFeatureHolder
import io.novafoundation.nova.caip.di.CaipApi
import io.novafoundation.nova.caip.di.CaipFeatureHolder
import io.novafoundation.nova.common.di.FeatureApiHolder
import io.novafoundation.nova.common.di.FeatureContainer
import io.novafoundation.nova.common.di.scope.ApplicationScope
import io.novafoundation.nova.core_db.di.DbApi
import io.novafoundation.nova.core_db.di.DbHolder
import io.novafoundation.nova.feature_account_api.di.AccountFeatureApi
import io.novafoundation.nova.feature_account_impl.di.AccountFeatureHolder
import io.novafoundation.nova.feature_assets.di.AssetsFeatureApi
import io.novafoundation.nova.feature_assets.di.AssetsFeatureHolder
import io.novafoundation.nova.feature_buy_api.di.BuyFeatureApi
import io.novafoundation.nova.feature_buy_impl.di.BuyFeatureHolder
import io.novafoundation.nova.feature_crowdloan_api.di.CrowdloanFeatureApi
import io.novafoundation.nova.feature_crowdloan_impl.di.CrowdloanFeatureHolder
import io.novafoundation.nova.feature_currency_api.di.CurrencyFeatureApi
import io.novafoundation.nova.feature_currency_impl.di.CurrencyFeatureHolder
import io.novafoundation.nova.feature_dapp_api.di.DAppFeatureApi
import io.novafoundation.nova.feature_dapp_impl.di.DAppFeatureHolder
import io.novafoundation.nova.feature_deep_linking.di.DeepLinkingFeatureApi
import io.novafoundation.nova.feature_deep_linking.di.DeepLinkingFeatureHolder
import io.novafoundation.nova.feature_external_sign_api.di.ExternalSignFeatureApi
import io.novafoundation.nova.feature_external_sign_impl.di.ExternalSignFeatureHolder
import io.novafoundation.nova.feature_governance_api.di.GovernanceFeatureApi
import io.novafoundation.nova.feature_governance_impl.di.GovernanceFeatureHolder
import io.novafoundation.nova.feature_ledger_api.di.LedgerFeatureApi
import io.novafoundation.nova.feature_ledger_core.LedgerCoreHolder
import io.novafoundation.nova.feature_ledger_core.di.LedgerCoreApi
import io.novafoundation.nova.feature_ledger_impl.di.LedgerFeatureHolder
import io.novafoundation.nova.feature_nft_api.NftFeatureApi
import io.novafoundation.nova.feature_nft_impl.di.NftFeatureHolder
import io.novafoundation.nova.feature_onboarding_api.di.OnboardingFeatureApi
import io.novafoundation.nova.feature_onboarding_impl.di.OnboardingFeatureHolder
import io.novafoundation.nova.feature_proxy_api.di.ProxyFeatureApi
import io.novafoundation.nova.feature_proxy_impl.di.ProxyFeatureHolder
import io.novafoundation.nova.feature_push_notifications.di.PushNotificationsFeatureApi
import io.novafoundation.nova.feature_push_notifications.di.PushNotificationsFeatureHolder
import io.novafoundation.nova.feature_settings_api.SettingsFeatureApi
import io.novafoundation.nova.feature_settings_impl.di.SettingsFeatureHolder
import io.novafoundation.nova.feature_staking_api.di.StakingFeatureApi
import io.novafoundation.nova.feature_staking_impl.di.StakingFeatureHolder
import io.novafoundation.nova.feature_swap_api.di.SwapFeatureApi
import io.novafoundation.nova.feature_swap_impl.di.SwapFeatureHolder
import io.novafoundation.nova.feature_versions_api.di.VersionsFeatureApi
import io.novafoundation.nova.feature_versions_impl.di.VersionsFeatureHolder
import io.novafoundation.nova.feature_vote.di.VoteFeatureApi
import io.novafoundation.nova.feature_vote.di.VoteFeatureHolder
import io.novafoundation.nova.feature_wallet_api.di.WalletFeatureApi
import io.novafoundation.nova.feature_wallet_connect_api.di.WalletConnectFeatureApi
import io.novafoundation.nova.feature_wallet_connect_impl.di.WalletConnectFeatureHolder
import io.novafoundation.nova.feature_wallet_impl.di.WalletFeatureHolder
import io.novafoundation.nova.runtime.di.RuntimeApi
import io.novafoundation.nova.runtime.di.RuntimeHolder
import io.novafoundation.nova.splash.di.SplashFeatureApi
import io.novafoundation.nova.splash.di.SplashFeatureHolder
import io.novafoundation.nova.web3names.di.Web3NamesApi
import io.novafoundation.nova.web3names.di.Web3NamesHolder

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
    @ClassKey(LedgerCoreApi::class)
    @IntoMap
    fun provideLedgerCore(accountFeatureHolder: LedgerCoreHolder): FeatureApiHolder

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
    @ClassKey(Web3NamesApi::class)
    @IntoMap
    fun provideWeb3Names(web3NamesHolder: Web3NamesHolder): FeatureApiHolder

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

    @ApplicationScope
    @Binds
    @ClassKey(CaipApi::class)
    @IntoMap
    fun provideCaipFeature(holder: CaipFeatureHolder): FeatureApiHolder

    @ApplicationScope
    @Binds
    @ClassKey(ExternalSignFeatureApi::class)
    @IntoMap
    fun provideExternalSignFeature(holder: ExternalSignFeatureHolder): FeatureApiHolder

    @ApplicationScope
    @Binds
    @ClassKey(WalletConnectFeatureApi::class)
    @IntoMap
    fun provideWalletConnectFeature(holder: WalletConnectFeatureHolder): FeatureApiHolder

    @ApplicationScope
    @Binds
    @ClassKey(SettingsFeatureApi::class)
    @IntoMap
    fun provideSettingsFeature(holder: SettingsFeatureHolder): FeatureApiHolder

    @ApplicationScope
    @Binds
    @ClassKey(SwapFeatureApi::class)
    @IntoMap
    fun provideSwapFeature(holder: SwapFeatureHolder): FeatureApiHolder

    @ApplicationScope
    @Binds
    @ClassKey(BuyFeatureApi::class)
    @IntoMap
    fun provideBuyFeature(holder: BuyFeatureHolder): FeatureApiHolder

    @ApplicationScope
    @Binds
    @ClassKey(PushNotificationsFeatureApi::class)
    @IntoMap
    fun providePushNotificationsFeature(holder: PushNotificationsFeatureHolder): FeatureApiHolder

    @ApplicationScope
    @Binds
    @ClassKey(ProxyFeatureApi::class)
    @IntoMap
    fun provideProxyFeature(holder: ProxyFeatureHolder): FeatureApiHolder

    @ApplicationScope
    @Binds
    @ClassKey(DeepLinkingFeatureApi::class)
    @IntoMap
    fun provideDeepLinkingFeatureHolder(holder: DeepLinkingFeatureHolder): FeatureApiHolder
}
