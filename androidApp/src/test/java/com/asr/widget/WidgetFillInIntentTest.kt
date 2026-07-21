package com.asr.widget

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class WidgetFillInIntentTest {

    @Test
    fun `task fill-in intent has toggle action and task id`() {
        val intent = taskFillInIntent(taskId = 42, appWidgetId = 1)
        assertEquals(TodayWidgetProvider.ACTION_TOGGLE_TASK, intent.action)
        assertEquals(42L, intent.getLongExtra("task_id", 0))
        assertEquals(1, intent.getIntExtra("appWidgetId", 0))
    }

    @Test
    fun `habit fill-in intent has increment action and habit id`() {
        val intent = habitFillInIntent(habitId = 99, appWidgetId = 1)
        assertEquals(TodayWidgetProvider.ACTION_INCREMENT_HABIT, intent.action)
        assertEquals(99L, intent.getLongExtra("habit_id", 0))
        assertEquals(1, intent.getIntExtra("appWidgetId", 0))
    }
}
