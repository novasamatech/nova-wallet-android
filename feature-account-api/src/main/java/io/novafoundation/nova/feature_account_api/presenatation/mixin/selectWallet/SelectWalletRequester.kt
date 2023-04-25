package io.novafoundation.nova.feature_account_api.presenatation.mixin.selectWallet

import io.novafoundation.nova.common.navigation.InterScreenRequester
import io.novafoundation.nova.common.navigation.InterScreenResponder
import io.novafoundation.nova.feature_account_api.presenatation.mixin.selectWallet.SelectWalletCommunicator.Payload
import io.novafoundation.nova.feature_account_api.presenatation.mixin.selectWallet.SelectWalletCommunicator.Response

interface SelectWalletRequester : InterScreenRequester<Payload, Response>

interface SelectWalletResponder : InterScreenResponder<Payload, Response>

interface SelectWalletCommunicator : SelectWalletRequester, SelectWalletResponder {

    class Payload(val currentMetaId: Long)

    class Response(val newMetaId: Long)
}
