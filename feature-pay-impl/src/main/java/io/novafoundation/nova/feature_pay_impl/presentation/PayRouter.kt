package io.novafoundation.nova.feature_pay_impl.presentation

import io.novafoundation.nova.common.navigation.ReturnableRouter

interface PayRouter : ReturnableRouter {
    fun openSwitchWallet()

    fun openShopSearch()
}
