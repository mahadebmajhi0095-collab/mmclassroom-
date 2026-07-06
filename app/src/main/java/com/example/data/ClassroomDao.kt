package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ClassroomDao {

    // User Operations
    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    fun getUserFlow(userId: Int = 1): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    suspend fun getUserSync(userId: Int = 1): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    // Classroom Operations
    @Query("SELECT * FROM classrooms ORDER BY name ASC")
    fun getAllClassrooms(): Flow<List<ClassroomEntity>>

    @Query("SELECT * FROM classrooms WHERE id = :classroomId LIMIT 1")
    fun getClassroomById(classroomId: String): Flow<ClassroomEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClassroom(classroom: ClassroomEntity)

    @Delete
    suspend fun deleteClassroom(classroom: ClassroomEntity)

    // Recorded Classes Operations
    @Query("SELECT * FROM recorded_classes WHERE classroomId = :classroomId ORDER BY id DESC")
    fun getRecordedClasses(classroomId: String): Flow<List<RecordedClassEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecordedClass(recordedClass: RecordedClassEntity)

    // Message/Chat Operations
    @Query("SELECT * FROM messages WHERE classroomId = :classroomId ORDER BY timestamp ASC")
    fun getMessagesForClassroom(classroomId: String): Flow<List<MessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    @Query("DELETE FROM messages WHERE classroomId = :classroomId")
    suspend fun clearMessagesForClassroom(classroomId: String)

    // Student Progress Operations
    @Query("SELECT * FROM student_progress WHERE classroomId = :classroomId ORDER BY studentName ASC")
    fun getStudentProgressForClassroom(classroomId: String): Flow<List<StudentProgressEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudentProgress(progress: StudentProgressEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudentProgressList(progressList: List<StudentProgressEntity>)

    // Quiz Operations
    @Query("SELECT * FROM quizzes WHERE classroomId = :classroomId ORDER BY createdAt DESC")
    fun getQuizzesForClassroom(classroomId: String): Flow<List<QuizEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuiz(quiz: QuizEntity)

    @Query("UPDATE quizzes SET isDeployed = :isDeployed WHERE id = :quizId")
    suspend fun updateQuizDeployment(quizId: String, isDeployed: Boolean)

    // Quiz Submission Operations
    @Query("SELECT * FROM quiz_submissions WHERE classroomId = :classroomId ORDER BY timestamp DESC")
    fun getQuizSubmissionsForClassroom(classroomId: String): Flow<List<QuizSubmissionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuizSubmission(submission: QuizSubmissionEntity)
}
