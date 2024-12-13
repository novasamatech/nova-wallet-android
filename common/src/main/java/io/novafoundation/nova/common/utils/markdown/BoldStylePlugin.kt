package io.novafoundation.nova.common.utils.markdown

import android.content.Context
import androidx.annotation.ColorRes
import androidx.annotation.FontRes
import androidx.core.content.res.ResourcesCompat
import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.MarkwonConfiguration
import io.noties.markwon.MarkwonSpansFactory
import io.noties.markwon.RenderProps
import io.noties.markwon.SpanFactory
import io.novafoundation.nova.common.utils.colorSpan
import io.novafoundation.nova.common.utils.fontSpan
import org.commonmark.node.StrongEmphasis

class BoldStylePlugin(
    private val context: Context,
    @FontRes private val typefaceRes: Int,
    @ColorRes private val colorRes: Int
) : AbstractMarkwonPlugin() {

    override fun configureSpansFactory(builder: MarkwonSpansFactory.Builder) {
        builder.setFactory(StrongEmphasis::class.java, BoldSpanFactory(context, typefaceRes, colorRes))
    }
}

private class BoldSpanFactory(private val context: Context, private val typefaceRes: Int, private val colorRes: Int) : SpanFactory {

    override fun getSpans(configuration: MarkwonConfiguration, props: RenderProps): Any {
        val font = ResourcesCompat.getFont(context, typefaceRes)
        return arrayOf(
            fontSpan(font),
            colorSpan(context.getColor(colorRes))
        )
    }
}
