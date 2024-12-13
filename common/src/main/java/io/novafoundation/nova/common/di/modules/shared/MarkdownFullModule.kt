package io.novafoundation.nova.common.di.modules.shared

import android.content.Context
import android.text.util.Linkify
import coil.ImageLoader
import dagger.Module
import dagger.Provides
import io.noties.markwon.Markwon
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.html.HtmlPlugin
import io.noties.markwon.image.coil.CoilImagesPlugin
import io.noties.markwon.linkify.LinkifyPlugin
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.common.utils.markdown.LinkStylePlugin

@Module
class MarkdownFullModule {

    @Provides
    @ScreenScope
    fun provideMarkwon(context: Context, imageLoader: ImageLoader): Markwon {
        return Markwon.builder(context)
            .usePlugin(LinkifyPlugin.create(Linkify.EMAIL_ADDRESSES or Linkify.WEB_URLS))
            .usePlugin(LinkStylePlugin(context))
            .usePlugin(CoilImagesPlugin.create(context, imageLoader))
            .usePlugin(StrikethroughPlugin.create())
            .usePlugin(TablePlugin.create(context))
            .usePlugin(HtmlPlugin.create())
            .build()
    }
}
