package io.novafoundation.nova.test_shared

import com.google.gson.Gson
import com.neovisionaries.ws.client.WebSocketFactory
import io.novasama.substrate_sdk_android.wsrpc.SocketService
import io.novasama.substrate_sdk_android.wsrpc.recovery.Reconnector
import io.novasama.substrate_sdk_android.wsrpc.request.RequestExecutor

fun createTestSocket() = SocketService(Gson(), NoOpLogger, WebSocketFactory(), Reconnector(), RequestExecutor())
