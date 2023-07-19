package io.novafoundation.nova.common.di.modules

import dagger.Module
import dagger.Provides
import io.novafoundation.nova.common.di.scope.ApplicationScope
import io.novafoundation.nova.common.view.parallaxCard.BackingParallaxCardLruCache

@Module()
class ParallaxCardModule {

    @Provides
    @ApplicationScope
    fun provideBackingParallaxCardLruCache() = BackingParallaxCardLruCache(8)
}
