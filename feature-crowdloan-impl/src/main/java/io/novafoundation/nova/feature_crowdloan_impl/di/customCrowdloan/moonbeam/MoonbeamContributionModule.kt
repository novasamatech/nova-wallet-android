package io.novafoundation.nova.feature_crowdloan_impl.di.customCrowdloan.moonbeam

import coil.ImageLoader
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoSet
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.data.network.HttpExceptionHandler
import io.novafoundation.nova.common.data.network.NetworkApiCreator
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.mixin.api.CustomDialogDisplayer
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_account_api.data.extrinsic.ExtrinsicService
import io.novafoundation.nova.feature_account_api.data.signer.SignerProvider
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_crowdloan_impl.data.CrowdloanSharedState
import io.novafoundation.nova.feature_crowdloan_impl.data.network.api.moonbeam.MoonbeamApi
import io.novafoundation.nova.feature_crowdloan_impl.di.customCrowdloan.CustomContributeFactory
import io.novafoundation.nova.feature_crowdloan_impl.domain.contribute.custom.moonbeam.MoonbeamCrowdloanInteractor
import io.novafoundation.nova.feature_crowdloan_impl.domain.contribute.custom.moonbeam.MoonbeamPrivateSignatureProvider
import io.novafoundation.nova.feature_crowdloan_impl.presentation.CrowdloanRouter
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.custom.moonbeam.MoonbeamCrowdloanSubmitter
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.custom.moonbeam.MoonbeamStartFlowInterceptor
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.custom.moonbeam.main.ConfirmContributeMoonbeamCustomization
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.custom.moonbeam.main.MoonbeamMainFlowCustomViewStateFactory
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.custom.moonbeam.main.SelectContributeMoonbeamCustomization
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

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
        chainRegistry: ChainRegistry,
        signerProvider: SignerProvider
    ) = MoonbeamCrowdloanInteractor(
        accountRepository = accountRepository,
        extrinsicService = extrinsicService,
        moonbeamApi = moonbeamApi,
        selectedChainAssetState = selectedAssetSharedState,
        chainRegistry = chainRegistry,
        httpExceptionHandler = httpExceptionHandler,
        signerProvider = signerProvider
    )

    @Provides
    @FeatureScope
    fun provideMoonbeamSubmitter(interactor: MoonbeamCrowdloanInteractor) = MoonbeamCrowdloanSubmitter(interactor)

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
        moonbeamApi: MoonbeamApi,
        httpExceptionHandler: HttpExceptionHandler,
    ) = MoonbeamPrivateSignatureProvider(moonbeamApi, httpExceptionHandler)

    @Provides
    @FeatureScope
    fun provideSelectContributeMoonbeamViewStateFactory(
        interactor: MoonbeamCrowdloanInteractor,
        resourceManager: ResourceManager,
        iconGenerator: AddressIconGenerator,
    ) = MoonbeamMainFlowCustomViewStateFactory(interactor, resourceManager, iconGenerator)

    @Provides
    @FeatureScope
    fun provideSelectContributeMoonbeamCustomization(
        viewStateFactory: MoonbeamMainFlowCustomViewStateFactory,
        imageLoader: ImageLoader,
    ) = SelectContributeMoonbeamCustomization(viewStateFactory, imageLoader)

    @Provides
    @FeatureScope
    fun provideConfirmContributeMoonbeamCustomization(
        viewStateFactory: MoonbeamMainFlowCustomViewStateFactory,
        imageLoader: ImageLoader,
    ) = ConfirmContributeMoonbeamCustomization(viewStateFactory, imageLoader)

    @Provides
    @FeatureScope
    @IntoSet
    fun provideMoonbeamFactory(
        submitter: MoonbeamCrowdloanSubmitter,
        moonbeamStartFlowInterceptor: MoonbeamStartFlowInterceptor,
        privateSignatureProvider: MoonbeamPrivateSignatureProvider,
        selectContributeMoonbeamCustomization: SelectContributeMoonbeamCustomization,
        confirmContributeMoonbeamCustomization: ConfirmContributeMoonbeamCustomization,
    ): CustomContributeFactory = MoonbeamContributeFactory(
        submitter = submitter,
        startFlowInterceptor = moonbeamStartFlowInterceptor,
        privateCrowdloanSignatureProvider = privateSignatureProvider,
        selectContributeCustomization = selectContributeMoonbeamCustomization,
        confirmContributeCustomization = confirmContributeMoonbeamCustomization
    )
}
