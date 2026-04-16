package com.vestor.animalfinder

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val app = context.applicationContext as AnimalFinderApplication

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var passwordVisible by remember { mutableStateOf(false) }

    fun isValidEmail(email: String): Boolean {
        val emailRegex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
        return emailRegex.matches(email)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Вход", fontSize = 20.sp) }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "🐾 AnimalFinder",
                fontSize = 32.sp,
                style = MaterialTheme.typography.headlineMedium
            )

            Text(
                text = "Добро пожаловать!",
                style = MaterialTheme.typography.bodyLarge
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                isError = email.isNotBlank() && !isValidEmail(email),
                supportingText = {
                    if (email.isNotBlank() && !isValidEmail(email)) {
                        Text("Введите корректный email")
                    }
                }
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Пароль") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = null
                        )
                    }
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    if (!isValidEmail(email)) {
                        errorMessage = "Введите корректный email"
                        return@Button
                    }
                    if (password.isBlank()) {
                        errorMessage = "Введите пароль"
                        return@Button
                    }

                    scope.launch {
                        isLoading = true
                        errorMessage = null

                        try {
                            // Выполняем вход
                            app.supabase.auth.signInWith(Email) {
                                this.email = email
                                this.password = password
                            }

                            // Получаем текущую сессию и user ID
                            val session = app.supabase.auth.currentSessionOrNull()
                            val userId = session?.user?.id
                            val userEmail = session?.user?.email ?: email

                            if (userId != null) {
                                app.authManager.saveUserSession(
                                    userId = userId,
                                    email = userEmail
                                )
                                onLoginSuccess()
                            } else {
                                errorMessage = "Ошибка: не удалось получить ID пользователя"
                            }
                        } catch (e: Exception) {
                            errorMessage = when {
                                e.message?.contains("Invalid login credentials") == true ->
                                    "Неверный email или пароль"
                                e.message?.contains("Email not confirmed") == true ->
                                    "Подтвердите email. Проверьте почту"
                                else -> "Ошибка: ${e.message}"
                            }
                        } finally {
                            isLoading = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(if (isLoading) "Вход..." else "Войти")
            }

            errorMessage?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Row(
                modifier = Modifier.padding(top = 16.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Text("Нет аккаунта? ")
                TextButton(onClick = onNavigateToRegister) {
                    Text("Зарегистрироваться")
                }
            }
        }
    }
}