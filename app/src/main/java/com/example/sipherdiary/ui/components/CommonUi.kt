package com.example.sipherdiary.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sipherdiary.ui.theme.SipherColor

/** Judul layar besar seperti "Catatanmu" di Beranda, dengan tombol kembali opsional. */
@Composable
fun ScreenHeader(
    title: String,
    overline: String? = null,
    onBack: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            Surface(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable { onBack() },
                shape = CircleShape,
                color = SipherColor.Bg,
                border = BorderStroke(1.dp, SipherColor.Line)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Kembali", tint = SipherColor.Ink900, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(Modifier.width(14.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            if (overline != null) {
                Text(
                    text = overline.uppercase(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.8.sp,
                    color = SipherColor.Ink600
                )
                Spacer(Modifier.height(2.dp))
            }
            Text(
                text = title,
                fontSize = if (onBack != null) 22.sp else 28.sp,
                fontWeight = FontWeight.Bold,
                color = SipherColor.Ink900
            )
        }
        trailing?.invoke()
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        fontSize = 11.5.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.8.sp,
        color = SipherColor.Ink600,
        modifier = modifier.padding(top = 6.dp, bottom = 2.dp)
    )
}

@Composable
fun SipherCard(
    modifier: Modifier = Modifier,
    color: Color = SipherColor.Bg,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        shape = RoundedCornerShape(16.dp),
        color = color,
        border = BorderStroke(1.dp, SipherColor.Line)
    ) {
        Column(modifier = Modifier.padding(16.dp), content = content)
    }
}

/** Kotak ikon berwarna 40dp seperti di kartu status Beranda. */
@Composable
fun IconBadge(icon: ImageVector, tint: Color = SipherColor.Blue500, background: Color = SipherColor.Tint) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(background),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(21.dp))
    }
}

@Composable
fun SettingToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconBadge(icon)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = SipherColor.Ink900)
            Text(subtitle, fontSize = 12.5.sp, color = SipherColor.Ink600)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = SipherColor.Blue500,
                uncheckedThumbColor = SipherColor.Ink600,
                uncheckedTrackColor = SipherColor.Subtle,
                uncheckedBorderColor = SipherColor.Line
            )
        )
    }
}

@Composable
fun NavRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    tint: Color = SipherColor.Blue500,
    badgeBackground: Color = SipherColor.Tint,
    titleColor: Color = SipherColor.Ink900,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconBadge(icon, tint = tint, background = badgeBackground)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = titleColor)
            if (subtitle != null) Text(subtitle, fontSize = 12.5.sp, color = SipherColor.Ink600)
        }
        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = SipherColor.Ink600)
    }
}

/** Baris label–nilai, nilai teknis memakai font monospace (seperti JetBrains Mono di Figma). */
@Composable
fun InfoRow(label: String, value: String, valueColor: Color = SipherColor.Ink900, mono: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 13.5.sp, color = SipherColor.Ink600)
        Spacer(Modifier.width(12.dp))
        Text(
            value,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = valueColor,
            fontFamily = if (mono) FontFamily.Monospace else FontFamily.Default
        )
    }
}

@Composable
fun StatusChip(text: String, color: Color) {
    Surface(shape = CircleShape, color = color.copy(alpha = 0.12f)) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(Modifier.width(6.dp))
            Text(text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = color)
        }
    }
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    color: Color = SipherColor.Blue500
) {
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color, contentColor = Color.White)
    ) {
        if (loading) {
            CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
        } else {
            Text(text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun OutlineButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    color: Color = SipherColor.Ink900
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, SipherColor.Line),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = color)
    ) {
        Text(text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** Banner kecil bila Firebase belum dipasang. */
@Composable
fun DemoModeBanner() {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = SipherColor.Warning.copy(alpha = 0.10f),
        border = BorderStroke(1.dp, SipherColor.Warning.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Mode demo: Firebase belum dikonfigurasi. Data cloud disimulasikan di HP ini.",
            fontSize = 12.5.sp,
            color = SipherColor.Warning,
            modifier = Modifier.padding(12.dp)
        )
    }
}
