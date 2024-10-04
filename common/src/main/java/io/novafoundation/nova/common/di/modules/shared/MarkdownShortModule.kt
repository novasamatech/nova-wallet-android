package io.novafoundation.nova.common.di.modules.shared

import android.content.Context
import android.text.util.Linkify
import dagger.Module
import dagger.Provides
import io.noties.markwon.Markwon
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.html.HtmlPlugin
import io.noties.markwon.linkify.LinkifyPlugin
import io.novafoundation.nova.common.di.scope.ScreenScope
import io.novafoundation.nova.common.utils.markdown.RemoveHtmlTagsPlugin
import io.novafoundation.nova.common.utils.markdown.LinkStylePlugin

private const val IMG_HTML_TAG = "img"
private const val TABLE_HTML_TAG = "table"

@Module
class MarkdownShortModule {

    @Provides
    @ScreenScope
    fun provideMarkwon(context: Context): Markwon {
        return Markwon.builder(context)
            .usePlugin(RemoveHtmlTagsPlugin(IMG_HTML_TAG, TABLE_HTML_TAG))
            .usePlugin(LinkifyPlugin.create(Linkify.EMAIL_ADDRESSES or Linkify.WEB_URLS))
            .usePlugin(LinkStylePlugin(context))
            .usePlugin(StrikethroughPlugin.create())
            .usePlugin(HtmlPlugin.create())
            .build()
    }
}
