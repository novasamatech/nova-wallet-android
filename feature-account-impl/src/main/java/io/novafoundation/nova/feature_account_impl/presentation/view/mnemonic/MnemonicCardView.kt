package io.novafoundation.nova.feature_account_impl.presentation.view.mnemonic

import android.content.Context
import android.util.AttributeSet
import io.novafoundation.nova.common.utils.WithContextExtensions
import io.novafoundation.nova.common.utils.colorSpan
import io.novafoundation.nova.common.utils.formatting.spannable.SpannableFormatter
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.common.utils.toSpannable
import io.novafoundation.nova.common.utils.useAttributes
import io.novafoundation.nova.common.view.TapToViewContainer
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.databinding.ViewMnemonicCardViewBinding
import io.novafoundation.nova.feature_account_impl.presentation.common.mnemonic.BackupMnemonicAdapter
import io.novafoundation.nova.feature_account_impl.presentation.mnemonic.confirm.MnemonicWord

class MnemonicCardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : TapToViewContainer(context, attrs, defStyleAttr),
    BackupMnemonicAdapter.ItemHandler,
    WithContextExtensions by WithContextExtensions(context) {

    private val adapter = BackupMnemonicAdapter(this)

    private var wordClickedListener: BackupMnemonicAdapter.ItemHandler? = null

    private val binder = ViewMnemonicCardViewBinding.inflate(inflater(), this)

    init {
        setTitleOrHide(context.getString(R.string.common_tap_to_reveal_title))
        setSubtitleOrHide(context.getString(R.string.mnemonic_card_reveal_subtitle))
        setBackgroundResource(R.drawable.ic_parallax_card_background)
        setTapToViewBackground(R.drawable.ic_mnemonic_card_blur)
        setCardCornerRadius(12.dpF)

        binder.mnemonicCardPhrase.setItemPadding(4.dp)
        binder.mnemonicCardPhrase.adapter = adapter

        binder.mnemonicCardTitle.text = SpannableFormatter.format(
            context.getString(R.string.mnemonic_card_title),
            context.getString(R.string.mnemonic_card_title_highlight)
                .toSpannable(colorSpan(context.getColor(R.color.text_primary)))
        )

        attrs?.let(::applyAttrs)
    }

    override fun wordClicked(word: MnemonicWord) {
        wordClickedListener?.wordClicked(word)
    }

    fun setWords(words: List<MnemonicWord>) {
        adapter.submitList(words)
    }

    fun setWordsString(list: List<String>) {
        val words = list.mapIndexed { index, item ->
            MnemonicWord(
                id = index,
                content = item,
                indexDisplay = index.plus(1).toString(),
                removed = false
            )
        }
        setWords(words)
    }

    fun setWordClickedListener(listener: BackupMnemonicAdapter.ItemHandler?) {
        wordClickedListener = listener
    }

    private fun applyAttrs(attributeSet: AttributeSet) = context.useAttributes(attributeSet, R.styleable.MnemonicCardView) {
        val showRevealContainer = it.getBoolean(R.styleable.MnemonicCardView_showRevealContainer, false)
        showContent(!showRevealContainer)
    }
}
