package io.novafoundation.nova.runtime.ethereum

import io.reactivex.Observable
import io.novasama.substrate_sdk_android.wsrpc.SocketService
import io.novasama.substrate_sdk_android.wsrpc.request.DeliveryType
import io.novasama.substrate_sdk_android.wsrpc.request.base.RpcRequest
import io.novasama.substrate_sdk_android.wsrpc.response.RpcResponse
import io.novasama.substrate_sdk_android.wsrpc.subscription.response.SubscriptionChange
import kotlinx.coroutines.future.await
import org.web3j.protocol.core.Request
import org.web3j.protocol.core.Response
import java.util.concurrent.CompletableFuture

fun SocketService.executeRequestAsFuture(
    request: RpcRequest,
    deliveryType: DeliveryType = DeliveryType.AT_LEAST_ONCE,
): CompletableFuture<RpcResponse> {
    val future = RequestCancellableFuture<RpcResponse>()

    val callback = object : SocketService.ResponseListener<RpcResponse> {
        override fun onError(throwable: Throwable) {
            future.completeExceptionally(throwable)
        }

        override fun onNext(response: RpcResponse) {
            future.complete(response)
        }
    }

    future.cancellable = executeRequest(request, deliveryType, callback)

    return future
}

fun SocketService.executeBatchRequestAsFuture(
    requests: List<RpcRequest>,
    deliveryType: DeliveryType = DeliveryType.AT_LEAST_ONCE,
): CompletableFuture<List<RpcResponse>> {
    val future = RequestCancellableFuture<List<RpcResponse>>()

    val callback = object : SocketService.ResponseListener<List<RpcResponse>> {
        override fun onError(throwable: Throwable) {
            future.completeExceptionally(throwable)
        }

        override fun onNext(response: List<RpcResponse>) {
            future.complete(response)
        }
    }

    future.cancellable = executeAccumulatingBatchRequest(requests, deliveryType, callback)

    return future
}

fun SocketService.subscribeAsObservable(
    request: RpcRequest,
    unsubscribeMethod: String
): Observable<SubscriptionChange> {
    return Observable.create { emitter ->
        val callback = object : SocketService.ResponseListener<SubscriptionChange> {
            override fun onError(throwable: Throwable) {
                emitter.tryOnError(throwable)
            }

            override fun onNext(response: SubscriptionChange) {
                emitter.onNext(response)
            }
        }

        val cancellable = subscribe(request, callback, unsubscribeMethod)

        emitter.setCancellable(cancellable::cancel)
    }
}

private class RequestCancellableFuture<T> : CompletableFuture<T>() {

    var cancellable: SocketService.Cancellable? = null

    override fun cancel(mayInterruptIfRunning: Boolean): Boolean {
        cancellable?.cancel()

        return super.cancel(mayInterruptIfRunning)
    }
}

suspend fun <S, T : Response<*>> Request<S, T>.sendSuspend(): T = sendAsync().await()
