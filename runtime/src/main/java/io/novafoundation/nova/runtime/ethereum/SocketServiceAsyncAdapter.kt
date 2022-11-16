package io.novafoundation.nova.runtime.ethereum

import io.reactivex.Observable
import io.reactivex.subjects.BehaviorSubject
import jp.co.soramitsu.fearless_utils.wsrpc.SocketService
import jp.co.soramitsu.fearless_utils.wsrpc.request.DeliveryType
import jp.co.soramitsu.fearless_utils.wsrpc.request.base.RpcRequest
import jp.co.soramitsu.fearless_utils.wsrpc.response.RpcResponse
import jp.co.soramitsu.fearless_utils.wsrpc.subscription.response.SubscriptionChange
import kotlinx.coroutines.future.asDeferred
import org.web3j.protocol.core.RemoteCall
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

    future.cancellable = executeBatchRequest(requests, deliveryType, callback)

    return future
}

fun SocketService.subscribeAsObservable(
    request: RpcRequest,
    unsubscribeMethod: String
): Observable<SubscriptionChange> {
    val subject = BehaviorSubject.create<SubscriptionChange>()

    val callback = object : SocketService.ResponseListener<SubscriptionChange> {
        override fun onError(throwable: Throwable) {
            subject.onError(throwable)
        }

        override fun onNext(response: SubscriptionChange) {
            subject.onNext(response)
        }
    }

    val cancellable = subscribe(request, callback, unsubscribeMethod)

    return subject.doOnDispose { cancellable.cancel() }
}

private class RequestCancellableFuture<T> : CompletableFuture<T>() {

    var cancellable: SocketService.Cancellable? = null

    override fun cancel(mayInterruptIfRunning: Boolean): Boolean {
        cancellable?.cancel()

        return super.cancel(mayInterruptIfRunning)
    }
}

suspend fun <S, T : Response<*>> Request<S, T>.sendSuspend(): T = sendAsync().asDeferred().await()
suspend fun <T> RemoteCall<T>.sendSuspend(): T = sendAsync().asDeferred().await()
