package io.novafoundation.nova.feature_account_impl.presentation.exporting.json.di

import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoSet
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.common.validation.from
import io.novafoundation.nova.feature_account_impl.domain.account.export.json.validations.ExportJsonPasswordValidation
import io.novafoundation.nova.feature_account_impl.domain.account.export.json.validations.ExportJsonPasswordValidationSystem
import io.novafoundation.nova.feature_account_impl.domain.account.export.json.validations.PasswordMatchConfirmationValidation

@Module
class ValidationsModule {

    @Provides
    @ScreenScope
    @IntoSet
    fun passwordMatchConfirmationValidation(): ExportJsonPasswordValidation = PasswordMatchConfirmationValidation()

    @Provides
    @ScreenScope
    fun provideValidationSystem(
        validations: Set<@JvmSuppressWildcards ExportJsonPasswordValidation>
    ) = ExportJsonPasswordValidationSystem.from(validations)
}
