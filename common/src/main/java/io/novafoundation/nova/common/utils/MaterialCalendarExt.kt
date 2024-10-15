package io.novafoundation.nova.common.utils

import com.google.android.material.datepicker.CalendarConstraints.DateValidator
import kotlinx.parcelize.Parcelize

@Parcelize
class RangeDateValidator(private val start: Long?, private val end: Long?) : DateValidator {

    override fun isValid(date: Long): Boolean {
        if (start == null && end == null) return true
        if (start != null && date < start) return false
        if (end != null && date > end) return false

        return true
    }
}
