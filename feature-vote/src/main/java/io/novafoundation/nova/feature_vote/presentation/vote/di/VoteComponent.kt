package io.novafoundation.nova.feature_vote.presentation.vote.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_vote.presentation.vote.VoteFragment

@Subcomponent(
    modules = [
        VoteModule::class
    ]
)
@ScreenScope
interface VoteComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment
        ): VoteComponent
    }

    fun inject(fragment: VoteFragment)
}
