package com.example.sipherdiary.ui.calendar

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sipherdiary.data.repository.DiaryEntry
import com.example.sipherdiary.data.repository.MoodType
import com.example.sipherdiary.ui.components.*
import com.example.sipherdiary.ui.theme.SipherColor
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Layar 12 — Kalender mood bulanan. Data diambil dari catatan yang sudah didekripsi di memori. */
@Composable
fun CalendarScreen(
    entries: List<DiaryEntry>,
    onOpenEntry: (String) -> Unit,
    onWriteForToday: () -> Unit,
    onNavigateTab: (String) -> Unit
) {
    val localeId = Locale.forLanguageTag("id-ID")
    var month by remember { mutableStateOf(YearMonth.now()) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }

    val byDate = remember(entries) { entries.groupBy { it.date } }
    val monthEntries = remember(entries, month) { entries.filter { YearMonth.from(it.date) == month } }
    // Semua mood (unik) per tanggal → ditampilkan sebagai titik-titik di kalender
    val moodsByDate: Map<LocalDate, List<MoodType>> = remember(byDate) {
        byDate.mapValues { (_, list) -> list.map { it.mood }.distinct().sortedBy { it.code } }
    }
    // Ringkasan dihitung per CATATAN (bukan per hari), jadi 3 catatan di hari yang sama tetap terhitung 3
    val moodSummary = remember(monthEntries) { monthEntries.groupingBy { it.mood }.eachCount() }
    val selectedEntries = byDate[selectedDate].orEmpty()

    Scaffold(
        containerColor = SipherColor.Bg,
        bottomBar = { SipherNavBar(currentRoute = "calendar", onNavigate = onNavigateTab) }
    ) { inner ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { ScreenHeader(title = "Kalender", overline = "Suasana hatimu") }

            item {
                SipherCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { month = month.minusMonths(1) }) {
                            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, "Bulan sebelumnya", tint = SipherColor.Ink900)
                        }
                        Text(
                            month.format(DateTimeFormatter.ofPattern("MMMM yyyy", localeId))
                                .replaceFirstChar { it.titlecase(localeId) },
                            fontSize = 17.sp, fontWeight = FontWeight.Bold, color = SipherColor.Ink900,
                            modifier = Modifier.weight(1f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        IconButton(onClick = { month = month.plusMonths(1) }) {
                            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, "Bulan berikutnya", tint = SipherColor.Ink900)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth()) {
                        listOf("Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min").forEach {
                            Text(
                                it, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = SipherColor.Ink600,
                                modifier = Modifier.weight(1f),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    MonthGrid(
                        month = month,
                        selected = selectedDate,
                        moodsByDate = moodsByDate,
                        onSelect = { selectedDate = it }
                    )
                }
            }

            item { SectionLabel("Ringkasan bulan ini · ${monthEntries.size} catatan") }
            item {
                SipherCard {
                    if (moodSummary.isEmpty()) {
                        Text("Belum ada catatan di bulan ini.", fontSize = 13.5.sp, color = SipherColor.Ink600)
                    } else {
                        val total = moodSummary.values.sum().toFloat()
                        MoodType.entries.forEach { mood ->
                            val count = moodSummary[mood] ?: 0
                            Row(
                                modifier = Modifier.padding(vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(Modifier.size(10.dp).clip(CircleShape).background(Color(mood.colorHex)))
                                Spacer(Modifier.width(8.dp))
                                Text(mood.label, fontSize = 13.5.sp, color = SipherColor.Ink900, modifier = Modifier.width(70.dp))
                                Box(
                                    Modifier
                                        .weight(1f)
                                        .height(8.dp)
                                        .clip(CircleShape)
                                        .background(SipherColor.Subtle)
                                ) {
                                    Box(
                                        Modifier
                                            .fillMaxHeight()
                                            .fillMaxWidth(count / total)
                                            .clip(CircleShape)
                                            .background(Color(mood.colorHex))
                                    )
                                }
                                Spacer(Modifier.width(10.dp))
                                Text("$count catatan", fontSize = 12.5.sp, color = SipherColor.Ink600, modifier = Modifier.width(64.dp))
                            }
                        }
                    }
                }
            }

            item {
                SectionLabel(
                    selectedDate.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", localeId))
                )
            }
            if (selectedEntries.isEmpty()) {
                item {
                    SipherCard(color = SipherColor.Subtle) {
                        Text("Tidak ada catatan di tanggal ini.", fontSize = 13.5.sp, color = SipherColor.Ink600)
                        if (selectedDate == LocalDate.now()) {
                            Spacer(Modifier.height(10.dp))
                            TextButton(onClick = onWriteForToday, contentPadding = PaddingValues(0.dp)) {
                                Text("+ Tulis catatan hari ini", color = SipherColor.Blue500, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            } else {
                items(selectedEntries, key = { it.id }) { entry ->
                    EntryCard(entry = entry, onClick = { onOpenEntry(entry.id) })
                }
            }
        }
    }
}

@Composable
private fun MonthGrid(
    month: YearMonth,
    selected: LocalDate,
    moodsByDate: Map<LocalDate, List<MoodType>>,
    onSelect: (LocalDate) -> Unit
) {
    val first = month.atDay(1)
    val leadingBlanks = first.dayOfWeek.value - 1 // Senin = 1
    val days = month.lengthOfMonth()
    val cells = leadingBlanks + days
    val rows = (cells + 6) / 7
    val today = LocalDate.now()

    Column {
        for (r in 0 until rows) {
            Row(Modifier.fillMaxWidth()) {
                for (c in 0 until 7) {
                    val index = r * 7 + c
                    val day = index - leadingBlanks + 1
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .padding(2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (day in 1..days) {
                            val date = month.atDay(day)
                            val isSelected = date == selected
                            val moods = moodsByDate[date].orEmpty()
                            Surface(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { onSelect(date) },
                                shape = RoundedCornerShape(10.dp),
                                color = when {
                                    isSelected -> SipherColor.Blue500
                                    date == today -> SipherColor.Tint
                                    else -> Color.Transparent
                                },
                                border = if (date == today && !isSelected) BorderStroke(1.dp, SipherColor.Blue100) else null
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        "$day",
                                        fontSize = 14.sp,
                                        fontWeight = if (isSelected || date == today) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else SipherColor.Ink900
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    // Maks. 3 titik mood per tanggal
                                    Row(
                                        modifier = Modifier.height(6.dp),
                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        moods.take(3).forEach { mood ->
                                            Box(
                                                Modifier
                                                    .size(5.dp)
                                                    .clip(CircleShape)
                                                    .background(if (isSelected) Color.White else Color(mood.colorHex))
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
