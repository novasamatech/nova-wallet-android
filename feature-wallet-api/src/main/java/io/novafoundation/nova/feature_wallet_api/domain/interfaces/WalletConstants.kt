package io.novafoundation.nova.feature_wallet_api.domain.interfaces

import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import java.math.BigInteger

interface WalletConstants {

    suspend fun existentialDeposit(chainId: ChainId): BigInteger
}
