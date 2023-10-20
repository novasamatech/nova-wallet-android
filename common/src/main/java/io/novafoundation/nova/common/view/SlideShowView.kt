package io.novafoundation.nova.common.view

import android.content.Context
import android.graphics.Bitmap
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.coroutineScope
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.utils.WithContextExtensions
import io.novafoundation.nova.common.utils.useAttributes
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

private const val DEFAULT_DELAY_MILLIS = 100

class SlideShowView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : AppCompatImageView(context, attrs, defStyleAttr), WithContextExtensions by WithContextExtensions(context), LifecycleOwner {

    private val slideVisibilityLyfeCycle = LifecycleRegistry(this)

    private var currentDelay: Duration = DEFAULT_DELAY_MILLIS.milliseconds

    private var currentIterator: Iterator<Bitmap>? = null

    private var slideUpdateJob: Job? = null

    init {
        attrs?.let { applyAttrs(it) }

        setupSlideShow()
    }

    fun setDelay(delay: Duration) {
        this.currentDelay = delay
    }

    fun setIterator(iterator: Iterator<Bitmap>) {
        currentIterator = iterator
        setupSlideShow()
    }

    fun pauseSlideShow() {
        slideUpdateJob?.cancel()
        slideUpdateJob = null
    }

    fun resumeSlideShow() {
        setupSlideShow()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        slideVisibilityLyfeCycle.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()

        slideVisibilityLyfeCycle.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    }

    override fun getLifecycle(): Lifecycle {
        return slideVisibilityLyfeCycle
    }

    private fun setupSlideShow() {
        slideUpdateJob?.cancel()

        slideUpdateJob = slideVisibilityLyfeCycle.coroutineScope.launchWhenCreated {
            val iterator = currentIterator

            while (iterator != null && iterator.hasNext()) {
                val nextFrame = iterator.next()
                showFrame(nextFrame)

                delay(currentDelay)
            }
        }
    }

    private fun showFrame(frame: Bitmap) {
        setImageBitmap(frame)
    }

    private fun applyAttrs(attrs: AttributeSet) = context.useAttributes(attrs, R.styleable.SlideShowView) { typedArray ->
        val delayMillis = typedArray.getInt(R.styleable.SlideShowView_SlideShowView_slideDelayMillis, DEFAULT_DELAY_MILLIS)
        setDelay(delayMillis.milliseconds)
    }
}

fun SlideShowView.setSequence(sequence: Sequence<Bitmap>) = setIterator(sequence.iterator())
