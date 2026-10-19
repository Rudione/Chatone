package io.rudione.chatone.presentation.stream.channel

import io.rudione.chatone.domain.channel.ChannelSnapshot
import io.rudione.chatone.domain.channel.ChannelSnapshotSource
import io.rudione.chatone.domain.follow.ChannelFollowGateway
import io.rudione.chatone.domain.follow.FollowFailure
import io.rudione.chatone.domain.follow.FollowResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class StreamChannelViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    private class FakeFollows(
        var following: FollowResult<Boolean> = FollowResult.Done(false),
        var change: FollowResult<Unit> = FollowResult.Done(Unit)
    ) : ChannelFollowGateway {
        val changes = mutableListOf<Pair<String, Boolean>>()
        override suspend fun isFollowing(channelId: String) = following
        override suspend fun setFollowing(channelId: String, follow: Boolean): FollowResult<Unit> {
            changes += channelId to follow
            return change
        }
    }

    private class FakeSnapshots(var answer: ChannelSnapshot? = null) : ChannelSnapshotSource {
        val lookups = mutableListOf<String>()
        override suspend fun snapshot(login: String): ChannelSnapshot? {
            lookups += login
            return answer
        }
    }

    private val full = ChannelSnapshot(
        login = "streamer",
        channelId = "42",
        displayName = "Streamer",
        avatarUrl = "https://cdn/avatar.png",
        title = "Title",
        category = "Just Chatting"
    )

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun completeHintNeedsNoLookupAndLoadsTheFollowState() = runTest(dispatcher) {
        val snapshots = FakeSnapshots()
        val vm = StreamChannelViewModel(FakeFollows(following = FollowResult.Done(true)), snapshots)

        vm.sendEvent(StreamChannelEvent.Bind("streamer", full))
        advanceUntilIdle()

        assertEquals(full, vm.state.value.channel)
        assertEquals(FollowStatus.FOLLOWING, vm.state.value.follow)
        assertEquals(emptyList(), snapshots.lookups)
    }

    @Test
    fun switchingTheChatAwayKeepsWhatIsKnownAboutTheStream() = runTest(dispatcher) {
        val vm = StreamChannelViewModel(FakeFollows(), FakeSnapshots())

        vm.sendEvent(StreamChannelEvent.Bind("streamer", full))
        advanceUntilIdle()
        vm.sendEvent(StreamChannelEvent.Bind("streamer", ChannelSnapshot(login = "streamer")))
        advanceUntilIdle()

        assertEquals(full, vm.state.value.channel)
    }

    @Test
    fun missingDetailsAreLookedUpOnce() = runTest(dispatcher) {
        val snapshots = FakeSnapshots(answer = full)
        val vm = StreamChannelViewModel(FakeFollows(), snapshots)

        vm.sendEvent(StreamChannelEvent.Bind("streamer", ChannelSnapshot(login = "streamer", displayName = "Streamer")))
        advanceUntilIdle()
        vm.sendEvent(StreamChannelEvent.Bind("streamer"))
        advanceUntilIdle()

        assertEquals(listOf("streamer"), snapshots.lookups)
        assertEquals("https://cdn/avatar.png", vm.state.value.channel.avatarUrl)
        assertEquals("42", vm.state.value.channel.channelId)
        assertEquals(FollowStatus.NOT_FOLLOWING, vm.state.value.follow)
    }

    @Test
    fun signedOutViewerCannotToggle() = runTest(dispatcher) {
        val follows = FakeFollows(following = FollowResult.Failed(FollowFailure.SIGNED_OUT))
        val vm = StreamChannelViewModel(follows, FakeSnapshots())

        vm.sendEvent(StreamChannelEvent.Bind("streamer", full))
        advanceUntilIdle()
        vm.sendEvent(StreamChannelEvent.ToggleFollow)
        advanceUntilIdle()

        assertFalse(vm.state.value.canToggleFollow)
        assertEquals(emptyList(), follows.changes)
    }

    @Test
    fun toggleFollowsAChannelThatIsNotFollowed() = runTest(dispatcher) {
        val follows = FakeFollows()
        val vm = StreamChannelViewModel(follows, FakeSnapshots())

        vm.sendEvent(StreamChannelEvent.Bind("streamer", full))
        advanceUntilIdle()
        vm.sendEvent(StreamChannelEvent.ToggleFollow)
        advanceUntilIdle()

        assertEquals(listOf("42" to true), follows.changes)
        assertEquals(FollowStatus.FOLLOWING, vm.state.value.follow)
        assertFalse(vm.state.value.followBusy)
    }

    @Test
    fun blockedFollowKeepsTheStatusAndShowsTheReasonForAWhile() = runTest(dispatcher) {
        val vm = StreamChannelViewModel(FakeFollows(change = FollowResult.Failed(FollowFailure.BLOCKED)), FakeSnapshots())

        vm.sendEvent(StreamChannelEvent.Bind("streamer", full))
        advanceUntilIdle()
        vm.sendEvent(StreamChannelEvent.ToggleFollow)
        advanceTimeBy(100)

        assertEquals(FollowStatus.NOT_FOLLOWING, vm.state.value.follow)
        assertEquals(FollowFailure.BLOCKED, vm.state.value.failure)

        advanceUntilIdle()
        assertNull(vm.state.value.failure)
    }

    @Test
    fun anotherStreamResetsTheFollowState() = runTest(dispatcher) {
        val follows = FakeFollows(following = FollowResult.Done(true))
        val vm = StreamChannelViewModel(follows, FakeSnapshots())

        vm.sendEvent(StreamChannelEvent.Bind("streamer", full))
        advanceUntilIdle()
        follows.following = FollowResult.Done(false)
        vm.sendEvent(StreamChannelEvent.Bind("other", full.copy(login = "other", channelId = "7")))
        advanceUntilIdle()

        assertEquals("other", vm.state.value.channel.login)
        assertEquals(FollowStatus.NOT_FOLLOWING, vm.state.value.follow)
    }
}
