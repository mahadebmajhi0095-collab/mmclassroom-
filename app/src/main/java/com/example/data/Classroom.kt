package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: Int = 1,
    val name: String,
    val emailOrPhone: String,
    val profilePic: String, // "avatar_1", "avatar_2", etc.
    val role: String, // "TEACHER" or "STUDENT"
    val promoApplied: Boolean = false // If Ranacr7 was applied
)

@Entity(tableName = "classrooms")
data class ClassroomEntity(
    @PrimaryKey val id: String,
    val name: String,
    val subject: String,
    val teacherName: String,
    val inviteLink: String
)

@Entity(tableName = "recorded_classes")
data class RecordedClassEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val classroomId: String,
    val title: String,
    val subject: String,
    val duration: String,
    val videoUrl: String,
    val date: String,
    val description: String
)

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val classroomId: String,
    val senderName: String,
    val senderAvatar: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isTeacher: Boolean = false,
    val isVoice: Boolean = false
)

@Entity(tableName = "student_progress")
data class StudentProgressEntity(
    @PrimaryKey val id: String, // "$classroomId-$studentName"
    val classroomId: String,
    val studentName: String,
    val attendanceCount: Int,
    val totalLiveClasses: Int = 20,
    val completedLecturesCount: Int,
    val totalRecordedLectures: Int = 5,
    val quiz1Score: Int,
    val quiz2Score: Int,
    val assignmentScore: Int,
    val notes: String
)

@Entity(tableName = "quizzes")
data class QuizEntity(
    @PrimaryKey val id: String,
    val classroomId: String,
    val title: String,
    val questionsJson: String, // JSON list of QuizQuestion
    val isDeployed: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "quiz_submissions")
data class QuizSubmissionEntity(
    @PrimaryKey val id: String, // "$quizId-$studentName"
    val quizId: String,
    val classroomId: String,
    val studentName: String,
    val score: Int,
    val totalQuestions: Int,
    val submittedAnswersJson: String, // JSON list of selected options
    val timestamp: Long = System.currentTimeMillis()
)


