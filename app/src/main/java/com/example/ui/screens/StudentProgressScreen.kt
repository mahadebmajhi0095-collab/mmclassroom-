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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.StudentProgressEntity
import com.example.viewmodel.ClassroomViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentProgressScreen(
    viewModel: ClassroomViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val activeClassroom by viewModel.activeClassroom.collectAsState()
    val progressList by viewModel.studentProgressList.collectAsState()

    var selectedStudentId by remember { mutableStateOf<String?>(null) }
    
    // Automatically select the first student if none is selected
    LaunchedEffect(progressList) {
        if (selectedStudentId == null && progressList.isNotEmpty()) {
            selectedStudentId = progressList.first().id
        }
    }

    val selectedStudent = progressList.find { it.id == selectedStudentId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = activeClassroom?.name ?: "Student Tracking",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Text(
                            text = "Subject: ${activeClassroom?.subject ?: "General"}",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color.LightGray)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("student_progress_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF161925),
                    titleContentColor = Color.White
                )
            )
        },
        containerColor = Color(0xFF0F111A)
    ) { innerPadding ->
        if (progressList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.People,
                        contentDescription = null,
                        tint = Color.DarkGray,
                        modifier = Modifier.size(64.dp)
                    )
                    Text(
                        text = "No Students Found",
                        color = Color.Gray,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Left Column: Student List Selection Panel (take 1/3 width or 320dp)
                Surface(
                    modifier = Modifier
                        .width(280.dp)
                        .fillMaxHeight(),
                    color = Color(0xFF131622)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "Class Roll (${progressList.size})",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(progressList) { progress ->
                                val isSelected = progress.id == selectedStudentId
                                val avatarEmoji = getStudentAvatarEmoji(progress.studentName)
                                val avatarGradient = getStudentAvatarGradient(progress.studentName)

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedStudentId = progress.id }
                                        .testTag("student_item_${progress.studentName.replace(" ", "_")}"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) Color(0xFF005FB0) else Color(0xFF1E2230)
                                    ),
                                    border = if (isSelected) null else BorderStroke(1.dp, Color(0xFF2B3047))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(avatarGradient),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(text = avatarEmoji, fontSize = 18.sp)
                                        }

                                        Spacer(modifier = Modifier.width(10.dp))

                                        Column {
                                            Text(
                                                text = progress.studentName,
                                                color = Color.White,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "Att: ${(progress.attendanceCount * 100 / progress.totalLiveClasses)}%",
                                                color = Color.LightGray,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Divider line between list and details
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(1.dp)
                        .background(Color(0xFF2B3047))
                )

                // Right Column: Dashboard Details Panel
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(Color(0xFF0F111A))
                ) {
                    if (selectedStudent != null) {
                        StudentDashboardDetails(
                            student = selectedStudent,
                            onUpdate = { updated ->
                                viewModel.saveOrUpdateProgress(updated)
                            },
                            context = context
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Select a student to view tracking metrics",
                                color = Color.Gray,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StudentDashboardDetails(
    student: StudentProgressEntity,
    onUpdate: (StudentProgressEntity) -> Unit,
    context: android.content.Context
) {
    var teacherNotesInput by remember(student.id) { mutableStateOf(student.notes) }
    
    // Editable local states so the teacher can interactively tweak indicators live!
    var localAttendance by remember(student.id) { mutableStateOf(student.attendanceCount) }
    var localRecordedCount by remember(student.id) { mutableStateOf(student.completedLecturesCount) }
    var localQuiz1Score by remember(student.id) { mutableStateOf(student.quiz1Score) }
    var localQuiz2Score by remember(student.id) { mutableStateOf(student.quiz2Score) }
    var localAssignmentScore by remember(student.id) { mutableStateOf(student.assignmentScore) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Row header: Identity Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2230))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(getStudentAvatarGradient(student.studentName)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = getStudentAvatarEmoji(student.studentName), fontSize = 32.sp)
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = student.studentName,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Text(
                            text = "Student Progress Profile • Role: Learner",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color.LightGray)
                        )
                    }
                }
            }
        }

        // Row 1: Attendance and Recorded lectures side-by-side
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Live Class Attendance
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF161925)),
                    border = BorderStroke(1.dp, Color(0xFF2B3047))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LiveTv,
                                    contentDescription = null,
                                    tint = Color(0xFFEF5350),
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Live Attendance",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            
                            // Adjust Attendance buttons
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF2B3047))
                                        .clickable {
                                            if (localAttendance > 0) {
                                                localAttendance--
                                                onUpdate(student.copy(attendanceCount = localAttendance))
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = "Minus", tint = Color.White, modifier = Modifier.size(12.dp))
                                }
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF2B3047))
                                        .clickable {
                                            if (localAttendance < student.totalLiveClasses) {
                                                localAttendance++
                                                onUpdate(student.copy(attendanceCount = localAttendance))
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Plus", tint = Color.White, modifier = Modifier.size(12.dp))
                                }
                            }
                        }

                        val attendancePercent = (localAttendance.toFloat() / student.totalLiveClasses.toFloat())
                        LinearProgressIndicator(
                            progress = attendancePercent,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = Color(0xFFEF5350),
                            trackColor = Color(0xFF321E23)
                        )

                        Text(
                            text = "$localAttendance / ${student.totalLiveClasses} Lectures Participated (${(attendancePercent * 100).toInt()}%)",
                            fontSize = 11.sp,
                            color = Color.LightGray
                        )
                    }
                }

                // Recorded lecture completion
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF161925)),
                    border = BorderStroke(1.dp, Color(0xFF2B3047))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VideoLibrary,
                                    contentDescription = null,
                                    tint = Color(0xFF2196F3),
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Recorded Lectures",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            
                            // Adjust Recorded buttons
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF2B3047))
                                        .clickable {
                                            if (localRecordedCount > 0) {
                                                localRecordedCount--
                                                onUpdate(student.copy(completedLecturesCount = localRecordedCount))
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = "Minus", tint = Color.White, modifier = Modifier.size(12.dp))
                                }
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF2B3047))
                                        .clickable {
                                            if (localRecordedCount < student.totalRecordedLectures) {
                                                localRecordedCount++
                                                onUpdate(student.copy(completedLecturesCount = localRecordedCount))
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Plus", tint = Color.White, modifier = Modifier.size(12.dp))
                                }
                            }
                        }

                        val completionPercent = (localRecordedCount.toFloat() / student.totalRecordedLectures.toFloat())
                        LinearProgressIndicator(
                            progress = completionPercent,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = Color(0xFF2196F3),
                            trackColor = Color(0xFF16253A)
                        )

                        Text(
                            text = "$localRecordedCount / ${student.totalRecordedLectures} Videos Watched (${(completionPercent * 100).toInt()}%)",
                            fontSize = 11.sp,
                            color = Color.LightGray
                        )
                    }
                }
            }
        }

        // Row 2: Quiz & Assignment Scores Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF161925)),
                border = BorderStroke(1.dp, Color(0xFF2B3047))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Assessment,
                            contentDescription = null,
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Scores & Academic Performance",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                    }

                    Divider(color = Color(0xFF2B3047))

                    // Quiz 1 Row
                    ScoreAdjustmentRow(
                        title = "Quiz 1: Foundation Assessment",
                        score = localQuiz1Score,
                        maxScore = 20,
                        accentColor = Color(0xFF4CAF50),
                        onScoreChanged = { newScore ->
                            localQuiz1Score = newScore
                            onUpdate(student.copy(quiz1Score = newScore))
                        }
                    )

                    // Quiz 2 Row
                    ScoreAdjustmentRow(
                        title = "Quiz 2: Subjective Evaluation",
                        score = localQuiz2Score,
                        maxScore = 20,
                        accentColor = Color(0xFF4CAF50),
                        onScoreChanged = { newScore ->
                            localQuiz2Score = newScore
                            onUpdate(student.copy(quiz2Score = newScore))
                        }
                    )

                    // Assignment Row
                    ScoreAdjustmentRow(
                        title = "Assignment 1: Comprehensive Homework",
                        score = localAssignmentScore,
                        maxScore = 100,
                        accentColor = Color(0xFFFF9800),
                        onScoreChanged = { newScore ->
                            localAssignmentScore = newScore
                            onUpdate(student.copy(assignmentScore = newScore))
                        }
                    )
                }
            }
        }

        // Row 3: Teacher's Performance Notes Panel
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF161925)),
                border = BorderStroke(1.dp, Color(0xFF2B3047))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.RateReview,
                            contentDescription = null,
                            tint = Color(0xFFE040FB),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Teacher's Performance Evaluation Notes",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                    }

                    Text(
                        text = "Add custom observation notes regarding student performance, focus attention points, behavior, or remedial steps.",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )

                    OutlinedTextField(
                        value = teacherNotesInput,
                        onValueChange = { teacherNotesInput = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .testTag("teacher_notes_input_field"),
                        placeholder = { Text("Write performance remarks...", color = Color.Gray) },
                        maxLines = 4,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFFE040FB),
                            unfocusedBorderColor = Color(0xFF2B3047),
                            focusedContainerColor = Color(0xFF0F111A),
                            unfocusedContainerColor = Color(0xFF0F111A)
                        )
                    )

                    Button(
                        onClick = {
                            val finalProgress = student.copy(
                                attendanceCount = localAttendance,
                                completedLecturesCount = localRecordedCount,
                                quiz1Score = localQuiz1Score,
                                quiz2Score = localQuiz2Score,
                                assignmentScore = localAssignmentScore,
                                notes = teacherNotesInput.trim()
                            )
                            onUpdate(finalProgress)
                            Toast.makeText(context, "Remark notes saved successfully for ${student.studentName}!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE040FB)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .align(Alignment.End)
                            .testTag("save_notes_btn")
                    ) {
                        Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save Note Remarks", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun ScoreAdjustmentRow(
    title: String,
    score: Int,
    maxScore: Int,
    accentColor: Color,
    onScoreChanged: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 12.sp, color = Color.LightGray, fontWeight = FontWeight.SemiBold)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "$score / $maxScore pts",
                    fontSize = 15.sp,
                    color = accentColor,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "(${((score.toFloat() / maxScore) * 100).toInt()}%)",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }
        }

        // Tweak scores interactively
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF2B3047))
                    .clickable {
                        if (score > 0) {
                            onScoreChanged(score - 1)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Decrement", tint = Color.White, modifier = Modifier.size(14.dp))
            }
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF2B3047))
                    .clickable {
                        if (score < maxScore) {
                            onScoreChanged(score + 1)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Add, contentDescription = "Increment", tint = Color.White, modifier = Modifier.size(14.dp))
            }
        }
    }
}

// Avatar helper functions dedicated to Students
private fun getStudentAvatarEmoji(name: String): String {
    return when (name) {
        "Amit Sharma" -> "👨‍🎓"
        "Priya Patel" -> "👩‍🎓"
        "Anjali Rao" -> "👩‍💻"
        "Rahul Sen" -> "🧑‍🎓"
        "Vijay Kumar" -> "👨‍💻"
        "Siddharth J." -> "🧑‍🚀"
        "Sneha Das" -> "👩‍🎨"
        else -> "🧑‍🎓"
    }
}

private fun getStudentAvatarGradient(name: String): Brush {
    val colors = when (name) {
        "Amit Sharma" -> listOf(Color(0xFF1E88E5), Color(0xFF1565C0))
        "Priya Patel" -> listOf(Color(0xFFE91E63), Color(0xFFC2185B))
        "Anjali Rao" -> listOf(Color(0xFF9C27B0), Color(0xFF7B1FA2))
        "Rahul Sen" -> listOf(Color(0xFF4CAF50), Color(0xFF388E3C))
        "Vijay Kumar" -> listOf(Color(0xFFFF9800), Color(0xFFF57C00))
        "Siddharth J." -> listOf(Color(0xFF00BCD4), Color(0xFF0097A7))
        "Sneha Das" -> listOf(Color(0xFF9E9D24), Color(0xFF827717))
        else -> listOf(Color(0xFF607D8B), Color(0xFF455A64))
    }
    return Brush.radialGradient(colors)
}
