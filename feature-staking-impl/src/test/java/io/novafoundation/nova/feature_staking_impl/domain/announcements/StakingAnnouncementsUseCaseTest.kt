package io.novafoundation.nova.feature_staking_impl.domain.announcements

import io.novafoundation.nova.common.data.announcements.AnnouncementsRepository
import io.novafoundation.nova.common.domain.announcements.Announcement
import io.novafoundation.nova.common.domain.announcements.AnnouncementSection
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

private const val POLKADOT = "polkadot"
private const val KUSAMA = "kusama"

class StakingAnnouncementsUseCaseTest {

    private val general = announcement(chainId = null, description = "General")
    private val polkadot = announcement(chainId = POLKADOT, description = "Polkadot")
    private val kusama = announcement(chainId = KUSAMA, description = "Kusama")

    @Test
    fun `general flow keeps only announcements without a chain`() = runBlocking {
        val result = useCase(general, polkadot, kusama).generalAnnouncementsFlow().first()

        assertEquals(listOf("General"), result.map { it.description })
    }

    @Test
    fun `by chain flow keeps only announcements with a chain`() = runBlocking {
        val result = useCase(general, polkadot, kusama).announcementsByChainFlow().first()

        assertEquals(setOf(POLKADOT, KUSAMA), result.keys)
        assertEquals("Polkadot", result.getValue(POLKADOT).description)
    }

    @Test
    fun `first announcement wins when a chain has several`() = runBlocking {
        val second = announcement(chainId = POLKADOT, description = "Second")

        val byChain = useCase(polkadot, second).announcementsByChainFlow().first()
        val single = useCase(polkadot, second).announcementFlow(POLKADOT).first()

        assertEquals("Polkadot", byChain.getValue(POLKADOT).description)
        assertEquals("Polkadot", single?.description)
    }

    @Test
    fun `announcement flow ignores other chains and general ones`() = runBlocking {
        val useCase = useCase(general, kusama)

        assertNull(useCase.announcementFlow(POLKADOT).first())
    }

    private fun useCase(vararg announcements: Announcement): StakingAnnouncementsUseCase {
        return RealStakingAnnouncementsUseCase(FakeAnnouncementsRepository(announcements.toList()))
    }

    private fun announcement(chainId: String?, description: String): Announcement {
        return Announcement(chainId = chainId, style = Announcement.Style.INFO, description = description)
    }
}

private class FakeAnnouncementsRepository(
    private val announcements: List<Announcement>
) : AnnouncementsRepository {

    override fun announcementsFlow(section: AnnouncementSection): Flow<List<Announcement>> {
        return flowOf(announcements)
    }
}
