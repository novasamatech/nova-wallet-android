package io.novafoundation.nova.feature_dapp_impl.domain.search

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StakingKeywordMatcherTest {

    @Test
    fun emptyAndWhitespaceQueriesReturnFalse() {
        assertFalse(StakingKeywordMatcher.isStakingQuery(""))
        assertFalse(StakingKeywordMatcher.isStakingQuery("   "))
        assertFalse(StakingKeywordMatcher.isStakingQuery("\n\t"))
    }

    @Test
    fun englishExactMatchReturnsTrue() {
        assertTrue(StakingKeywordMatcher.isStakingQuery("staking"))
        assertTrue(StakingKeywordMatcher.isStakingQuery("validator"))
        assertTrue(StakingKeywordMatcher.isStakingQuery("apy"))
        assertTrue(StakingKeywordMatcher.isStakingQuery("polkadot staking"))
    }

    @Test
    fun englishMatchIsCaseInsensitive() {
        assertTrue(StakingKeywordMatcher.isStakingQuery("STAKING"))
        assertTrue(StakingKeywordMatcher.isStakingQuery("Validator"))
    }

    @Test
    fun englishWhitespaceIsTrimmed() {
        assertTrue(StakingKeywordMatcher.isStakingQuery("  staking  "))
    }

    @Test
    fun englishPrefixMatchReturnsTrue() {
        assertTrue(StakingKeywordMatcher.isStakingQuery("staking apy"))
        assertTrue(StakingKeywordMatcher.isStakingQuery("validator selection"))
    }

    // English keywords intentionally use exact-or-prefix matching to avoid
    // false positives on common short words. Mid-string English queries
    // should NOT trigger.
    @Test
    fun englishMidStringDoesNotMatch() {
        assertFalse(StakingKeywordMatcher.isStakingQuery("best staking option"))
        assertFalse(StakingKeywordMatcher.isStakingQuery("where can i find a validator"))
    }

    @Test
    fun falsePositiveExclusionsReturnFalse() {
        assertFalse(StakingKeywordMatcher.isStakingQuery("stakeholder"))
        assertFalse(StakingKeywordMatcher.isStakingQuery("stakeholder meeting"))
        assertFalse(StakingKeywordMatcher.isStakingQuery("mistake"))
        assertFalse(StakingKeywordMatcher.isStakingQuery("validate email"))
        assertFalse(StakingKeywordMatcher.isStakingQuery("james bond"))
        assertFalse(StakingKeywordMatcher.isStakingQuery("sweepstake winner"))
    }

    @Test
    fun russianExactAndMidStringMatch() {
        assertTrue(StakingKeywordMatcher.isStakingQuery("стейкинг"))
        assertTrue(StakingKeywordMatcher.isStakingQuery("валидатор"))
        // mid-string substring match (NEW after fix)
        assertTrue(StakingKeywordMatcher.isStakingQuery("что такое стейкинг"))
        assertTrue(StakingKeywordMatcher.isStakingQuery("лучшие валидаторы"))
        assertTrue(StakingKeywordMatcher.isStakingQuery("как делать стейкинг"))
    }

    @Test
    fun turkishExactAndAgglutinatedMatch() {
        assertTrue(StakingKeywordMatcher.isStakingQuery("stake yapmak"))
        assertTrue(StakingKeywordMatcher.isStakingQuery("ödüller"))
        assertTrue(StakingKeywordMatcher.isStakingQuery("doğrulayıcı"))
        // mid-string + agglutination
        assertTrue(StakingKeywordMatcher.isStakingQuery("en iyi doğrulayıcılar"))
    }

    @Test
    fun japaneseSubstringMatch() {
        assertTrue(StakingKeywordMatcher.isStakingQuery("ステーキング"))
        assertTrue(StakingKeywordMatcher.isStakingQuery("ステーキング 方法"))
        assertTrue(StakingKeywordMatcher.isStakingQuery("Polkadotステーキング"))
    }

    @Test
    fun koreanSubstringMatch() {
        assertTrue(StakingKeywordMatcher.isStakingQuery("스테이킹"))
        assertTrue(StakingKeywordMatcher.isStakingQuery("DOT 스테이킹"))
    }

    @Test
    fun chineseSubstringMatch() {
        assertTrue(StakingKeywordMatcher.isStakingQuery("质押"))
        assertTrue(StakingKeywordMatcher.isStakingQuery("如何质押"))
    }

    @Test
    fun nonStakingQueriesReturnFalse() {
        assertFalse(StakingKeywordMatcher.isStakingQuery("weather today"))
        assertFalse(StakingKeywordMatcher.isStakingQuery("uniswap"))
        assertFalse(StakingKeywordMatcher.isStakingQuery("swap dot"))
        assertFalse(StakingKeywordMatcher.isStakingQuery("nft marketplace"))
    }
}
