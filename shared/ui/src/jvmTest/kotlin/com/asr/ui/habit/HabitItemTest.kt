package com.asr.ui.habit

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.asr.core.habit.Habit
import com.asr.core.habit.HabitFrequency
import com.asr.core.habit.HabitRecord
import com.asr.core.habit.HabitState
import com.asr.core.interfaces.SoundPlayer
import kotlinx.datetime.LocalDate
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.core.context.stopKoin
import org.koin.core.context.startKoin
import org.koin.dsl.module

class HabitItemTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val habit = Habit(
        id = 1,
        title = "Morning run",
        frequencyType = HabitFrequency.DAILY,
        frequencyCount = 1,
    )

    private val date = LocalDate(2026, 7, 14)

    @Before
    fun setup() {
        stopKoin()
        startKoin {
            modules(module {
                single<SoundPlayer> { object : SoundPlayer { override fun play(pitch: Float) {} } }
            })
        }
    }

    @After
    fun teardown() {
        stopKoin()
    }

    @Test
    fun `shows done button and skip when not done`() {
        composeTestRule.setContent {
            HabitItem(
                habit = habit,
                record = HabitRecord(habitId = 1, date = date, state = HabitState.NOT_DONE),
                onSetState = {},
            )
        }
        composeTestRule.onNodeWithContentDescription("Mark done").assertIsDisplayed()
        composeTestRule.onNodeWithText("Skip").assertIsDisplayed()
    }

    @Test
    fun `clicking done button fires done state`() {
        var captured: HabitState? = null
        composeTestRule.setContent {
            HabitItem(
                habit = habit,
                record = HabitRecord(habitId = 1, date = date, state = HabitState.NOT_DONE),
                onSetState = { captured = it },
            )
        }
        composeTestRule.onNodeWithContentDescription("Mark done").performClick()
        assert(captured == HabitState.DONE) { "expected DONE, got $captured" }
    }

    @Test
    fun `shows done icon when done`() {
        composeTestRule.setContent {
            HabitItem(
                habit = habit,
                record = HabitRecord(habitId = 1, date = date, state = HabitState.DONE),
                onSetState = {},
            )
        }
        composeTestRule.onNodeWithContentDescription("Done").assertIsDisplayed()
    }

    @Test
    fun `hides skip button when skipped`() {
        composeTestRule.setContent {
            HabitItem(
                habit = habit,
                record = HabitRecord(habitId = 1, date = date, state = HabitState.SKIPPED),
                onSetState = {},
            )
        }
        composeTestRule.onAllNodesWithText("Skip").assertCountEquals(0)
    }

    @Test
    fun `shows streak when greater than zero`() {
        composeTestRule.setContent {
            HabitItem(
                habit = habit,
                record = HabitRecord(habitId = 1, date = date, state = HabitState.DONE),
                onSetState = {},
                streak = 5,
            )
        }
        composeTestRule.onNodeWithText(" 5").assertIsDisplayed()
    }

    @Test
    fun `shows overflow menu when optional callbacks provided`() {
        composeTestRule.setContent {
            HabitItem(
                habit = habit,
                record = HabitRecord(habitId = 1, date = date, state = HabitState.NOT_DONE),
                onSetState = {},
                onEdit = {},
                onDelete = {},
            )
        }
        composeTestRule.onNodeWithText("⋮").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Log").assertCountEquals(0)
        composeTestRule.onAllNodesWithText("↑").assertCountEquals(0)
    }

    @Test
    fun `shows title and progress text`() {
        composeTestRule.setContent {
            HabitItem(
                habit = habit,
                record = HabitRecord(habitId = 1, date = date, state = HabitState.DONE, count = 1),
                periodCount = 1,
                onSetState = {},
            )
        }
        composeTestRule.onNodeWithText("Morning run").assertIsDisplayed()
        composeTestRule.onNodeWithText("1 / 1 Daily").assertIsDisplayed()
    }
}
