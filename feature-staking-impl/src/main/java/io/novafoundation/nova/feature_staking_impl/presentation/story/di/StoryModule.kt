package io.novafoundation.nova.feature_staking_impl.presentation.story.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.feature_staking_impl.presentation.StakingRouter
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.model.StakingStoryModel
import io.novafoundation.nova.feature_staking_impl.presentation.story.StoryViewModel

@Module(includes = [ViewModelModule::class])
class StoryModule {

    @Provides
    @IntoMap
    @ViewModelKey(StoryViewModel::class)
    fun provideViewModel(
        router: StakingRouter,
        story: StakingStoryModel
    ): ViewModel {
        return StoryViewModel(router, story)
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory
    ): StoryViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(StoryViewModel::class.java)
    }
}
