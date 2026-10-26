package io.novafoundation.nova.infrastructure.attestation.profile3

import android.content.Context
import android.os.SystemClock
import android.provider.Settings

class AndroidProfile3TokenClock(private val context: Context) : Profile3TokenClock {

    override fun elapsedRealtimeMillis(): Long = SystemClock.elapsedRealtime()

    override fun bootCount(): Int = Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT)
}
