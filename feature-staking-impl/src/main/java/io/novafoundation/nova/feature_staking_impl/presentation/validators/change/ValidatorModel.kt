package io.novafoundation.nova.feature_staking_impl.presentation.validators.change

import androidx.annotation.ColorRes
import io.novafoundation.nova.common.address.AddressModel
import io.novafoundation.nova.feature_staking_api.domain.model.Validator

typealias ValidatorModel = StakeTargetModel<Validator>

data class StakeTargetModel<V>(
    val accountIdHex: String,
    val slashed: Boolean,
    val scoring: Scoring?,
    val subtitle: Subtitle?,
    val addressModel: AddressModel,
    val isChecked: Boolean?,
    val stakeTarget: V,
) {

    data class ColoredText(
        val text: String,
        @ColorRes val colorRes: Int,
    )

    data class Subtitle(
        val label: String,
        val value: ColoredText
    )

    sealed class Scoring {
        class OneField(val field: ColoredText) : Scoring()

        class TwoFields(val primary: String, val secondary: String?) : Scoring()
    }
}
