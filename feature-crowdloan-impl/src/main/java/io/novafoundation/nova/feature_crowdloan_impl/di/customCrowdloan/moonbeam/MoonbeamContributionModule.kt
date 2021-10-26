package io.novafoundation.nova.feature_crowdloan_impl.di.customCrowdloan.moonbeam

import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoSet
import io.novafoundation.nova.common.data.network.HttpExceptionHandler
import io.novafoundation.nova.common.data.network.NetworkApiCreator
import io.novafoundation.nova.common.data.secrets.v2.SecretStoreV2
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.mixin.api.CustomDialogDisplayer
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_crowdloan_impl.data.CrowdloanSharedState
import io.novafoundation.nova.feature_crowdloan_impl.data.network.api.moonbeam.MoonbeamApi
import io.novafoundation.nova.feature_crowdloan_impl.di.customCrowdloan.CustomContributeFactory
import io.novafoundation.nova.feature_crowdloan_impl.domain.contribute.custom.moonbeam.MoonbeamCrowdloanInteractor
import io.novafoundation.nova.feature_crowdloan_impl.domain.contribute.custom.moonbeam.MoonbeamPrivateSignatureProvider
import io.novafoundation.nova.feature_crowdloan_impl.presentation.CrowdloanRouter
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.custom.moonbeam.MoonbeamCrowdloanSubmitter
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.custom.moonbeam.MoonbeamStartFlowInterceptor

@Module
class MoonbeamContributionModule {

    @Provides
    @FeatureScope
    fun provideMoonbeamApi(
        networkApiCreator: NetworkApiCreator,
    ) = networkApiCreator.create(MoonbeamApi::class.java)

    @Provides
    @FeatureScope
    fun provideMoonbeamInteractor(
        accountRepository: AccountRepository,
        extrinsicService: ExtrinsicService,
        moonbeamApi: MoonbeamApi,
        selectedAssetSharedState: CrowdloanSharedState,
        httpExceptionHandler: HttpExceptionHandler,
        secretStoreV2: SecretStoreV2,
    ) = MoonbeamCrowdloanInteractor(
        accountRepository,
        extrinsicService,
        moonbeamApi,
        selectedAssetSharedState,
        httpExceptionHandler,
        secretStoreV2
    )

    @Provides
    @FeatureScope
    fun provideMoonbeamSubmitter() = MoonbeamCrowdloanSubmitter()

    @Provides
    @FeatureScope
    fun provideMoonbeamStartFlowInterceptor(
        router: CrowdloanRouter,
        resourceManager: ResourceManager,
        interactor: MoonbeamCrowdloanInteractor,
        customDialogDisplayer: CustomDialogDisplayer.Presentation,
    ) = MoonbeamStartFlowInterceptor(
        crowdloanRouter = router,
        resourceManager = resourceManager,
        moonbeamInteractor = interactor,
        customDialogDisplayer = customDialogDisplayer,
    )

    @Provides
    @FeatureScope
    fun provideMoonbeamPrivateSignatureProvider(
        interactor: MoonbeamCrowdloanInteractor,
    ) = MoonbeamPrivateSignatureProvider(interactor)

    @Provides
    @FeatureScope
    @IntoSet
    fun provideMoonbeamFactory(
        submitter: MoonbeamCrowdloanSubmitter,
        moonbeamStartFlowInterceptor: MoonbeamStartFlowInterceptor,
        privateSignatureProvider: MoonbeamPrivateSignatureProvider,
    ): CustomContributeFactory = MoonbeamContributeFactory(
        submitter = submitter,
        startFlowInterceptor = moonbeamStartFlowInterceptor,
        privateCrowdloanSignatureProvider = privateSignatureProvider
    )
}
