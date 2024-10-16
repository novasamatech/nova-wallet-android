package io.novafoundation.nova.feature_account_api.data.fee.types.hydra

import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.runtime.extrinsic.ExtrinsicBuilder
import java.math.BigInteger

interface HydrationFeeInjector {

    class SetFeesMode(
        val setMode: SetMode,
        val resetMode: ResetMode
    )

    sealed class SetMode {

        /**
         * Always sets the fee to the required token, regardless of whether fees are already in the needed state or not
         */
        object Always : SetMode()

        /**
         * Sets the fee token to the required one only the current fee payment asset is different
         */
        class Lazy(val currentlySetFeeAsset: BigInteger) : SetMode()
    }

    sealed class ResetMode {

        /**
         * Always resets the fee to the native token, regardless of whether fees are already in the needed state or not
         */
        object ToNative : ResetMode()

        /**
         * Resets the the fee to the native one only the current fee payment asset is different
         */
        class ToNativeLazily(val feeAssetBeforeTransaction: BigInteger) : ResetMode()
    }

    suspend fun setFees(
        extrinsicBuilder: ExtrinsicBuilder,
        paymentAsset: Chain.Asset,
        mode: SetFeesMode,
    )
}
