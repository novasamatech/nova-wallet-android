package io.novafoundation.nova.feature_account_api.presenatation.mixin.addressInput

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import androidx.core.view.isGone
import androidx.core.view.isVisible
import io.novafoundation.nova.common.domain.ExtendedLoadingState
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
import io.novafoundation.nova.feature_account_api.presenatation.mixin.addressInput.externalAccount.ExternalAccount
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

        addressInputW3NAddress.setDrawableStart(R.drawable.ic_checkmark_circle_16, tint = R.color.icon_positive, paddingInDp = 4)
        addressInputW3NAddress.setDrawableEnd(R.drawable.ic_info, paddingInDp = 4)

        setAddStatesFromChildren(true)

        setBackgrounds()

        attrs?.let(::applyAttributes)
    }

    fun setState(state: AddressInputState) {
        setIdenticonState(state.iconState)

        addressInputScan.setVisible(state.scanShown && isEnabled)
        addressInputPaste.setVisible(state.pasteShown && isEnabled)
        addressInputClear.setVisible(state.clearShown && isEnabled)
        addressInputMyself.setVisible(state.myselfShown && isEnabled)
    }

    fun setExternalAccount(externalAccountState: ExtendedLoadingState<ExternalAccount?>) {
        if (addressInputW3NContainer.isGone) return

        when {
            externalAccountState is ExtendedLoadingState.Loading -> {
                addressInputW3NAddress.makeInvisible()
                addressInputW3NProgress.makeVisible()
            }

            externalAccountState is ExtendedLoadingState.Loaded && externalAccountState.data != null -> {
                val externalAccount = externalAccountState.data!!
                addressInputW3NAddress.text = externalAccount.addressWithDescription
                addressInputW3NAddress.makeVisible()
                addressInputW3NProgress.makeInvisible()
            }

            externalAccountState is ExtendedLoadingState.Loaded && externalAccountState.data == null -> {
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

    override fun setEnabled(enabled: Boolean) {
        content.isEnabled = enabled
        super.setEnabled(enabled)
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
        isEnabled = it.getBoolean(R.styleable.AddressInputField_android_enabled, true)

        val hint = it.getString(R.styleable.AddressInputField_android_hint)
        hint?.let { content.hint = hint }

        val hasExternalAccountIdentifiers = it.getBoolean(R.styleable.AddressInputField_hasExternalAccountIdentifiers, false)
        addressInputW3NContainer.isVisible = hasExternalAccountIdentifiers
        if (hasExternalAccountIdentifiers) {
            addressInputW3NAddress.background = getRoundedCornerDrawable(cornerSizeDp = 6)
                .withRippleMask(getRippleMask(cornerSizeDp = 6))
        }
    }
}
