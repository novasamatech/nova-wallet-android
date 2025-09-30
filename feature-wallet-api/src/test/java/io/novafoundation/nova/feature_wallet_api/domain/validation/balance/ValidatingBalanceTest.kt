package io.novafoundation.nova.feature_wallet_api.domain.validation.balance

import io.novafoundation.nova.common.domain.balance.EDCountingMode
import io.novafoundation.nova.common.domain.balance.TransferableMode
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets.balances.model.ChainAssetBalance
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance
import io.novafoundation.nova.feature_wallet_api.domain.validation.balance.ValidatingBalance.BalancePreservation
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner

private val TEST_ED = 10.toBigInteger()
private val ED_COUNTING_MODE = EDCountingMode.FREE
private val TRANSFERABLE_MODE = TransferableMode.HOLDS_AND_FREEZES

@RunWith(MockitoJUnitRunner::class)
class ValidatingBalanceTest {

    @Mock
    lateinit var chainAsset: Chain.Asset

    @Test
    fun shouldWithdrawWithoutCrossingEd() {
        val initial = createBalance(free = 100, frozen = 0, reserved = 0)
        val actual = initial.tryWithdraw(10, BalancePreservation.KEEP_ALIVE)

        assertDeductSuccess(free = 90, reserved = 0, frozen = 0, actual = actual)
    }

    @Test
    fun shouldWithdrawCrossingEd() {
        val initial = createBalance(free = 11, frozen = 0, reserved = 0)
        val actual = initial.tryWithdraw(2, BalancePreservation.ALLOW_DEATH)

        assertDeductSuccess(free = 0, reserved = 0, frozen = 0, actual = actual)
    }

    @Test
    fun shouldWithdrawFailingToCrossEd() {
        val initial = createBalance(free = 11, frozen = 0, reserved = 0)
        val actual = initial.tryWithdraw(2, BalancePreservation.KEEP_ALIVE)

        // After fixing ed-related imbalance, the resulting free should just be ed
        assertDeductFailure(free = 10, reserved = 0, frozen = 0, negativeImbalance = 1, actual = actual)
    }

    @Test
    fun withdrawShouldChooseMaxOfTwoImbalances() {
        val initial = createBalance(free = 20, frozen = 10, reserved = 5)
        assetBalanceEquals(20, initial.assetBalance.countedTowardsEd)
        // See holdAndFreezesTransferable
        assetBalanceEquals(15, initial.assetBalance.transferable)

        var actual = initial.tryWithdraw(25, BalancePreservation.ALLOW_DEATH)
        // Since AllowDeath is used, countedTowardsEd check does not apply - we are just 10 tokens short due to transferable
        // Note: free < ed since reserved > 0. See reservedPreventsDusting
        assertDeductFailure(negativeImbalance = 10, free = 5, frozen = 10, reserved = 5, actual)

        actual = initial.tryWithdraw(25, BalancePreservation.KEEP_ALIVE)
        // Now due to KEEP_ALIVE we are 15 tokens short due to countedTowardsEd
        assertDeductFailure(negativeImbalance = 15, free = 10, frozen = 10, reserved = 5, actual)
    }

    @Test
    fun shouldDoSimpleReserve() {
        val initial = createBalance(free = 20, frozen = 0, reserved = 0)
        val actual = initial.tryReserve(5)

        assertDeductSuccess(free = 15, frozen = 0, reserved = 5, actual)
    }

    @Test
    fun shouldFailReserve() {
        val initial = createBalance(free = 20, frozen = 0, reserved = 0)
        assetBalanceEquals(10, initial.reservable())

        val actual = initial.tryReserve(15)

        assertDeductFailure(negativeImbalance = 5, free = 10, frozen = 0, reserved = 15, actual)
    }

    @Test
    fun shouldDoAroundEdReserve() {
        val initial = createBalance(free = 20, frozen = 5, reserved = 10)
        val actual = initial.tryReserve(10)

        assertDeductSuccess(free = 10, frozen = 5, reserved = 20, actual)
    }

    @Test
    fun shouldFreeze() {
        val initial =  createBalance(free = 20, frozen = 5, reserved = 10)
        assetBalanceEquals(30, initial.assetBalance.total)
        val actual = initial.tryFreeze(30)

        assertDeductSuccess(free = 20, frozen = 30, reserved = 10, actual)
    }

    @Test
    fun shouldFailFreeze() {
        val initial =  createBalance(free = 20, frozen = 5, reserved = 10)
        assetBalanceEquals(30, initial.assetBalance.total)
        val actual = initial.tryFreeze(35)

        assertDeductFailure(negativeImbalance = 5, free = 25, frozen = 35, reserved = 10, actual)
    }

    // This test describe cross-chain transfer case where we first pay fee, then withdraw the transfer amount and then withdraw the delivery fee
    @Test
    fun shouldCombineMultipleWithdraws() {
        val initial = createBalance(free = 20, frozen = 0, reserved = 0)

        val actual = initial
            .tryWithdraw(5, BalancePreservation.KEEP_ALIVE) // aka withdraw fee. New free is 15
            .tryWithdraw(20,  BalancePreservation.KEEP_ALIVE) // aka withdraw transfer amount. We are 15 tokens short here. Fixed imbalance results in ed (10) in free
            .tryWithdraw(15, BalancePreservation.ALLOW_DEATH) // aka withdraw delivery fee. We are 5 tokens short here

        assertDeductFailure(negativeImbalance = 20, free = 0, frozen = 0, reserved = 0, actual)
    }

    // This test describe cross-chain transfer case where we first pay fee, then withdraw the transfer amount and then withdraw the delivery fee
    @Test
    fun shouldCombineMultipleDifferentDeductions() {
        val initial = createBalance(free = 20, frozen = 0, reserved = 0)

        val actual = initial
            .tryWithdraw(5, BalancePreservation.KEEP_ALIVE) // aka withdraw fee. New free is 15
            .tryReserve(10) // we are 5 tokens short here (reservable = 15)
            .tryFreeze(10) // this succeeds after fixing previous imbalance

        assertDeductFailure(negativeImbalance = 5, free = 10, frozen = 10, reserved = 10, actual)
    }

    private fun ValidatingBalance.reservable(): Balance {
        return assetBalance.reservable(existentialDeposit)
    }

    private fun BalanceValidationResult.tryWithdraw(amount: Int, preservation: BalancePreservation): BalanceValidationResult {
        return tryWithdraw(amount.toBigInteger(), preservation)
    }

    private fun BalanceValidationResult.tryFreeze(amount: Int): BalanceValidationResult {
        return tryFreeze(amount.toBigInteger())
    }

    private fun BalanceValidationResult.tryReserve(amount: Int): BalanceValidationResult {
        return tryReserve(amount.toBigInteger())
    }

    private fun ValidatingBalance.tryWithdraw(amount: Int, preservation: BalancePreservation): BalanceValidationResult {
        return tryWithdraw(amount.toBigInteger(), preservation)
    }

    private fun ValidatingBalance.tryReserve(amount: Int): BalanceValidationResult {
        return tryReserve(amount.toBigInteger())
    }

    private fun ValidatingBalance.tryFreeze(amount: Int): BalanceValidationResult {
        return tryFreeze(amount.toBigInteger())
    }

    private fun assetBalanceEquals(expected: Int, actual: Balance) {
        assertEquals(expected.toBigInteger(), actual)
    }

    private fun assertDeductSuccess(
        free: Int,
        frozen: Int,
        reserved: Int,
        actual: BalanceValidationResult
    ) {
        assert(actual is BalanceValidationResult.Success)
        actual as BalanceValidationResult.Success

        assertEquals(free.toBigInteger(), actual.newBalance.assetBalance.free)
        assertEquals(frozen.toBigInteger(), actual.newBalance.assetBalance.frozen)
        assertEquals(reserved.toBigInteger(), actual.newBalance.assetBalance.reserved)
    }

    // free, frozen, reserved - amount of successful deduction when negativeImbalance is satisfied
    private fun assertDeductFailure(
        negativeImbalance: Int,
        free: Int,
        frozen: Int,
        reserved: Int,
        actual: BalanceValidationResult
    ) {
        assert(actual is BalanceValidationResult.Failure)
        actual as BalanceValidationResult.Failure

        assertEquals(negativeImbalance.toBigInteger(), actual.negativeImbalance.value)
        assertEquals(free.toBigInteger(), actual.newBalanceAfterFixingImbalance.assetBalance.free)
        assertEquals(frozen.toBigInteger(), actual.newBalanceAfterFixingImbalance.assetBalance.frozen)
        assertEquals(reserved.toBigInteger(), actual.newBalanceAfterFixingImbalance.assetBalance.reserved)
    }

    private fun createBalance(free: Int, frozen: Int, reserved: Int) : ValidatingBalance {
        val balance = ChainAssetBalance(
            chainAsset = chainAsset,
            free = free.toBigInteger(),
            reserved = reserved.toBigInteger(),
            frozen = frozen.toBigInteger(),
            transferableMode = TRANSFERABLE_MODE,
            edCountingMode = ED_COUNTING_MODE
        )

        return ValidatingBalance(balance, TEST_ED)
    }
}
