package com.example.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import com.example.R
import com.example.viewmodel.MarketViewModel

@Composable
fun AuthScreen(
    viewModel: MarketViewModel,
    onGuestContinue: () -> Unit
) {
    var isSignup by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var showForgotPassword by remember { mutableStateOf(false) }
    var resetEmail by remember { mutableStateOf("") }

    val showWalkthrough by viewModel.showWalkthrough.collectAsState()

    if (showWalkthrough) {
        WalkthroughDialog(viewModel = viewModel)
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_statistical_logo),
                contentDescription = "Statistical Portfolio Logo",
                tint = androidx.compose.ui.graphics.Color.Unspecified,
                modifier = Modifier.size(56.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Statistical Portfolio",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Professional Nepal Stock Exchange Terminal",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            Card(
                modifier = Modifier.fillMaxWidth().widthIn(max = 400.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = if (isSignup) "Create Account" else "Terminal Login",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email Address") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                    )

                    if (errorMessage != null) {
                        Text(
                            text = errorMessage!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    if (successMessage != null) {
                        Text(
                            text = successMessage!!,
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Button(
                        onClick = {
                            if (email.isBlank() || password.isBlank()) {
                                errorMessage = "Please enter both email and password"
                                return@Button
                            }
                            if (isSignup) {
                                viewModel.signup(email, password) { success, err ->
                                    if (success) {
                                        successMessage = "Account created successfully!"
                                    } else {
                                        errorMessage = err ?: "Signup failed"
                                    }
                                }
                            } else {
                                viewModel.login(email, password) { success, err ->
                                    if (!success) {
                                        errorMessage = err ?: "Login failed"
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(if (isSignup) "Sign Up" else "Log In")
                    }

                    val context = androidx.compose.ui.platform.LocalContext.current
                    OutlinedButton(
                        onClick = {
                            val url = "https://bfuatuhoosiwaugcxhjt.supabase.co/auth/v1/authorize?provider=google&redirect_to=https://bfuatuhoosiwaugcxhjt.supabase.co/auth/v1/callback"
                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
                            context.startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Sign in with Google", fontWeight = FontWeight.Medium)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(onClick = { isSignup = !isSignup }) {
                            Text(if (isSignup) "Existing user? Log in" else "New user? Sign up")
                        }
                        if (!isSignup) {
                            TextButton(onClick = { showForgotPassword = true }) {
                                Text("Forgot Password?")
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = onGuestContinue,
                modifier = Modifier.fillMaxWidth().widthIn(max = 400.dp).height(48.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Continue as Guest (Browse Market & Search)")
            }
        }
    }

    if (showForgotPassword) {
        AlertDialog(
            onDismissRequest = { showForgotPassword = false },
            title = { Text("Reset Password") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Enter your email address to receive password reset instructions.")
                    OutlinedTextField(
                        value = resetEmail,
                        onValueChange = { resetEmail = it },
                        label = { Text("Email") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (resetEmail.isNotBlank()) {
                        viewModel.resetPassword(resetEmail) { success, err ->
                            showForgotPassword = false
                            if (success) {
                                successMessage = "Password reset email sent!"
                            } else {
                                errorMessage = err ?: "Reset failed"
                            }
                        }
                    }
                }) {
                    Text("Send")
                }
            },
            dismissButton = {
                TextButton(onClick = { showForgotPassword = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun WalkthroughDialog(viewModel: MarketViewModel) {
    var step by remember { mutableStateOf(1) }

    AlertDialog(
        onDismissRequest = { viewModel.dismissWalkthrough() },
        title = { Text("Welcome to Statistical Portfolio (Step $step of 3)") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                when (step) {
                    1 -> Text("1. Watchlist: Track your favorite NEPSE symbols in real time with synced cloud storage and custom alerts.")
                    2 -> Text("2. Portfolio Tracking: Log your buy/sell trades or import from CSV (TMS, Meroshare, WACC) with automated FIFO P/L calculation.")
                    3 -> Text("3. Price Alerts: Set custom price limits and receive instant notifications when targets are crossed.")
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (step < 3) step++ else viewModel.dismissWalkthrough()
            }) {
                Text(if (step < 3) "Next" else "Get Started")
            }
        }
    )
}
