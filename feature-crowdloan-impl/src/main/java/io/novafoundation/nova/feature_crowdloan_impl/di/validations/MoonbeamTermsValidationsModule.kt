package io.novafoundation.nova.feature_crowdloan_impl.di.validations

import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoSet
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.validation.CompositeValidation
import io.novafoundation.nova.feature_crowdloan_impl.domain.contribute.validations.custom.moonbeam.MoonbeamTermsFeeValidation
import io.novafoundation.nova.feature_crowdloan_impl.domain.contribute.validations.custom.moonbeam.MoonbeamTermsValidation
import io.novafoundation.nova.feature_crowdloan_impl.domain.contribute.validations.custom.moonbeam.MoonbeamTermsValidationFailure
import io.novafoundation.nova.feature_crowdloan_impl.domain.contribute.validations.custom.moonbeam.MoonbeamTermsValidationSystem

@Module
class MoonbeamTermsValidationsModule {

    @Provides
    @IntoSet
    @FeatureScope
    fun provideFeesValidation(): MoonbeamTermsValidation = MoonbeamTermsFeeValidation(
        feeExtractor = { it.fee },
        availableBalanceProducer = { it.asset.transferable },
        errorProducer = { MoonbeamTermsValidationFailure.CANNOT_PAY_FEES }
    )

    @Provides
    @FeatureScope
    fun provideValidationSystem(
        contributeValidations: @JvmSuppressWildcards Set<MoonbeamTermsValidation>
    ) = MoonbeamTermsValidationSystem(
        validation = CompositeValidation(contributeValidations.toList())
    )
}
