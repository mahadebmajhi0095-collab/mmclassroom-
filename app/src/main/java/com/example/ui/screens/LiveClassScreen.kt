package com.example.ui.screens

import android.content.Context
import android.media.AudioManager
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke as DrawScopeStroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.MessageEntity
import com.example.data.ClassroomEntity
import com.example.data.UserEntity
import com.example.viewmodel.ClassroomViewModel
import com.example.viewmodel.Stroke
import kotlinx.coroutines.launch

@Composable
fun LiveClassScreen(
    viewModel: ClassroomViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val user by viewModel.userState.collectAsState()
    val classroom by viewModel.activeClassroom.collectAsState()
    val boardStyle by viewModel.boardStyle.collectAsState()
    val currentColor by viewModel.currentColor.collectAsState()
    val strokeWidth by viewModel.currentStrokeWidth.collectAsState()
    val isEraser by viewModel.isEraserMode.collectAsState()

    val leavingStudent by viewModel.leavingStudentName.collectAsState()
    val onlineStudents by viewModel.onlineStudents.collectAsState()
    val messages by viewModel.chatMessages.collectAsState()

    // PDF state
    val activePdfName by viewModel.activePdfName.collectAsState()
    val currentPdfPage by viewModel.currentPdfPageIndex.collectAsState()
    val pdfPages by viewModel.pdfPages.collectAsState()

    // Voice & Chat locks
    val promoApplied = user?.promoApplied == true
    var showPromoDialog by remember { mutableStateOf(false) }
    var promoCodeInput by remember { mutableStateOf("") }

    // Live Speaking variables
    val isVoiceActive by viewModel.isVoiceActive.collectAsState()
    val activeSpeakerName by viewModel.activeSpeakerName.collectAsState()

    val isRecordingActive by viewModel.isRecordingActive.collectAsState()
    var recordingSeconds by remember { mutableStateOf(0) }
    var showSaveRecordingDialog by remember { mutableStateOf(false) }

    LaunchedEffect(isRecordingActive) {
        if (isRecordingActive) {
            recordingSeconds = 0
            while (true) {
                kotlinx.coroutines.delay(1000)
                recordingSeconds++
            }
        } else {
            recordingSeconds = 0
        }
    }

    val formattedDuration = remember(recordingSeconds) {
        val mins = recordingSeconds / 60
        val secs = recordingSeconds % 60
        String.format("%02d:%02d", mins, secs)
    }

    // Messaging input
    var chatInputText by remember { mutableStateOf("") }
    val chatListState = rememberLazyListState()

    var showQuizDialog by remember { mutableStateOf(false) }
    var activeLiveTab by remember { mutableStateOf("BOARD") }

    var isMicMuted by remember { mutableStateOf(false) }
    var isCameraOff by remember { mutableStateOf(false) }
    var isSpeakerphoneOn by remember { mutableStateOf(true) }
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }

    // Drawing variables
    var currentPathPoints = remember { mutableStateListOf<Offset>() }

    // Auto scroll chat list to end when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            chatListState.animateScrollToItem(messages.size - 1)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
        ) {
            val outlineColor = MaterialTheme.colorScheme.outline
            // Live Header Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .drawBehind {
                        drawLine(
                            color = outlineColor,
                            start = Offset(0f, size.height),
                            end = Offset(size.width, size.height),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("live_back_btn")) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFD32F2F))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "LIVE CLASS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFD32F2F),
                            letterSpacing = 1.sp
                        )

                        if (isRecordingActive) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier
                                    .background(Color(0xFFFFEBEE), shape = RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFD32F2F))
                                )
                                Text(
                                    text = "REC $formattedDuration",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFD32F2F)
                                )
                            }
                        }
                    }
                    Text(
                        text = classroom?.name ?: "Interactive Board Room",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Header tools for all
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    // Online students counter button
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.People, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        Text(
                            text = "${onlineStudents.size + 1} Online",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Recording button for teacher
                    if (user?.role == "TEACHER") {
                        Button(
                            onClick = {
                                if (isRecordingActive) {
                                    showSaveRecordingDialog = true
                                } else {
                                    viewModel.startRecording()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isRecordingActive) Color(0xFFFFEBEE) else Color(0xFFE1F5FE),
                                contentColor = if (isRecordingActive) Color(0xFFC62828) else Color(0xFF0288D1)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("record_class_btn")
                        ) {
                            Icon(
                                imageVector = if (isRecordingActive) Icons.Default.Stop else Icons.Default.Videocam,
                                contentDescription = null,
                                tint = if (isRecordingActive) Color(0xFFC62828) else Color(0xFF0288D1),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isRecordingActive) "Stop Rec" else "Record",
                                color = if (isRecordingActive) Color(0xFFC62828) else Color(0xFF0288D1),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Simulation Trigger Button (Simulates student leaving class)
                    Button(
                        onClick = { viewModel.simulateStudentLeaving() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFFEBEE),
                            contentColor = Color(0xFFC62828)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("simulate_leave_btn")
                    ) {
                        Icon(Icons.Default.Logout, contentDescription = null, tint = Color(0xFFC62828), modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Simulate Leave", color = Color(0xFFC62828), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    // Quizzes Button
                    Button(
                        onClick = { showQuizDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE8F5E9),
                            contentColor = Color(0xFF2E7D32)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("quiz_dashboard_trigger_btn")
                    ) {
                        Icon(Icons.Default.Quiz, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Quizzes", color = Color(0xFF2E7D32), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // MAIN INTERACTIVE AREA: Drawing Board (Top half) & Discussion Area (Bottom half)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                // Tab Row to select between Whiteboard/Blackboard and Jitsi Video Call
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { activeLiveTab = "BOARD" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (activeLiveTab == "BOARD") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = if (activeLiveTab == "BOARD") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).testTag("tab_whiteboard_btn"),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Gesture, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Interactive Board", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { activeLiveTab = "VIDEO" },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (activeLiveTab == "VIDEO") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = if (activeLiveTab == "VIDEO") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).testTag("tab_video_call_btn"),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.VideoCameraFront, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Live Video Call", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (activeLiveTab == "BOARD") {
                    // SECTION 1: THE INTERACTIVE BOARD (Whiteboard / Blackboard)
                    Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1.2f)
                        .padding(10.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(2.dp, if (boardStyle == "BLACKBOARD") Color(0xFF2E3A2F) else MaterialTheme.colorScheme.outline)
                ) {
                    val boardBg = if (boardStyle == "BLACKBOARD") Color(0xFF1E3A2F) else Color.White
                    val gridColor = if (boardStyle == "BLACKBOARD") Color(0xFF25483A) else Color(0xFFF0F0F0)
                    
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(boardBg)
                    ) {
                        // Grid board lining
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val gridSpacing = 40.dp.toPx()
                            val width = size.width
                            val height = size.height

                            // Vertical grid lines
                            var x = 0f
                            while (x < width) {
                                drawLine(
                                    color = gridColor,
                                    start = Offset(x, 0f),
                                    end = Offset(x, height),
                                    strokeWidth = 1f
                                )
                                x += gridSpacing
                            }
                            // Horizontal grid lines
                            var y = 0f
                            while (y < height) {
                                drawLine(
                                    color = gridColor,
                                    start = Offset(0f, y),
                                    end = Offset(width, y),
                                    strokeWidth = 1f
                                )
                                y += gridSpacing
                            }
                        }

                        // PDF overlay content if loaded
                        activePdfName?.let { pdfTitle ->
                            val currentPageObj = pdfPages.getOrNull(currentPdfPage)
                            if (currentPageObj != null) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(24.dp),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    // Slide Header
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Description,
                                                contentDescription = null,
                                                tint = if (boardStyle == "BLACKBOARD") Color(0xFF81C784) else Color(0xFF2196F3),
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = pdfTitle,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (boardStyle == "BLACKBOARD") Color.White.copy(alpha = 0.8f) else Color.Black.copy(alpha = 0.8f)
                                            )
                                        }

                                        Text(
                                            text = "Page ${currentPdfPage + 1} of ${pdfPages.size}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (boardStyle == "BLACKBOARD") Color.White.copy(alpha = 0.6f) else Color.Black.copy(alpha = 0.6f),
                                            modifier = Modifier
                                                .background(
                                                    if (boardStyle == "BLACKBOARD") Color.Black.copy(alpha = 0.2f) else Color.Black.copy(alpha = 0.05f),
                                                    shape = RoundedCornerShape(4.dp)
                                                )
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }

                                    // Slide Content Body
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f)
                                            .padding(vertical = 12.dp),
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = currentPageObj.title,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (boardStyle == "BLACKBOARD") Color(0xFFFFD54F) else Color(0xFF0D47A1),
                                            lineHeight = 22.sp
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = currentPageObj.description,
                                            fontSize = 13.sp,
                                            color = if (boardStyle == "BLACKBOARD") Color.White.copy(alpha = 0.85f) else Color.DarkGray,
                                            lineHeight = 16.sp
                                        )

                                        // Slide equations if any
                                        if (currentPageObj.equations.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(
                                                        if (boardStyle == "BLACKBOARD") Color.Black.copy(alpha = 0.25f) else Color(0xFFF5F5F5),
                                                        shape = RoundedCornerShape(8.dp)
                                                    )
                                                    .padding(8.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                currentPageObj.equations.forEach { eq ->
                                                    Text(
                                                        text = eq,
                                                        fontSize = 16.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        fontFamily = FontFamily.Monospace,
                                                        color = if (boardStyle == "BLACKBOARD") Color(0xFF4FC3F7) else Color(0xFFD32F2F)
                                                    )
                                                }
                                            }
                                        }

                                        // Points of Interest
                                        if (currentPageObj.pointsOfInterest.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(10.dp))
                                            currentPageObj.pointsOfInterest.forEach { pt ->
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.padding(vertical = 2.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = null,
                                                        tint = Color.Green,
                                                        modifier = Modifier.size(12.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = pt,
                                                        fontSize = 12.sp,
                                                        color = if (boardStyle == "BLACKBOARD") Color.White.copy(alpha = 0.75f) else Color.DarkGray
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // Controls inside PDF
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        IconButton(
                                            onClick = { viewModel.prevPage() },
                                            enabled = currentPdfPage > 0,
                                            modifier = Modifier.testTag("pdf_prev_page")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ChevronLeft,
                                                contentDescription = "Previous Slide",
                                                tint = if (currentPdfPage > 0) (if (boardStyle == "BLACKBOARD") Color.White else Color.Black) else Color.Gray
                                            )
                                        }

                                        IconButton(
                                            onClick = { viewModel.removePdfFromBoard() },
                                            modifier = Modifier
                                                .background(
                                                    if (boardStyle == "BLACKBOARD") Color.Red.copy(alpha = 0.2f) else Color.Red.copy(alpha = 0.05f),
                                                    shape = RoundedCornerShape(4.dp)
                                                )
                                                .padding(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Clear, contentDescription = null, tint = Color.Red, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Remove PDF", color = Color.Red, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }

                                        IconButton(
                                            onClick = { viewModel.nextPage() },
                                            enabled = currentPdfPage < pdfPages.size - 1,
                                            modifier = Modifier.testTag("pdf_next_page")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ChevronRight,
                                                contentDescription = "Next Slide",
                                                tint = if (currentPdfPage < pdfPages.size - 1) (if (boardStyle == "BLACKBOARD") Color.White else Color.Black) else Color.Gray
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Drawing Canvas (Receives drags to paint lines)
                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(boardStyle, isEraser, currentColor, strokeWidth) {
                                    detectDragGestures(
                                        onDragStart = { startOffset ->
                                            currentPathPoints.clear()
                                            currentPathPoints.add(startOffset)
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            currentPathPoints.add(change.position)
                                        },
                                        onDragEnd = {
                                            if (currentPathPoints.isNotEmpty()) {
                                                // Save the stroke
                                                val finalPoints = currentPathPoints.toList()
                                                val strokeColor = if (isEraser) {
                                                    if (boardStyle == "BLACKBOARD") Color(0xFF1E3A2F) else Color.White
                                                } else {
                                                    currentColor
                                                }
                                                viewModel.strokes.add(
                                                    Stroke(
                                                        points = finalPoints,
                                                        color = strokeColor,
                                                        width = strokeWidth,
                                                        isEraser = isEraser
                                                    )
                                                )
                                                currentPathPoints.clear()
                                            }
                                        }
                                    )
                                }
                        ) {
                            // Draw existing strokes
                            viewModel.strokes.forEach { stroke ->
                                if (stroke.points.size > 1) {
                                    val path = Path().apply {
                                        moveTo(stroke.points[0].x, stroke.points[0].y)
                                        for (i in 1 until stroke.points.size) {
                                            lineTo(stroke.points[i].x, stroke.points[i].y)
                                        }
                                    }
                                    drawPath(
                                        path = path,
                                        color = stroke.color,
                                        style = DrawScopeStroke(
                                            width = stroke.width,
                                            cap = StrokeCap.Round,
                                            join = StrokeJoin.Round
                                        )
                                    )
                                }
                            }

                            // Draw the current active stroke while dragging
                            if (currentPathPoints.size > 1) {
                                val path = Path().apply {
                                    moveTo(currentPathPoints[0].x, currentPathPoints[0].y)
                                    for (i in 1 until currentPathPoints.size) {
                                        lineTo(currentPathPoints[i].x, currentPathPoints[i].y)
                                    }
                                }
                                val activeColor = if (isEraser) {
                                    if (boardStyle == "BLACKBOARD") Color(0xFF1E3A2F) else Color.White
                                } else {
                                    currentColor
                                }
                                drawPath(
                                    path = path,
                                    color = activeColor,
                                    style = DrawScopeStroke(
                                        width = strokeWidth,
                                        cap = StrokeCap.Round,
                                        join = StrokeJoin.Round
                                    )
                                )
                            }
                        }

                        // BIG RED LETTERS POP-UP: STUDENT LEAVING OVERLAY
                        if (leavingStudent != null) {
                            val name = leavingStudent ?: ""
                            Box(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .background(Color.Black.copy(alpha = 0.85f), shape = RoundedCornerShape(12.dp))
                                    .padding(20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = name.uppercase(),
                                        style = MaterialTheme.typography.displayMedium.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color.Red,
                                            letterSpacing = 2.sp
                                        ),
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.testTag("leaving_student_popup")
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "HAS LEFT THE LIVE CLASS!",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        ),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }

                        // Floating board format switch + PDF loader button
                        Row(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Board Style toggle
                            IconButton(
                                onClick = { viewModel.toggleBoardStyle() },
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.9f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SwapHoriz,
                                    contentDescription = "Switch Board Style",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            // Add PDF overlay
                            if (user?.role == "TEACHER") {
                                var showPdfMenu by remember { mutableStateOf(false) }
                                Box {
                                    Button(
                                        onClick = { showPdfMenu = true },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.9f),
                                            contentColor = MaterialTheme.colorScheme.primary
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                        modifier = Modifier
                                            .height(34.dp)
                                            .testTag("live_add_pdf_btn")
                                    ) {
                                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color(0xFFD32F2F), modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Add PDF", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }

                                    DropdownMenu(
                                        expanded = showPdfMenu,
                                        onDismissRequest = { showPdfMenu = false },
                                        modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Physics - Mechanics.pdf", color = MaterialTheme.colorScheme.onSurface) },
                                            onClick = {
                                                viewModel.loadPdfPreset("Physics - Mechanics.pdf")
                                                showPdfMenu = false
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Mathematics - Algebra & Matrices.pdf", color = MaterialTheme.colorScheme.onSurface) },
                                            onClick = {
                                                viewModel.loadPdfPreset("Mathematics - Algebra & Matrices.pdf")
                                                showPdfMenu = false
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Template - General Slides.pdf", color = MaterialTheme.colorScheme.onSurface) },
                                            onClick = {
                                                viewModel.loadPdfPreset("Template - General Slides.pdf")
                                                showPdfMenu = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Mini indicator for board type
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(8.dp)
                                .background(Color.Black.copy(alpha = 0.6f), shape = RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (boardStyle == "BLACKBOARD") "Chalk Blackboard" else "Dry Whiteboard",
                                fontSize = 10.sp,
                                color = Color.White
                            )
                        }
                    }
                }
                } else {
                    // JITSI VIDEO CALL INTEGRATION PANEL (Height weight matched perfectly)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1.2f)
                            .padding(10.dp),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        LiveVideoConferenceLayout(
                            classroom = classroom,
                            user = user,
                            viewModel = viewModel,
                            context = context,
                            isMicMuted = isMicMuted,
                            onMicMutedChange = { isMicMuted = it },
                            isCameraOff = isCameraOff,
                            onCameraOffChange = { isCameraOff = it },
                            isSpeakerphoneOn = isSpeakerphoneOn,
                            onSpeakerphoneOnChange = { isSpeakerphoneOn = it }
                        )
                    }
                }

                if (activeLiveTab == "BOARD") {
                    // SECTION 2: CANVAS CONTROLS (Colors, Pen, Eraser, Size, Undo, Clear, Simulation)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // ROW 1: TOOLS (Pen & Eraser) + Color Palette
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Active Tools Selector (Pen & Eraser)
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Pen Tool Button
                                IconButton(
                                    onClick = { viewModel.enableEraser(false) },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (!isEraser) MaterialTheme.colorScheme.primary 
                                            else MaterialTheme.colorScheme.secondaryContainer
                                        )
                                        .testTag("tool_pen")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Brush,
                                        contentDescription = "Pen Tool",
                                        tint = if (!isEraser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                // Eraser Tool Button
                                IconButton(
                                    onClick = { viewModel.enableEraser(true) },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (isEraser) MaterialTheme.colorScheme.primary 
                                            else MaterialTheme.colorScheme.secondaryContainer
                                        )
                                        .testTag("eraser_tool")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CleaningServices,
                                        contentDescription = "Eraser Tool",
                                        tint = if (isEraser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            // Color Palette (Horizontal color circles)
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val colors = listOf(Color.White, Color.Yellow, Color.Green, Color.Cyan, Color.Red, Color.Magenta)
                                colors.forEach { col ->
                                    val mappedCol = if (col == Color.White && boardStyle == "WHITEBOARD") Color.Black else col
                                    val isSelected = currentColor == mappedCol && !isEraser
                                    val borderCol = if (boardStyle == "WHITEBOARD") Color.LightGray else Color.White
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(if (isSelected) borderCol else Color.Transparent)
                                            .padding(if (isSelected) 2.dp else 0.dp)
                                            .clip(CircleShape)
                                            .background(mappedCol)
                                            .clickable { viewModel.selectColor(mappedCol) }
                                            .testTag("color_${mappedCol.value}")
                                    )
                                }
                            }
                        }

                        // ROW 2: Stroke Width (Brush sizes) + History controls + Real-time Student Annotation Sim
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Brush size selections
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Size:", 
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 11.sp, 
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                                Spacer(modifier = Modifier.width(2.dp))

                                val sizes = listOf(
                                    Triple(6f, "S", "width_small"),
                                    Triple(12f, "M", "width_medium"),
                                    Triple(20f, "L", "width_large"),
                                    Triple(32f, "XL", "width_xlarge")
                                )

                                sizes.forEach { (sz, label, tag) ->
                                    val isSizeSelected = strokeWidth == sz
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isSizeSelected) MaterialTheme.colorScheme.primary 
                                                else MaterialTheme.colorScheme.surfaceVariant
                                            )
                                            .clickable { viewModel.selectStrokeWidth(sz) }
                                            .testTag(tag),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSizeSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            // Undo, Clear, & Real-time student draw interactive simulation
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Simulate real-time interaction
                                Button(
                                    onClick = {
                                        viewModel.simulateStudentDrawing()
                                        Toast.makeText(
                                            context, 
                                            "📝 Student annotation added in real-time!", 
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier
                                        .height(28.dp)
                                        .testTag("btn_student_draw")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Gesture,
                                        contentDescription = null,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Student Draw", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }

                                // Undo button
                                IconButton(
                                    onClick = { viewModel.undoDrawing() },
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.secondaryContainer)
                                        .testTag("draw_undo")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Undo, 
                                        contentDescription = "Undo", 
                                        tint = MaterialTheme.colorScheme.primary, 
                                        modifier = Modifier.size(14.dp)
                                    )
                                }

                                // Clear button
                                IconButton(
                                    onClick = { viewModel.clearDrawing() },
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFFFEBEE))
                                        .testTag("draw_clear")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete, 
                                        contentDescription = "Clear Board", 
                                        tint = Color(0xFFC62828), 
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // SECTION 3: LIVE CLASS CHAT, COMMUNICATION & VOICE (Bottom half)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1.3f)
                        .padding(horizontal = 10.dp)
                        .padding(bottom = 10.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp)
                    ) {
                        // Section Header: Speaking Indicator
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Forum, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Class Discussions",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            // Dynamic Speaker wave ripple
                            if (isVoiceActive) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier
                                        .background(Color(0xFFE8F5E9), shape = RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Icon(Icons.Default.RecordVoiceOver, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(12.dp))
                                    Text(
                                        text = "${activeSpeakerName ?: "Someone"} speaking...",
                                        fontSize = 10.sp,
                                        color = Color(0xFF2E7D32),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            } else if (isMicMuted) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier
                                        .background(MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                        .testTag("live_mic_muted_indicator")
                                ) {
                                    Icon(Icons.Default.MicOff, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(12.dp))
                                    Text(
                                        text = "Mic Muted",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.error,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Chat list
                        LazyColumn(
                            state = chatListState,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(messages) { msg ->
                                val isMe = msg.senderName == user?.name
                                ChatBubble(message = msg, isMe = isMe)
                            }
                        }

                        // Voice and Input Action Bar
                        if (promoApplied) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Microphone Mute/Unmute Toggle Button with Visual Indicator
                                IconButton(
                                    onClick = {
                                        isMicMuted = !isMicMuted
                                        audioManager.isMicrophoneMute = isMicMuted
                                        Toast.makeText(
                                            context,
                                            if (isMicMuted) "🎤 Microphone muted" else "🎤 Microphone unmuted",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    },
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isMicMuted) MaterialTheme.colorScheme.errorContainer 
                                            else Color(0xFFE8F5E9)
                                        )
                                        .border(
                                            width = 1.5.dp,
                                            color = if (isMicMuted) MaterialTheme.colorScheme.error 
                                                    else Color(0xFF2E7D32),
                                            shape = CircleShape
                                        )
                                        .testTag("live_chat_mic_mute_toggle_btn")
                                ) {
                                    Icon(
                                        imageVector = if (isMicMuted) Icons.Default.MicOff else Icons.Default.Mic,
                                        contentDescription = "Toggle Microphone Mute",
                                        tint = if (isMicMuted) MaterialTheme.colorScheme.onErrorContainer 
                                               else Color(0xFF2E7D32),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                // Microphone/Voice Button (Press to speak)
                                IconButton(
                                    onClick = {
                                        if (isMicMuted) {
                                            Toast.makeText(context, "🎤 Cannot transmit voice while microphone is muted! Please unmute first.", Toast.LENGTH_LONG).show()
                                        } else {
                                            viewModel.sendVoiceMessage()
                                            Toast.makeText(context, "🎤 Simulated 4s voice transmission started!", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isVoiceActive) Color(0xFFD32F2F) 
                                            else if (isMicMuted) MaterialTheme.colorScheme.surfaceVariant
                                            else MaterialTheme.colorScheme.primaryContainer
                                        )
                                        .testTag("live_mic_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.RecordVoiceOver,
                                        contentDescription = "Voice Talk",
                                        tint = if (isVoiceActive) Color.White 
                                               else if (isMicMuted) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                               else MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }

                                // Text Chat Input
                                OutlinedTextField(
                                    value = chatInputText,
                                    onValueChange = { chatInputText = it },
                                    placeholder = { Text("Chat with teacher and students...", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSecondaryContainer) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("live_chat_input"),
                                    singleLine = true,
                                    trailingIcon = {
                                        IconButton(
                                            onClick = {
                                                if (chatInputText.trim().isNotEmpty()) {
                                                    viewModel.postMessage(chatInputText)
                                                    chatInputText = ""
                                                }
                                            },
                                            enabled = chatInputText.trim().isNotEmpty(),
                                            modifier = Modifier.testTag("live_send_chat_btn")
                                        ) {
                                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = MaterialTheme.colorScheme.primary)
                                        }
                                    },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                        focusedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        unfocusedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                    )
                                )
                            }
                        } else {
                            // Promo Code Lock Overlay
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.secondaryContainer)
                                    .clickable { showPromoDialog = true }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Lock, contentDescription = "Locked", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "Chat & Voice Locked",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Apply promo code Ranacr7 to unlock interactions",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                Button(
                                    onClick = { showPromoDialog = true },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("unlock_promo_overlay_btn")
                                ) {
                                    Text("Unlock", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Promo Unlock Dialog for in-class unlocking
        if (showPromoDialog) {
            Dialog(onDismissRequest = { showPromoDialog = false }) {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Unlock Chat & Voice",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )

                        Text(
                            text = "Applying the promo code 'Ranacr7' will instantly unlock the chat room and microphone voice options.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )

                        OutlinedTextField(
                            value = promoCodeInput,
                            onValueChange = { promoCodeInput = it },
                            label = { Text("Promo Code") },
                            placeholder = { Text("Ranacr7") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("inclass_promo_input"),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                unfocusedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                focusedLabelColor = MaterialTheme.colorScheme.primary
                            )
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = { showPromoDialog = false }) {
                                Text("Cancel", color = MaterialTheme.colorScheme.primary)
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Button(
                                onClick = {
                                    val success = viewModel.applyPromoCode(promoCodeInput)
                                    if (success) {
                                        Toast.makeText(context, "🌟 Success! Chat & Voice Unlocked!", Toast.LENGTH_LONG).show()
                                        showPromoDialog = false
                                        promoCodeInput = ""
                                    } else {
                                        Toast.makeText(context, "❌ Incorrect code!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                enabled = promoCodeInput.trim().isNotEmpty(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("inclass_promo_submit")
                            ) {
                                Text("Unlock Now", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Interactive Quiz Dashboard Dialog
        if (showQuizDialog) {
            QuizDashboardDialog(
                viewModel = viewModel,
                onDismiss = { showQuizDialog = false }
            )
        }

        // Save Recording Dialog
        if (showSaveRecordingDialog) {
            var recordingTitle by remember { mutableStateOf("Lecture: ${classroom?.name ?: "Interactive Session"}") }
            var recordingDescription by remember { mutableStateOf("Interactive whiteboard session covering ${classroom?.subject ?: "class materials"}.") }
            val finalDurationText = formattedDuration
            
            Dialog(onDismissRequest = { showSaveRecordingDialog = false }) {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2235)),
                    border = BorderStroke(1.dp, Color(0xFF2B3047))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Videocam,
                                contentDescription = null,
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(28.dp)
                            )
                            Text(
                                text = "Save Recorded Lecture",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                        }

                        Text(
                            text = "The live class recording is ready! Provide a title and description to save and publish it automatically for your students.",
                            fontSize = 12.sp,
                            color = Color.LightGray
                        )

                        // Display Duration
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF131622), shape = RoundedCornerShape(8.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Recording Duration", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                            Text(
                                text = finalDurationText,
                                fontSize = 14.sp,
                                color = Color(0xFF81C784),
                                fontWeight = FontWeight.ExtraBold
                            )
                        }

                        OutlinedTextField(
                            value = recordingTitle,
                            onValueChange = { recordingTitle = it },
                            label = { Text("Lecture Title") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("save_recording_title_input"),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedLabelColor = Color(0xFF81C784),
                                unfocusedLabelColor = Color.Gray,
                                focusedBorderColor = Color(0xFF4CAF50),
                                unfocusedBorderColor = Color(0xFF2B3047)
                            )
                        )

                        OutlinedTextField(
                            value = recordingDescription,
                            onValueChange = { recordingDescription = it },
                            label = { Text("Lecture Description") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("save_recording_desc_input"),
                            maxLines = 3,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedLabelColor = Color(0xFF81C784),
                                unfocusedLabelColor = Color.Gray,
                                focusedBorderColor = Color(0xFF4CAF50),
                                unfocusedBorderColor = Color(0xFF2B3047)
                            )
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = {
                                showSaveRecordingDialog = false
                            }) {
                                Text("Discard", color = Color(0xFFEF5350))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    if (recordingTitle.trim().isNotEmpty()) {
                                        viewModel.addCustomRecordedClass(
                                            title = recordingTitle,
                                            duration = finalDurationText,
                                            desc = recordingDescription
                                        )
                                        viewModel.stopRecording()
                                        showSaveRecordingDialog = false
                                        Toast.makeText(context, "Recording saved and published successfully!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                enabled = recordingTitle.trim().isNotEmpty(),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50), contentColor = Color.White),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("save_recording_confirm_btn")
                            ) {
                                Text("Save & Publish")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChatBubble(message: MessageEntity, isMe: Boolean) {
    val bubbleColor = if (msgIsSystem(message.senderName)) {
        Color(0xFFFFEBEE)
    } else if (isMe) {
        MaterialTheme.colorScheme.primary
    } else if (message.isTeacher) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.secondaryContainer
    }

    val textColor = if (msgIsSystem(message.senderName)) {
        Color(0xFFC62828)
    } else if (isMe) {
        MaterialTheme.colorScheme.onPrimary
    } else if (message.isTeacher) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSecondaryContainer
    }

    val alignment = if (isMe) Alignment.End else Alignment.Start

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalAlignment = alignment
    ) {
        // Sender Name Header (If not system or me)
        if (!isMe && !msgIsSystem(message.senderName)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(bottom = 2.dp, start = 4.dp)
            ) {
                Text(
                    text = message.senderName,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Bold
                )

                if (message.isTeacher) {
                    Text(
                        text = "Teacher",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(2.dp))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }
        }

        // Bubble shape
        Card(
            shape = RoundedCornerShape(
                topStart = 12.dp,
                topEnd = 12.dp,
                bottomStart = if (isMe) 12.dp else 2.dp,
                bottomEnd = if (isMe) 2.dp else 12.dp
            ),
            colors = CardDefaults.cardColors(containerColor = bubbleColor),
            border = if (msgIsSystem(message.senderName)) BorderStroke(1.dp, Color(0xFFEF5350)) else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                if (message.isVoice) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val activeIconTint = if (isMe) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
                        val activeTextTint = if (isMe) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                        
                        Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = activeIconTint, modifier = Modifier.size(16.dp))
                        
                        // Fake voice wave visualizer dots
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            listOf(6, 14, 18, 10, 16, 22, 12, 8, 4, 10, 14, 6).forEach { ht ->
                                Box(
                                    modifier = Modifier
                                        .width(2.dp)
                                        .height(ht.dp)
                                        .background(activeIconTint)
                                )
                            }
                        }
                        
                        Text(
                            text = "0:04",
                            fontSize = 11.sp,
                            color = activeTextTint
                        )
                    }
                } else {
                    Text(
                        text = message.message,
                        fontSize = 13.sp,
                        color = textColor,
                        fontWeight = if (msgIsSystem(message.senderName)) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

fun msgIsSystem(name: String): Boolean {
    return name.contains("System", ignoreCase = true) || name.contains("Alert", ignoreCase = true)
}

@Composable
fun LiveVideoConferenceLayout(
    classroom: ClassroomEntity?,
    user: UserEntity?,
    viewModel: ClassroomViewModel,
    context: Context,
    isMicMuted: Boolean,
    onMicMutedChange: (Boolean) -> Unit,
    isCameraOff: Boolean,
    onCameraOffChange: (Boolean) -> Unit,
    isSpeakerphoneOn: Boolean,
    onSpeakerphoneOnChange: (Boolean) -> Unit
) {
    var videoMode by remember { mutableStateOf("SIMULATOR") } // "JITSI" or "SIMULATOR"

    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121420))
            .padding(8.dp)
    ) {
        // Toggle Mode Selector (Jitsi Web vs Multi-user Grid)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { videoMode = "JITSI" },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (videoMode == "JITSI") Color(0xFF1E88E5) else Color(0xFF1F2335),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f).height(36.dp).testTag("video_mode_jitsi_btn")
            ) {
                Icon(Icons.Default.Language, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Jitsi Web Meet", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = { videoMode = "SIMULATOR" },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (videoMode == "SIMULATOR") Color(0xFF1E88E5) else Color(0xFF1F2335),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f).height(36.dp).testTag("video_mode_sim_btn")
            ) {
                Icon(Icons.Default.GridView, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Class Video Grid", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color(0xFF0F111A), shape = RoundedCornerShape(12.dp))
                .border(BorderStroke(1.dp, Color(0xFF1F2335)), shape = RoundedCornerShape(12.dp))
                .clip(RoundedCornerShape(12.dp))
        ) {
            if (videoMode == "JITSI") {
                // Real WebView loading public Jitsi Meet room
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                mediaPlaybackRequiresUserGesture = false
                                useWideViewPort = true
                                loadWithOverviewMode = true
                            }
                            webViewClient = WebViewClient()
                            webChromeClient = object : WebChromeClient() {
                                override fun onPermissionRequest(request: PermissionRequest) {
                                    request.grant(request.resources)
                                }
                            }
                            val safeId = (classroom?.id ?: "General").replace(" ", "-").trim()
                            val roomUrl = "https://meet.jit.si/MMClassroom-$safeId"
                            loadUrl(roomUrl)
                        }
                    },
                    modifier = Modifier.fillMaxSize().testTag("jitsi_webview")
                )
            } else {
                // Multi-participant interactive grid simulator
                val activeSpeaker by viewModel.activeSpeakerName.collectAsState()
                
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Row 1: Teacher & Amit
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Teacher Participant
                        VideoParticipantCard(
                            name = classroom?.teacherName ?: "Instructor",
                            role = "TEACHER",
                            isSpeaking = activeSpeaker == (classroom?.teacherName ?: "Instructor"),
                            isMuted = false,
                            isVideoOff = false,
                            modifier = Modifier.weight(1f)
                        )

                        // Student 1: Amit Sharma
                        VideoParticipantCard(
                            name = "Amit Sharma",
                            role = "STUDENT",
                            isSpeaking = activeSpeaker == "Amit Sharma",
                            isMuted = false,
                            isVideoOff = false,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Row 2: Priya Patel & User (Self)
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Student 2: Priya Patel
                        VideoParticipantCard(
                            name = "Priya Patel",
                            role = "STUDENT",
                            isSpeaking = activeSpeaker == "Priya Patel",
                            isMuted = true,
                            isVideoOff = false,
                            modifier = Modifier.weight(1f)
                        )

                        // Self Participant
                        VideoParticipantCard(
                            name = "${user?.name ?: "Me"} (You)",
                            role = user?.role ?: "STUDENT",
                            isSpeaking = false,
                            isMuted = isMicMuted,
                            isVideoOff = isCameraOff,
                            isSelf = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Live Controls Action Bar at the Bottom of video panel
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Mic Toggle
            IconButton(
                onClick = {
                    val newValue = !isMicMuted
                    onMicMutedChange(newValue)
                    audioManager.isMicrophoneMute = newValue
                    Toast.makeText(
                        context, 
                        if (newValue) "Microphone muted" else "Microphone unmuted", 
                        Toast.LENGTH_SHORT
                    ).show()
                },
                modifier = Modifier
                    .size(40.dp)
                    .background(if (isMicMuted) Color(0xFFEF5350) else Color(0xFF2E7D32), shape = CircleShape)
                    .testTag("video_mic_toggle_btn")
            ) {
                Icon(
                    imageVector = if (isMicMuted) Icons.Default.MicOff else Icons.Default.Mic,
                    contentDescription = "Toggle Mic",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Camera Toggle
            IconButton(
                onClick = {
                    val newValue = !isCameraOff
                    onCameraOffChange(newValue)
                    Toast.makeText(
                        context, 
                        if (newValue) "Camera feed disabled" else "Camera feed active", 
                        Toast.LENGTH_SHORT
                    ).show()
                },
                modifier = Modifier
                    .size(40.dp)
                    .background(if (isCameraOff) Color(0xFFEF5350) else Color(0xFF1E88E5), shape = CircleShape)
                    .testTag("video_camera_toggle_btn")
            ) {
                Icon(
                    imageVector = if (isCameraOff) Icons.Default.VideocamOff else Icons.Default.Videocam,
                    contentDescription = "Toggle Camera",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Speaker Toggle (Routing audio output using AudioManager)
            IconButton(
                onClick = {
                    val newValue = !isSpeakerphoneOn
                    onSpeakerphoneOnChange(newValue)
                    audioManager.isSpeakerphoneOn = newValue
                    Toast.makeText(
                        context, 
                        if (newValue) "Output routed to Speakerphone" else "Output routed to Earpiece", 
                        Toast.LENGTH_SHORT
                    ).show()
                },
                modifier = Modifier
                    .size(40.dp)
                    .background(if (isSpeakerphoneOn) Color(0xFF8E24AA) else Color(0xFF757575), shape = CircleShape)
                    .testTag("video_speaker_toggle_btn")
            ) {
                Icon(
                    imageVector = if (isSpeakerphoneOn) Icons.Default.VolumeUp else Icons.Default.VolumeDown,
                    contentDescription = "Toggle Speakerphone",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Simulated Active Speaker Trigger (Simulate Amit/Priya asking a question)
            Button(
                onClick = {
                    val speakers = listOf("Amit Sharma", "Priya Patel", classroom?.teacherName ?: "Instructor")
                    val randomSpeaker = speakers.random()
                    viewModel.simulateActiveSpeaker(randomSpeaker)
                    Toast.makeText(context, "$randomSpeaker is now speaking", Toast.LENGTH_SHORT).show()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3F51B5), contentColor = Color.White),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 8.dp),
                modifier = Modifier.height(34.dp).testTag("video_sim_speaker_btn")
            ) {
                Icon(Icons.Default.RecordVoiceOver, contentDescription = null, modifier = Modifier.size(12.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Sim Speaker", fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun VideoParticipantCard(
    name: String,
    role: String,
    isSpeaking: Boolean,
    isMuted: Boolean,
    isVideoOff: Boolean,
    isSelf: Boolean = false,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxHeight()
            .border(
                border = BorderStroke(
                    width = if (isSpeaking) 2.dp else 1.dp,
                    color = if (isSpeaking) Color(0xFF4CAF50) else Color(0xFF1F2335)
                ),
                shape = RoundedCornerShape(8.dp)
            ),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2235))
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            if (isVideoOff) {
                // Video Off placeholder
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF131622)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(Color(0xFF2B3047), shape = CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = Color.LightGray,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Camera Off", fontSize = 10.sp, color = Color.Gray)
                    }
                }
            } else {
                // Simulated camera stream color waves
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                colors = if (isSelf) {
                                    listOf(Color(0xFF1B2E3C), Color(0xFF12232E))
                                } else if (role == "TEACHER") {
                                    listOf(Color(0xFF2A1B3D), Color(0xFF1D122E))
                                } else {
                                    listOf(Color(0xFF1B3D2B), Color(0xFF122E1F))
                                }
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    // Audio spectrum bars if speaking
                    if (isSpeaking) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            repeat(3) { index ->
                                val infiniteTransition = rememberInfiniteTransition(label = "audio_bar_$index")
                                val heightScale by infiniteTransition.animateFloat(
                                    initialValue = 12f,
                                    targetValue = 28f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(durationMillis = 300 + index * 100, easing = LinearEasing),
                                        repeatMode = RepeatMode.Reverse
                                    ),
                                    label = "audio_bar_scale"
                                )
                                Box(
                                    modifier = Modifier
                                        .width(3.dp)
                                        .height(heightScale.dp)
                                        .background(Color(0xFF4CAF50), shape = RoundedCornerShape(2.dp))
                                )
                            }
                        }
                    } else {
                        // User avatar initials
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(Color.White.copy(alpha = 0.08f), shape = CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = name.split(" ").mapNotNull { it.firstOrNull() }.joinToString("").take(2).uppercase(),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            // Badge with Name & Muted Status
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(6.dp)
                    .background(Color.Black.copy(alpha = 0.6f), shape = RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = name,
                    fontSize = 10.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 60.dp)
                )

                if (isMuted) {
                    Icon(
                        imageVector = Icons.Default.MicOff,
                        contentDescription = "Muted",
                        tint = Color(0xFFEF5350),
                        modifier = Modifier.size(10.dp)
                    )
                }
            }

            // Role Badge
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .background(
                        color = if (role == "TEACHER") Color(0xFF8E24AA) else Color(0xFF78909C),
                        shape = RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Text(
                    text = role,
                    fontSize = 8.sp,
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}
