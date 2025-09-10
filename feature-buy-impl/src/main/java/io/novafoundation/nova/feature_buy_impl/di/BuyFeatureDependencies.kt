package io.novafoundation.nova.feature_buy_impl.di

import com.google.gson.Gson
import io.novafoundation.nova.common.mixin.actionAwaitable.ActionAwaitableMixin
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.ip.IpAddressReceiver
import io.novafoundation.nova.common.utils.webView.InterceptingWebViewClientFactory
import io.novafoundation.nova.feature_account_api.domain.interfaces.SelectedAccountUseCase
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.amount.AmountFormatter
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import okhttp3.OkHttpClient

interface BuyFeatureDependencies {

    val amountFormatter: AmountFormatter

    val chainRegistry: ChainRegistry

    val accountUseCase: SelectedAccountUseCase

    val actionAwaitableMixinFactory: ActionAwaitableMixin.Factory

    val interceptingWebViewClientFactory: InterceptingWebViewClientFactory

    val gson: Gson

    val okHttpClient: OkHttpClient

    val resourceManager: ResourceManager

    val ipAddressReceiver: IpAddressReceiver
}
