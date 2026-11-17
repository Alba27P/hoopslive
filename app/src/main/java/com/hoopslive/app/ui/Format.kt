package com.hoopslive.app.ui

import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

object Fmt {
    private val time = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
    private val dayShort = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault())
    private val dayNum = DateTimeFormatter.ofPattern("d", Locale.getDefault())
    private val wdFmt = DateTimeFormatter.ofPattern("EEE", Locale.getDefault())
    private val monFmt = DateTimeFormatter.ofPattern("MMM", Locale.getDefault())
    private val longDate = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)

    fun localTime(i: Instant?): String =
        i?.atZone(ZoneId.systemDefault())?.format(time) ?: "TBD"

    fun localDay(i: Instant?): String =
        i?.atZone(ZoneId.systemDefault())?.format(dayShort) ?: ""

    fun day(d: LocalDate): String = d.format(dayShort)
    fun dayNumber(d: LocalDate): String = d.format(dayNum)
    fun weekday(d: LocalDate): String = d.format(wdFmt).uppercase()
    fun month(d: LocalDate): String = d.format(monFmt).uppercase()

    fun isoDate(s: String?): String? =
        s?.let { runCatching { LocalDate.parse(it).format(longDate) }.getOrNull() ?: it }

    fun money(v: Long?): String {
        if (v == null) return "-"
        return NumberFormat.getCurrencyInstance(Locale.US).apply { maximumFractionDigits = 0 }.format(v)
    }

    fun moneyShort(v: Long?): String {
        if (v == null) return "-"
        return when {
            v >= 1_000_000 -> String.format(Locale.US, "$%.1fM", v / 1_000_000.0)
            v >= 1_000 -> String.format(Locale.US, "$%.0fK", v / 1_000.0)
            else -> "$$v"
        }
    }

    fun seasonLabel(year: Int): String = "${year - 1}-${(year % 100).toString().padStart(2, '0')}"

    fun periodName(index: Int): String = when {
        index < 4 -> "Q${index + 1}"
        index == 4 -> "OT"
        else -> "${index - 3}OT"
    }
}
