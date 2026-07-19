package com.example.geoorakel.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.geoorakel.viewmodel.ChatMessage
import com.example.geoorakel.viewmodel.ChatViewModel

@Composable
fun ChatScreen(
    modifier: Modifier = Modifier,
    chatViewModel: ChatViewModel = viewModel()
) {
    var inputText by remember { mutableStateOf("") }
    val messages = chatViewModel.messages

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            reverseLayout = true
        ) {
            items(messages.reversed()) { message ->
                MessageBubble(message = message)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Frage das GeoOrakel...") },
                singleLine = true
            )

            IconButton(
                onClick = {
                    chatViewModel.sendMessage(inputText)
                    inputText = ""
                },
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Senden")
            }
        }
    }
}

@Composable
fun MessageBubble(message: ChatMessage) {
    val backgroundColor = if (message.isFromUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
    val alignment = if (message.isFromUser) Alignment.End else Alignment.Start

    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = alignment) {
        Box(
            modifier = Modifier
                .background(
                    color = if (message.isError) MaterialTheme.colorScheme.errorContainer else backgroundColor,
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(12.dp)
        ) {
            Text(
                text = message.text,
                color = if (message.isError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}