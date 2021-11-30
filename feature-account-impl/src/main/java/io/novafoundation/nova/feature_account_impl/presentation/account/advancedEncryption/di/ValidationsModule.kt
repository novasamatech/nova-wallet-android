package io.novafoundation.nova.feature_account_impl.presentation.account.advancedEncryption.di

import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoSet
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.common.validation.from
import io.novafoundation.nova.feature_account_impl.domain.account.advancedEncryption.valiadtion.AdvancedEncryptionValidation
import io.novafoundation.nova.feature_account_impl.domain.account.advancedEncryption.valiadtion.AdvancedEncryptionValidationSystem
import io.novafoundation.nova.feature_account_impl.domain.account.advancedEncryption.valiadtion.EthereumDerivationPathValidation
import io.novafoundation.nova.feature_account_impl.domain.account.advancedEncryption.valiadtion.SubstrateDerivationPathValidation

@Module
class ValidationsModule {

    @Provides
    @ScreenScope
    @IntoSet
    fun substrateDerivationPathValidation(): AdvancedEncryptionValidation = SubstrateDerivationPathValidation()

    @Provides
    @ScreenScope
    @IntoSet
    fun ethereumDerivationPathValidation(): AdvancedEncryptionValidation = EthereumDerivationPathValidation()

    @Provides
    @ScreenScope
    fun provideValidationSystem(
        validations: Set<@JvmSuppressWildcards AdvancedEncryptionValidation>
    ) = AdvancedEncryptionValidationSystem.from(validations)
}
