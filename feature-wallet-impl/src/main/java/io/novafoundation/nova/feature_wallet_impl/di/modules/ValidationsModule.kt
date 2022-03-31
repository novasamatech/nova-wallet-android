package io.novafoundation.nova.feature_wallet_impl.di.modules

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.feature_wallet_api.domain.interfaces.WalletRepository
import io.novafoundation.nova.feature_wallet_api.domain.validation.PhishingValidationFactory

@Module
class ValidationsModule {

    @Provides
    @FeatureScope
    fun providePhishingValidationFactory(
        walletRepository: WalletRepository
    ) = PhishingValidationFactory(walletRepository)
}
