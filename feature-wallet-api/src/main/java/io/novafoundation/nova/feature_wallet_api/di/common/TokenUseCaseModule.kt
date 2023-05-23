package io.novafoundation.nova.feature_wallet_api.di.common

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_wallet_api.domain.TokenUseCase
import io.novafoundation.nova.feature_wallet_api.domain.implementations.SharedStateTokenUseCase
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.TokenRepository
import io.novafoundation.nova.runtime.state.SelectedAssetOptionSharedState

@Module
class TokenUseCaseModule {

    @Provides
    @FeatureScope
    fun provideTokenUseCase(
        tokenRepository: TokenRepository,
        sharedState: SelectedAssetOptionSharedState<*>,
    ): TokenUseCase = SharedStateTokenUseCase(
        tokenRepository,
        sharedState
    )
}
