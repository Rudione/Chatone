package io.rudione.chatone.domain.mention

import kotlin.test.Test
import kotlin.test.assertEquals

class MentionInsertionTest {

    @Test
    fun emptyInputGetsTheMentionAndATrailingSpace() {
        assertEquals("@Rudionee ", MentionInsertion.append("", "Rudionee"))
        assertEquals("@Rudionee ", MentionInsertion.append("   ", "Rudionee"))
    }

    @Test
    fun textIsSeparatedBySingleSpaces() {
        assertEquals("какой-то текст @Rudionee ", MentionInsertion.append("какой-то текст", "Rudionee"))
        assertEquals("какой-то текст @Rudionee ", MentionInsertion.append("какой-то текст ", "Rudionee"))
    }

    @Test
    fun severalUsersChainNaturally() {
        val first = MentionInsertion.append("", "user1")
        val second = MentionInsertion.append(first, "user2")
        assertEquals("@user1 @user2 ", second)
        val mixed = MentionInsertion.append(MentionInsertion.append("текст @user1 текст", "user2"), "user3")
        assertEquals("текст @user1 текст @user2 @user3 ", mixed)
    }

    @Test
    fun theSameUserTwiceInARowIsNotDuplicated() {
        assertEquals("@user1 ", MentionInsertion.append("@user1 ", "user1"))
        assertEquals("hi @User1 ", MentionInsertion.append("hi @User1", "user1"))
    }

    @Test
    fun atSignsAndBlankNamesAreHandled() {
        assertEquals("@user2 ", MentionInsertion.append("", "@user2"))
        assertEquals("hello", MentionInsertion.append("hello", " "))
    }

    @Test
    fun appendAllAddsEveryAuthorOnceWithATrailingSpace() {
        assertEquals("@user1 @user2 @user3 ", MentionInsertion.appendAll("", listOf("user1", "user2", "User1", "user3")))
    }

    @Test
    fun appendAllSkipsNamesAlreadyTaggedAnywhere() {
        assertEquals("@user2, привет @user1 ", MentionInsertion.appendAll("@user2, привет", listOf("user2", "user1")))
    }

    @Test
    fun appendAllKeepsTheInputWhenNothingIsNew() {
        assertEquals("@user1 ", MentionInsertion.appendAll("@user1 ", listOf("@User1")))
        assertEquals("text", MentionInsertion.appendAll("text", emptyList()))
    }

    @Test
    fun localisedDisplayNamesFallBackToLogin() {
        assertEquals("Rudionee", MentionInsertion.preferredName("Rudionee", "rudionee"))
        assertEquals("hanako", MentionInsertion.preferredName("花子", "hanako"))
    }
}
