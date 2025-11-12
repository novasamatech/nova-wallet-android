package io.novafoundation.nova.common.view

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import androidx.lifecycle.LifecycleOwner
import com.github.razir.progressbutton.DrawableButton
import com.github.razir.progressbutton.bindProgressButton
import com.github.razir.progressbutton.hideProgress
import com.github.razir.progressbutton.isProgressActive
import com.github.razir.progressbutton.showProgress
import com.google.android.material.button.MaterialButton
import io.novafoundation.nova.common.presentation.DescriptiveButtonState
import io.novafoundation.nova.common.presentation.textOrNull

class PrimaryButtonV2 @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : MaterialButton(context, attrs, defStyle) {

    private var cachedIcon: Drawable? = null
    private var cachedText: String? = null

    private var preparedForProgress = false

    fun prepareForProgress(lifecycleOwner: LifecycleOwner) {
        lifecycleOwner.bindProgressButton(this)

        preparedForProgress = true
    }

    fun checkPreparedForProgress() {
        if (!preparedForProgress) {
            throw IllegalArgumentException("You must call prepareForProgress() first!")
        }
    }

    fun showProgress(show: Boolean) {
        isEnabled = !show

        if (show) {
            checkPreparedForProgress()

            showButtonProgress()
        } else {
            hideProgress()
        }
    }

    fun hideButtonProgress() {
        if (isProgressActive()) {
            icon = cachedIcon
            hideProgress(cachedText)
        }
    }

    fun showButtonProgress() {
        if (isProgressActive()) return

        cachedIcon = icon
        cachedText = text.toString()

        icon = null
        showProgress {
            progressColor = currentTextColor
            gravity = DrawableButton.GRAVITY_CENTER
        }
    }
}

fun PrimaryButtonV2.setState(state: DescriptiveButtonState) {
    isEnabled = state is DescriptiveButtonState.Enabled

    text = state.textOrNull()

    visibility = when (state) {
        DescriptiveButtonState.Gone -> View.GONE
        DescriptiveButtonState.Invisible -> View.INVISIBLE
        else -> View.VISIBLE
    }

    if (state == DescriptiveButtonState.Loading) {
        checkPreparedForProgress()

        showButtonProgress()
    } else {
        hideButtonProgress()
    }
}
