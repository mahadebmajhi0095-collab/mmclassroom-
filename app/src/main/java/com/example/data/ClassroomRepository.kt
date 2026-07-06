package com.example.data

import kotlinx.coroutines.flow.Flow

class ClassroomRepository(private val classroomDao: ClassroomDao) {

    val userFlow: Flow<UserEntity?> = classroomDao.getUserFlow()

    suspend fun getUserSync(): UserEntity? = classroomDao.getUserSync()

    suspend fun saveUser(user: UserEntity) = classroomDao.insertUser(user)

    val classroomsFlow: Flow<List<ClassroomEntity>> = classroomDao.getAllClassrooms()

    fun getClassroomById(classroomId: String): Flow<ClassroomEntity?> {
        return classroomDao.getClassroomById(classroomId)
    }

    suspend fun createClassroom(classroom: ClassroomEntity) {
        classroomDao.insertClassroom(classroom)
    }

    suspend fun deleteClassroom(classroom: ClassroomEntity) {
        classroomDao.deleteClassroom(classroom)
    }

    fun getRecordedClasses(classroomId: String): Flow<List<RecordedClassEntity>> {
        return classroomDao.getRecordedClasses(classroomId)
    }

    suspend fun addRecordedClass(recordedClass: RecordedClassEntity) {
        classroomDao.insertRecordedClass(recordedClass)
    }

    fun getMessages(classroomId: String): Flow<List<MessageEntity>> {
        return classroomDao.getMessagesForClassroom(classroomId)
    }

    suspend fun sendMessage(message: MessageEntity) {
        classroomDao.insertMessage(message)
    }

    suspend fun clearMessages(classroomId: String) {
        classroomDao.clearMessagesForClassroom(classroomId)
    }

    fun getStudentProgressList(classroomId: String): Flow<List<StudentProgressEntity>> {
        return classroomDao.getStudentProgressForClassroom(classroomId)
    }

    suspend fun saveStudentProgress(progress: StudentProgressEntity) {
        classroomDao.insertStudentProgress(progress)
    }

    suspend fun saveStudentProgressList(progressList: List<StudentProgressEntity>) {
        classroomDao.insertStudentProgressList(progressList)
    }

    // Quiz Operations
    fun getQuizzes(classroomId: String): Flow<List<QuizEntity>> {
        return classroomDao.getQuizzesForClassroom(classroomId)
    }

    suspend fun addQuiz(quiz: QuizEntity) {
        classroomDao.insertQuiz(quiz)
    }

    suspend fun updateQuizDeployment(quizId: String, isDeployed: Boolean) {
        classroomDao.updateQuizDeployment(quizId, isDeployed)
    }

    // Quiz Submission Operations
    fun getQuizSubmissions(classroomId: String): Flow<List<QuizSubmissionEntity>> {
        return classroomDao.getQuizSubmissionsForClassroom(classroomId)
    }

    suspend fun addQuizSubmission(submission: QuizSubmissionEntity) {
        classroomDao.insertQuizSubmission(submission)
    }
}
