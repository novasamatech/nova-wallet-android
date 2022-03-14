package io.novafoundation.nova.feature_account_api.presenatation.mixin.addressInput

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import io.novafoundation.nova.common.utils.makeVisible
import io.novafoundation.nova.common.utils.setVisible
import io.novafoundation.nova.common.view.shape.addRipple
import io.novafoundation.nova.common.view.shape.getInputBackground
import io.novafoundation.nova.common.view.shape.getRoundedCornerDrawable
import io.novafoundation.nova.feature_account_api.R
import kotlinx.android.synthetic.main.view_address_input.view.addressInputAddress
import kotlinx.android.synthetic.main.view_address_input.view.addressInputClear
import kotlinx.android.synthetic.main.view_address_input.view.addressInputIdenticon
import kotlinx.android.synthetic.main.view_address_input.view.addressInputPaste
import kotlinx.android.synthetic.main.view_address_input.view.addressInputScan

class AddressInputField @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : LinearLayout(context, attrs, defStyle) {

    val content: EditText
        get() = addressInputAddress

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL

        View.inflate(context, R.layout.view_address_input, this)

        setAddStatesFromChildren(true)

        setBackgrounds()
    }

    fun setState(state: AddressInputState) {
        setIdenticonState(state.iconState)

        addressInputScan.setVisible(state.scanShown)
        addressInputPaste.setVisible(state.pasteShown)
        addressInputClear.setVisible(state.clearShown)
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
        background = context.getInputBackground()

        addressInputPaste.background = addRipple(getRoundedCornerDrawable(R.color.white_8))
        addressInputScan.background = addRipple(getRoundedCornerDrawable(R.color.white_8))
    }
}
