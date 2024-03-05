package io.novafoundation.nova.common.interfaces

import android.content.Intent

interface ActivityIntentProvider {
    fun getIntent(): Intent
}
