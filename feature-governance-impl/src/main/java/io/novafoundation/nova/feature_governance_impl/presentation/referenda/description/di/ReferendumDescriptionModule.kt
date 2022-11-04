package io.novafoundation.nova.feature_governance_impl.presentation.referenda.description.di

import android.content.Context
import android.text.util.Linkify
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import coil.ImageLoader
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import io.noties.markwon.Markwon
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.image.coil.CoilImagesPlugin
import io.noties.markwon.linkify.LinkifyPlugin
import io.novafoundation.nova.common.di.viewmodel.ViewModelKey
import io.novafoundation.nova.common.di.viewmodel.ViewModelModule
import io.novafoundation.nova.feature_governance_impl.markdown.StylePlugin
import io.novafoundation.nova.feature_governance_impl.presentation.GovernanceRouter
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.description.ReferendumDescriptionPayload
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.description.ReferendumDescriptionViewModel

@Module(includes = [ViewModelModule::class])
class ReferendumDescriptionModule {

    @Provides
    fun provideMarkwon(context: Context, imageLoader: ImageLoader): Markwon {
        return Markwon.builder(context)
            .usePlugin(LinkifyPlugin.create(Linkify.EMAIL_ADDRESSES or Linkify.WEB_URLS))
            .usePlugin(StylePlugin(context))
            .usePlugin(CoilImagesPlugin.create(context, imageLoader))
            .usePlugin(StrikethroughPlugin.create())
            .usePlugin(TablePlugin.create(context))
            .build()
    }

    @Provides
    @IntoMap
    @ViewModelKey(ReferendumDescriptionViewModel::class)
    fun provideViewModel(
        router: GovernanceRouter,
        payload: ReferendumDescriptionPayload,
        markwon: Markwon
    ): ViewModel {
        return ReferendumDescriptionViewModel(
            router = router,
            payload = payload,
            markwon = markwon
        )
    }

    @Provides
    fun provideViewModelCreator(
        fragment: Fragment,
        viewModelFactory: ViewModelProvider.Factory,
    ): ReferendumDescriptionViewModel {
        return ViewModelProvider(fragment, viewModelFactory).get(ReferendumDescriptionViewModel::class.java)
    }
}
