package com.example.viewmodel

import android.app.Application
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

// Stroke data model for Blackboard/Whiteboard
data class Stroke(
    val points: List<Offset>,
    val color: Color,
    val width: Float,
    val isEraser: Boolean = false
)

// PDF Page data model
data class PdfPage(
    val title: String,
    val description: String,
    val equations: List<String> = emptyList(),
    val pointsOfInterest: List<String> = emptyList()
)

class ClassroomViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ClassroomRepository
    
    val userState: StateFlow<UserEntity?>
    val classroomsState: StateFlow<List<ClassroomEntity>>
    
    private val _activeClassroom = MutableStateFlow<ClassroomEntity?>(null)
    val activeClassroom: StateFlow<ClassroomEntity?> = _activeClassroom.asStateFlow()

    private val _recordedClasses = MutableStateFlow<List<RecordedClassEntity>>(emptyList())
    val recordedClasses: StateFlow<List<RecordedClassEntity>> = _recordedClasses.asStateFlow()

    private val _chatMessages = MutableStateFlow<List<MessageEntity>>(emptyList())
    val chatMessages: StateFlow<List<MessageEntity>> = _chatMessages.asStateFlow()

    private val _studentProgressList = MutableStateFlow<List<StudentProgressEntity>>(emptyList())
    val studentProgressList: StateFlow<List<StudentProgressEntity>> = _studentProgressList.asStateFlow()

    private val _quizzes = MutableStateFlow<List<QuizEntity>>(emptyList())
    val quizzes: StateFlow<List<QuizEntity>> = _quizzes.asStateFlow()

    private val _quizSubmissions = MutableStateFlow<List<QuizSubmissionEntity>>(emptyList())
    val quizSubmissions: StateFlow<List<QuizSubmissionEntity>> = _quizSubmissions.asStateFlow()

    // Board configuration
    private val _boardStyle = MutableStateFlow("BLACKBOARD") // "BLACKBOARD" or "WHITEBOARD"
    val boardStyle: StateFlow<String> = _boardStyle.asStateFlow()

    // Interactive Drawing state
    val strokes = mutableStateListOf<Stroke>()
    private val _currentColor = MutableStateFlow(Color.White)
    val currentColor: StateFlow<Color> = _currentColor.asStateFlow()

    private val _currentStrokeWidth = MutableStateFlow(8f)
    val currentStrokeWidth: StateFlow<Float> = _currentStrokeWidth.asStateFlow()

    private val _isEraserMode = MutableStateFlow(false)
    val isEraserMode: StateFlow<Boolean> = _isEraserMode.asStateFlow()

    // PDF / Slides on Board
    private val _activePdfName = MutableStateFlow<String?>(null)
    val activePdfName: StateFlow<String?> = _activePdfName.asStateFlow()

    private val _currentPdfPageIndex = MutableStateFlow(0)
    val currentPdfPageIndex: StateFlow<Int> = _currentPdfPageIndex.asStateFlow()

    private val _pdfPages = MutableStateFlow<List<PdfPage>>(emptyList())
    val pdfPages: StateFlow<List<PdfPage>> = _pdfPages.asStateFlow()

    // Left Student Warning Overlay
    private val _leavingStudentName = MutableStateFlow<String?>(null)
    val leavingStudentName: StateFlow<String?> = _leavingStudentName.asStateFlow()

    // Simulated students currently in class
    private val _onlineStudents = MutableStateFlow<List<String>>(
        listOf("Amit Sharma", "Priya Patel", "Anjali Rao", "Rahul Sen", "Vijay Kumar", "Siddharth J.", "Sneha Das")
    )
    val onlineStudents: StateFlow<List<String>> = _onlineStudents.asStateFlow()

    // Live Voice Chat status
    private val _isVoiceActive = MutableStateFlow(false)
    val isVoiceActive: StateFlow<Boolean> = _isVoiceActive.asStateFlow()

    private val _activeSpeakerName = MutableStateFlow<String?>(null)
    val activeSpeakerName: StateFlow<String?> = _activeSpeakerName.asStateFlow()

    // Error and navigation helpers
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        repository = ClassroomRepository(database.classroomDao())
        
        userState = repository.userFlow.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

        classroomsState = repository.classroomsFlow.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        // Generate mock data if classrooms are empty
        viewModelScope.launch {
            repository.classroomsFlow.first().let { list ->
                if (list.isEmpty()) {
                    setupPreloadedClassrooms()
                }
            }
        }
    }

    private suspend fun setupPreloadedClassrooms() {
        // Preload classroom 1
        val class1Id = "math-matrix"
        val class1 = ClassroomEntity(
            id = class1Id,
            name = "Calculus & Linear Algebra",
            subject = "Mathematics",
            teacherName = "Prof. R. Rana",
            inviteLink = "mmclassroom://join/$class1Id"
        )
        repository.createClassroom(class1)

        // Preload recorded classes for classroom 1
        repository.addRecordedClass(
            RecordedClassEntity(
                classroomId = class1Id,
                title = "Introduction to Limits and Continuity",
                subject = "Mathematics",
                duration = "45 mins",
                videoUrl = "mock_limits",
                date = "June 28, 2026",
                description = "An intuitive introduction to the concept of limits, left-hand and right-hand limits, and formal definitions of continuity."
            )
        )
        repository.addRecordedClass(
            RecordedClassEntity(
                classroomId = class1Id,
                title = "Derivatives and Tangent Lines",
                subject = "Mathematics",
                duration = "52 mins",
                videoUrl = "mock_derivatives",
                date = "July 02, 2026",
                description = "How to find slope of tangent lines using first principles, standard derivative rules, and physical interpretations of rate of change."
            )
        )

        // Preload classroom 2
        val class2Id = "quantum-physics"
        val class2 = ClassroomEntity(
            id = class2Id,
            name = "Quantum Mechanics Basics",
            subject = "Physics",
            teacherName = "Dr. Shaswat",
            inviteLink = "mmclassroom://join/$class2Id"
        )
        repository.createClassroom(class2)

        repository.addRecordedClass(
            RecordedClassEntity(
                classroomId = class2Id,
                title = "Blackbody Radiation & Photoelectric Effect",
                subject = "Physics",
                duration = "1 hr 10 mins",
                videoUrl = "mock_photoelectric",
                date = "June 15, 2026",
                description = "Exploring the failure of classical physics, Planck's constant discovery, and Einstein's explanation of the Photoelectric effect."
            )
        )
    }

    // User Profile Authentication
    fun registerOrLogin(name: String, emailOrPhone: String, avatar: String, role: String, promoCode: String) {
        viewModelScope.launch {
            val promoApplied = promoCode.trim().equals("Ranacr7", ignoreCase = true)
            val user = UserEntity(
                id = 1,
                name = name.trim().ifEmpty { "Anonymous User" },
                emailOrPhone = emailOrPhone.trim().ifEmpty { "no-contact@mmclassroom.com" },
                profilePic = avatar,
                role = role,
                promoApplied = promoApplied
            )
            repository.saveUser(user)
        }
    }

    // Apply Promo Code after Login
    fun applyPromoCode(code: String): Boolean {
        val isValid = code.trim().equals("Ranacr7", ignoreCase = true)
        if (isValid) {
            viewModelScope.launch {
                val currentUser = repository.getUserSync()
                if (currentUser != null) {
                    repository.saveUser(currentUser.copy(promoApplied = true))
                }
            }
        }
        return isValid
    }

    // Classroom Host / Join Management
    fun createNewClassroom(name: String, subject: String) {
        viewModelScope.launch {
            val currentUser = userState.value
            val teacherName = currentUser?.name ?: "Teacher"
            val classId = UUID.randomUUID().toString().substring(0, 8)
            val inviteLink = "mmclassroom://join/$classId"
            
            val newClass = ClassroomEntity(
                id = classId,
                name = name.trim().ifEmpty { "Untitled Class" },
                subject = subject.trim().ifEmpty { "General Study" },
                teacherName = teacherName,
                inviteLink = inviteLink
            )
            repository.createClassroom(newClass)
            _activeClassroom.value = newClass
            loadClassroomContext(classId)
        }
    }

    fun joinClassroomByLink(linkOrId: String): Boolean {
        // Parse link like "mmclassroom://join/classId" or use raw ID
        val parsedId = if (linkOrId.startsWith("mmclassroom://join/")) {
            linkOrId.substringAfter("mmclassroom://join/").trim()
        } else {
            linkOrId.trim()
        }

        val foundClass = classroomsState.value.find { it.id == parsedId }
        return if (foundClass != null) {
            _activeClassroom.value = foundClass
            loadClassroomContext(parsedId)
            _errorMessage.value = null
            true
        } else {
            _errorMessage.value = "Classroom link/ID not found. Please verify and try again."
            false
        }
    }

    fun exitActiveClassroom() {
        _activeClassroom.value = null
        strokes.clear()
        _activePdfName.value = null
        _pdfPages.value = emptyList()
        _currentPdfPageIndex.value = 0
        _leavingStudentName.value = null
        _isVoiceActive.value = false
        _activeSpeakerName.value = null
        _onlineStudents.value = listOf("Amit Sharma", "Priya Patel", "Anjali Rao", "Rahul Sen", "Vijay Kumar", "Siddharth J.", "Sneha Das")
    }

    private suspend fun prepopulateStudentProgressIfEmpty(classroomId: String) {
        val list = repository.getStudentProgressList(classroomId).first()
        if (list.isEmpty()) {
            val defaultProgress = listOf(
                StudentProgressEntity(
                    id = "$classroomId-Amit Sharma",
                    classroomId = classroomId,
                    studentName = "Amit Sharma",
                    attendanceCount = 18,
                    totalLiveClasses = 20,
                    completedLecturesCount = 4,
                    totalRecordedLectures = 5,
                    quiz1Score = 18,
                    quiz2Score = 17,
                    assignmentScore = 95,
                    notes = "Excellent performance. Highly interactive during live solving sessions."
                ),
                StudentProgressEntity(
                    id = "$classroomId-Priya Patel",
                    classroomId = classroomId,
                    studentName = "Priya Patel",
                    attendanceCount = 16,
                    totalLiveClasses = 20,
                    completedLecturesCount = 5,
                    totalRecordedLectures = 5,
                    quiz1Score = 19,
                    quiz2Score = 20,
                    assignmentScore = 98,
                    notes = "Exceptional analytical skills. Completed all recorded sessions ahead of schedule."
                ),
                StudentProgressEntity(
                    id = "$classroomId-Anjali Rao",
                    classroomId = classroomId,
                    studentName = "Anjali Rao",
                    attendanceCount = 19,
                    totalLiveClasses = 20,
                    completedLecturesCount = 3,
                    totalRecordedLectures = 5,
                    quiz1Score = 14,
                    quiz2Score = 15,
                    assignmentScore = 88,
                    notes = "Regular attendee. Needs minor improvement in complex equations but participates well."
                ),
                StudentProgressEntity(
                    id = "$classroomId-Rahul Sen",
                    classroomId = classroomId,
                    studentName = "Rahul Sen",
                    attendanceCount = 12,
                    totalLiveClasses = 20,
                    completedLecturesCount = 2,
                    totalRecordedLectures = 5,
                    quiz1Score = 11,
                    quiz2Score = 12,
                    assignmentScore = 75,
                    notes = "Missed some classes due to network issues. Advised to review recorded video library."
                ),
                StudentProgressEntity(
                    id = "$classroomId-Vijay Kumar",
                    classroomId = classroomId,
                    studentName = "Vijay Kumar",
                    attendanceCount = 15,
                    totalLiveClasses = 20,
                    completedLecturesCount = 4,
                    totalRecordedLectures = 5,
                    quiz1Score = 16,
                    quiz2Score = 15,
                    assignmentScore = 85,
                    notes = "Consistently submitting assignments on time. Good conceptual grasp."
                ),
                StudentProgressEntity(
                    id = "$classroomId-Siddharth J.",
                    classroomId = classroomId,
                    studentName = "Siddharth J.",
                    attendanceCount = 17,
                    totalLiveClasses = 20,
                    completedLecturesCount = 4,
                    totalRecordedLectures = 5,
                    quiz1Score = 17,
                    quiz2Score = 18,
                    assignmentScore = 90,
                    notes = "Strong math foundation. Proactive in answering whiteboard questions."
                ),
                StudentProgressEntity(
                    id = "$classroomId-Sneha Das",
                    classroomId = classroomId,
                    studentName = "Sneha Das",
                    attendanceCount = 14,
                    totalLiveClasses = 20,
                    completedLecturesCount = 3,
                    totalRecordedLectures = 5,
                    quiz1Score = 13,
                    quiz2Score = 14,
                    assignmentScore = 82,
                    notes = "Attentive and steady. Encouraged to take practice quizzes for faster computation."
                )
            )
            repository.saveStudentProgressList(defaultProgress)
        }
    }

    private suspend fun prepopulateQuizzesIfEmpty(classroomId: String) {
        val list = repository.getQuizzes(classroomId).first()
        if (list.isEmpty()) {
            val questions = when (classroomId) {
                "math-matrix" -> listOf(
                    QuizQuestion(
                        text = "What is the limit of sin(x)/x as x approaches 0?",
                        options = listOf("Infinity", "0", "1", "Does not exist"),
                        correctAnswerIndex = 2
                    ),
                    QuizQuestion(
                        text = "What is the derivative of x² with respect to x?",
                        options = listOf("x", "2x", "x³", "2"),
                        correctAnswerIndex = 1
                    ),
                    QuizQuestion(
                        text = "Which mathematician invented the Leibniz notation for calculus?",
                        options = listOf("Isaac Newton", "Gottfried Wilhelm Leibniz", "Leonhard Euler", "Carl Friedrich Gauss"),
                        correctAnswerIndex = 1
                    )
                )
                "quantum-physics" -> listOf(
                    QuizQuestion(
                        text = "Which physicist proposed that energy is radiated in discrete packets (quanta)?",
                        options = listOf("Albert Einstein", "Max Planck", "Niels Bohr", "Erwin Schrödinger"),
                        correctAnswerIndex = 1
                    ),
                    QuizQuestion(
                        text = "What is the physical interpretation of the square of the wavefunction, |Ψ|²?",
                        options = listOf("Energy density", "Momentum vector", "Probability density", "Particle velocity"),
                        correctAnswerIndex = 2
                    )
                )
                else -> listOf(
                    QuizQuestion(
                        text = "What is 2 + 2?",
                        options = listOf("3", "4", "5", "6"),
                        correctAnswerIndex = 1
                    )
                )
            }
            
            val quiz = QuizEntity(
                id = "preloaded-quiz-$classroomId",
                classroomId = classroomId,
                title = if (classroomId == "math-matrix") "Calculus Basics Quiz" else "Quantum Mechanics Basics Quiz",
                questionsJson = QuizJsonHelper.serializeQuestions(questions),
                isDeployed = true
            )
            repository.addQuiz(quiz)
        }
    }

    fun loadClassroomContext(classroomId: String) {
        viewModelScope.launch {
            // Prepopulate progress if empty
            prepopulateStudentProgressIfEmpty(classroomId)
            prepopulateQuizzesIfEmpty(classroomId)
            
            // Load student progress list
            repository.getStudentProgressList(classroomId).collectLatest { progress ->
                _studentProgressList.value = progress
            }
        }
        viewModelScope.launch {
            // Load messages
            repository.getMessages(classroomId).collectLatest { msgs ->
                _chatMessages.value = msgs
            }
        }
        viewModelScope.launch {
            // Load recorded classes
            repository.getRecordedClasses(classroomId).collectLatest { recorded ->
                _recordedClasses.value = recorded
            }
        }
        viewModelScope.launch {
            // Load quizzes
            repository.getQuizzes(classroomId).collectLatest { qList ->
                _quizzes.value = qList
            }
        }
        viewModelScope.launch {
            // Load submissions
            repository.getQuizSubmissions(classroomId).collectLatest { sList ->
                _quizSubmissions.value = sList
            }
        }
    }

    fun saveOrUpdateProgress(progress: StudentProgressEntity) {
        viewModelScope.launch {
            repository.saveStudentProgress(progress)
        }
    }

    fun createAndAddQuiz(title: String, questions: List<QuizQuestion>) {
        val classroom = _activeClassroom.value ?: return
        viewModelScope.launch {
            val id = UUID.randomUUID().toString().substring(0, 8)
            val questionsJson = QuizJsonHelper.serializeQuestions(questions)
            val newQuiz = QuizEntity(
                id = id,
                classroomId = classroom.id,
                title = title.trim().ifEmpty { "Classroom Quiz" },
                questionsJson = questionsJson,
                isDeployed = false
            )
            repository.addQuiz(newQuiz)
            
            // Post system notification message
            repository.sendMessage(
                MessageEntity(
                    classroomId = classroom.id,
                    senderName = "System Alert",
                    senderAvatar = "avatar_system",
                    message = "📝 New quiz created: '${newQuiz.title}' is ready for deployment.",
                    isTeacher = true,
                    isVoice = false
                )
            )
        }
    }

    fun toggleQuizDeployment(quizId: String, currentStatus: Boolean) {
        val classroom = _activeClassroom.value ?: return
        viewModelScope.launch {
            val newStatus = !currentStatus
            repository.updateQuizDeployment(quizId, newStatus)
            
            if (newStatus) {
                val quiz = _quizzes.value.find { it.id == quizId }
                repository.sendMessage(
                    MessageEntity(
                        classroomId = classroom.id,
                        senderName = "System Alert",
                        senderAvatar = "avatar_system",
                        message = "🔔 LIVE QUIZ ACTIVE: '${quiz?.title ?: "Assessment"}' has been deployed! Click take quiz button below.",
                        isTeacher = true,
                        isVoice = false
                    )
                )
            }
        }
    }

    fun submitQuizAnswers(quizId: String, answers: List<Int>) {
        val classroom = _activeClassroom.value ?: return
        val user = userState.value ?: return
        viewModelScope.launch {
            val quiz = _quizzes.value.find { it.id == quizId } ?: return@launch
            val questions = QuizJsonHelper.deserializeQuestions(quiz.questionsJson)
            
            // Automated scoring logic
            var score = 0
            questions.forEachIndexed { index, question ->
                val submittedAnswer = answers.getOrNull(index)
                if (submittedAnswer == question.correctAnswerIndex) {
                    score++
                }
            }
            
            val submissionId = "$quizId-${user.name}"
            val submission = QuizSubmissionEntity(
                id = submissionId,
                quizId = quizId,
                classroomId = classroom.id,
                studentName = user.name,
                score = score,
                totalQuestions = questions.size,
                submittedAnswersJson = QuizJsonHelper.serializeAnswers(answers)
            )
            repository.addQuizSubmission(submission)
            
            // Post chat notification
            repository.sendMessage(
                MessageEntity(
                    classroomId = classroom.id,
                    senderName = "System Alert",
                    senderAvatar = "avatar_system",
                    message = "✅ Student '${user.name}' submitted quiz '${quiz.title}' - Score: $score/${questions.size}",
                    isTeacher = false,
                    isVoice = false
                )
            )

            // Auto-sync score to progress dashboard
            val currentProgress = _studentProgressList.value.find { it.studentName == user.name }
            if (currentProgress != null) {
                val scaledScore20 = if (questions.isNotEmpty()) (score.toFloat() / questions.size * 20).toInt() else 0
                val updatedProgress = if (currentProgress.quiz1Score == 0 || currentProgress.quiz1Score == 18 || currentProgress.quiz1Score == 19 || currentProgress.quiz1Score == 14 || currentProgress.quiz1Score == 11 || currentProgress.quiz1Score == 16 || currentProgress.quiz1Score == 17 || currentProgress.quiz1Score == 13) {
                    currentProgress.copy(
                        quiz1Score = scaledScore20,
                        notes = currentProgress.notes + "\n[Quiz Score] Took '${quiz.title}' and scored $score/${questions.size} ($scaledScore20/20)."
                    )
                } else {
                    currentProgress.copy(
                        quiz2Score = scaledScore20,
                        notes = currentProgress.notes + "\n[Quiz Score] Took '${quiz.title}' and scored $score/${questions.size} ($scaledScore20/20)."
                    )
                }
                repository.saveStudentProgress(updatedProgress)
            } else {
                val scaledScore20 = if (questions.isNotEmpty()) (score.toFloat() / questions.size * 20).toInt() else 0
                val newProgress = StudentProgressEntity(
                    id = "${classroom.id}-${user.name}",
                    classroomId = classroom.id,
                    studentName = user.name,
                    attendanceCount = 1,
                    completedLecturesCount = 0,
                    quiz1Score = scaledScore20,
                    quiz2Score = 0,
                    assignmentScore = 80,
                    notes = "[Quiz Score] Completed '${quiz.title}': $score/${questions.size}."
                )
                repository.saveStudentProgress(newProgress)
            }
        }
    }

    // Voice & Chat Operations
    fun postMessage(text: String) {
        val classroom = _activeClassroom.value ?: return
        val user = userState.value ?: return
        
        viewModelScope.launch {
            val message = MessageEntity(
                classroomId = classroom.id,
                senderName = user.name,
                senderAvatar = user.profilePic,
                message = text.trim(),
                isTeacher = user.role == "TEACHER",
                isVoice = false
            )
            repository.sendMessage(message)

            // Auto-reply simulation from other students to make it interactive!
            delay(1500)
            simulateStudentReply(classroom.id)
        }
    }

    fun sendVoiceMessage() {
        val classroom = _activeClassroom.value ?: return
        val user = userState.value ?: return

        viewModelScope.launch {
            val voiceMsg = MessageEntity(
                classroomId = classroom.id,
                senderName = user.name,
                senderAvatar = user.profilePic,
                message = "🎤 Voice Note (0:04)",
                isTeacher = user.role == "TEACHER",
                isVoice = true
            )
            repository.sendMessage(voiceMsg)

            // Voice transmitting ripple animation
            _isVoiceActive.value = true
            _activeSpeakerName.value = user.name
            delay(3000)
            _isVoiceActive.value = false
            _activeSpeakerName.value = null
        }
    }

    private suspend fun simulateStudentReply(classroomId: String) {
        val list = _onlineStudents.value
        if (list.isEmpty()) return
        val randomStudent = list.random()
        val messages = listOf(
            "Understood, sir!",
            "Can you explain that equation again?",
            "Yes, the blackboard is clear.",
            "Wow! Nice explanation.",
            "Will we get homework on this?",
            "Should we write this down?",
            "I applied the formula. It works!"
        )
        val reply = MessageEntity(
            classroomId = classroomId,
            senderName = randomStudent,
            senderAvatar = "avatar_${(1..6).random()}",
            message = messages.random(),
            isTeacher = false,
            isVoice = false
        )
        repository.sendMessage(reply)
    }

    // Toggle board styles
    fun toggleBoardStyle() {
        _boardStyle.value = if (_boardStyle.value == "BLACKBOARD") {
            _currentColor.value = Color.Black
            "WHITEBOARD"
        } else {
            _currentColor.value = Color.White
            "BLACKBOARD"
        }
        strokes.clear()
    }

    // Drawing Canvas controls
    fun selectColor(color: Color) {
        _currentColor.value = color
        _isEraserMode.value = false
    }

    fun selectStrokeWidth(width: Float) {
        _currentStrokeWidth.value = width
    }

    fun enableEraser(enabled: Boolean) {
        _isEraserMode.value = enabled
    }

    fun clearDrawing() {
        strokes.clear()
    }

    fun undoDrawing() {
        if (strokes.isNotEmpty()) {
            strokes.removeAt(strokes.size - 1)
        }
    }

    // Student Left - BIG RED POPUP SIMULATION
    fun simulateStudentLeaving() {
        val currentList = _onlineStudents.value.toMutableList()
        if (currentList.isEmpty()) {
            // Reset students
            _onlineStudents.value = listOf("Amit Sharma", "Priya Patel", "Anjali Rao", "Rahul Sen", "Vijay Kumar", "Siddharth J.", "Sneha Das")
            return
        }

        viewModelScope.launch {
            val leavingStudent = currentList.removeAt((0 until currentList.size).random())
            _onlineStudents.value = currentList
            
            // Set name to trigger big red letters popup
            _leavingStudentName.value = leavingStudent
            
            // Send system message in chat
            val classroom = _activeClassroom.value
            if (classroom != null) {
                repository.sendMessage(
                    MessageEntity(
                        classroomId = classroom.id,
                        senderName = "System Alert",
                        senderAvatar = "avatar_system",
                        message = "⚠️ Student '$leavingStudent' has left the live class.",
                        isTeacher = false,
                        isVoice = false
                    )
                )
            }

            // Fades/disappears after 4 seconds
            delay(4000)
            if (_leavingStudentName.value == leavingStudent) {
                _leavingStudentName.value = null
            }
        }
    }

    // PDF Board overlays
    fun loadPdfPreset(pdfName: String) {
        _activePdfName.value = pdfName
        _currentPdfPageIndex.value = 0
        strokes.clear() // Clear drawing board when loading a new PDF

        // Preload rich simulated slides content
        _pdfPages.value = when (pdfName) {
            "Physics - Mechanics.pdf" -> listOf(
                PdfPage(
                    title = "Newton's First Law of Motion",
                    description = "An object remains in a state of rest or of uniform motion in a straight line unless compelled to change that state by applied forces.",
                    equations = listOf("Σ F = 0 ⇒ dv/dt = 0"),
                    pointsOfInterest = listOf("Also known as the Law of Inertia.", "Discovered originally by Galileo.")
                ),
                PdfPage(
                    title = "Newton's Second Law: Acceleration",
                    description = "The acceleration of an object as produced by a net force is directly proportional to the magnitude of the net force, in the same direction, and inversely proportional to the mass of the object.",
                    equations = listOf("F = m * a", "a = F / m"),
                    pointsOfInterest = listOf("F is net Force in Newtons.", "m is inertial mass in kg.", "a is acceleration vector.")
                ),
                PdfPage(
                    title = "Newton's Third Law: Pairs",
                    description = "When one body exerts a force on a second body, the second body simultaneously exerts a force equal in magnitude and opposite in direction on the first body.",
                    equations = listOf("F_ab = - F_ba"),
                    pointsOfInterest = listOf("Forces always occur in action-reaction pairs.", "Forces are interactions between bodies.")
                )
            )
            "Mathematics - Algebra & Matrices.pdf" -> listOf(
                PdfPage(
                    title = "Introduction to Matrices",
                    description = "A matrix is a rectangular array or table of numbers, symbols, or expressions, arranged in rows and columns.",
                    equations = listOf("A = [a_ij]", "A * X = B"),
                    pointsOfInterest = listOf("Used to represent linear transformations.", "Essential for computer graphics and physics.")
                ),
                PdfPage(
                    title = "Matrix Multiplication Rules",
                    description = "The product of matrices A and B is defined if and only if the number of columns in A equals the number of rows in B.",
                    equations = listOf("C_ij = Σ (a_ik * b_kj)"),
                    pointsOfInterest = listOf("Matrix multiplication is non-commutative: AB ≠ BA.", "Identity Matrix: A * I = A.")
                )
            )
            else -> listOf(
                PdfPage(
                    title = "General Lecture Slides",
                    description = "Welcome to mm classroom. This slide serves as an interactive PDF template. Add drawings or chalk annotations anywhere on the board.",
                    equations = listOf("E = m * c²"),
                    pointsOfInterest = listOf("Host interactive Q&As.", "Use voice and chat functions to explain.")
                )
            )
        }
    }

    fun nextPage() {
        if (_currentPdfPageIndex.value < _pdfPages.value.size - 1) {
            _currentPdfPageIndex.value += 1
            strokes.clear() // Clear drawings for the new page
        }
    }

    fun prevPage() {
        if (_currentPdfPageIndex.value > 0) {
            _currentPdfPageIndex.value -= 1
            strokes.clear() // Clear drawings for the previous page
        }
    }

    fun removePdfFromBoard() {
        _activePdfName.value = null
        _pdfPages.value = emptyList()
        _currentPdfPageIndex.value = 0
        strokes.clear()
    }

    fun addCustomRecordedClass(title: String, duration: String, desc: String) {
        val classroom = _activeClassroom.value ?: return
        viewModelScope.launch {
            val rec = RecordedClassEntity(
                classroomId = classroom.id,
                title = title.trim().ifEmpty { "Untitled Session" },
                subject = classroom.subject,
                duration = duration.trim().ifEmpty { "30 mins" },
                videoUrl = "custom_rec_${UUID.randomUUID()}",
                date = "Today",
                description = desc.trim().ifEmpty { "No description provided." }
            )
            repository.addRecordedClass(rec)
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
