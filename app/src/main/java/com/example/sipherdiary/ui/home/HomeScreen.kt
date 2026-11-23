package com.example.sipherdiary.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.VerifiedUser
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
import com.example.sipherdiary.ui.components.EntryCard
import com.example.sipherdiary.ui.components.SipherNavBar
import com.example.sipherdiary.ui.theme.SipherColor
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HomeScreen(
    entries: List<DiaryEntry>,
    userInitial: String = "J",
    onOpenEditor: (String?) -> Unit,
    onNavigateTab: (String) -> Unit
) {
    var selectedMood by remember { mutableStateOf<MoodType?>(null) }
    var isSearchOpen by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val localeId = Locale.forLanguageTag("id-ID")
    val headerDate = LocalDate.now()
        .format(DateTimeFormatter.ofPattern("EEEE, d MMMM", localeId))
        .uppercase(localeId)

    // Pencarian & filter dilakukan di memori pada teks yang sudah didekripsi
    val filteredEntries = remember(entries, selectedMood, searchQuery) {
        entries.filter { item ->
            val matchMood = selectedMood == null || item.mood == selectedMood
            val matchQuery = searchQuery.isBlank() ||
                    item.title.contains(searchQuery, ignoreCase = true) ||
                    item.body.contains(searchQuery, ignoreCase = true)
            matchMood && matchQuery
        }
    }

    Scaffold(
        containerColor = SipherColor.Bg,
        bottomBar = {
            SipherNavBar(
                currentRoute = "home",
                onNavigate = onNavigateTab
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onOpenEditor(null) },
                shape = RoundedCornerShape(16.dp),
                containerColor = SipherColor.Blue500,
                contentColor = Color.White
            ) {
                Icon(
                    imageVector = Icons.Outlined.Edit,
                    contentDescription = "Tulis catatan",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Tulis",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Tanggal + Judul "Catatanmu" + Tombol Cari & Profil
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = headerDate,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.8.sp,
                            color = SipherColor.Ink600
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Catatanmu",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = SipherColor.Ink900
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .clickable {
                                    isSearchOpen = !isSearchOpen
                                    if (!isSearchOpen) searchQuery = ""
                                },
                            shape = CircleShape,
                            color = SipherColor.Bg,
                            border = BorderStroke(1.dp, SipherColor.Line)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isSearchOpen) Icons.Outlined.Close else Icons.Outlined.Search,
                                    contentDescription = "Cari catatan",
                                    tint = SipherColor.Ink900,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(SipherColor.Tint)
                                .clickable { onNavigateTab("profile") },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = userInitial,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = SipherColor.Blue500
                            )
                        }
                    }
                }
            }

            // Kolom Pencarian In-Memory (muncul saat ikon cari diklik)
            if (isSearchOpen) {
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Cari judul atau isi catatan...", fontSize = 14.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Kartu Status Enkripsi
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onNavigateTab("security") },
                    shape = RoundedCornerShape(16.dp),
                    color = SipherColor.Subtle,
                    border = BorderStroke(1.dp, SipherColor.Line)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(SipherColor.Blue500),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.VerifiedUser,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${entries.size} catatan terenkripsi",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = SipherColor.Ink900
                            )
                            Text(
                                text = "Terkunci AES-256-GCM di perangkat ini",
                                fontSize = 12.5.sp,
                                color = SipherColor.Ink600
                            )
                        }

                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                            contentDescription = null,
                            tint = SipherColor.Ink600
                        )
                    }
                }
            }

            // Baris Chip Filter Mood
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        val isAllSelected = selectedMood == null
                        Surface(
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { selectedMood = null },
                            shape = CircleShape,
                            color = if (isAllSelected) SipherColor.Ink900 else SipherColor.Bg,
                            border = if (isAllSelected) null else BorderStroke(1.dp, SipherColor.Line)
                        ) {
                            Text(
                                text = "Semua",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isAllSelected) Color.White else SipherColor.Ink900,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }

                    items(MoodType.entries) { mood ->
                        val isSelected = selectedMood == mood
                        Surface(
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable {
                                    selectedMood = if (isSelected) null else mood
                                },
                            shape = CircleShape,
                            color = if (isSelected) SipherColor.Tint else SipherColor.Bg,
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (isSelected) SipherColor.Blue500 else SipherColor.Line
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color(mood.colorHex))
                                )
                                Spacer(modifier = Modifier.width(7.dp))
                                Text(
                                    text = mood.label,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = SipherColor.Ink900
                                )
                            }
                        }
                    }
                }
            }

            // Header Daftar Catatan
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "DAFTAR CATATAN",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp,
                        color = SipherColor.Ink600
                    )
                    Text(
                        text = "${filteredEntries.size} catatan",
                        fontSize = 12.sp,
                        color = SipherColor.Ink600
                    )
                }
            }

            // Daftar Kartu Catatan atau Tampilan Kosong
            if (filteredEntries.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = SipherColor.Subtle
                    ) {
                        Column(
                            modifier = Modifier.padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Belum ada catatan",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = SipherColor.Ink900
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Tekan tombol + Tulis di pojok kanan bawah untuk mulai menulis diary terenkripsi.",
                                fontSize = 13.sp,
                                color = SipherColor.Ink600
                            )
                        }
                    }
                }
            } else {
                items(filteredEntries, key = { it.id }) { entry ->
                    EntryCard(
                        entry = entry,
                        onClick = { onOpenEditor(entry.id) }
                    )
                }
            }
        }
    }
}