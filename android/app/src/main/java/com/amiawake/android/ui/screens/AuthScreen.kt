package com.amiawake.android.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.amiawake.android.ui.components.PrimaryActionButton

@Composable
fun AuthScreen(loading: Boolean, serverError: String?, onSubmit: (String, String, Boolean) -> Unit) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var register by remember { mutableStateOf(false) }
    var revealPassword by remember { mutableStateOf(false) }
    var attempted by remember { mutableStateOf(false) }
    val focus = LocalFocusManager.current
    val usernameError = attempted && !validUsername(username)
    val passwordError = attempted && password.length !in 8..256
    val submit = {
        attempted = true
        if (!usernameError && !passwordError && validUsername(username) && password.length in 8..256) {
            focus.clearFocus()
            onSubmit(username, password, register)
        }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding().padding(horizontal = 24.dp, vertical = 40.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Surface(shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.primaryContainer) {
            Icon(Icons.Outlined.Bedtime, null, Modifier.padding(16.dp).size(32.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
        }
        Spacer(Modifier.height(24.dp))
        Text(if (register) "Будем знакомы" else "С возвращением", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(8.dp))
        Text(
            if (register) "Создайте аккаунт, чтобы делиться удобным временем для общения."
            else "Узнайте, кому сейчас удобно написать или позвонить.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(Modifier.height(32.dp))
        OutlinedTextField(
            value = username,
            onValueChange = { if (it.length <= 32) username = it },
            label = { Text("Имя пользователя") },
            supportingText = { if (usernameError) Text("От 3 символов: латинские буквы, цифры или _") else Text("Ваше уникальное имя в приложении") },
            isError = usernameError,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii, imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focus.moveFocus(FocusDirection.Down) }),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { if (it.length <= 256) password = it },
            label = { Text("Пароль") },
            supportingText = { if (passwordError) Text("Пароль должен содержать не менее 8 символов") },
            isError = passwordError,
            singleLine = true,
            visualTransformation = if (revealPassword) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { revealPassword = !revealPassword }) {
                    Icon(if (revealPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility, if (revealPassword) "Скрыть пароль" else "Показать пароль")
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { submit() }),
            modifier = Modifier.fillMaxWidth(),
        )
        if (serverError != null) {
            Spacer(Modifier.height(10.dp))
            Text(serverError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(Modifier.height(20.dp))
        PrimaryActionButton(if (register) "Создать аккаунт" else "Войти", submit, loading, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Text(if (register) "Уже есть аккаунт?" else "Впервые здесь?", color = MaterialTheme.colorScheme.onSurfaceVariant)
            TextButton(onClick = { register = !register; attempted = false }) { Text(if (register) "Войти" else "Создать аккаунт") }
        }
    }
}

private fun validUsername(value: String): Boolean = value.length in 3..32 && value.matches(Regex("^[A-Za-z0-9_]+$"))
