package io.novafoundation.nova.feature_dapp_impl.domain.browser.signExtrinsic

import io.novafoundation.nova.common.address.AddressModel
import io.novafoundation.nova.feature_account_api.presenatation.chain.ChainUi
import io.novafoundation.nova.feature_dapp_impl.presentation.browser.signExtrinsic.DAppSignCommunicator
import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import kotlinx.coroutines.flow.Flow
import java.math.BigInteger

interface DAppSignInteractor {

    val validationSystem: ConfirmDAppOperationValidationSystem

    suspend fun createAccountAddressModel(): AddressModel

    suspend fun chainUi(): ChainUi?

    fun commissionTokenFlow(): Flow<Token>?

    suspend fun calculateFee(): BigInteger

    suspend fun performOperation(): DAppSignCommunicator.Response?

    suspend fun readableOperationContent(): String

    fun shutdown() {}
}
