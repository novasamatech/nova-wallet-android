package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.confirm

import android.content.DialogInterface
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.inOrder
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify

class BittensorStakeResultUiBindingTest {

    @Test
    fun `clearing the sheet detaches its dismiss listener before dismiss and releases it`() {
        val sheet = mock(BittensorStakeResultBottomSheet::class.java)
        var dismissCalls = 0
        var listener: DialogInterface.OnDismissListener? = DialogInterface.OnDismissListener { dismissCalls++ }
        doAnswer {
            listener = null
            null
        }.`when`(sheet).setOnDismissListener(null)
        doAnswer {
            listener?.onDismiss(sheet)
            null
        }.`when`(sheet).dismiss()

        var currentSheet: BittensorStakeResultBottomSheet? = sheet
        currentSheet = currentSheet.dismissWithoutResultCallback()

        assertNull(currentSheet)
        assertEquals(0, dismissCalls)
        val order = inOrder(sheet)
        order.verify(sheet).setOnDismissListener(null)
        order.verify(sheet).dismiss()

        // A second null emission or view destruction must not retain/dismiss the old dialog again.
        currentSheet = currentSheet.dismissWithoutResultCallback()
        assertNull(currentSheet)
        verify(sheet, times(1)).dismiss()
    }
}
