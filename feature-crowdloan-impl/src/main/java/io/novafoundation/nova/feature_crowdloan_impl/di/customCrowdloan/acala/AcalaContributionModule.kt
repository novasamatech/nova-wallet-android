package io.novafoundation.nova.feature_crowdloan_impl.di.customCrowdloan.acala

import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoSet
import io.novafoundation.nova.common.data.network.HttpExceptionHandler
import io.novafoundation.nova.common.data.network.NetworkApiCreator
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_account_api.data.signer.SignerProvider
import io.novafoundation.nova.feature_account_api.domain.interfaces.AccountRepository
import io.novafoundation.nova.feature_crowdloan_impl.data.CrowdloanSharedState
import io.novafoundation.nova.feature_crowdloan_impl.data.network.api.acala.AcalaApi
import io.novafoundation.nova.feature_crowdloan_impl.di.customCrowdloan.CustomContributeFactory
import io.novafoundation.nova.feature_crowdloan_impl.domain.contribute.custom.acala.AcalaContributeInteractor
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.custom.acala.bonus.AcalaContributeSubmitter
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.custom.acala.main.confirm.AcalaConfirmContributeCustomization
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.custom.acala.main.confirm.AcalaConfirmContributeViewStateFactory
import io.novafoundation.nova.feature_crowdloan_impl.presentation.contribute.custom.acala.main.select.AcalaSelectContributeCustomization
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
        selectAssetSharedState: CrowdloanSharedState,
        chainRegistry: ChainRegistry,
        accountRepository: AccountRepository,
        signerProvider: SignerProvider
    ) = AcalaContributeInteractor(
        acalaApi = acalaApi,
        httpExceptionHandler = httpExceptionHandler,
        accountRepository = accountRepository,
        chainRegistry = chainRegistry,
        selectedAssetState = selectAssetSharedState,
        signerProvider = signerProvider
    )

    @Provides
    @FeatureScope
    fun provideAcalaSubmitter(
        interactor: AcalaContributeInteractor,
    ) = AcalaContributeSubmitter(interactor)

    @Provides
    @FeatureScope
    fun provideAcalaExtraBonusFlow(
        acalaInteractor: AcalaContributeInteractor,
        resourceManager: ResourceManager,
    ): AcalaExtraBonusFlow = AcalaExtraBonusFlow(
        interactor = acalaInteractor,
        resourceManager = resourceManager,
    )

    @Provides
    @FeatureScope
    fun provideKaruraExtraBonusFlow(
        acalaInteractor: AcalaContributeInteractor,
        resourceManager: ResourceManager,
    ): KaruraExtraBonusFlow = KaruraExtraBonusFlow(
        interactor = acalaInteractor,
        resourceManager = resourceManager,
    )

    @Provides
    @FeatureScope
    fun provideAcalaSelectContributeCustomization(): AcalaSelectContributeCustomization = AcalaSelectContributeCustomization()

    @Provides
    @FeatureScope
    fun provideAcalaConfirmContributeViewStateFactory(
        resourceManager: ResourceManager,
    ) = AcalaConfirmContributeViewStateFactory(resourceManager)

    @Provides
    @FeatureScope
    fun provideAcalaConfirmContributeCustomization(
        viewStateFactory: AcalaConfirmContributeViewStateFactory,
    ): AcalaConfirmContributeCustomization = AcalaConfirmContributeCustomization(viewStateFactory)

    @Provides
    @FeatureScope
    @IntoSet
    fun provideAcalaFactory(
        submitter: AcalaContributeSubmitter,
        acalaExtraBonusFlow: AcalaExtraBonusFlow,
        acalaSelectContributeCustomization: AcalaSelectContributeCustomization,
        acalaConfirmContributeCustomization: AcalaConfirmContributeCustomization,
    ): CustomContributeFactory = AcalaContributeFactory(
        submitter = submitter,
        extraBonusFlow = acalaExtraBonusFlow,
        selectContributeCustomization = acalaSelectContributeCustomization,
        confirmContributeCustomization = acalaConfirmContributeCustomization
    )

    @Provides
    @FeatureScope
    @IntoSet
    fun provideKaruraFactory(
        submitter: AcalaContributeSubmitter,
        karuraExtraBonusFlow: KaruraExtraBonusFlow,
    ): CustomContributeFactory = KaruraContributeFactory(
        submitter = submitter,
        extraBonusFlow = karuraExtraBonusFlow
    )
}
