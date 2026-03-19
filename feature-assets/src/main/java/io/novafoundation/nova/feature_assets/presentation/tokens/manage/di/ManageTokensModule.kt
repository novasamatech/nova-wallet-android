package io.novafoundation.nova.feature_assets.presentation.tokens.manage.di

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.common.presentation.AssetIconProvider
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_assets.data.repository.assetFilters.AssetFiltersRepository
import io.novafoundation.nova.feature_assets.data.repository.assetFilters.DustFilterPreferences
import io.novafoundation.nova.feature_assets.data.repository.defaultTokens.DefaultTokensRepository
import io.novafoundation.nova.feature_assets.data.repository.defaultTokens.LoadMoreTokensPreferences
import io.novafoundation.nova.feature_assets.domain.assets.filters.AssetFiltersInteractor
import io.novafoundation.nova.feature_assets.domain.tokens.manage.ManageTokenInteractor
import io.novafoundation.nova.feature_assets.presentation.AssetsRouter
import io.novafoundation.nova.feature_assets.presentation.tokens.manage.ManageTokensViewModel

@Module(includes = [ViewModelModule::class])
class ManageTokensModule {

    @Provides
    @ScreenScope
    fun provideAssetFiltersInteractor(
        assetFiltersRepository: AssetFiltersRepository
    ) = AssetFiltersInteractor(assetFiltersRepository)

    @Provides
    internal fun provideViewModel(fragment: Fragment, factory: ViewModelProvider.Factory): ManageTokensViewModel {
        return ViewModelProvider(fragment, factory).get(ManageTokensViewModel::class.java)
    }

    @Provides
    @IntoMap
    @ViewModelKey(ManageTokensViewModel::class)
    fun provideViewModel(
        router: AssetsRouter,
        interactor: ManageTokenInteractor,
        assetFiltersInteractor: AssetFiltersInteractor,
        assetIconProvider: AssetIconProvider,
        loadMoreTokensPreferences: LoadMoreTokensPreferences,
        dustFilterPreferences: DustFilterPreferences,
        actionAwaitableMixinFactory: ActionAwaitableMixin.Factory,
        resourceManager: ResourceManager,
        defaultTokensRepository: DefaultTokensRepository,
    ): ViewModel {
        return ManageTokensViewModel(
            router = router,
            interactor = interactor,
            assetFiltersInteractor = assetFiltersInteractor,
            assetIconProvider = assetIconProvider,
            loadMoreTokensPreferences = loadMoreTokensPreferences,
            dustFilterPreferences = dustFilterPreferences,
            actionAwaitableMixinFactory = actionAwaitableMixinFactory,
            resourceManager = resourceManager,
            defaultTokensRepository = defaultTokensRepository,
        )
    }
}
