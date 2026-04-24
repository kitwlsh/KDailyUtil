package com.kitwlshcom.kdailyutil.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingFlat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kitwlshcom.kdailyutil.data.model.AudioItem
import com.kitwlshcom.kdailyutil.ui.viewmodel.PlaybackMode

fun formatTime(ms: Long): String {
    val seconds = (ms / 1000) % 60
    val minutes = (ms / (1000 * 60)) % 60
    return String.format("%02d:%02d", minutes, seconds)
}

@Composable
fun BottomPlayerBar(
    modifier: Modifier = Modifier,
    item: AudioItem,
    isPaused: Boolean,
    playbackMode: PlaybackMode,
    isEditLocked: Boolean,
    progress: Float,
    totalDuration: Long,
    onTogglePlay: () -> Unit,
    onToggleMode: () -> Unit,
    onToggleLock: () -> Unit,
    onSeek: (Float) -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 12.dp,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier.padding(bottom = 0.dp)
        ) {
            // 재생 위치 슬라이더를 맨 위로 (선 형태로)
            val currentPos = progress.toLong()
            val total = totalDuration
            
            Slider(
                value = if (total > 0) currentPos.toFloat() / total else 0f,
                onValueChange = { onSeek(it * total) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp),
                colors = SliderDefaults.colors(
                    thumbColor = Color.Transparent,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )

            Column(modifier = Modifier.padding(16.dp)) {
                // 현재 곡 제목 및 탐색 버튼
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Row {
                            Text(formatTime(currentPos), style = MaterialTheme.typography.labelSmall)
                            Text(" / ", style = MaterialTheme.typography.labelSmall)
                            Text(formatTime(total), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    
                    Row {
                        IconButton(onClick = onPrev) {
                            Icon(Icons.Default.SkipPrevious, contentDescription = "이전 곡", modifier = Modifier.size(28.dp))
                        }
                        IconButton(onClick = onNext) {
                            Icon(Icons.Default.SkipNext, contentDescription = "다음 곡", modifier = Modifier.size(28.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 컨트롤 버튼들
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onToggleMode) {
                        val (icon, label) = when (playbackMode) {
                            PlaybackMode.SEQUENTIAL -> Icons.AutoMirrored.Filled.TrendingFlat to "순차"
                            PlaybackMode.LOOP_LIST -> Icons.Default.Repeat to "전체"
                            PlaybackMode.SHUFFLE -> Icons.Default.Shuffle to "랜덤"
                            PlaybackMode.REPEAT_ONE -> Icons.Default.RepeatOne to "1곡"
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp))
                            Text(label, style = MaterialTheme.typography.labelSmall, fontSize = 9.sp)
                        }
                    }

                    // 메인 재생 버튼
                    Surface(
                        onClick = onTogglePlay,
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(54.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }

                    IconButton(onClick = onToggleLock) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                if (isEditLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                                contentDescription = null,
                                tint = if (isEditLocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                if (isEditLocked) "잠금" else "편집",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.sp,
                                color = if (isEditLocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }
        }
    }
}
