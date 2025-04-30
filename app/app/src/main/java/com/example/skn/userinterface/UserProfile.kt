package com.example.skn.userinterface

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.skn.model.UserProfile
import com.example.skn.viewmodel.AuthViewModel
import com.example.skn.viewmodel.UserProfileViewModel
import com.example.skn.navigation.AppBottomNavigation
import com.example.skn.navigation.NavigationTab

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    authViewModel: AuthViewModel,
    profileViewModel: UserProfileViewModel,
    onNavigateBack: () -> Unit,
    onCreatePostClick: () -> Unit = {},
    onScanClick: () -> Unit = {},
    onSearchClick: () -> Unit = {},
    onLogout: () -> Unit = {},
    onHomeClick: () -> Unit = {},
) {
    val profile by profileViewModel.userProfile.collectAsStateWithLifecycle()
    val updateSuccess by profileViewModel.updateSuccess.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    // Editing State
    var isEditing by remember { mutableStateOf(false) }
    var isChangingPassword by remember { mutableStateOf(false) }

    // Profile field states
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var skinType by remember { mutableStateOf("") }
    var skinConcerns by remember { mutableStateOf<List<String>>(emptyList()) }

    // Password field states
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmNewPassword by remember { mutableStateOf("") }

    // Password update states
    var passwordErrorMessage by remember { mutableStateOf<String?>(null) }
    var passwordSuccessMessage by remember { mutableStateOf<String?>(null) }
    var isPasswordLoading by remember { mutableStateOf(false) }

    var selectedTab by remember { mutableStateOf(NavigationTab.PROFILE) }

    // Populate fields once when profile is loaded
    LaunchedEffect(profile) {
        profile?.let {
            firstName = it.firstName
            lastName = it.lastName
            skinType = it.skinType.orEmpty()
            skinConcerns = it.skinConcerns ?: emptyList()
        }
    }

    // Show snackbar on profile update success
    LaunchedEffect(updateSuccess) {
        if (updateSuccess) {
            snackbarHostState.showSnackbar("Profile updated successfully!")
            profileViewModel.resetUpdateStatus()
            isEditing = false
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Edit Profile") }) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = { AppBottomNavigation(selectedTab = selectedTab,
                onHomeClick = { selectedTab = NavigationTab.HOME
                    onHomeClick() },
                onSearchClick = { selectedTab = NavigationTab.SEARCH
                    onSearchClick() },
                onScanClick = { selectedTab = NavigationTab.SCAN
                    onScanClick() },
                onProfileClick = { selectedTab = NavigationTab.PROFILE},

            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            if (profile != null) {
                UserProfileCard(
                    profile = profile,
                    isEditing = isEditing,
                    isChangingPassword = isChangingPassword,
                    email = profile!!.email,
                    firstName = firstName,
                    lastName = lastName,
                    skinType = skinType,
                    skinConcerns = skinConcerns,
                    onFirstNameChange = { firstName = it },
                    onLastNameChange = { lastName = it },
                    onSkinTypeChange = { skinType = it },
                    onSkinConcernsChange = { skinConcerns = it },
                    onToggleEdit = { isEditing = !isEditing },
                    onTogglePasswordChange = { isChangingPassword = !isChangingPassword },
                    onSaveProfile = {
                        profileViewModel.updateProfile(
                            firstName = firstName,
                            lastName = lastName,
                            skinType = skinType,
                            skinConcerns = skinConcerns
                        )
                    }
                )

                Spacer(Modifier.height(16.dp))

                if (isChangingPassword) {
                    PasswordChangeCard(
                        currentPassword = currentPassword,
                        newPassword = newPassword,
                        confirmNewPassword = confirmNewPassword,
                        errorMessage = passwordErrorMessage,
                        successMessage = passwordSuccessMessage,
                        isLoading = isPasswordLoading,
                        onCurrentPasswordChange = { currentPassword = it },
                        onNewPasswordChange = { newPassword = it },
                        onConfirmNewPasswordChange = { confirmNewPassword = it },
                        onUpdatePassword = {
                            // Validate new password
                            if (newPassword != confirmNewPassword) {
                                passwordErrorMessage = "Passwords do not match"
                                passwordSuccessMessage = null
                                return@PasswordChangeCard
                            }

                            // Call authViewModel to update password
                            isPasswordLoading = true
                            passwordErrorMessage = null
                            passwordSuccessMessage = null

                            authViewModel.changePassword(
                                currentPassword = currentPassword,
                                newPassword = newPassword,
                                onResult = { success, errorMessage ->
                                    isPasswordLoading = false
                                    if (success) {
                                        passwordSuccessMessage = "Password updated successfully"
                                        passwordErrorMessage = null
                                        currentPassword = ""
                                        newPassword = ""
                                        confirmNewPassword = ""
                                        isChangingPassword = false
                                    } else {
                                        passwordErrorMessage =
                                            errorMessage ?: "Failed to update password"
                                    }
                                }
                            )
                        }
                    )
                }
            } else {
                // Loading
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    authViewModel.logout()
                    onLogout()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            ) {
                Text("Logout")
            }

        }
    }
}


@Composable
fun UserProfileCard(
    profile: UserProfile?,
    isEditing: Boolean,
    isChangingPassword: Boolean,
    email: String,
    firstName: String,
    lastName: String,
    skinType: String,
    skinConcerns: List<String>,
    onFirstNameChange: (String) -> Unit,
    onLastNameChange: (String) -> Unit,
    onSkinTypeChange: (String) -> Unit,
    onSkinConcernsChange: (List<String>) -> Unit,
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
            Text("Email: ${email}", style = MaterialTheme.typography.bodyMedium)

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
                SkinTypeDropdown(
                    options = listOf("Dry", "Oily", "Combination", "Normal","Sensitive"),
                    selected = skinType,
                    onSelectionChange = onSkinTypeChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                )

                Spacer(Modifier.height(8.dp))

                MultiSelectSkinConcernDropdown(
                    options           = listOf("Acne", "Dryness", "Sun Damage", "Hyperpigmentation"),
                    selected          = skinConcerns,
                    onSelectionChange = onSkinConcernsChange,
                    label             = "Skin Concerns",
                    modifier          = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiSelectSkinConcernDropdown(
    options: List<String>,
    selected: List<String>,
    onSelectionChange: (List<String>) -> Unit,
    label: String = "Skin Concerns",
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = if (selected.isEmpty()) "" else selected.joinToString(", "),
            onValueChange = { /* read-only */ },
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                val isChecked = option in selected
                DropdownMenuItem(
                    onClick = {
                        val newList = if (isChecked) selected - option else selected + option
                        onSelectionChange(newList)
                    },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = null
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(option)
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SkinTypeDropdown(
    options: List<String>,
    selected: String,
    onSelectionChange: (String) -> Unit,
    label: String = "Skin Type",
    modifier: Modifier = Modifier
){
    var expanded by remember { mutableStateOf(false)}

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = {expanded = !expanded},
        modifier = modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selected,
            onValueChange = {/* read only */},
            readOnly = true,
            label = {Text(label)},
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded)},
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false}
        ) {
            options.forEach {option ->
                DropdownMenuItem(
                    text = {Text(option)},
                    onClick = {
                        onSelectionChange(option)
                        expanded = false
                    }
                )
            }
        }
    }
}
