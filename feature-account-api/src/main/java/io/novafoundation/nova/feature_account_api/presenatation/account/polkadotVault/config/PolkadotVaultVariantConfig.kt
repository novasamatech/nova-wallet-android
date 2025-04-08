package io.novafoundation.nova.feature_account_api.presenatation.account.polkadotVault.config

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

class PolkadotVaultVariantConfig(
    val connect: Connect,
    val sign: Sign,
    val common: Common
) {

    class Connect(val instructions: List<Instruction>) {

        sealed class Instruction {

            class Step(val index: Int, val content: CharSequence) : Instruction()

            class Image(val label: String, @DrawableRes val imageRes: Int) : Instruction()
        }
    }

    class Sign(val troubleShootingLink: String, val supportsProofSigning: Boolean)

    class Common(@DrawableRes val iconRes: Int, @StringRes val nameRes: Int)
}
