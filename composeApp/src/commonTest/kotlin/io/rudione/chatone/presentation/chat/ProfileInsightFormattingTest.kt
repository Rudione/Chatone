package io.rudione.chatone.presentation.chat

import io.rudione.chatone.data.remote.LogMonth
import io.rudione.chatone.presentation.chat.roles.groupDigits
import io.rudione.chatone.presentation.theme.i18n.ProfileInsightStringsEn
import io.rudione.chatone.presentation.theme.i18n.ProfileInsightStringsRu
import kotlin.test.Test
import kotlin.test.assertEquals

class ProfileInsightFormattingTest {

    @Test
    fun monthsAreShortWithTwoDigitYear() {
        assertEquals("sep'26", shortMonthLabel(ProfileInsightStringsEn, LogMonth(2026, 9)))
        assertEquals("янв'05", shortMonthLabel(ProfileInsightStringsRu, LogMonth(2005, 1)))
    }

    @Test
    fun followerCountsAreGrouped() {
        assertEquals("77,816", groupDigits(77_816, ","))
        assertEquals("1 234 567", groupDigits(1_234_567, " "))
        assertEquals("999", groupDigits(999, ","))
    }

    @Test
    fun russianPluralsFollowTheCount() {
        assertEquals("1 канал", ProfileInsightStringsRu.rolesChannels(1))
        assertEquals("3 канала", ProfileInsightStringsRu.rolesChannels(3))
        assertEquals("12 каналов", ProfileInsightStringsRu.rolesChannels(12))
        assertEquals("21 канал", ProfileInsightStringsRu.rolesChannels(21))
    }
}
