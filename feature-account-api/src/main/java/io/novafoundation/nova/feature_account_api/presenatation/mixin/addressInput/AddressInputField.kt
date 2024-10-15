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
import io.novafoundation.nova.common.utils.inflater
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
import io.novafoundation.nova.feature_account_api.databinding.ViewAddressInputBinding
import io.novafoundation.nova.feature_account_api.presenatation.mixin.addressInput.externalAccount.ExternalAccount

class AddressInputField @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : LinearLayout(context, attrs, defStyle), WithContextExtensions by WithContextExtensions(context) {

    private val binder = ViewAddressInputBinding.inflate(inflater(), this)

    val content: EditText
        get() = binder.addressInputAddress

    init {
        orientation = VERTICAL

        View.inflate(context, R.layout.view_address_input, this)

        binder.addressInputW3NAddress.setDrawableStart(R.drawable.ic_checkmark_circle_16, tint = R.color.icon_positive, paddingInDp = 4)
        binder.addressInputW3NAddress.setDrawableEnd(R.drawable.ic_info, paddingInDp = 4)

        setAddStatesFromChildren(true)

        setBackgrounds()

        attrs?.let(::applyAttributes)
    }

    fun setState(state: AddressInputState) {
        setIdenticonState(state.iconState)

        binder.addressInputScan.setVisible(state.scanShown)
        binder.addressInputPaste.setVisible(state.pasteShown)
        binder.addressInputClear.setVisible(state.clearShown)
        binder.addressInputMyself.setVisible(state.myselfShown)
    }

    fun setExternalAccount(externalAccountState: ExtendedLoadingState<ExternalAccount?>) {
        if (binder.addressInputW3NContainer.isGone) return

        when {
            externalAccountState is ExtendedLoadingState.Loading -> {
                binder.addressInputW3NAddress.makeInvisible()
                binder.addressInputW3NProgress.makeVisible()
            }

            externalAccountState is ExtendedLoadingState.Loaded && externalAccountState.data != null -> {
                val externalAccount = externalAccountState.data!!
                binder.addressInputW3NAddress.text = externalAccount.addressWithDescription
                binder.addressInputW3NAddress.makeVisible()
                binder.addressInputW3NProgress.makeInvisible()
            }

            externalAccountState is ExtendedLoadingState.Loaded && externalAccountState.data == null -> {
                binder.addressInputW3NAddress.text = null
                binder.addressInputW3NAddress.makeInvisible()
                binder.addressInputW3NProgress.makeInvisible()
            }
        }
    }

    fun onPasteClicked(listener: OnClickListener) {
        binder.addressInputPaste.setOnClickListener(listener)
    }

    fun onClearClicked(listener: OnClickListener) {
        binder.addressInputClear.setOnClickListener(listener)
    }

    fun onScanClicked(listener: OnClickListener) {
        binder.addressInputScan.setOnClickListener(listener)
    }

    fun onMyselfClicked(listener: OnClickListener) {
        binder.addressInputMyself.setOnClickListener(listener)
    }

    fun onExternalAddressClicked(listener: OnClickListener) {
        binder.addressInputW3NAddress.setOnClickListener(listener)
    }

    private fun setIdenticonState(state: AddressInputState.IdenticonState) {
        when (state) {
            is AddressInputState.IdenticonState.Address -> {
                binder.addressInputIdenticon.makeVisible()
                binder.addressInputIdenticon.setImageDrawable(state.drawable)
            }
            AddressInputState.IdenticonState.Placeholder -> {
                binder.addressInputIdenticon.makeVisible()
                binder.addressInputIdenticon.setImageResource(R.drawable.ic_identicon_placeholder)
            }
        }
    }

    private fun setBackgrounds() = with(context) {
        binder.addressInputField.background = context.getInputBackground()

        binder.addressInputPaste.background = buttonBackground()
        binder.addressInputMyself.background = buttonBackground()
        binder.addressInputScan.background = buttonBackground()
    }

    private fun Context.buttonBackground() = addRipple(getRoundedCornerDrawable(R.color.button_background_secondary))

    private fun applyAttributes(attrs: AttributeSet) = context.useAttributes(attrs, R.styleable.AddressInputField) {
        val hint = it.getString(R.styleable.AddressInputField_android_hint)
        hint?.let { content.hint = hint }

        val hasExternalAccountIdentifiers = it.getBoolean(R.styleable.AddressInputField_hasExternalAccountIdentifiers, false)
        binder.addressInputW3NContainer.isVisible = hasExternalAccountIdentifiers
        if (hasExternalAccountIdentifiers) {
            binder.addressInputW3NAddress.background = getRoundedCornerDrawable(cornerSizeDp = 6)
                .withRippleMask(getRippleMask(cornerSizeDp = 6))
        }
    }
}
