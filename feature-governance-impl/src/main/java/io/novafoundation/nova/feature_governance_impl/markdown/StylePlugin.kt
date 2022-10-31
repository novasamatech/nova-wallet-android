package io.novafoundation.nova.feature_governance_impl.markdown

import android.content.Context
import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.core.MarkwonTheme
import io.novafoundation.nova.feature_governance_impl.R

class StylePlugin(private val context: Context) : AbstractMarkwonPlugin() {

    override fun configureTheme(builder: MarkwonTheme.Builder) {
        builder.isLinkUnderlined(false)
            .linkColor(context.getColor(R.color.accentBlue))
    }
}
