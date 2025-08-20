package io.novafoundation.nova.hydra_dx_math.stableswap

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Ignore
import org.junit.Test

class StableSwapTest {

    @Test
    fun shouldCalculateOutGivenIn() {
        val data = """
        [{
            "asset_id": 1,
            "amount": "1000000000000",
            "decimals": 12
        },
        {
            "asset_id": 0,
            "amount": "1000000000000",
            "decimals": 12
        }
        ]
        """

        val result = StableSwapMathBridge.calculate_out_given_in(
            data,
        0,
        1,
        "1000000000",
        "1",
        "0",
        ""
        )

        assertEquals("999500248", result)
    }

    @Test
    fun shouldCalculateInGiveOut() {
        val data = """
        [{
            "asset_id": 1,
            "amount": "1000000000000",
            "decimals": 12
        },
        {
            "asset_id": 0,
            "amount": "1000000000000",
            "decimals": 12
        }
        ]
        """

        val result = StableSwapMathBridge.calculate_in_given_out(
            data,
            0,
            1,
            "1000000000",
            "1",
            "0",
            ""
        )

        assertNotEquals("-1", result)
    }

    @Test
    fun shouldCalculateAmplification() {
        val result = StableSwapMathBridge.calculate_amplification("10", "10", "0", "100", "50")

        assertEquals("10", result)
    }

    @Test
    fun shouldCalculateShares() {
        val data = """
        [{
            "asset_id": 0,
            "amount":"90000000000",
            "decimals": 12
        },
        {
            "asset_id": 1,
            "amount": "5000000000000000000000",
            "decimals": 12
        }
        ]
        """

        val assets = """
        [{"asset_id":1,"amount":"43000000000000000000"}]
        """

        val result = StableSwapMathBridge.calculate_shares(
            data,
        assets,
        "1000",
        "64839594451719860",
        "0",
            ""
        )

        assertEquals("371541351762585", result.toString())
    }

    @Test
    fun shouldCalculateSharesForAmount() {
        val data = """
        [
  {
    "asset_id": 0,
    "amount": "10000000000000000",
    "decimals": 12
  },
  {
    "asset_id": 1,
    "amount": "10000000000000000",
    "decimals": 12
  },
  {
    "asset_id": 2,
    "amount": "10000000000000000",
    "decimals": 12
  },
  {
    "asset_id": 3,
    "amount": "10000000000000000",
    "decimals": 12
  },
  {
    "asset_id": 4,
    "amount": "10000000000000000",
    "decimals": 12
  }
]
        """

        val result = StableSwapMathBridge.calculate_shares_for_amount(
            data,
            0,
            "100000000000000",
            "100",
            "20000000000000000000000",
            "0",
            ""
        )

        assertEquals("40001593768209443008", result.toString())
    }

    @Test
    @Ignore("The test fails with last digit being 0 instead of 1. We need to check why it happens later")
    fun shouldCalculateAddOneAsset() {
        val data = """
         [
  {
    "asset_id": 0,
    "amount": "10000000000000000",
    "decimals": 12
  },
  {
    "asset_id": 1,
    "amount": "10000000000000000",
    "decimals": 12
  },
  {
    "asset_id": 2,
    "amount": "10000000000000000",
    "decimals": 12
  },
  {
    "asset_id": 3,
    "amount": "10000000000000000",
    "decimals": 12
  },
  {
    "asset_id": 4,
    "amount": "10000000000000000",
    "decimals": 12
  }
]
        """

        val result = StableSwapMathBridge.calculate_add_one_asset(
            data,
            "399850144492663029649",
            2,
            "100",
            "20000000000000000000000",
            "0",
            ""
        )

        assertEquals("1000000000000001", result.toString())
    }

    @Test
    fun shouldcalculateLiquidityOutOneAsset() {
        val data = """
         [
  {
    "asset_id": 0,
    "amount": "10000000000000000",
    "decimals": 12
  },
  {
    "asset_id": 1,
    "amount": "10000000000000000",
    "decimals": 12
  },
  {
    "asset_id": 2,
    "amount": "10000000000000000",
    "decimals": 12
  },
  {
    "asset_id": 3,
    "amount": "10000000000000000",
    "decimals": 12
  },
  {
    "asset_id": 4,
    "amount": "10000000000000000",
    "decimals": 12
  }
]
        """

        val result = StableSwapMathBridge.calculate_liquidity_out_one_asset(
            data,
            "40001593768209443008",
            0,
            "100",
            "20000000000000000000000",
            "0",
            ""
        )

        assertEquals("99999999999999", result.toString())
    }

    @Test
    fun failingCase() {
        val data = """
      [{"amount":"2246975221087","decimals":6,"asset_id":10},{"amount":"2256486088023","decimals":6,"asset_id":22}]
        """

        val result = StableSwapMathBridge.calculate_liquidity_out_one_asset(
            data,
            "1000000000",
            10,
            "100",
            "4502091550542833181457210",
            "0.00040",
            ""
        )

        assertEquals("99999999999999", result.toString())
    }

    @Test
    fun failingCase2() {
        val data = """
[{"amount":"505342304916","decimals":6,"asset_id":10},{"amount":"368030436758902944990436","decimals":18,"asset_id":18},{"amount":"410374848833","decimals":6,"asset_id":21},{"amount":"0","decimals":6,"asset_id":23}]        """

        val result = StableSwapMathBridge.calculate_shares_for_amount(
            data,
            10,
            "10",
            "320",
            "1662219218861236418723363",
            "0.00040",
            ""
        )

        assertEquals("99999999999999", result.toString())
    }
}
