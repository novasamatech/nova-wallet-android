package io.novafoundation.nova.feature_account_impl.di.modules

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.SharedState
import io.novafoundation.nova.feature_account_api.data.signer.SeparateFlowSignerState
import io.novafoundation.nova.feature_account_api.data.signer.SigningSharedState
import io.novafoundation.nova.feature_account_impl.data.signer.paritySigner.PolkadotVaultVariantSignCommunicator
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import io.novafoundation.nova.feature_account_impl.presentation.paritySigner.sign.common.QrCodeExpiredPresentableFactory

@Module
class ParitySignerModule {

    @Provides
    @FeatureScope
    fun provideReadOnlySharedState(
        mutableSharedState: SigningSharedState
    ): SharedState<SeparateFlowSignerState> = mutableSharedState

    @Provides
    @FeatureScope
    fun provideQrCodeExpiredPresentableFactory(
        resourceManager: ResourceManager,
        actionAwaitableMixinFactory: ActionAwaitableMixin.Factory,
        router: AccountRouter,
        communicator: PolkadotVaultVariantSignCommunicator
    ) = QrCodeExpiredPresentableFactory(resourceManager, actionAwaitableMixinFactory, router, communicator)
}
