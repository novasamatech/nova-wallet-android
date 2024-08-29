package io.novafoundation.nova.feature_governance_impl.presentation.tindergov.cards.di

import androidx.fragment.app.Fragment
import dagger.BindsInstance
import dagger.Subcomponent
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.list.ReferendaListFragment
import io.novafoundation.nova.feature_governance_impl.presentation.tindergov.cards.TinderGovCardsFragment

@Subcomponent(
    modules = [
        TinderGovCardsModule::class
    ]
)
@ScreenScope
interface TinderGovCardsComponent {

    @Subcomponent.Factory
    interface Factory {

        fun create(
            @BindsInstance fragment: Fragment,
        ): TinderGovCardsComponent
    }

    fun inject(fragment: TinderGovCardsFragment)
}
