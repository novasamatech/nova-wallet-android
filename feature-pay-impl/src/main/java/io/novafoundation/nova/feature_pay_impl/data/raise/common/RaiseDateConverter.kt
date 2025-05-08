package io.novafoundation.nova.feature_pay_impl.data.raise.common

import android.icu.text.SimpleDateFormat
import io.novafoundation.nova.common.domain.model.Timestamp
import java.util.Locale

interface RaiseDateConverter {

    fun convertFromApiDate(date: String): Timestamp?
}

class RealRaiseDateConverter() : RaiseDateConverter {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    override fun convertFromApiDate(date: String): Timestamp? {
        // Raise uses empty strings for null dates
        if (date.isEmpty()) return null

        return convertDateToTimestamp(date).getOrNull()
    }

    private fun convertDateToTimestamp(dateString: String): Result<Timestamp> {
        return runCatching {
            dateFormat.parse(dateString).time
        }
    }
}
