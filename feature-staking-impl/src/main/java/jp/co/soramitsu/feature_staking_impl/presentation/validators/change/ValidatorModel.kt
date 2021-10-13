package jp.co.soramitsu.feature_staking_impl.presentation.validators.change

import android.graphics.drawable.Drawable
import jp.co.soramitsu.feature_staking_api.domain.model.Validator

data class ValidatorModel(
    val accountIdHex: String,
    val slashed: Boolean,
    val image: Drawable,
    val address: String,
    val scoring: Scoring?,
    val title: String,
    val isChecked: Boolean?,
    val validator: Validator,
) {

    sealed class Scoring {
        class OneField(val field: String) : Scoring()

        class TwoFields(val primary: String, val secondary: String?) : Scoring()
    }
}
