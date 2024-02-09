package io.novafoundation.nova.feature_proxy_api.di

import io.novafoundation.nova.feature_proxy_api.data.common.ProxyDepositCalculator
import io.novafoundation.nova.feature_proxy_api.data.repository.GetProxyRepository
import io.novafoundation.nova.feature_proxy_api.data.repository.ProxyConstantsRepository

interface ProxyFeatureApi {

    val proxyRepository: GetProxyRepository

    val proxyDepositCalculator: ProxyDepositCalculator

    val proxyConstantsRepository: ProxyConstantsRepository
}
