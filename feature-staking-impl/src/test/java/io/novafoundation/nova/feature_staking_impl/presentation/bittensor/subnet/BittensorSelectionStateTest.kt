package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.subnet

import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorNetuid
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorQuickPickOrder
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorSubnetSorting
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorValidatorSorting
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BittensorSelectionStateTest {

    @Test
    fun `quick pick catalogue starts with corresponding sort and thin pool filter`() {
        val expected = mapOf(
            BittensorQuickPickOrder.POOL_SIZE to BittensorSubnetSorting.POOL_DEPTH,
            BittensorQuickPickOrder.AGE to BittensorSubnetSorting.AGE,
            BittensorQuickPickOrder.APY to BittensorSubnetSorting.APY
        )
        expected.forEach { (order, sorting) ->
            val browse = BittensorSubnetBrowseState.fromQuickPick(order)
            val selection = BittensorSelectionState()
            val payload = BittensorChooseSubnetPayload(true, browse, selection)
            assertEquals(sorting, payload.browseState.sorting)
            assertTrue(payload.browseState.hideThinPools)
        }
    }

    @Test
    fun `manual catalogue keeps default sort and shows thin pools`() {
        val browse = BittensorSubnetBrowseState.fromQuickPick(null)
        assertEquals(BittensorSubnetSorting.SEVEN_DAY_CHANGE, browse.sorting)
        assertFalse(browse.hideThinPools)
    }

    @Test
    fun `catalogue edits survive returning through details and amount without changing validator filters`() {
        val initial = BittensorSelectionState()
        val browse = BittensorSubnetBrowseState(BittensorSubnetSorting.NAME, true)
        val edited = initial.copy(browseState = browse)
        val details = BittensorSubnetDetailsPayload(1, selectionState = edited)
        val amount = BittensorSubnetAmountPayload(1, selectionState = details.selectionState)
        val back = BittensorSelectValidatorCommunicator.Response(null, null, amount.selectionState)
        val returned = initial.withResponse(back)

        assertEquals(browse, returned.browseState)
        assertEquals(initial.rootFilters, returned.rootFilters)
        assertEquals(initial.subnetFilters, returned.subnetFilters)
    }

    @Test
    fun `root and subnet settings survive a back response independently`() {
        val initial = BittensorSelectionState()
        val rootFilters = BittensorValidatorFilters(BittensorValidatorSorting.NAME, false)
        val subnetFilters = BittensorValidatorFilters(BittensorValidatorSorting.TOTAL_STAKE, true)
        val rootChanged = initial.withFilters(BittensorNetuid.Root, rootFilters)
        val rootBack = BittensorSelectValidatorCommunicator.Response(null, null, rootChanged)
        val parent = initial.withResponse(rootBack)
        val subnetChanged = parent.withFilters(BittensorNetuid.Subnet(1), subnetFilters)
        val subnetBack = BittensorSelectValidatorCommunicator.Response(null, null, subnetChanged)
        val returned = parent.withResponse(subnetBack)

        assertEquals(rootFilters, returned.filtersFor(BittensorNetuid.Root))
        assertEquals(subnetFilters, returned.filtersFor(BittensorNetuid.Subnet(2)))
        assertEquals(BittensorValidatorFilters(), initial.rootFilters)
        assertEquals(BittensorValidatorFilters(), initial.subnetFilters)
    }

    @Test
    fun `a different flow cannot return filters or a selected address into this flow`() {
        val own = BittensorSelectionState()
        val foreign = BittensorSelectionState()
        val changed = foreign.withFilters(BittensorNetuid.Root, BittensorValidatorFilters(BittensorValidatorSorting.NAME, false))
        val response = BittensorSelectValidatorCommunicator.Response(0, "foreign-validator", changed)

        assertFalse(own.accepts(response))
        assertEquals(own, own.withResponse(response))
    }

    @Test
    fun `subnet details amount and root switch carry the same independent filter snapshot`() {
        val rootFilters = BittensorValidatorFilters(BittensorValidatorSorting.NAME, false)
        val initial = BittensorSelectionState(rootFilters = rootFilters)
        val picker = BittensorChooseSubnetPayload(selectionState = initial)
        val details = BittensorSubnetDetailsPayload(1, selectionState = picker.selectionState)
        val amount = BittensorSubnetAmountPayload(1, selectionState = details.selectionState)
        val request = BittensorSelectValidatorCommunicator.Request(0, null, amount.selectionState)

        assertEquals(rootFilters, request.selectionState.filtersFor(BittensorNetuid.Root))
        assertEquals(BittensorValidatorFilters(), request.selectionState.filtersFor(BittensorNetuid.Subnet(1)))
        val newFlow = BittensorChooseSubnetPayload()
        assertNotEquals(initial.flowId, newFlow.selectionState.flowId)
        assertEquals(BittensorValidatorFilters(), newFlow.selectionState.rootFilters)
    }
}
