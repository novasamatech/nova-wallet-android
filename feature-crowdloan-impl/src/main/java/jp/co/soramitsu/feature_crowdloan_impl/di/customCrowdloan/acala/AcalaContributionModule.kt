package jp.co.soramitsu.feature_crowdloan_impl.di.customCrowdloan.acala

import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoSet
import jp.co.soramitsu.common.data.network.HttpExceptionHandler
import jp.co.soramitsu.common.data.network.NetworkApiCreator
import jp.co.soramitsu.common.data.secrets.v2.SecretStoreV2
import jp.co.soramitsu.common.di.scope.FeatureScope
import jp.co.soramitsu.common.resources.ResourceManager
import jp.co.soramitsu.feature_account_api.domain.interfaces.AccountRepository
import jp.co.soramitsu.feature_crowdloan_impl.data.CrowdloanSharedState
import jp.co.soramitsu.feature_crowdloan_impl.data.network.api.karura.AcalaApi
import jp.co.soramitsu.feature_crowdloan_impl.di.customCrowdloan.CustomContributeFactory
import jp.co.soramitsu.feature_crowdloan_impl.domain.contribute.custom.karura.AcalaContributeInteractor
import jp.co.soramitsu.feature_crowdloan_impl.presentation.contribute.custom.karura.AcalaContributeSubmitter

@Module
class AcalaContributionModule {

    @Provides
    @FeatureScope
    fun provideAcalaApi(
        networkApiCreator: NetworkApiCreator,
    ) = networkApiCreator.create(AcalaApi::class.java)

    @Provides
    @FeatureScope
    fun provideAcalaInteractor(
        acalaApi: AcalaApi,
        httpExceptionHandler: HttpExceptionHandler,
        secretStoreV2: SecretStoreV2,
        selectAssetSharedState: CrowdloanSharedState,
        accountRepository: AccountRepository,
    ) = AcalaContributeInteractor(acalaApi, httpExceptionHandler, accountRepository, secretStoreV2, selectAssetSharedState)

    @Provides
    @FeatureScope
    fun provideAcalaSubmitter(
        interactor: AcalaContributeInteractor,
    ) = AcalaContributeSubmitter(interactor)

    @Provides
    @FeatureScope
    @IntoSet
    fun provideAcalaFactory(
        submitter: AcalaContributeSubmitter,
        acalaInteractor: AcalaContributeInteractor,
        resourceManager: ResourceManager,
    ): CustomContributeFactory = AcalaContributeFactory(
        submitter = submitter,
        interactor = acalaInteractor,
        resourceManager = resourceManager,
    )

    @Provides
    @FeatureScope
    @IntoSet
    fun provideKaruraFactory(
        submitter: AcalaContributeSubmitter,
        acalaInteractor: AcalaContributeInteractor,
        resourceManager: ResourceManager,
    ): CustomContributeFactory = KaruraContributeFactory(
        submitter = submitter,
        interactor = acalaInteractor,
        resourceManager = resourceManager
    )
}
