package io.novafoundation.nova.common.view.parallaxCard

import android.animation.TimeAnimator
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.core.content.ContextCompat

private const val SECOND_IN_MILLIS = 1000f
private const val VELOCITY = 0.1f
private const val SENSOR_Z_INDEX = 1

class CardGyroscopeListener(
    context: Context,
    private val deviceRotationAngle: Float,
    private val callback: (rotation: Float) -> Unit
) : SensorEventListener {

    private val timeAnimator = TimeAnimator()
    private val sensorManager = ContextCompat.getSystemService(context, SensorManager::class.java)

    private var screenRotation = 0f
    private var deviceRotation = 0f
    private var previousEventMillis: Long = 0

    init {
        timeAnimator.setTimeListener(::onTimeChanged)
    }

    fun start() {
        if (sensorManager != null) {
            val gyroscopeSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
            if (gyroscopeSensor != null) {
                previousEventMillis = System.currentTimeMillis()
                sensorManager.registerListener(this, gyroscopeSensor, SensorManager.SENSOR_DELAY_UI)
                timeAnimator.start()
            }
        }
    }

    fun cancel() {
        if (sensorManager != null) {
            previousEventMillis = System.currentTimeMillis()
            sensorManager.unregisterListener(this)
            timeAnimator.cancel()
        }
    }

    private fun onTimeChanged(animator: TimeAnimator, totalTime: Long, deltaTime: Long) {
        screenRotation += (deviceRotation - screenRotation) * VELOCITY
        val resultRotation = screenRotation / deviceRotationAngle
        callback(resultRotation)
    }

    override fun onSensorChanged(event: SensorEvent) {
        val currentMillis = System.currentTimeMillis()
        val dT = (currentMillis - previousEventMillis) / SECOND_IN_MILLIS
        val rotationDistanceInRadians = event.values[SENSOR_Z_INDEX] * dT
        val rotationDistanceInDegrees = Math.toDegrees(rotationDistanceInRadians.toDouble())
        deviceRotation += rotationDistanceInDegrees.toFloat()

        deviceRotation = deviceRotation.coerceIn(-deviceRotationAngle, deviceRotationAngle)

        previousEventMillis = currentMillis
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
