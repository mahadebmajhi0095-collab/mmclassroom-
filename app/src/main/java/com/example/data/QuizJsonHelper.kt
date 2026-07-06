package com.example.data

import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

data class QuizQuestion(
    val text: String,
    val options: List<String>,
    val correctAnswerIndex: Int
)

object QuizJsonHelper {
    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val questionsType = Types.newParameterizedType(List::class.java, QuizQuestion::class.java)
    private val questionsAdapter = moshi.adapter<List<QuizQuestion>>(questionsType)

    private val answersType = Types.newParameterizedType(List::class.java, java.lang.Integer::class.java)
    private val answersAdapter = moshi.adapter<List<Int>>(answersType)

    fun serializeQuestions(questions: List<QuizQuestion>): String {
        return try {
            questionsAdapter.toJson(questions) ?: "[]"
        } catch (e: Exception) {
            "[]"
        }
    }

    fun deserializeQuestions(json: String): List<QuizQuestion> {
        return try {
            questionsAdapter.fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun serializeAnswers(answers: List<Int>): String {
        return try {
            answersAdapter.toJson(answers) ?: "[]"
        } catch (e: Exception) {
            "[]"
        }
    }

    fun deserializeAnswers(json: String): List<Int> {
        return try {
            answersAdapter.fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
