package io.novafoundation.nova.app.root.presentation.common

import android.content.Context
import android.content.Intent
import io.novafoundation.nova.app.root.presentation.RootActivity
import io.novafoundation.nova.common.interfaces.ActivityIntentProvider

class RootActivityIntentProvider(private val context: Context) : ActivityIntentProvider {

    override fun getIntent(): Intent {
        return Intent(context, RootActivity::class.java)
    }
}
