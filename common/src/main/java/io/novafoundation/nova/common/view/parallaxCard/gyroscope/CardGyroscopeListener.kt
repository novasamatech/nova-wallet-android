package io.novafoundation.nova.common.view.parallaxCard.gyroscope

import android.animation.TimeAnimator
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.core.content.ContextCompat
import io.novafoundation.nova.common.view.parallaxCard.TravelVector
import kotlin.time.Duration.Companion.seconds

private const val SECOND_IN_MILLIS = 1000f
private const val SENSOR_X_INDEX = 0
private const val INTERPOLATION_VELOCITY = 0.3f
private const val SENSOR_Y_INDEX = 1
private val SENSOR_FREQUENCY_MICROSECONDS = (1.seconds.inWholeMicroseconds / 60).toInt()

class CardGyroscopeListener(
    context: Context,
    private val deviceRotationAngle: TravelVector,
    private val callback: (rotation: TravelVector) -> Unit
) : SensorEventListener {

    private val sensorManager = ContextCompat.getSystemService(context, SensorManager::class.java)
    private val timeAnimator = TimeAnimator()
    private var interpolatedRotation = TravelVector(0f, 0f)
    private var deviceRotation = TravelVector(0f, 0f)
    private var previousEventMillis: Long = 0

    fun start() {
        if (sensorManager != null) {
            val gyroscopeSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
            if (gyroscopeSensor != null) {
                previousEventMillis = System.currentTimeMillis()
                sensorManager.registerListener(this, gyroscopeSensor, SENSOR_FREQUENCY_MICROSECONDS)
                timeAnimator.setTimeListener(::onTimerUpdate)
                timeAnimator.start()
            }
        }
    }

    private fun onTimerUpdate(timeAnimator: TimeAnimator, totalTime: Long, deltaTime: Long) {
        interpolatedRotation += (deviceRotation - interpolatedRotation) * INTERPOLATION_VELOCITY
        callback(interpolatedRotation / deviceRotationAngle)
    }

    fun cancel() {
        if (sensorManager != null) {
            previousEventMillis = System.currentTimeMillis()
            sensorManager.unregisterListener(this)
            timeAnimator.setTimeListener(null)
            timeAnimator.cancel()
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        val currentMillis = System.currentTimeMillis()
        val dT = (currentMillis - previousEventMillis) / SECOND_IN_MILLIS
        val yRadians = event.values[SENSOR_Y_INDEX] * dT.toDouble()
        val xRadians = event.values[SENSOR_X_INDEX] * dT.toDouble()

        // y and x are inverted due to the device orientation
        deviceRotation += TravelVector(
            x = Math.toDegrees(yRadians).toFloat(),
            y = Math.toDegrees(xRadians).toFloat()
        )

        deviceRotation = deviceRotation.coerceIn(-deviceRotationAngle, deviceRotationAngle)

        previousEventMillis = currentMillis
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
