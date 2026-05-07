package io.novafoundation.nova.feature_staking_impl.presentation.subtensor.stake.validatorPicker

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Selection threaded back from [SubtensorValidatorPickerFragment] to the
 * stake setup screen. Pairs the SS58 hotkey (used for extrinsic signing
 * and address-actions) with the resolved identity (used for cell labels
 * on Setup + Confirm) so we don't need a second lookup downstream.
 */
@Parcelize
data class SubtensorPickedValidator(
    val hotkeyAddress: String,
    val identity: String?,
) : Parcelable
