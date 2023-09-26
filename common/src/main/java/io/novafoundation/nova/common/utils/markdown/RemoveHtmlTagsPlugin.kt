package io.novafoundation.nova.common.utils.markdown

import io.noties.markwon.AbstractMarkwonPlugin

class RemoveHtmlTagsPlugin(vararg tagNames: String) : AbstractMarkwonPlugin() {

    private val typeNamesRegex = tagNames.map { "\\n?<$it(\\s[^>]*)?>.*?</$it>|\\n?<$it(\\s[^>]*)?>".toRegex() }

    override fun processMarkdown(markdown: String): String {
        var result = markdown
        typeNamesRegex.forEach {
            result = result.replace(it, "")
        }
        return result
    }
}
