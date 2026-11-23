package com.example.sipherdiary.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sipherdiary.data.repository.DiaryEntry
import com.example.sipherdiary.ui.theme.SipherColor
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun EntryCard(
    entry: DiaryEntry,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val localeId = Locale.forLanguageTag("id-ID")
    val dayNumber = entry.date.format(DateTimeFormatter.ofPattern("dd", localeId))
    val dayName = entry.date.format(DateTimeFormatter.ofPattern("EEE", localeId))
        .uppercase(localeId)
        .take(3)
    val moodColor = Color(entry.mood.colorHex)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = SipherColor.Bg,
        border = BorderStroke(1.dp, SipherColor.Line)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Kotak Tanggal (05 / SEN)
            Box(
                modifier = Modifier
                    .size(width = 50.dp, height = 56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(SipherColor.Subtle),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = dayNumber,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = SipherColor.Ink900
                    )
                    Text(
                        text = dayName,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SipherColor.Ink600
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Judul, Cuplikan, Mood & Badge AES-256
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.title.ifBlank { "Tanpa judul" },
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = SipherColor.Ink900,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = entry.body.ifBlank { "Tidak ada isi catatan..." },
                    fontSize = 13.sp,
                    color = SipherColor.Ink600,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(moodColor)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = entry.mood.label,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = SipherColor.Ink600
                    )
                    Text(
                        text = "  ·  ",
                        fontSize = 12.sp,
                        color = SipherColor.Ink600
                    )
                    Icon(
                        imageVector = Icons.Outlined.Lock,
                        contentDescription = null,
                        tint = SipherColor.Ink600,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "AES-256",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = SipherColor.Ink600
                    )
                }
            }
        }
    }
}

private data class NavTabItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

@Composable
fun SipherNavBar(
    currentRoute: String,
    onNavigate: (String) -> Unit
) {
    val items = listOf(
        NavTabItem("home", "Catatan", Icons.AutoMirrored.Outlined.MenuBook),
        NavTabItem("calendar", "Kalender", Icons.Outlined.CalendarToday),
        NavTabItem("security", "Keamanan", Icons.Outlined.VerifiedUser),
        NavTabItem("profile", "Profil", Icons.Outlined.Person)
    )

    Column(modifier = Modifier.background(SipherColor.Bg)) {
        HorizontalDivider(color = SipherColor.Line, thickness = 1.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(vertical = 8.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                val selected = currentRoute == item.route
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { if (!selected) onNavigate(item.route) }
                        .padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (selected) SipherColor.Tint else Color.Transparent)
                            .padding(horizontal = 18.dp, vertical = 5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.label,
                            tint = if (selected) SipherColor.Blue500 else SipherColor.Ink600,
                            modifier = Modifier.size(21.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = item.label,
                        fontSize = 11.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                        color = if (selected) SipherColor.Blue500 else SipherColor.Ink600
                    )
                }
            }
        }
    }
}