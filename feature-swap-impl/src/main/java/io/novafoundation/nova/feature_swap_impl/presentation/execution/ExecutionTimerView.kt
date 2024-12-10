package io.novafoundation.nova.feature_swap_impl.presentation.execution

import android.content.Context
import android.os.CountDownTimer
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.animation.BaseInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.view.animation.RotateAnimation
import android.widget.TextSwitcher
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import io.novafoundation.nova.common.databinding.ViewChooseAmountOldBinding
import io.novafoundation.nova.common.utils.WithContextExtensions
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.common.utils.makeGone
import io.novafoundation.nova.common.utils.makeVisible
import io.novafoundation.nova.common.utils.setTextColorRes
import io.novafoundation.nova.feature_swap_impl.R
import io.novafoundation.nova.feature_swap_impl.databinding.ViewExecutionTimerBinding
import kotlin.math.cos
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

private const val SECOND_MILLIS = 1000L
private const val HIDE_SCALE = 0.7f

class ExecutionTimerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr), WithContextExtensions by WithContextExtensions(context) {

    sealed interface State {

        object Success : State

        object Error : State

        class CountdownTimer(val duration: Duration) : State
    }

    private val binder = ViewExecutionTimerBinding.inflate(inflater(), this)

    private var currentState: State? = null

    private val slideTopInAnimation = AnimationUtils.loadAnimation(context, R.anim.fade_slide_bottom_in)
    private val slideTopOutAnimation = AnimationUtils.loadAnimation(context, R.anim.fade_slide_bottom_out)

    private var currentTimer: CountDownTimer? = null

    init {
        inflate(context, R.layout.view_execution_timer, this)
        setupTimerSwitcher()
    }

    fun setState(state: State) {
        currentState = state

        currentTimer?.cancel()

        when (state) {
            State.Success -> {
                hideTimerWithAnimation()
                binder.executionResult.setImageResource(R.drawable.ic_execution_result_success)
                binder.executionResult.fadeInWithScale()
            }

            State.Error -> {
                hideTimerWithAnimation()
                binder.executionResult.setImageResource(R.drawable.ic_execution_result_error)
                binder.executionResult.fadeInWithScale()
            }

            is State.CountdownTimer -> {
                binder.executionResult.fadeOutWithScale()
                showTimerWithAnimation()

                binder.executionProgress.runInfinityRotationAnimation()

                // We add delay to match progress animation perfectly
                // Text should be switched in the middle of a progress animation with small offset
                val middleOfAnimation = SECOND_MILLIS / 2
                val switchAnimationOffset = slideTopOutAnimation.duration / 2
                val delay = middleOfAnimation - switchAnimationOffset
                currentTimer = CountdownSwitcherTimer(binder.executionTimeSwitcher, state.duration)
                runTimerWithDelay(delay, currentTimer!!)
            }
        }
    }

    private fun hideTimerWithAnimation() {
        binder.executionProgress.fadeOut()
        binder.executionTimeSwitcher.fadeOutWithScale()
        binder.executionTimeSeconds.fadeOutWithScale()
    }

    private fun showTimerWithAnimation() {
        binder.executionProgress.fadeIn()
        binder.executionTimeSwitcher.fadeInWithScale()
        binder.executionTimeSeconds.fadeInWithScale()
    }

    private fun runTimerWithDelay(delay: Long, timer: CountDownTimer) {
        postDelayed({ timer.start() }, delay)
    }

    private fun setupTimerSwitcher() {
        binder.executionTimeSwitcher.setFactory {
            val textView = TextView(context, null, 0, R.style.TextAppearance_NovaFoundation_Bold_Title3)
            textView.setGravity(Gravity.CENTER)
            textView.setTextColorRes(R.color.text_primary)
            textView.includeFontPadding = false
            textView
        }

        binder.executionTimeSwitcher.inAnimation = slideTopInAnimation
        binder.executionTimeSwitcher.outAnimation = slideTopOutAnimation
    }

    private fun View.runInfinityRotationAnimation() {
        val anim = RotateAnimation(
            0f,
            -360f,
            Animation.RELATIVE_TO_SELF,
            0.5f,
            Animation.RELATIVE_TO_SELF,
            0.5f
        )

        anim.duration = SECOND_MILLIS
        anim.repeatCount = Animation.INFINITE
        anim.interpolator = StartSpeedAccelerateDecelerateInterpolator()
        startAnimation(anim)
    }

    private fun View.fadeOut() {
        animate()
            .alpha(0f)
            .setDuration(400)
            .withEndAction { makeGone() }
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    private fun View.fadeIn() {
        alpha = 0f
        makeVisible()
        animate()
            .alpha(1f)
            .setDuration(400)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    private fun View.fadeOutWithScale() {
        animate()
            .alpha(0f)
            .scaleX(HIDE_SCALE)
            .scaleY(HIDE_SCALE)
            .setDuration(400)
            .withEndAction { makeGone() }
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    private fun View.fadeInWithScale() {
        scaleX = HIDE_SCALE
        scaleY = HIDE_SCALE
        alpha = 0f
        makeVisible()
        animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(400)
            .setInterpolator(OvershootInterpolator())
            .start()
    }
}

private class StartSpeedAccelerateDecelerateInterpolator : BaseInterpolator() {

    override fun getInterpolation(input: Float): Float {
        val speed = 0.085 // A constant
        val result = input + cos((input * 2 * Math.PI) + Math.PI / 2) * speed
        return result.toFloat()
    }
}

private class CountdownSwitcherTimer(val switcher: TextSwitcher, duration: Duration) :
    CountDownTimer(
        duration.inWholeMilliseconds + SECOND_MILLIS, // Add a seconds to show max value to user
        SECOND_MILLIS
    ) {

    init {
        switcher.setText(duration.inWholeSeconds.toString())
    }

    override fun onTick(millisUntilFinished: Long) {
        val duration = millisUntilFinished.milliseconds
        val seconds = duration.inWholeSeconds.toString()

        if (shouldPlayAnimation(seconds)) {
            switcher.setText(seconds)
        }
    }

    override fun onFinish() {
        // Nothing to do
    }

    private fun shouldPlayAnimation(seconds: String): Boolean {
        val currentTextView = switcher.currentView as? TextView ?: return true

        return currentTextView.text != seconds
    }
}
