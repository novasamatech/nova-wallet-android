package io.novafoundation.nova.common.sequrity

import kotlinx.coroutines.flow.Flow

sealed interface BiometricResponse {

    object Success : BiometricResponse

    object Fail : BiometricResponse

    object NotReady : BiometricResponse

    class Error(val cancelledByUser: Boolean, val message: String) : BiometricResponse
}

interface BiometricService {

    val biometryServiceResponseFlow: Flow<BiometricResponse>

    fun isEnabled(): Boolean

    fun isEnabledFlow(): Flow<Boolean>

    suspend fun toggle()

    fun requestBiometric()

    fun isBiometricReady(): Boolean

    fun cancel()

    fun enableBiometry(enable: Boolean)

    fun refreshBiometryState()
}
