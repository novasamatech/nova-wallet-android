package io.novafoundation.nova.feature_account_impl.presentation.account.advancedEncryption.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_account_impl.presentation.account.advancedEncryption.AdvancedEncryptionFragment
import io.novafoundation.nova.feature_account_impl.presentation.account.advancedEncryption.AdvancedEncryptionPayload

@Subcomponent(
    modules = [
        AdvancedEncryptionModule::class
    ]
)
@ScreenScope
interface AdvancedEncryptionComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
            @BindsInstance payload: AdvancedEncryptionPayload
        ): AdvancedEncryptionComponent
    }

    fun inject(fragment: AdvancedEncryptionFragment)
}
