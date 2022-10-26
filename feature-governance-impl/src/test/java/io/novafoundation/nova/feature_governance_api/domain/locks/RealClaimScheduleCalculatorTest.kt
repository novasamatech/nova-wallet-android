package io.novafoundation.nova.feature_governance_api.domain.locks

import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class RealClaimScheduleCalculatorTest {

    @Test
    fun `should handle empty case`() = ClaimScheduleTest {
        given {
        }

        expect {

        }
    }

    @Test
    fun `should handle single claimable`() = ClaimScheduleTest {
        given {
            currentBlock(1000)

            track(0) {
                voting {
                    vote(amount = 1, referendumId = 0, unlockAt = 1000)
                }
            }
        }

        expect {
            claimable(amount = 1) {
                removeVote(trackId = 0, referendumId = 0)
                unlock(trackId = 0)
            }
        }
    }

    @Test
    fun `should handle both passed and not priors`() = ClaimScheduleTest {
        given {
            currentBlock(1000)

            track(0) {
                voting {
                    prior(amount = 2, unlockAt = 1000)
                }
            }

            track(1) {
                voting {
                    prior(amount = 1, unlockAt = 1100)
                }
            }
        }

        expect {
            claimable(amount = 1) {
                unlock(trackId = 0)
            }

            nonClaimable(amount = 1, claimAt = 1100)
        }
    }

    @Test
    fun `should extend votes by prior`() = ClaimScheduleTest {
        given {
            currentBlock(1000)

            track(0) {
                voting {
                    prior(amount = 1, unlockAt = 1100)

                    vote(amount = 2, unlockAt = 1000, referendumId = 1)
                }
            }
        }

        expect {
            nonClaimable(amount = 2, claimAt = 1100)
        }
    }

    @Test
    fun `should handle rejigged prior`() = ClaimScheduleTest {
        given {
            currentBlock(1200)

            track(0) {
                voting {
                    prior(amount = 1, unlockAt = 1100)

                    vote(amount = 2, unlockAt = 1000, referendumId = 1)
                }
            }
        }

        expect {
            claimable(amount = 2) {
                unlock(trackId = 0)
                removeVote(trackId = 0, referendumId = 1)
                unlock(trackId = 0)
            }
        }
    }

    @Test
    fun `should fold several claimable to one`() = ClaimScheduleTest {
        given {
            currentBlock(1100)

            track(0) {
                lock(0)

                voting {
                    vote(amount = 1, referendumId = 0, unlockAt = 1100)
                }
            }

            track(1) {
                lock(0)

                voting {
                    vote(amount = 2, referendumId = 1, unlockAt = 1000)
                }
            }
        }

        expect {
            claimable(amount = 2) {
                removeVote(trackId = 0, referendumId = 0)
                unlock(trackId = 0)

                removeVote(trackId = 1, referendumId = 1)
                unlock(trackId = 1)
            }
        }
    }

    /*
    a. 1 KSM unlocks at 1000
    b. 2 KSM unlocks at 1100
    c. 1 KSM unlocks at 1200
    thus a is shadowed by b
    ----
    Claimable should include actions for all (a), (b) and (c).
    */
    @Test
    fun `should include shadowed actions`() = ClaimScheduleTest {
        given {
            currentBlock(1200)

            track(1) {
                lock(0)

                voting {
                    vote(amount = 1, referendumId = 1, unlockAt = 1000)
                }
            }

            track(2) {
                lock(0)

                voting {
                    vote(amount = 2, referendumId = 2, unlockAt = 1100)
                }
            }

            track(3) {
                lock(0)

                voting {
                    vote(1, referendumId = 3, unlockAt = 1200)
                }
            }
        }

        expect {
            claimable(amount = 2) {
                removeVote(trackId = 3, referendumId = 3)
                unlock(3)

                removeVote(trackId = 2, referendumId = 2)
                unlock(2)

                removeVote(trackId = 1, referendumId = 1)
                unlock(1)
            }
        }
    }

    /*
      For track 0:
        track lock is 10, voting is 2. Thus, 2 claimable.
        There is one claimable voting for 1.
        So overall claimable from track is 9.
      For track 1:
        lock is 5, voting is 3: Thus, 2 claimable

      Overall, there is 9 claimable
      ---
      Track 0 has claimable voting => no extra action to claim gap required
      Track 1 has nothing to claim => need extra unlock
      Track 2 has prior that is not claimable => need extra unlock
     */
    @Test
    fun `should take gap into account`() = ClaimScheduleTest {
        given {
            currentBlock(1000)

            track(0) {
                lock(10)

                voting {
                    vote(amount = 2, referendumId = 0, unlockAt = 1000)
                }
            }

            track(1) {
                lock(5)
            }

            track(2) {
                lock(5)

                voting {
                    prior(amount = 1, unlockAt = 1100)
                }
            }
        }

        expect {
            claimable(amount = 9) {
                unlock(trackId = 1)
                unlock(trackId = 2)

                removeVote(trackId = 0, referendumId = 0)
                unlock(trackId = 0)
            }

            nonClaimable(amount = 1, claimAt = 1100)
        }
    }
}
