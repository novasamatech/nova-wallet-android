package io.novafoundation.nova.common.utils.markdown

import android.content.Context
import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.core.MarkwonTheme
import io.novafoundation.nova.common.R

class LinkStylePlugin(private val context: Context) : AbstractMarkwonPlugin() {

    override fun configureTheme(builder: MarkwonTheme.Builder) {
        builder.isLinkUnderlined(false)
            .linkColor(context.getColor(R.color.button_background_primary))
    }
}
