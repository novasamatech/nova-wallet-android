package io.novafoundation.nova.feature_assets.di.modules

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_assets.domain.tokens.add.AddTokensInteractor
import io.novafoundation.nova.feature_assets.domain.tokens.add.RealAddTokensInteractor
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry

@Module
class AddTokenModule {

    @Provides
    @FeatureScope
    fun provideInteractor(chainRegistry: ChainRegistry): AddTokensInteractor {
        return RealAddTokensInteractor(chainRegistry)
    }
}
