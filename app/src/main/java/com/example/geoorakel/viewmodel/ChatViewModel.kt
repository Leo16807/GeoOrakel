package com.example.geoorakel.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.launch
import com.example.geoorakel.BuildConfig


// Datenklasse für Nachrichten
data class ChatMessage(
    val text: String,
    val isFromUser: Boolean,
    val isError: Boolean = false
)

class ChatViewModel : ViewModel() {

    private val generativeModel = GenerativeModel(
        modelName = "gemini-2.5-flash",
        apiKey = BuildConfig.GEMINI_API_KEY
    )

    private val chat = generativeModel.startChat()

    private val _messages = mutableStateListOf<ChatMessage>()
    val messages: List<ChatMessage> = _messages

    fun sendMessage(userText: String) {
        if (userText.isBlank()) return
        _messages.add(ChatMessage(text = userText, isFromUser = true))

        viewModelScope.launch {
            try {
                val response = chat.sendMessage(userText)
                _messages.add(ChatMessage(text = response.text ?: "Keine Antwort.", isFromUser = false))
            } catch (e: Exception) {
                // potenzielle Fehler abfangen
                val errorMessage = when {
                    e.message?.contains("503") == true -> "Das Orakel ist gerade überlastet. Bitte warte kurz und versuche es dann nochmal."
                    e.message?.contains("429") == true -> "Zu viele Anfragen. Probiere es später nochmal."
                    e.message?.contains("You exceeded your current quota") == true -> "Du hast das Nutzungslimit überschritten. Versuche es später nochmal."
                    else -> "Ein Fehler ist aufgetreten: ${e.localizedMessage}"
                }
                _messages.add(ChatMessage(text = errorMessage, isFromUser = false, isError = true))
            }
        }
    }
}