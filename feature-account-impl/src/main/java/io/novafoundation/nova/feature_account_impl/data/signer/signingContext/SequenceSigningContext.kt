package io.novafoundation.nova.feature_account_impl.data.signer.signingContext

import io.novafoundation.nova.common.address.AccountIdKey
import io.novafoundation.nova.feature_account_api.data.signer.SigningContext
import io.novasama.substrate_sdk_android.runtime.extrinsic.Nonce
import java.math.BigInteger

class SequenceSigningContext(
    private val delegate: SigningContext
) : SigningContext by delegate {

    private var offset: BigInteger = BigInteger.ZERO

    fun incrementNonceOffset() {
        offset += BigInteger.ONE
    }

    override suspend fun getNonce(accountId: AccountIdKey): Nonce {
        val delegateNonce = delegate.getNonce(accountId)

        return delegateNonce + offset
    }
}

fun SigningContext.withSequenceSigning(): SequenceSigningContext {
    return SequenceSigningContext(this)
}
