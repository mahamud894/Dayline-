package com.dayline.platform.summary

import java.time.LocalDate
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class SharedPreferencesSummaryDeliveryGateTest {
    @Test
    fun persistsSuccessfulDeliveryAcrossGateInstancesAndAllowsNextDate() = runTest {
        val context = RuntimeEnvironment.getApplication()
        val date = LocalDate.of(2026, 10, 18)
        var deliveries = 0
        val firstGate = SharedPreferencesSummaryDeliveryGate(context)

        assertTrue(firstGate.deliverOnce(date) { deliveries++ })
        assertFalse(SharedPreferencesSummaryDeliveryGate(context).deliverOnce(date) { deliveries++ })
        assertTrue(
            SharedPreferencesSummaryDeliveryGate(context)
                .deliverOnce(date.plusDays(1)) { deliveries++ }
        )

        assertEquals(2, deliveries)
    }

    @Test
    fun failedNotificationDoesNotConsumeTheDayDeliveryMarker() = runTest {
        val gate = SharedPreferencesSummaryDeliveryGate(RuntimeEnvironment.getApplication())
        val date = LocalDate.of(2026, 10, 19)

        try {
            gate.deliverOnce(date) { error("notification failure") }
            throw AssertionError("Expected delivery failure")
        } catch (exception: IllegalStateException) {
            assertEquals("notification failure", exception.message)
        }

        var delivered = false
        assertTrue(gate.deliverOnce(date) { delivered = true })
        assertTrue(delivered)
    }
}
