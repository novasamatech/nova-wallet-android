package io.novafoundation.nova.feature_staking_impl.domain.announcements

import io.novafoundation.nova.common.data.announcements.AnnouncementsRepository
import io.novafoundation.nova.common.domain.announcements.Announcement
import io.novafoundation.nova.common.domain.announcements.AnnouncementSection
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface StakingAnnouncementsUseCase {

    fun generalAnnouncementsFlow(): Flow<List<Announcement>>

    fun announcementsByChainFlow(): Flow<Map<ChainId, Announcement>>

    fun announcementFlow(chainId: ChainId): Flow<Announcement?>
}

class RealStakingAnnouncementsUseCase(
    private val announcementsRepository: AnnouncementsRepository
) : StakingAnnouncementsUseCase {

    private val stakingAnnouncements = announcementsRepository.announcementsFlow(AnnouncementSection.STAKING)

    override fun generalAnnouncementsFlow(): Flow<List<Announcement>> {
        return stakingAnnouncements.map { announcements -> announcements.filter { it.chainId == null } }
    }

    override fun announcementsByChainFlow(): Flow<Map<ChainId, Announcement>> {
        return stakingAnnouncements.map { announcements ->
            announcements.mapNotNull { announcement -> announcement.chainId?.let { it to announcement } }
                .groupBy({ it.first }, { it.second })
                .mapValues { (_, forChain) -> forChain.first() }
        }
    }

    override fun announcementFlow(chainId: ChainId): Flow<Announcement?> {
        return stakingAnnouncements.map { announcements -> announcements.firstOrNull { it.chainId == chainId } }
    }
}
