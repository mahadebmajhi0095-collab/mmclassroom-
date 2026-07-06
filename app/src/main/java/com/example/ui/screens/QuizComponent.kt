package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.*
import com.example.viewmodel.ClassroomViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizDashboardDialog(
    viewModel: ClassroomViewModel,
    onDismiss: () -> Unit
) {
    val user by viewModel.userState.collectAsState()
    val quizzes by viewModel.quizzes.collectAsState()
    val submissions by viewModel.quizSubmissions.collectAsState()
    val context = LocalContext.current

    val isTeacher = user?.role == "TEACHER"

    var currentView by remember { mutableStateOf("LIST") } // "LIST", "BUILDER", "TAKER", "REVIEW"
    var selectedQuizForTaker by remember { mutableStateOf<QuizEntity?>(null) }
    var selectedQuizForReview by remember { mutableStateOf<QuizEntity?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.85f)
                .testTag("quiz_dashboard_card"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF131622)),
            border = BorderStroke(1.dp, Color(0xFF2B3047))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Modal Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Quiz,
                            contentDescription = "Quiz Icon",
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(28.dp)
                        )
                        Column {
                            Text(
                                text = "Interactive Classroom Quizzes",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                            )
                            Text(
                                text = if (isTeacher) "Build, deploy & track assessments" else "Test your knowledge & earn scores",
                                style = MaterialTheme.typography.bodySmall.copy(color = Color.LightGray)
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color(0xFF1E2230))
                            .size(36.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color(0xFF2B3047))
                Spacer(modifier = Modifier.height(16.dp))

                AnimatedContent(
                    targetState = currentView,
                    transitionSpec = {
                        fadeIn() togetherWith fadeOut()
                    },
                    label = "quiz_view_transition",
                    modifier = Modifier.weight(1f)
                ) { viewState ->
                    when (viewState) {
                        "LIST" -> {
                            QuizListView(
                                quizzes = quizzes,
                                submissions = submissions,
                                isTeacher = isTeacher,
                                currentUsername = user?.name ?: "",
                                onCreateNewQuiz = { currentView = "BUILDER" },
                                onDeployToggle = { quizId, active ->
                                    viewModel.toggleQuizDeployment(quizId, active)
                                },
                                onTakeQuiz = { quiz ->
                                    selectedQuizForTaker = quiz
                                    currentView = "TAKER"
                                },
                                onReviewQuiz = { quiz ->
                                    selectedQuizForReview = quiz
                                    currentView = "REVIEW"
                                }
                            )
                        }
                        "BUILDER" -> {
                            QuizBuilderView(
                                onSaveQuiz = { title, questions ->
                                    viewModel.createAndAddQuiz(title, questions)
                                    Toast.makeText(context, "Quiz Created successfully!", Toast.LENGTH_SHORT).show()
                                    currentView = "LIST"
                                },
                                onCancel = { currentView = "LIST" }
                            )
                        }
                        "TAKER" -> {
                            selectedQuizForTaker?.let { quiz ->
                                QuizTakeView(
                                    quiz = quiz,
                                    onSubmitAnswers = { answers ->
                                        viewModel.submitQuizAnswers(quiz.id, answers)
                                        Toast.makeText(context, "Quiz Answers Submitted!", Toast.LENGTH_LONG).show()
                                        currentView = "LIST"
                                    },
                                    onCancel = { currentView = "LIST" }
                                )
                            }
                        }
                        "REVIEW" -> {
                            selectedQuizForReview?.let { quiz ->
                                val userSubmission = submissions.find { it.quizId == quiz.id && it.studentName == user?.name }
                                QuizReviewView(
                                    quiz = quiz,
                                    submission = userSubmission,
                                    onBack = { currentView = "LIST" }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QuizListView(
    quizzes: List<QuizEntity>,
    submissions: List<QuizSubmissionEntity>,
    isTeacher: Boolean,
    currentUsername: String,
    onCreateNewQuiz: () -> Unit,
    onDeployToggle: (String, Boolean) -> Unit,
    onTakeQuiz: (QuizEntity) -> Unit,
    onReviewQuiz: (QuizEntity) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Quizzes in this class (${quizzes.size})",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )

            if (isTeacher) {
                Button(
                    onClick = onCreateNewQuiz,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50), contentColor = Color.White),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("create_new_quiz_btn")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Build Quiz", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (quizzes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MenuBook,
                        contentDescription = null,
                        tint = Color.DarkGray,
                        modifier = Modifier.size(64.dp)
                    )
                    Text(
                        text = "No Quizzes Configured",
                        color = Color.Gray,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = if (isTeacher) "Click 'Build Quiz' to compose and launch an evaluation!" else "The teacher hasn't published any quizzes yet.",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(quizzes) { quiz ->
                    val quizQuestions = QuizJsonHelper.deserializeQuestions(quiz.questionsJson)
                    val studentSubmission = submissions.find { it.quizId == quiz.id && it.studentName == currentUsername }
                    val quizSubmissionsList = submissions.filter { it.quizId == quiz.id }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2230)),
                        border = BorderStroke(1.dp, Color(0xFF2B3047))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // Header Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = quiz.title,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    )
                                    Text(
                                        text = "${quizQuestions.size} Multiple Choice Questions",
                                        style = MaterialTheme.typography.bodySmall.copy(color = Color.LightGray)
                                    )
                                }

                                // Status Badge
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier
                                        .background(
                                            if (quiz.isDeployed) Color(0xFF1B5E20) else Color(0xFF37474F),
                                            shape = RoundedCornerShape(6.dp)
                                        )
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(if (quiz.isDeployed) Color(0xFF81C784) else Color(0xFFB0BEC5))
                                    )
                                    Text(
                                        text = if (quiz.isDeployed) "DEPLOYED" else "DRAFT",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            if (isTeacher) {
                                // TEACHER TOOLS
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${quizSubmissionsList.size} Student Submissions",
                                        fontSize = 12.sp,
                                        color = Color.LightGray,
                                        fontWeight = FontWeight.Bold
                                    )

                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(
                                            onClick = { onDeployToggle(quiz.id, quiz.isDeployed) },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (quiz.isDeployed) Color(0xFFEF5350) else Color(0xFF00E676),
                                                contentColor = if (quiz.isDeployed) Color.White else Color.Black
                                            ),
                                            shape = RoundedCornerShape(6.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            modifier = Modifier.height(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (quiz.isDeployed) Icons.Default.Cancel else Icons.Default.Send,
                                                contentDescription = null,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = if (quiz.isDeployed) "Undeploy" else "Deploy Now",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.ExtraBold
                                            )
                                        }
                                    }
                                }

                                if (quizSubmissionsList.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    HorizontalDivider(color = Color(0xFF2B3047))
                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = "Submissions Ledger:",
                                        fontSize = 11.sp,
                                        color = Color.LightGray,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(bottom = 6.dp)
                                    )

                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        quizSubmissionsList.forEach { sub ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(Color(0xFF131622), shape = RoundedCornerShape(6.dp))
                                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(text = sub.studentName, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                Text(
                                                    text = "Score: ${sub.score}/${sub.totalQuestions} (${(sub.score * 100 / sub.totalQuestions)}%)",
                                                    color = if (sub.score * 100 / sub.totalQuestions >= 70) Color(0xFF81C784) else Color(0xFFFFB74D),
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.ExtraBold
                                                )
                                            }
                                        }
                                    }
                                }
                            } else {
                                // STUDENT TOOLS
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (studentSubmission != null) {
                                        // Completed
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(16.dp))
                                            Text(
                                                text = "Completed: ${studentSubmission.score}/${studentSubmission.totalQuestions} Correct",
                                                color = Color(0xFF81C784),
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        Button(
                                            onClick = { onReviewQuiz(quiz) },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2B3047), contentColor = Color.White),
                                            shape = RoundedCornerShape(6.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                            modifier = Modifier.height(32.dp)
                                        ) {
                                            Text("Review Answers", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    } else {
                                        // Not Completed
                                        Text(
                                            text = "Not attempted yet",
                                            fontSize = 12.sp,
                                            color = Color.LightGray
                                        )

                                        Button(
                                            onClick = { onTakeQuiz(quiz) },
                                            enabled = quiz.isDeployed,
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color(0xFF4CAF50),
                                                contentColor = Color.White,
                                                disabledContainerColor = Color(0xFF37474F),
                                                disabledContentColor = Color.Gray
                                            ),
                                            shape = RoundedCornerShape(6.dp),
                                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                                            modifier = Modifier.height(32.dp)
                                        ) {
                                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = if (quiz.isDeployed) "Take Quiz 📝" else "Locked",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.ExtraBold
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizBuilderView(
    onSaveQuiz: (String, List<QuizQuestion>) -> Unit,
    onCancel: () -> Unit
) {
    var quizTitle by remember { mutableStateOf("") }
    val questions = remember { mutableStateListOf<QuizQuestion>() }

    // State variables for adding a new question
    var newQuestionText by remember { mutableStateOf("") }
    var optionA by remember { mutableStateOf("") }
    var optionB by remember { mutableStateOf("") }
    var optionC by remember { mutableStateOf("") }
    var optionD by remember { mutableStateOf("") }
    var correctIndex by remember { mutableStateOf(0) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "Assemble Classroom Quiz",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = quizTitle,
                onValueChange = { quizTitle = it },
                label = { Text("Quiz Title (e.g. Limits & Continuity Quiz)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedLabelColor = Color(0xFF4CAF50),
                    unfocusedLabelColor = Color.LightGray,
                    focusedBorderColor = Color(0xFF4CAF50),
                    unfocusedBorderColor = Color(0xFF2B3047)
                )
            )
        }

        if (questions.isNotEmpty()) {
            item {
                Text(
                    text = "Configured Questions (${questions.size}):",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            items(questions.toList()) { q ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2230)),
                    border = BorderStroke(1.dp, Color(0xFF2B3047))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(text = q.text, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.weight(1f))
                            IconButton(onClick = { questions.remove(q) }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red, modifier = Modifier.size(16.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        q.options.forEachIndexed { i, opt ->
                            Row(
                                modifier = Modifier.padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (i == q.correctAnswerIndex) Icons.Default.CheckCircle else Icons.Default.Circle,
                                    contentDescription = null,
                                    tint = if (i == q.correctAnswerIndex) Color(0xFF4CAF50) else Color.Gray,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = opt, color = if (i == q.correctAnswerIndex) Color(0xFF81C784) else Color.LightGray, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF161925)),
                border = BorderStroke(1.dp, Color(0xFF2B3047))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(text = "Add Multiple Choice Question", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)

                    OutlinedTextField(
                        value = newQuestionText,
                        onValueChange = { newQuestionText = it },
                        label = { Text("Question Text") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF2196F3),
                            unfocusedBorderColor = Color(0xFF2B3047)
                        )
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(
                            value = optionA,
                            onValueChange = { optionA = it },
                            label = { Text("Option A") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                        )
                        OutlinedTextField(
                            value = optionB,
                            onValueChange = { optionB = it },
                            label = { Text("Option B") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                        )
                        OutlinedTextField(
                            value = optionC,
                            onValueChange = { optionC = it },
                            label = { Text("Option C (Optional)") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                        )
                        OutlinedTextField(
                            value = optionD,
                            onValueChange = { optionD = it },
                            label = { Text("Option D (Optional)") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                        )
                    }

                    // Choose Correct Option Row
                    Text(text = "Select Correct Answer Index:", color = Color.LightGray, fontSize = 12.sp)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf("A", "B", "C", "D").forEachIndexed { index, name ->
                            val isSelected = correctIndex == index
                            FilterChip(
                                selected = isSelected,
                                onClick = { correctIndex = index },
                                label = { Text(name) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF4CAF50),
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    Button(
                        onClick = {
                            if (newQuestionText.trim().isNotEmpty() && optionA.trim().isNotEmpty() && optionB.trim().isNotEmpty()) {
                                val opts = mutableListOf<String>()
                                opts.add(optionA.trim())
                                opts.add(optionB.trim())
                                if (optionC.trim().isNotEmpty()) opts.add(optionC.trim())
                                if (optionD.trim().isNotEmpty()) opts.add(optionD.trim())

                                val adjustedCorrectIndex = correctIndex.coerceAtMost(opts.size - 1)

                                questions.add(
                                    QuizQuestion(
                                        text = newQuestionText.trim(),
                                        options = opts,
                                        correctAnswerIndex = adjustedCorrectIndex
                                    )
                                )

                                // Reset Fields
                                newQuestionText = ""
                                optionA = ""
                                optionB = ""
                                optionC = ""
                                optionD = ""
                                correctIndex = 0
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3), contentColor = Color.White),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Add Question To List", fontSize = 12.sp)
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onCancel) {
                    Text("Cancel", color = Color.LightGray)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Button(
                    onClick = {
                        if (quizTitle.trim().isNotEmpty() && questions.isNotEmpty()) {
                            onSaveQuiz(quizTitle.trim(), questions.toList())
                        }
                    },
                    enabled = quizTitle.trim().isNotEmpty() && questions.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50),
                        contentColor = Color.White,
                        disabledContainerColor = Color(0xFF2E3B2F)
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Save & Publish", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun QuizTakeView(
    quiz: QuizEntity,
    onSubmitAnswers: (List<Int>) -> Unit,
    onCancel: () -> Unit
) {
    val questions = remember(quiz) { QuizJsonHelper.deserializeQuestions(quiz.questionsJson) }
    var currentQuestionIndex by remember { mutableStateOf(0) }
    val selectedAnswers = remember(quiz) { mutableStateListOf<Int>().apply {
        // Initialize with unselected (-1)
        repeat(questions.size) { add(-1) }
    } }

    val currentQuestion = questions.getOrNull(currentQuestionIndex)

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Progress Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Taking: ${quiz.title}",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Question ${currentQuestionIndex + 1} of ${questions.size}",
                color = Color.LightGray,
                fontSize = 13.sp
            )
        }

        // Progress bar
        LinearProgressIndicator(
            progress = (currentQuestionIndex + 1).toFloat() / questions.size.toFloat(),
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = Color(0xFF4CAF50),
            trackColor = Color(0xFF2B3047)
        )

        if (currentQuestion != null) {
            // Card holding active question
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2230)),
                border = BorderStroke(1.dp, Color(0xFF2B3047))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = currentQuestion.text,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            lineHeight = 22.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(currentQuestion.options.size) { index ->
                            val optionText = currentQuestion.options[index]
                            val isSelected = selectedAnswers[currentQuestionIndex] == index

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedAnswers[currentQuestionIndex] = index },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) Color(0xFF1B5E20) else Color(0xFF131622)
                                ),
                                border = BorderStroke(1.dp, if (isSelected) Color(0xFF81C784) else Color(0xFF2B3047))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(if (isSelected) Color(0xFF81C784) else Color(0xFF2B3047)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = ('A' + index).toString(),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (isSelected) Color.Black else Color.White
                                        )
                                    }
                                    Text(
                                        text = optionText,
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Footer Navigation
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onCancel) {
                    Text("Exit", color = Color.Red)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (currentQuestionIndex > 0) {
                        OutlinedButton(
                            onClick = { currentQuestionIndex-- },
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFF2B3047)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                        ) {
                            Text("Prev")
                        }
                    }

                    if (currentQuestionIndex < questions.size - 1) {
                        Button(
                            onClick = { currentQuestionIndex++ },
                            enabled = selectedAnswers[currentQuestionIndex] != -1,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
                        ) {
                            Text("Next")
                        }
                    } else {
                        Button(
                            onClick = { onSubmitAnswers(selectedAnswers.toList()) },
                            enabled = !selectedAnswers.contains(-1),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50), contentColor = Color.White)
                        ) {
                            Text("Submit 🚀", fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QuizReviewView(
    quiz: QuizEntity,
    submission: QuizSubmissionEntity?,
    onBack: () -> Unit
) {
    val questions = remember(quiz) { QuizJsonHelper.deserializeQuestions(quiz.questionsJson) }
    val submittedAnswers = remember(submission) {
        if (submission != null) QuizJsonHelper.deserializeAnswers(submission.submittedAnswersJson) else emptyList()
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Reviewing: ${quiz.title}",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                submission?.let {
                    Text(
                        text = "Automated score: ${it.score}/${it.totalQuestions} (${(it.score * 100 / it.totalQuestions)}%)",
                        color = Color(0xFF81C784),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
        }

        HorizontalDivider(color = Color(0xFF2B3047))

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(questions.size) { index ->
                val q = questions[index]
                val submittedAns = submittedAnswers.getOrNull(index) ?: -1
                val correctAns = q.correctAnswerIndex
                val isCorrect = submittedAns == correctAns

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2230)),
                    border = BorderStroke(1.dp, if (isCorrect) Color(0xFF388E3C) else Color(0xFFD32F2F))
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = "${index + 1}. ${q.text}",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                modifier = Modifier.weight(1f)
                            )

                            Icon(
                                imageVector = if (isCorrect) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                contentDescription = null,
                                tint = if (isCorrect) Color(0xFF4CAF50) else Color(0xFFEF5350),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            q.options.forEachIndexed { optIdx, optTxt ->
                                val isSubmitted = submittedAns == optIdx
                                val isCorrectOpt = correctAns == optIdx

                                val itemBgColor = when {
                                    isCorrectOpt -> Color(0xFF1B5E20)
                                    isSubmitted && !isCorrect -> Color(0xFFB71C1C)
                                    else -> Color(0xFF131622)
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(itemBgColor, shape = RoundedCornerShape(8.dp))
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = optTxt, color = Color.White, fontSize = 12.sp)

                                    if (isCorrectOpt) {
                                        Text(text = "CORRECT ANSWER", color = Color(0xFF81C784), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    } else if (isSubmitted && !isCorrect) {
                                        Text(text = "YOUR ANSWER", color = Color(0xFFFF8A80), fontSize = 9.sp, fontWeight = FontWeight.Bold)
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
