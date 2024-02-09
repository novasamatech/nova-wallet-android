package io.novafoundation.nova.common.resources

import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.text.format.DateUtils
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.di.scope.ApplicationScope
import io.novafoundation.nova.common.utils.daysFromMillis
import io.novafoundation.nova.common.utils.formatting.baseDurationFormatter
import io.novafoundation.nova.common.utils.formatting.duration.EstimatedDurationFormatter
import io.novafoundation.nova.common.utils.formatting.formatDateTime
import io.novafoundation.nova.common.utils.getDrawableCompat
import io.novafoundation.nova.common.utils.readText
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.milliseconds

@OptIn(ExperimentalTime::class)
@ApplicationScope
class ResourceManagerImpl(
    private val contextManager: ContextManager
) : ResourceManager {

    private val defaultDurationFormatter by lazy { baseDurationFormatter(contextManager.getApplicationContext()) }

    private val estimatedDurationFormatter by lazy { EstimatedDurationFormatter(defaultDurationFormatter) }

    override fun loadRawString(res: Int): String {
        return contextManager.getApplicationContext().resources
            .openRawResource(res)
            .readText()
    }

    override fun getString(res: Int): String {
        return contextManager.getApplicationContext().getString(res)
    }

    override fun getString(res: Int, vararg arguments: Any): String {
        return contextManager.getApplicationContext().getString(res, *arguments)
    }

    override fun getText(res: Int): CharSequence {
        return contextManager.getApplicationContext().getText(res)
    }

    override fun getColor(res: Int): Int {
        contextManager.getApplicationContext().resources
        return ContextCompat.getColor(contextManager.getApplicationContext(), res)
    }

    override fun getQuantityString(id: Int, quantity: Int): String {
        return contextManager.getApplicationContext().resources.getQuantityString(id, quantity)
    }

    override fun getQuantityString(id: Int, quantity: Int, vararg arguments: Any): String {
        return contextManager.getApplicationContext().resources.getQuantityString(id, quantity, *arguments)
    }

    override fun measureInPx(dp: Int): Int {
        val px = contextManager.getApplicationContext().resources.displayMetrics.density * dp

        return px.toInt()
    }

    override fun formatDateTime(timestamp: Long): String {
        return timestamp.formatDateTime().toString()
    }

    override fun formatDate(timestamp: Long): String {
        return DateUtils.formatDateTime(
            contextManager.getApplicationContext(),
            timestamp,
            DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_YEAR or DateUtils.FORMAT_ABBREV_MONTH
        )
    }

    override fun formatTime(timestamp: Long): String {
        return DateUtils.formatDateTime(contextManager.getApplicationContext(), timestamp, DateUtils.FORMAT_SHOW_TIME)
    }

    @OptIn(ExperimentalTime::class)
    override fun formatDuration(elapsedTime: Long): String {
        val inDays = elapsedTime.daysFromMillis().toInt()

        return when {
            inDays > 0 -> getQuantityString(R.plurals.staking_main_lockup_period_value, inDays, inDays)
            else -> {
                val inSeconds = elapsedTime.milliseconds.inSeconds.toLong()

                DateUtils.formatElapsedTime(inSeconds)
            }
        }
    }

    override fun formatDuration(duration: Duration, estimated: Boolean): String {
        return if (estimated) {
            estimatedDurationFormatter.format(duration)
        } else {
            defaultDurationFormatter.format(duration)
        }
    }

    override fun getDrawable(id: Int): Drawable {
        return contextManager.getApplicationContext().getDrawableCompat(id)
    }

    override fun getDimensionPixelSize(id: Int): Int {
        return contextManager.getApplicationContext().resources.getDimensionPixelSize(id)
    }

    override fun getFont(fontRes: Int): Typeface? {
        return ResourcesCompat.getFont(contextManager.getApplicationContext(), fontRes)
    }
}
