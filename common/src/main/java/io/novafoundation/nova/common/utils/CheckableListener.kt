package io.novafoundation.nova.common.utils

import android.widget.Checkable
import android.widget.CompoundButton

interface CheckableListener : Checkable {
    fun setOnCheckedChangeListener(listener: CompoundButton.OnCheckedChangeListener)
}
