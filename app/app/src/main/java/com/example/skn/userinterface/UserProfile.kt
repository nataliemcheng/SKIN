package com.example.skn.userinterface

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.skn.api.UserProfile
import com.example.skn.viewmodel.AuthViewModel
import com.example.skn.viewmodel.UserProfileViewModel

@Composable
fun UserProfileScreen(
    authViewModel: AuthViewModel,
    profileViewModel: UserProfileViewModel,
    onNavigateBack: () -> Unit,
    onLogout: () -> Unit
) {
    // Collect state from ViewModels
    val currentUser by authViewModel.currentUser.collectAsStateWithLifecycle()
    val profile by profileViewModel.userProfile.collectAsStateWithLifecycle()
    val loading by profileViewModel.loading.collectAsStateWithLifecycle()
    val error by profileViewModel.error.collectAsStateWithLifecycle()

    // Local UI state
    var isEditingProfile by remember { mutableStateOf(false) }
    var isChangingPassword by remember { mutableStateOf(false) }
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmNewPassword by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var passwordSuccess by remember { mutableStateOf<String?>(null) }
    var isPasswordLoading by remember { mutableStateOf(false) }

    // Editable profile fields
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var skinType by remember { mutableStateOf("") }
    var skinConcernsText by remember { mutableStateOf("") }

    // Get profile on launch
    LaunchedEffect(currentUser) {
        currentUser?.uid?.let { userId ->
            profileViewModel.fetchUser(userId)
        }
    }

    // Update editable state whenever the profile changes
    LaunchedEffect(profile) {
        profile?.let {
            firstName = it.firstName
            lastName = it.lastName
            skinType = it.skinType ?: ""
            skinConcernsText = it.skinConcerns?.joinToString(", ") ?: ""
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp)
    ) {
        // Top bar with nav
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("Your Profile", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(start = 8.dp))
        }

        Spacer(Modifier.height(16.dp))

        // User profile card
        if (loading) {
            CircularProgressIndicator()
        } else if (error != null) {
            Text("Error: $error", color = MaterialTheme.colorScheme.error)
        } else if (profile == null) {
            Text("No user profile found")
        } else {
            UserProfileCard(
                profile = profile,
                isEditing = isEditingProfile,
                isChangingPassword = isChangingPassword,
                firstName = firstName,
                lastName = lastName,
                skinType = skinType,
                skinConcernsText = skinConcernsText,
                onFirstNameChange = { firstName = it },
                onLastNameChange = { lastName = it },
                onSkinTypeChange = { skinType = it },
                onSkinConcernsTextChange = { skinConcernsText = it },
                onToggleEdit = { isEditingProfile = !isEditingProfile },
                onTogglePasswordChange = { isChangingPassword = !isChangingPassword },
                onSaveProfile = {
                    val skinConcerns = skinConcernsText.split(",").map { it.trim() }.filter { it.isNotEmpty() }

                    // call from viewModel
                    profileViewModel.updateProfile(firstName = firstName, lastName = lastName,
                        skinType = skinType,
                        skinConcerns = skinConcerns)
                    isEditingProfile = false
                }
            )
        }

        // Password change section
        if (isChangingPassword) {
            Spacer(Modifier.height(20.dp))
            PasswordChangeCard(
                currentPassword = currentPassword,
                newPassword = newPassword,
                confirmNewPassword = confirmNewPassword,
                errorMessage = passwordError,
                successMessage = passwordSuccess,
                isLoading = isPasswordLoading,
                onCurrentPasswordChange = {
                    currentPassword = it
                    passwordError = null
                    passwordSuccess = null
                },
                onNewPasswordChange = {
                    newPassword = it
                    passwordError = null
                    passwordSuccess = null
                },
                onConfirmNewPasswordChange = {
                    confirmNewPassword = it
                    passwordError = null
                    passwordSuccess = null
                },
                onUpdatePassword = {
                    when {
                        newPassword != confirmNewPassword ->
                            passwordError = "New passwords do not match"
                        currentPassword.isBlank() || newPassword.isBlank() ->
                            passwordError = "Please fill out all fields"
                        newPassword.length < 6 ->
                            passwordError = "Password must be at least 6 characters"
                        else -> {
                            isPasswordLoading = true
                            // call function from authViewModel
                            authViewModel.changePassword(currentPassword, newPassword) { success, message ->
                                isPasswordLoading = false
                                if (success) {
                                    passwordSuccess = "Password updated successfully"
                                    passwordError = null
                                    currentPassword = ""
                                    newPassword = ""
                                    confirmNewPassword = ""
                                } else {
                                    passwordError = message ?: "Failed to change password"
                                    passwordSuccess = null
                                }
                            }
                        }
                    }
                }
            )
        }

        Spacer(Modifier.weight(1f))

        // Logout button
        Button(
            onClick = {
                // call from authViewModel
                authViewModel.logout()
                onLogout()
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer
            )
        ) {
            Text("Logout")
        }
    }
}

@Composable
fun UserProfileCard(
    profile: UserProfile?,
    isEditing: Boolean,
    isChangingPassword: Boolean,
    firstName: String,
    lastName: String,
    skinType: String,
    skinConcernsText: String,
    onFirstNameChange: (String) -> Unit,
    onLastNameChange: (String) -> Unit,
    onSkinTypeChange: (String) -> Unit,
    onSkinConcernsTextChange: (String) -> Unit,
    onToggleEdit: () -> Unit,
    onTogglePasswordChange: () -> Unit,
    onSaveProfile: () -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth()
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header & edit button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("User Profile", style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = onToggleEdit) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = if (isEditing) "Cancel Editing" else "Edit Profile"
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Show email
            Text("Email: ${profile?.email ?: "Email not available"}", style = MaterialTheme.typography.bodyMedium)

            Spacer(Modifier.height(16.dp))

            // If in edit mode, show editable fields
            if (isEditing) {
                OutlinedTextField(
                    value = firstName,
                    onValueChange = onFirstNameChange,
                    label = { Text("First Name") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = lastName,
                    onValueChange = onLastNameChange,
                    label = { Text("Last Name") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = skinType,
                    onValueChange = onSkinTypeChange,
                    label = { Text("Skin Type") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )

                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = skinConcernsText,
                    onValueChange = onSkinConcernsTextChange,
                    label = { Text("Skin Concerns (comma separated)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(16.dp))

                Button(onClick = onSaveProfile, modifier = Modifier.align(Alignment.End)) {
                    Text("Save Changes")
                }
            } else {
                // View mode -> display profile information
                if (profile != null && (profile.firstName.isNotBlank() || profile.lastName.isNotBlank() ||
                            !profile.skinType.isNullOrBlank() || !profile.skinConcerns.isNullOrEmpty())) {

                    if (profile.firstName.isNotBlank() || profile.lastName.isNotBlank()) {
                        Text("Name: ${profile.firstName} ${profile.lastName}", style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(8.dp))
                    }

                    if (!profile.skinType.isNullOrBlank()) {
                        Text("Skin Type: ${profile.skinType}", style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(8.dp))
                    }

                    if (!profile.skinConcerns.isNullOrEmpty()) {
                        Text(
                            "Skin Concerns: ${profile.skinConcerns.joinToString(", ")}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else {
                    Text(
                        "No profile information available. Click the edit button to add your details.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Password change button
            Button(onClick = onTogglePasswordChange,
                modifier = Modifier.align(Alignment.End).padding(top = 16.dp)
            ) {
                Text(if (isChangingPassword) "Cancel" else "Change Password")
            }
        }
    }
}

@Composable
fun PasswordChangeCard(
    currentPassword: String,
    newPassword: String,
    confirmNewPassword: String,
    errorMessage: String?,
    successMessage: String?,
    isLoading: Boolean,
    onCurrentPasswordChange: (String) -> Unit,
    onNewPasswordChange: (String) -> Unit,
    onConfirmNewPasswordChange: (String) -> Unit,
    onUpdatePassword: () -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth()
) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Change Password", style = MaterialTheme.typography.titleMedium)

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = currentPassword,
                onValueChange = onCurrentPasswordChange,
                label = { Text("Current Password") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = newPassword,
                onValueChange = onNewPasswordChange,
                label = { Text("New Password") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = confirmNewPassword,
                onValueChange = onConfirmNewPasswordChange,
                label = { Text("Confirm New Password") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )

            Spacer(Modifier.height(16.dp))

            errorMessage?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            successMessage?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(36.dp)
                    )
                } else {
                    Button(onClick = onUpdatePassword) {
                        Text("Update Password")
                    }
                }
            }
        }
    }
}