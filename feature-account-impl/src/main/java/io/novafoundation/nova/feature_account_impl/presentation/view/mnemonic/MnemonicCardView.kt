package io.novafoundation.nova.feature_account_impl.presentation.view.mnemonic

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.RectF
import android.os.Bundle
import android.os.Parcelable
import android.util.AttributeSet
import android.view.View
import androidx.core.view.isVisible
import com.google.android.material.card.MaterialCardView
import io.novafoundation.nova.common.utils.WithContextExtensions
import io.novafoundation.nova.common.utils.colorSpan
import io.novafoundation.nova.common.utils.formatting.spannable.SpannableFormatter
import io.novafoundation.nova.common.utils.getParcelableCompat
import io.novafoundation.nova.common.utils.toSpannable
import io.novafoundation.nova.common.utils.useAttributes
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.presentation.common.mnemonic.BackupMnemonicAdapter
import io.novafoundation.nova.feature_account_impl.presentation.mnemonic.confirm.MnemonicWord
import kotlinx.android.synthetic.main.view_mnemonic_card_view.view.mnemonicCardPhrase
import kotlinx.android.synthetic.main.view_mnemonic_card_view.view.mnemonicCardRevealContainer
import kotlinx.android.synthetic.main.view_mnemonic_card_view.view.mnemonicCardTitle

private const val SUPER_STATE = "super_state"
private const val REVEAL_CONTAINER_VISIBILITY = "reveal_container_visibility"

class MnemonicCardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : MaterialCardView(context, attrs, defStyleAttr),
    BackupMnemonicAdapter.ItemHandler,
    WithContextExtensions by WithContextExtensions(context) {

    private var onShowMnemonicClickListener: OnClickListener? = null
    private val adapter = BackupMnemonicAdapter(this)

    private var wordClickedListener: BackupMnemonicAdapter.ItemHandler? = null

    private val cardRect = RectF()
    private val cardBackgroundBitmap: Bitmap

    init {
        View.inflate(context, R.layout.view_mnemonic_card_view, this)
        cardElevation = 0f
        radius = 12.dpF
        strokeWidth = 1.dp
        strokeColor = context.getColor(R.color.container_border)

        mnemonicCardPhrase.setItemPadding(4.dp)
        mnemonicCardPhrase.adapter = adapter

        cardBackgroundBitmap = BitmapFactory.decodeResource(context.resources, io.novafoundation.nova.common.R.drawable.ic_parallax_card_background)

        mnemonicCardTitle.text = SpannableFormatter.format(
            context.getString(R.string.mnemonic_card_title),
            context.getString(R.string.mnemonic_card_title_highlight)
                .toSpannable(colorSpan(context.getColor(R.color.text_primary)))
        )

        mnemonicCardRevealContainer.setOnClickListener {
            onShowMnemonicClickListener?.onClick(this)
            mnemonicCardRevealContainer.animate()
                .alpha(0f)
                .withEndAction {
                    mnemonicCardRevealContainer.visibility = View.GONE
                    mnemonicCardRevealContainer.alpha = 1f
                }.start()
        }

        attrs?.let(::applyAttrs)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)

        cardRect.set(0f, 0f, width.toFloat(), height.toFloat())
    }

    override fun onSaveInstanceState(): Parcelable {
        val superState = super.onSaveInstanceState()

        return Bundle().apply {
            putParcelable(SUPER_STATE, superState)
            putInt(REVEAL_CONTAINER_VISIBILITY, mnemonicCardRevealContainer.visibility)
        }
    }

    override fun onRestoreInstanceState(state: Parcelable) {
        if (state is Bundle) {
            super.onRestoreInstanceState(state.getParcelableCompat<Parcelable>(SUPER_STATE))

            mnemonicCardRevealContainer.visibility = state.getInt(REVEAL_CONTAINER_VISIBILITY)
        } else {
            super.onRestoreInstanceState(state)
        }
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawBitmap(cardBackgroundBitmap, null, cardRect, null)
        super.onDraw(canvas)
    }

    override fun onDrawForeground(canvas: Canvas) {
        super.onDrawForeground(canvas)
    }

    override fun wordClicked(word: MnemonicWord) {
        wordClickedListener?.wordClicked(word)
    }

    fun setWords(list: List<MnemonicWord>) {
        adapter.submitList(list)
    }

    fun setWordClickedListener(listener: BackupMnemonicAdapter.ItemHandler?) {
        wordClickedListener = listener
    }

    fun showRevealContainer(show: Boolean) {
        mnemonicCardRevealContainer.isVisible = show
    }

    fun onMnemonicShownListener(listener: OnClickListener) {
        onShowMnemonicClickListener = listener
    }

    private fun applyAttrs(attributeSet: AttributeSet) = context.useAttributes(attributeSet, R.styleable.MnemonicCardView) {
        val showRevealContainer = it.getBoolean(R.styleable.MnemonicCardView_showRevealContainer, false)
        showRevealContainer(showRevealContainer)
    }
}
