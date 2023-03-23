package io.novafoundation.nova.feature_account_api.presenatation.mixin.addressInput

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import androidx.core.view.isGone
import androidx.core.view.isVisible
import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.common.domain.dataOrNull
import io.novafoundation.nova.common.domain.loadedNothing
import io.novafoundation.nova.common.presentation.toShortAddressFormat
import io.novafoundation.nova.common.utils.WithContextExtensions
import io.novafoundation.nova.common.utils.makeInvisible
import io.novafoundation.nova.common.utils.makeVisible
import io.novafoundation.nova.common.utils.setDrawableEnd
import io.novafoundation.nova.common.utils.setDrawableStart
import io.novafoundation.nova.common.utils.setVisible
import io.novafoundation.nova.common.utils.useAttributes
import io.novafoundation.nova.common.view.shape.addRipple
import io.novafoundation.nova.common.view.shape.getInputBackground
import io.novafoundation.nova.common.view.shape.getRoundedCornerDrawable
import io.novafoundation.nova.feature_account_api.R
import kotlinx.android.synthetic.main.view_address_input.view.addressInputAddress
import kotlinx.android.synthetic.main.view_address_input.view.addressInputClear
import kotlinx.android.synthetic.main.view_address_input.view.addressInputField
import kotlinx.android.synthetic.main.view_address_input.view.addressInputIdenticon
import kotlinx.android.synthetic.main.view_address_input.view.addressInputMyself
import kotlinx.android.synthetic.main.view_address_input.view.addressInputPaste
import kotlinx.android.synthetic.main.view_address_input.view.addressInputScan
import kotlinx.android.synthetic.main.view_address_input.view.addressInputW3NAddress
import kotlinx.android.synthetic.main.view_address_input.view.addressInputW3NContainer
import kotlinx.android.synthetic.main.view_address_input.view.addressInputW3NProgress

class AddressInputField @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : LinearLayout(context, attrs, defStyle), WithContextExtensions by WithContextExtensions(context) {

    val content: EditText
        get() = addressInputAddress

    init {
        orientation = VERTICAL

        View.inflate(context, R.layout.view_address_input, this)

        addressInputW3NAddress.setDrawableStart(R.drawable.ic_checkmark_circle_16, tint = R.color.icon_positive)
        addressInputW3NAddress.setDrawableEnd(R.drawable.ic_info_cicrle_filled_16, tint = R.color.icon_secondary)

        setAddStatesFromChildren(true)

        setBackgrounds()

        attrs?.let(::applyAttributes)
    }

    fun setState(state: AddressInputState) {
        setIdenticonState(state.iconState)

        setExternalAccount(loadedNothing())
        addressInputScan.setVisible(state.scanShown)
        addressInputPaste.setVisible(state.pasteShown)
        addressInputClear.setVisible(state.clearShown)
        addressInputMyself.setVisible(state.myselfShown)
    }

    fun setExternalAccount(externalAccountLoadingState: ExtendedLoadingState<ExternalAccount?>) {
        if (addressInputW3NContainer.isGone) return

        val externalAccount = externalAccountLoadingState.dataOrNull

        when {
            externalAccount != null -> {
                addressInputW3NAddress.text = externalAccount.addressWithDescription(context)
                setIdenticonState(externalAccount.icon)
                addressInputW3NAddress.makeVisible()
                addressInputW3NProgress.makeInvisible()
            }
            externalAccountLoadingState is ExtendedLoadingState.Loading -> {
                addressInputW3NAddress.makeInvisible()
                addressInputW3NProgress.makeVisible()
            }
            externalAccountLoadingState is ExtendedLoadingState.Error -> {}
            else -> {
                addressInputW3NAddress.text = null
                addressInputW3NAddress.makeInvisible()
                addressInputW3NProgress.makeInvisible()
            }
        }
    }

    fun onPasteClicked(listener: OnClickListener) {
        addressInputPaste.setOnClickListener(listener)
    }

    fun onClearClicked(listener: OnClickListener) {
        addressInputClear.setOnClickListener(listener)
    }

    fun onScanClicked(listener: OnClickListener) {
        addressInputScan.setOnClickListener(listener)
    }

    fun onMyselfClicked(listener: OnClickListener) {
        addressInputMyself.setOnClickListener(listener)
    }

    fun onExternalAddressClicked(listener: OnClickListener) {
        addressInputW3NAddress.setOnClickListener(listener)
    }

    private fun setIdenticonState(state: AddressInputState.IdenticonState) {
        when (state) {
            is AddressInputState.IdenticonState.Address -> {
                addressInputIdenticon.makeVisible()
                addressInputIdenticon.setImageDrawable(state.drawable)
            }
            AddressInputState.IdenticonState.Placeholder -> {
                addressInputIdenticon.makeVisible()
                addressInputIdenticon.setImageResource(R.drawable.ic_identicon_placeholder)
            }
        }
    }

    private fun setBackgrounds() = with(context) {
        addressInputField.background = context.getInputBackground()

        addressInputPaste.background = buttonBackground()
        addressInputMyself.background = buttonBackground()
        addressInputScan.background = buttonBackground()
    }

    private fun Context.buttonBackground() = addRipple(getRoundedCornerDrawable(R.color.button_background_secondary))

    private fun applyAttributes(attrs: AttributeSet) = context.useAttributes(attrs, R.styleable.AddressInputField) {
        val hint = it.getString(R.styleable.AddressInputField_android_hint)
        hint?.let { content.hint = hint }

        val externalAccountIdentifiers = it.getBoolean(R.styleable.AddressInputField_externalAccountIdentifiers, false)
        addressInputW3NContainer.isVisible = externalAccountIdentifiers
        if (externalAccountIdentifiers) {
            addressInputW3NAddress.background = getRoundedCornerDrawable(cornerSizeDp = 6)
                .withRippleMask(getRippleMask(cornerSizeDp = 6))
        }
    }
}
