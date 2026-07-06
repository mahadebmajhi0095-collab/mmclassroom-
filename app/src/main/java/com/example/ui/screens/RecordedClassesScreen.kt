package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.RecordedClassEntity
import com.example.viewmodel.ClassroomViewModel
import kotlinx.coroutines.delay

@Composable
fun RecordedClassesScreen(
    viewModel: ClassroomViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val user by viewModel.userState.collectAsState()
    val classroom by viewModel.activeClassroom.collectAsState()
    val recordings by viewModel.recordedClasses.collectAsState()

    var activeRecording by remember { mutableStateOf<RecordedClassEntity?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    // Add Session inputs
    var sessionTitle by remember { mutableStateOf("") }
    var sessionDuration by remember { mutableStateOf("") }
    var sessionDesc by remember { mutableStateOf("") }

    // Simulated Video Player status
    var isPlaying by remember { mutableStateOf(false) }
    var playbackProgress by remember { mutableStateOf(0f) }
    var currentPlaybackTime by remember { mutableStateOf("00:00") }

    // Effect for simulated ticking media player
    LaunchedEffect(isPlaying, activeRecording) {
        if (isPlaying && activeRecording != null) {
            while (isPlaying) {
                delay(1000)
                if (playbackProgress < 1f) {
                    playbackProgress += 0.05f
                    val totalSecs = (playbackProgress * 120).toInt() // simulated 2 min video length
                    val mins = totalSecs / 60
                    val secs = totalSecs % 60
                    currentPlaybackTime = String.format("%02d:%02d", mins, secs)
                } else {
                    isPlaying = false
                    playbackProgress = 0f
                    currentPlaybackTime = "00:00"
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F111A))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF161925))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("recorded_back_btn")) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Recorded Lectures",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = classroom?.name ?: "Course Library",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (user?.role == "TEACHER") {
                    Button(
                        onClick = { showAddDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("record_class_btn")
                    ) {
                        Icon(Icons.Default.Publish, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Publish", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // SIMULATED VIDEO PLAYER CARD
            activeRecording?.let { rec ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Black),
                    border = BorderStroke(1.dp, Color(0xFF1E2235))
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Simulated Screen Area
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .background(Color(0xFF12141A)),
                            contentAlignment = Alignment.Center
                        ) {
                            // Centered Icon / Screen contents
                            if (isPlaying) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.Videocam,
                                        contentDescription = "Playing",
                                        tint = Color(0xFF2196F3),
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = "Playing Lecture Segment...",
                                        fontSize = 12.sp,
                                        color = Color.LightGray,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "Topic: ${rec.title}",
                                        fontSize = 11.sp,
                                        color = Color.Gray,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF2196F3))
                                        .clickable { isPlaying = true }
                                        .testTag("play_video_overlay_btn"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Play",
                                        tint = Color.White,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                            }

                            // Watermark Label
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(12.dp)
                                    .background(Color.Red.copy(alpha = 0.75f), shape = RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("REC PLAYBACK", fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }

                            // Duration Badge
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(12.dp)
                                    .background(Color.Black.copy(alpha = 0.6f), shape = RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(rec.duration, fontSize = 10.sp, color = Color.White)
                            }
                        }

                        // Playback Control bar
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF161822))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Slider Progress
                            LinearProgressIndicator(
                                progress = { playbackProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .testTag("video_seek_progress"),
                                color = Color(0xFF2196F3),
                                trackColor = Color.DarkGray
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    IconButton(
                                        onClick = { isPlaying = !isPlaying },
                                        modifier = Modifier.testTag("video_play_toggle")
                                    ) {
                                        Icon(
                                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                            contentDescription = if (isPlaying) "Pause" else "Play",
                                            tint = Color.White
                                        )
                                    }

                                    Text(
                                        text = "$currentPlaybackTime / 02:00",
                                        fontSize = 11.sp,
                                        color = Color.LightGray
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    IconButton(onClick = {
                                        playbackProgress = 0f
                                        currentPlaybackTime = "00:00"
                                        isPlaying = false
                                        Toast.makeText(context, "Restarted playback", Toast.LENGTH_SHORT).show()
                                    }) {
                                        Icon(Icons.Default.Replay, contentDescription = "Restart", tint = Color.LightGray, modifier = Modifier.size(18.dp))
                                    }

                                    IconButton(onClick = {
                                        activeRecording = null
                                        isPlaying = false
                                        playbackProgress = 0f
                                        currentPlaybackTime = "00:00"
                                    }) {
                                        Icon(Icons.Default.Close, contentDescription = "Close Player", tint = Color.LightGray, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }

                            // Lecture Details
                            Text(
                                text = rec.title,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = rec.description,
                                fontSize = 12.sp,
                                color = Color.Gray,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }

            // LIST OF RECORDED CLASSES
            Text(
                text = "Available Lecture Files",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            if (recordings.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.VideoCameraBack, contentDescription = null, tint = Color.DarkGray, modifier = Modifier.size(48.dp))
                        Text("No recorded classes published yet.", color = Color.Gray, fontSize = 13.sp)
                        if (user?.role == "TEACHER") {
                            Text("Click 'Publish' at the top to publish one now!", color = Color.Gray, fontSize = 11.sp)
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("recorded_list"),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(recordings) { rec ->
                        val isCurrent = activeRecording?.id == rec.id
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    activeRecording = rec
                                    playbackProgress = 0f
                                    currentPlaybackTime = "00:00"
                                    isPlaying = true
                                }
                                .testTag("recording_item_${rec.id}"),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isCurrent) Color(0xFF1E283A) else Color(0xFF161925)
                            ),
                            border = if (isCurrent) BorderStroke(1.dp, Color(0xFF2196F3)) else null
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF0F111A)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isCurrent && isPlaying) Icons.Default.PauseCircle else Icons.Default.PlayCircle,
                                        contentDescription = "Play status",
                                        tint = if (isCurrent) Color(0xFF2196F3) else Color.LightGray
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = rec.title,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(rec.duration, fontSize = 11.sp, color = Color.LightGray)
                                        Text("•", fontSize = 11.sp, color = Color.Gray)
                                        Text(rec.date, fontSize = 11.sp, color = Color.Gray)
                                    }
                                }

                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = "Open",
                                    tint = Color.DarkGray
                                )
                            }
                        }
                    }
                }
            }
        }

        // Add Recording Dialog
        if (showAddDialog) {
            Dialog(onDismissRequest = { showAddDialog = false }) {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2235))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Publish Recorded Session",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )

                        OutlinedTextField(
                            value = sessionTitle,
                            onValueChange = { sessionTitle = it },
                            label = { Text("Lecture Title") },
                            placeholder = { Text("e.g. Newton's Laws Explained") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("publish_title_input"),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )

                        OutlinedTextField(
                            value = sessionDuration,
                            onValueChange = { sessionDuration = it },
                            label = { Text("Duration (e.g. 50 mins)") },
                            placeholder = { Text("e.g. 1 hr 15 mins") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("publish_duration_input"),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )

                        OutlinedTextField(
                            value = sessionDesc,
                            onValueChange = { sessionDesc = it },
                            label = { Text("Brief Summary/Description") },
                            placeholder = { Text("Write brief bullet points of lecture details...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("publish_desc_input"),
                            maxLines = 3,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = { showAddDialog = false }) {
                                Text("Cancel", color = Color.Gray)
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Button(
                                onClick = {
                                    if (sessionTitle.trim().isNotEmpty()) {
                                        viewModel.addCustomRecordedClass(
                                            title = sessionTitle,
                                            duration = sessionDuration,
                                            desc = sessionDesc
                                        )
                                        sessionTitle = ""
                                        sessionDuration = ""
                                        sessionDesc = ""
                                        showAddDialog = false
                                        Toast.makeText(context, "Recorded lecture published successfully!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                enabled = sessionTitle.trim().isNotEmpty(),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("publish_confirm_btn")
                            ) {
                                Text("Publish")
                            }
                        }
                    }
                }
            }
        }
    }
}
