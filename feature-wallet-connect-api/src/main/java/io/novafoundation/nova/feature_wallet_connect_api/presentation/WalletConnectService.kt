package io.novafoundation.nova.feature_wallet_connect_api.presentation

import io.novafoundation.nova.common.mixin.actionAwaitable.ConfirmOrDenyAwaitable
import io.novafoundation.nova.feature_external_sign_api.presentation.externalSign.AuthorizeDappBottomSheet
import kotlinx.coroutines.CoroutineScope

interface WalletConnectService {

    interface Factory {

        fun create(coroutineScope: CoroutineScope): WalletConnectService
    }

    val authorizeDapp: ConfirmOrDenyAwaitable<AuthorizeDappBottomSheet.Payload>

    fun connect()

    fun disconnect()
}
