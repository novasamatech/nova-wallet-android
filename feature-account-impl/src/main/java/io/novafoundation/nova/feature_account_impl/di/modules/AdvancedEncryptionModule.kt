package io.novafoundation.nova.feature_account_impl.di.modules

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.data.memory.ComputationalCache
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_account_impl.domain.common.AdvancedEncryptionSelectionStoreProvider

@Module
class AdvancedEncryptionStoreModule {

    @Provides
    @FeatureScope
    fun provideAdvancedEncryptionSelectionStoreProvider(
        computationalCache: ComputationalCache
    ): AdvancedEncryptionSelectionStoreProvider {
        return AdvancedEncryptionSelectionStoreProvider(computationalCache)
    }
}
