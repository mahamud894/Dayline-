package com.dayline.ui

import android.content.Context
import android.content.res.Configuration
import com.dayline.R
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class LocalizationTest {
    private val stringIds: List<Pair<String, Int>> = R.string::class.java.fields
        .map { it.name to it.getInt(null) }
        .filter { (name, _) -> name != "app_name" }

    @Test
    fun everyStringFormatsInEnglishAndBangla() {
        listOf(Locale.ENGLISH, Locale.forLanguageTag("bn")).forEach { locale ->
            val context = localized(locale)
            stringIds.forEach { (name, id) ->
                val raw = context.getString(id)
                val args = placeholderArgs(raw)
                val formatted = runCatching { context.getString(id, *args) }
                assertTrue("$locale/$name failed to format: $raw", formatted.isSuccess)
            }
        }
    }

    @Test
    fun banglaAndEnglishUseTheSamePlaceholders() {
        val english = localized(Locale.ENGLISH)
        val bangla = localized(Locale.forLanguageTag("bn"))
        stringIds.forEach { (name, id) ->
            assertEquals(
                "Placeholder mismatch in $name",
                placeholders(english.getString(id)).sorted(),
                placeholders(bangla.getString(id)).sorted()
            )
        }
    }

    @Test
    fun banglaIsActuallyTranslated() {
        val bangla = localized(Locale.forLanguageTag("bn"))
        val english = localized(Locale.ENGLISH)
        assertNotEquals(english.getString(R.string.nav_today), bangla.getString(R.string.nav_today))
        assertEquals("আজ", bangla.getString(R.string.nav_today))
    }

    @Test
    fun everyDatePatternIsValidInBothLanguages() {
        val sample = LocalDateTime.of(2026, 10, 19, 15, 0)
        val patterns = listOf(
            R.string.pattern_long_date,
            R.string.pattern_short_date,
            R.string.pattern_time,
            R.string.pattern_task_due,
            R.string.pattern_deadline_date
        )
        listOf(Locale.ENGLISH, Locale.forLanguageTag("bn")).forEach { locale ->
            val context = localized(locale)
            patterns.forEach { id ->
                sample.format(DateTimeFormatter.ofPattern(context.getString(id), locale))
            }
        }
        assertEquals(
            "Mon, Oct 19 at 3:00 PM",
            sample.format(
                DateTimeFormatter.ofPattern(
                    localized(Locale.ENGLISH).getString(R.string.pattern_task_due),
                    Locale.ENGLISH
                )
            )
        )
    }

    private fun localized(locale: Locale): Context {
        val base = RuntimeEnvironment.getApplication()
        val config = Configuration(base.resources.configuration).apply { setLocale(locale) }
        return base.createConfigurationContext(config)
    }

    private fun placeholders(text: String): List<String> =
        PLACEHOLDER.findAll(text).map { it.value }.toList()

    private fun placeholderArgs(text: String): Array<Any> {
        val byIndex = PLACEHOLDER.findAll(text).associate { match ->
            match.groupValues[1].toInt() to match.groupValues[2]
        }
        return Array(byIndex.keys.maxOrNull() ?: 0) { index ->
            if (byIndex[index + 1] == "d") 3 else "sample"
        }
    }

    private companion object {
        val PLACEHOLDER = Regex("%(\\d+)\\$([ds])")
    }
}
