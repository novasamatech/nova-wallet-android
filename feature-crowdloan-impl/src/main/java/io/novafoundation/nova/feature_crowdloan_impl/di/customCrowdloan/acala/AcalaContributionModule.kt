package io.novafoundation.nova.feature_crowdloan_impl.di.customCrowdloan.acala

import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoSet
import io.novafoundation.nova.common.data.network.HttpExceptionHandler
import io.novafoundation.nova.common.data.network.NetworkApiCreator
import io.novafoundation.nova.common.data.secrets.v2.SecretStoreV2
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_crowdloan_impl.data.CrowdloanSharedState
import io.novafoundation.nova.feature_crowdloan_impl.data.network.api.karura.AcalaApi
import io.novafoundation.nova.feature_crowdloan_impl.di.customCrowdloan.CustomContributeFactory
import io.novafoundation.nova.feature_crowdloan_impl.domain.contribute.custom.karura.AcalaContributeInteractor
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.custom.karura.AcalaContributeSubmitter
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

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
        chainRegistry: ChainRegistry,
        accountRepository: AccountRepository,
    ) = AcalaContributeInteractor(acalaApi, httpExceptionHandler, accountRepository, secretStoreV2, chainRegistry, selectAssetSharedState)

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
