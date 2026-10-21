package io.novafoundation.nova.app.root.presentation

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import io.novafoundation.nova.app.BuildConfig
import io.novafoundation.nova.common.data.storage.DebugSettings
import io.novafoundation.nova.feature_settings_impl.presentation.settings.DebugSettingsBottomSheet
import javax.inject.Inject

class DebugMenuShakeListener @Inject constructor(private val settings: DebugSettings) : SensorEventListener {

    private var manager: SensorManager? = null

    private var activity: RootActivity? = null

    private var menu: DebugSettingsBottomSheet? = null

    private var lastPeak = 0L

    private var firstPeak = 0L

    private var peaks = 0

    fun start(activity: RootActivity) {
        if (!BuildConfig.DEBUG) return
        this.activity = activity
        val sensors = activity.getSystemService(Context.SENSOR_SERVICE) as? SensorManager ?: return
        manager = sensors
        val sensor = sensors.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) ?: return
        sensors.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI)
    }

    fun stop() {
        manager?.unregisterListener(this)
        manager = null
        activity = null
        menu?.dismiss()
        menu = null
        peaks = 0
        lastPeak = 0
        firstPeak = 0
    }

    override fun onSensorChanged(event: SensorEvent) {
        val host = activity ?: return
        if (!BuildConfig.DEBUG || !host.hasWindowFocus() || menu?.isShowing == true) return
        val gravity = SensorManager.GRAVITY_EARTH
        val accelerationSquared = event.values.take(3).sumOf { (it / gravity).toDouble().let { value -> value * value } }
        if (accelerationSquared < SHAKE_GRAVITY * SHAKE_GRAVITY) return
        val now = event.timestamp / 1_000_000
        if (now - lastPeak < PEAK_INTERVAL_MS) return
        if (now - firstPeak > SHAKE_WINDOW_MS) {
            firstPeak = now
            peaks = 0
        }
        lastPeak = now
        if (++peaks < REQUIRED_PEAKS) return
        peaks = 0
        menu = DebugSettingsBottomSheet(
            host,
            settings.bittensorClaimEnabled(),
            settings::setBittensorClaimEnabled,
            settings.bittensorClaimButtonEnabled(),
            settings::setBittensorClaimButtonEnabled
        ).also { it.show() }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private companion object {
        const val SHAKE_GRAVITY = 2.7
        const val PEAK_INTERVAL_MS = 150
        const val SHAKE_WINDOW_MS = 1_200
        const val REQUIRED_PEAKS = 3
    }
}
