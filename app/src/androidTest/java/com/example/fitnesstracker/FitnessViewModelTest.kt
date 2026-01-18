package com.example.fitnesstracker

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class FitnessViewModelTest {

    private lateinit var viewModel: FitnessViewModel
    private lateinit var app: Application

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()

        // نفرغ SharedPreferences باش كل test يبدا نقي
        app.getSharedPreferences("fitness_prefs", Application.MODE_PRIVATE)
            .edit().clear().commit()

        viewModel = FitnessViewModel(app)
    }

    // ---------------- BASIC STATE TESTS ----------------

    @Test
    fun step_tracking_disabled_by_default() {
        assertFalse(viewModel.stepTrackingEnabled.value)
    }

    @Test
    fun toggle_theme_changes_value() {
        val initial = viewModel.isDarkTheme.value
        viewModel.toggleTheme()
        assertNotEquals(initial, viewModel.isDarkTheme.value)
    }

    // ---------------- STEPS & METRICS ----------------

    @Test
    fun onSensorStepChanged_does_nothing_when_tracking_disabled() {
        viewModel.onSensorStepChanged(1000f)
        assertEquals(0, viewModel.stepCount.value)
    }

    @Test
    fun steps_are_calculated_correctly_when_tracking_enabled() {
        viewModel.setStepTrackingEnabled(true)

        viewModel.onSensorStepChanged(1000f) // baseline
        viewModel.onSensorStepChanged(1500f)

        assertEquals(500, viewModel.stepCount.value)
    }

    @Test
    fun calories_are_calculated_correctly() {
        viewModel.setStepTrackingEnabled(true)

        viewModel.onSensorStepChanged(1000f)
        viewModel.onSensorStepChanged(2000f) // 1000 steps

        assertEquals(40, viewModel.calories.value) // 1000 * 0.04
    }

    @Test
    fun distance_is_calculated_correctly() {
        viewModel.setStepTrackingEnabled(true)

        viewModel.onSensorStepChanged(1000f)
        viewModel.onSensorStepChanged(2000f)

        assertEquals(0.76f, viewModel.distance.value, 0.001f)
    }

    @Test
    fun floors_are_calculated_correctly() {
        viewModel.setStepTrackingEnabled(true)

        viewModel.onSensorStepChanged(1000f)
        viewModel.onSensorStepChanged(2500f) // 1500 steps

        assertEquals(3, viewModel.floors.value) // 1500 / 500
    }

    // ---------------- ACTIVE TIME ----------------

    @Test
    fun active_time_increases_when_tracking_enabled() {
        viewModel.setStepTrackingEnabled(true)

        viewModel.addActiveTime(120_000L) // 2 minutes

        assertEquals(2, viewModel.activeTime.value)
    }

    // ---------------- WEEKLY STATS ----------------

    @Test
    fun weeklySteps_always_contains_7_days() {
//        assertEquals(7, viewModel.weeklySteps.value.size)
        assertTrue(7==viewModel.weeklySteps.value.size)
    }

    @Test
    fun topActiveDay_is_calculated() {
        viewModel.setStepTrackingEnabled(true)

        viewModel.onSensorStepChanged(1000f)
        viewModel.onSensorStepChanged(4000f)

        val topDay = viewModel.topActiveDay.value
        assertTrue(topDay.second >= 0)
    }
}
