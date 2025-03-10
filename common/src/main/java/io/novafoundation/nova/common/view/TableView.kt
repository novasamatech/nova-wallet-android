package io.novafoundation.nova.common.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.core.view.updateMarginsRelative
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.utils.WithContextExtensions
import io.novafoundation.nova.common.utils.dpF
import io.novafoundation.nova.common.utils.makeGone
import io.novafoundation.nova.common.utils.makeVisible
import io.novafoundation.nova.common.utils.setBackgroundColorRes
import io.novafoundation.nova.common.utils.setTextColorRes
import io.novafoundation.nova.common.utils.setTextOrHide
import io.novafoundation.nova.common.utils.updatePadding
import io.novafoundation.nova.common.utils.useAttributes

private const val SHOW_BACKGROUND_DEAULT = true

open class TableView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr), WithContextExtensions {

    override val providedContext: Context = context

    val titleView: TextView = addTitleView()

    private val childHorizontalPadding = 16.dpF(context)
    private val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val dividerPath = Path()

    private var showBackground: Boolean = SHOW_BACKGROUND_DEAULT

    init {
        orientation = VERTICAL

        if (attrs != null) {
            attrs.let(::applyAttributes)
        } else {
            noAttrsInit()
        }

        if (showBackground) {
            background = getRoundedCornerDrawable(R.color.block_background)
        } else {
            setBackgroundColorRes(android.R.color.transparent)
        }

        clipToOutline = true

        dividerPaint.apply {
            color = context.getColor(R.color.divider)
            style = Paint.Style.STROKE
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        setupTableChildrenAppearance()
    }

    /*
    We use setupTableChildrenAppearance here to support case when child view makes gone programmatically.
    The we recalculate dividers
     */
    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        super.onLayout(changed, l, t, r, b)

        setupTableChildrenAppearance()

        dividerPath.reset()
        children.forEachIndexed { idx, child ->
            val isVisible = child.isVisible
            val allowsToDrawDividers = child is TableItem && child.shouldDrawDivider()
            val hasNext = idx < childCount - 1

            if (isVisible && allowsToDrawDividers && hasNext) {
                dividerPath.moveTo(childHorizontalPadding, child.bottom.toFloat())
                dividerPath.lineTo(measuredWidth - childHorizontalPadding, child.bottom.toFloat())
            }
        }
    }

    fun setTitle(title: String?) {
        titleView.setTextOrHide(title)
    }

    private fun addTitleView(): TextView = TextView(context).also { title ->
        title.setTextAppearance(R.style.TextAppearance_NovaFoundation_Regular_SubHeadline)
        title.setTextColorRes(R.color.text_primary)
        title.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).also { params ->
            params.updateMarginsRelative(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp)
        }

        addView(title, 0)
    }

    private fun noAttrsInit() {
        setTitle(null)
    }

    private fun applyAttributes(attrs: AttributeSet) = context.useAttributes(attrs, R.styleable.TableView) {
        val title = it.getString(R.styleable.TableView_title)
        setTitle(title)

        showBackground = it.getBoolean(R.styleable.TableView_showBackground, SHOW_BACKGROUND_DEAULT)
    }

    private fun setupTableChildrenAppearance() {
        val tableChildren = children.filter { it != titleView }
            .filter { it.isVisible }
            .toList()

        if (tableChildren.isEmpty()) {
            makeGone()
            return
        } else {
            makeVisible()
        }

        tableChildren.forEach {
            if (it is TableItem) {
                it.disableOwnDividers()
            }

            it.updatePadding(start = 16.dp, end = 16.dp)
        }

        tableChildren.first().apply {
            updatePadding(top = 4.dp)
        }
        tableChildren.last().apply {
            updatePadding(bottom = 4.dp)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawPath(dividerPath, dividerPaint)
    }

    private fun <T> List<T>.withoutLast(): List<T> {
        return if (size <= 1) {
            listOf()
        } else {
            subList(0, size - 1)
        }
    }
}
