package io.novafoundation.nova.feature_staking_impl.presentation.parachainStaking.start.setup.model

sealed class ChooseCollatorResponse {

    object New: ChooseCollatorResponse()

    class Existing(val collatorModel: SelectCollatorModel): ChooseCollatorResponse()
}
