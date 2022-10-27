package io.novafoundation.nova.common.utils

import android.content.Context
import android.content.res.ColorStateList
import android.content.res.TypedArray
import android.graphics.drawable.Drawable
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.view.ViewTreeObserver
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.ScrollView
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.LayoutRes
import androidx.annotation.StyleableRes
import androidx.core.content.ContextCompat
import androidx.core.widget.TextViewCompat
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.facebook.shimmer.ShimmerFrameLayout
import dev.chrisbanes.insetter.applyInsetter
import io.novafoundation.nova.common.utils.input.Input
import io.novafoundation.nova.common.utils.input.valueOrNull

fun View.updatePadding(
    top: Int = paddingTop,
    bottom: Int = paddingBottom,
    start: Int = paddingStart,
    end: Int = paddingEnd,
) {
    setPadding(start, top, end, bottom)
}

inline fun EditText.onTextChanged(crossinline listener: (String) -> Unit) {
    addTextChangedListener(object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        }

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
            listener.invoke(s.toString())
        }
    })
}

inline fun EditText.onDoneClicked(crossinline listener: () -> Unit) {
    setOnEditorActionListener { _, actionId, _ ->
        if (actionId == EditorInfo.IME_ACTION_DONE) {
            listener.invoke()

            true
        }

        false
    }
}

fun ViewGroup.inflateChild(@LayoutRes id: Int, attachToRoot: Boolean = false): View {
    return LayoutInflater.from(context).run {
        inflate(id, this@inflateChild, attachToRoot)
    }
}

fun TextView.setTextColorRes(@ColorRes colorRes: Int) = setTextColor(ContextCompat.getColor(context, colorRes))

fun View.updateTopMargin(newMargin: Int) {
    (layoutParams as? MarginLayoutParams)?.let {
        it.setMargins(it.leftMargin, newMargin, it.rightMargin, it.bottomMargin)
    }
}

inline fun <T> View.letOrHide(value: T?, setup: (T) -> Unit) {
    if (value == null) {
        makeGone()
        return
    }

    makeVisible()
    setup(value)
}

fun ShimmerFrameLayout.setShimmerVisible(visible: Boolean) {
    if (visible) startShimmer() else stopShimmer()

    setVisible(visible)
}

private fun TextView.setCompoundDrawable(
    @DrawableRes drawableRes: Int?,
    widthInDp: Int?,
    heightInDp: Int?,
    @ColorRes tint: Int?,
    paddingInDp: Int = 0,
    applier: TextView.(Drawable) -> Unit,
) {
    if (drawableRes == null) {
        setCompoundDrawablesWithIntrinsicBounds(null, null, null, null)
        return
    }

    val drawable = context.getDrawableCompat(drawableRes)

    tint?.let { drawable.mutate().setTint(context.getColor(it)) }

    drawable.updateDimensions(context, widthInDp, heightInDp)

    applier(drawable)

    val paddingInPx = paddingInDp.dp(context)
    compoundDrawablePadding = paddingInPx
}

fun TextView.removeCompoundDrawables() = setCompoundDrawablesRelative(null, null, null, null)

fun TextView.setDrawableTop(
    @DrawableRes drawableRes: Int? = null,
    widthInDp: Int? = null,
    heightInDp: Int? = widthInDp,
    paddingInDp: Int = 0,
    @ColorRes tint: Int? = null,
) {
    val (start, _, end, bottom) = compoundDrawablesRelative

    setCompoundDrawable(drawableRes, widthInDp, heightInDp, tint, paddingInDp) {
        setCompoundDrawablesRelative(start, it, end, bottom)
    }
}

fun TextView.setDrawableEnd(
    @DrawableRes drawableRes: Int? = null,
    widthInDp: Int? = null,
    heightInDp: Int? = widthInDp,
    paddingInDp: Int = 0,
    @ColorRes tint: Int? = null,
) {
    val (start, top, _, bottom) = compoundDrawablesRelative

    setCompoundDrawable(drawableRes, widthInDp, heightInDp, tint, paddingInDp) {
        setCompoundDrawablesRelative(start, top, it, bottom)
    }
}

fun TextView.setDrawableStart(
    @DrawableRes drawableRes: Int? = null,
    widthInDp: Int? = null,
    heightInDp: Int? = widthInDp,
    paddingInDp: Int = 0,
    @ColorRes tint: Int? = null,
) {
    val (_, top, end, bottom) = compoundDrawablesRelative

    setCompoundDrawable(drawableRes, widthInDp, heightInDp, tint, paddingInDp) {
        setCompoundDrawablesRelative(it, top, end, bottom)
    }
}

private fun Drawable.updateDimensions(
    context: Context,
    widthInDp: Int?,
    heightInDp: Int?
) {
    val widthInPx = widthInDp?.dp(context) ?: intrinsicWidth
    val heightInPx = heightInDp?.dp(context) ?: intrinsicHeight

    setBounds(0, 0, widthInPx, heightInPx)
}

fun ImageView.setImageTintRes(@ColorRes tintRes: Int?) {
    imageTintList = tintRes?.let { ColorStateList.valueOf(context.getColor(tintRes)) }
}

fun ImageView.setImageTint(@ColorInt tint: Int?) {
    imageTintList = tint?.let { ColorStateList.valueOf(it) }
}

inline fun View.doOnGlobalLayout(crossinline action: () -> Unit) {
    viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
        override fun onGlobalLayout() {
            viewTreeObserver.removeOnGlobalLayoutListener(this)

            action()
        }
    })
}

fun View.setVisible(visible: Boolean, falseState: Int = View.GONE) {
    visibility = if (visible) View.VISIBLE else falseState
}

fun View.hideSoftKeyboard() {
    val inputMethodManager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    inputMethodManager.hideSoftInputFromWindow(windowToken, 0)
}

fun View.showSoftKeyboard() {
    requestFocus()

    val inputMethodManager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

    inputMethodManager.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
}

fun ViewGroup.addAfter(anchor: View, newViews: List<View>) {
    val index = indexOfChild(anchor)

    newViews.forEachIndexed { offset, view ->
        addView(view, index + offset + 1)
    }
}

fun ViewGroup.addAfter(anchor: View, child: View) {
    val index = indexOfChild(anchor)

    addView(child, index + 1)
}

fun RecyclerView.scrollToTopWhenItemsShuffled(lifecycleOwner: LifecycleOwner) {
    val adapterDataObserver = object : RecyclerView.AdapterDataObserver() {
        override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) {
            scrollToPosition(0)
        }

        override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
            scrollToPosition(0)
        }
    }

    adapter?.registerAdapterDataObserver(adapterDataObserver)

    lifecycleOwner.lifecycle.onDestroy { adapter?.unregisterAdapterDataObserver(adapterDataObserver) }
}

fun RecyclerView.enableShowingNewlyAddedTopElements(): RecyclerView.AdapterDataObserver {
    val adapterDataObserver = object : RecyclerView.AdapterDataObserver() {
        override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
            if (positionStart == 0 && wasAtBeginningBeforeInsertion(itemCount)) {
                scrollToPosition(0)
            }
        }
    }

    adapter?.registerAdapterDataObserver(adapterDataObserver)

    return adapterDataObserver
}

private fun RecyclerView.wasAtBeginningBeforeInsertion(insertedCount: Int) =
    findFirstVisiblePosition() < insertedCount && insertedCount != adapter!!.itemCount

fun RecyclerView.findFirstVisiblePosition(): Int {
    return (layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
}

fun ScrollView.scrollOnFocusTo(vararg focusableTargets: View) {
    val listener = View.OnFocusChangeListener { view, hasFocus ->
        if (hasFocus) {
            postToSelf { scrollTo(view.left, view.top) }
        }
    }

    focusableTargets.forEach { it.onFocusChangeListener = listener }
}

fun TextView.setCompoundDrawableTint(@ColorRes tintRes: Int) {
    val tintColor = context.getColor(tintRes)

    TextViewCompat.setCompoundDrawableTintList(this, ColorStateList.valueOf(tintColor))
}

fun TextView.setTextOrHide(newText: String?) {
    if (newText != null) {
        text = newText
        setVisible(true)
    } else {
        setVisible(false)
    }
}

inline fun <T : View> T.postToSelf(crossinline action: T.() -> Unit) = with(this) { post { action() } }

inline fun <reified T : Enum<T>> TypedArray.getEnum(index: Int, default: T) =
    getInt(index, /*defValue*/-1).let {
        if (it >= 0) enumValues<T>()[it] else default
    }

inline fun Context.useAttributes(
    attributeSet: AttributeSet,
    @StyleableRes styleable: IntArray,
    block: (TypedArray) -> Unit,
) {
    val typedArray = obtainStyledAttributes(attributeSet, styleable)

    block(typedArray)

    typedArray.recycle()
}

fun TypedArray.getResourceIdOrNull(@StyleableRes index: Int) = getResourceId(index, 0).takeIf { it != 0 }

fun View.applyBarMargin() = applyInsetter {
    type(statusBars = true) {
        margin()
    }
}

fun View.applyStatusBarInsets(consume: Boolean = true) = applyInsetter {
    type(statusBars = true) {
        padding()
    }

    consume(consume)
}

fun View.setBackgroundColorRes(@ColorRes colorRes: Int) = setBackgroundColor(context.getColor(colorRes))

fun <I> View.useInputValue(input: Input<I>, onValue: (I) -> Unit) {
    setVisible(input is Input.Enabled)
    isEnabled = input is Input.Enabled.Modifiable

    input.valueOrNull?.let(onValue)
}

fun <T> ListAdapter<T, *>.submitListPreservingViewPoint(
    data: List<T>,
    into: RecyclerView,
    extraDiffCompletedCallback: (() -> Unit)? = null
) {
    val recyclerViewState = into.layoutManager!!.onSaveInstanceState()

    submitList(data) {
        into.layoutManager!!.onRestoreInstanceState(recyclerViewState)

        extraDiffCompletedCallback?.invoke()
    }
}

fun ImageView.setImageResource(@DrawableRes imageRes: Int?) = if (imageRes == null) {
    setImageDrawable(null)
} else {
    setImageResource(imageRes)
}

fun EditText.moveCursorToTheEnd() = setSelection(length())
