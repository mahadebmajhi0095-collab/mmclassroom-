package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.ClassroomViewModel

// Preset Avatars mapping
val AVATAR_LIST = listOf(
    "avatar_1" to "🎓",
    "avatar_2" to "🔬",
    "avatar_3" to "🎨",
    "avatar_4" to "🍎",
    "avatar_5" to "🚀",
    "avatar_6" to "🧬"
)

fun getAvatarEmoji(key: String): String {
    return AVATAR_LIST.find { it.first == key }?.second ?: "👤"
}

fun getAvatarGradient(key: String): Brush {
    return when (key) {
        "avatar_1" -> Brush.sweepGradient(listOf(Color(0xFF3F51B5), Color(0xFF2196F3)))
        "avatar_2" -> Brush.sweepGradient(listOf(Color(0xFFE91E63), Color(0xFFFF5722)))
        "avatar_3" -> Brush.sweepGradient(listOf(Color(0xFF9C27B0), Color(0xFFE040FB)))
        "avatar_4" -> Brush.sweepGradient(listOf(Color(0xFF4CAF50), Color(0xFF8BC34A)))
        "avatar_5" -> Brush.sweepGradient(listOf(Color(0xFFFF9800), Color(0xFFFFC107)))
        "avatar_6" -> Brush.sweepGradient(listOf(Color(0xFF00BCD4), Color(0xFF009688)))
        else -> Brush.sweepGradient(listOf(Color.Gray, Color.LightGray))
    }
}

@Composable
fun LoginScreen(
    viewModel: ClassroomViewModel,
    onLoginSuccess: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var emailOrPhone by remember { mutableStateOf("") }
    var selectedAvatar by remember { mutableStateOf("avatar_1") }
    var selectedRole by remember { mutableStateOf("STUDENT") } // STUDENT or TEACHER
    var promoCode by remember { mutableStateOf("") }

    var isPromoValid by remember { mutableStateOf<Boolean?>(null) }

    // Geometric Balance: crisp light background
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .widthIn(max = 480.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // App branding
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // School branding container
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.School,
                        contentDescription = "App Icon",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }
                
                Text(
                    text = "mm classroom",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground,
                        letterSpacing = (-0.5).sp
                    ),
                    modifier = Modifier.testTag("app_logo_title")
                )
                Text(
                    text = "Online classroom with interactive boards & voice chat",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        textAlign = TextAlign.Center
                    )
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Card for login fields
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Create Profile",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )

                    // Avatar Selection
                    Text(
                        text = "Choose Profile Avatar",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            fontWeight = FontWeight.Medium
                        )
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        AVATAR_LIST.forEach { (key, emoji) ->
                            val isSelected = selectedAvatar == key
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                                    )
                                    .padding(if (isSelected) 3.dp else 0.dp)
                                    .clip(CircleShape)
                                    .background(getAvatarGradient(key))
                                    .clickable { selectedAvatar = key }
                                    .testTag("avatar_$key"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = emoji, fontSize = 22.sp)
                            }
                        }
                    }

                    // Role Picker (Student / Teacher)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Button(
                            onClick = { selectedRole = "STUDENT" },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("role_student_btn"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedRole == "STUDENT") MaterialTheme.colorScheme.primary else Color.Transparent,
                                contentColor = if (selectedRole == "STUDENT") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(vertical = 10.dp)
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Student", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { selectedRole = "TEACHER" },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("role_teacher_btn"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedRole == "TEACHER") MaterialTheme.colorScheme.primary else Color.Transparent,
                                contentColor = if (selectedRole == "TEACHER") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(vertical = 10.dp)
                        ) {
                            Icon(Icons.Default.CoPresent, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Teacher", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Name input
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Your Name") },
                        placeholder = { Text("e.g. John Doe") },
                        leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("name_input"),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            focusedLabelColor = MaterialTheme.colorScheme.primary
                        )
                    )

                    // Mobile / Email ID
                    OutlinedTextField(
                        value = emailOrPhone,
                        onValueChange = { emailOrPhone = it },
                        label = { Text("Mobile or Email ID") },
                        placeholder = { Text("e.g. +91 9876543210 or name@domain.com") },
                        leadingIcon = { Icon(Icons.Default.PhoneAndroid, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("contact_input"),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            focusedLabelColor = MaterialTheme.colorScheme.primary
                        )
                    )

                    // Promo Code Input
                    OutlinedTextField(
                        value = promoCode,
                        onValueChange = {
                            promoCode = it
                            isPromoValid = if (it.trim().equals("Ranacr7", ignoreCase = true)) true else null
                        },
                        label = { Text("Promo Code (Optional)") },
                        placeholder = { Text("Enter promo code here") },
                        leadingIcon = { Icon(Icons.Default.LocalActivity, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer) },
                        trailingIcon = {
                            if (promoCode.isNotEmpty()) {
                                if (promoCode.trim().equals("Ranacr7", ignoreCase = true)) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = "Valid", tint = Color(0xFF2E7D32))
                                } else {
                                    Icon(Icons.Default.Warning, contentDescription = "Invalid", tint = Color(0xFFC62828))
                                }
                            }
                        },
                        supportingText = {
                            Text(
                                text = "Use promo code 'Ranacr7' to unlock live chat & voice options!",
                                color = if (promoCode.trim().equals("Ranacr7", ignoreCase = true)) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("promo_input"),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            focusedLabelColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }

            // Submit Button
            Button(
                onClick = {
                    if (name.trim().isNotEmpty() && emailOrPhone.trim().isNotEmpty()) {
                        viewModel.registerOrLogin(
                            name = name,
                            emailOrPhone = emailOrPhone,
                            avatar = selectedAvatar,
                            role = selectedRole,
                            promoCode = promoCode
                        )
                        onLoginSuccess()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("login_submit_btn"),
                enabled = name.trim().isNotEmpty() && emailOrPhone.trim().isNotEmpty(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor = Color.LightGray,
                    disabledContentColor = Color.DarkGray
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "Get Started",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// Extension so that Kotlin compiles when writing open bracket
private fun Color.Companion.getOpenBracketPlaceholder() = Color.Yellow

