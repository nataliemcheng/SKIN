package com.example.skn.userinterface

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
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
        topBar = {
            TopAppBar(
                title = { Text("Your Profile") }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            AppBottomNavigation(
                selectedTab = selectedTab,
                onHomeClick = {
                    selectedTab = NavigationTab.HOME
                    onHomeClick()
                },
                onSearchClick = {
                    selectedTab = NavigationTab.SEARCH
                    onSearchClick()
                },
                onScanClick = {
                    selectedTab = NavigationTab.SCAN
                    onScanClick()
                },
                onProfileClick = {
                    selectedTab = NavigationTab.PROFILE
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            if (profile != null) {
                // Profile header section with edit button
                ProfileHeaderSection(
                    profile = profile!!,
                    isEditing = isEditing,
                    onToggleEdit = { isEditing = !isEditing }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

                if (isEditing) {
                    // Edit mode for profile info
                    ProfileEditSection(
                        firstName = firstName,
                        lastName = lastName,
                        skinType = skinType,
                        skinConcerns = skinConcerns,
                        onFirstNameChange = { firstName = it },
                        onLastNameChange = { lastName = it },
                        onSkinTypeChange = { skinType = it },
                        onSkinConcernsChange = { skinConcerns = it },
                        onSaveProfile = {
                            profileViewModel.updateProfile(
                                firstName = firstName,
                                lastName = lastName,
                                skinType = skinType,
                                skinConcerns = skinConcerns
                            )
                        }
                    )
                } else {
                    // View mode - display profile info
                    ProfileInfoSection(profile = profile!!)
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

                // Security section with change password button
                SecuritySection(
                    isChangingPassword = isChangingPassword,
                    onTogglePasswordChange = { isChangingPassword = !isChangingPassword }
                )

                if (isChangingPassword) {
                    Spacer(modifier = Modifier.height(16.dp))

                    PasswordChangeSection(
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
                                return@PasswordChangeSection
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

                Spacer(modifier = Modifier.height(16.dp))

                // Alternate logout option at bottom
                OutlinedButton(
                    onClick = {
                        authViewModel.logout()
                        onLogout()
                    },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Logout,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Logout")
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

            } else {
                // Loading
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@Composable
fun ProfileHeaderSection(
    profile: UserProfile,
    isEditing: Boolean,
    onToggleEdit: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                "${profile.firstName} ${profile.lastName}",
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                profile.email,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        IconButton(onClick = onToggleEdit) {
            Icon(
                Icons.Default.Edit,
                contentDescription = if (isEditing) "Cancel Editing" else "Edit Profile",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun ProfileInfoSection(profile: UserProfile) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "Profile Information",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )

        if (!profile.skinType.isNullOrBlank() || !profile.skinConcerns.isNullOrEmpty()) {
            if (!profile.skinType.isNullOrBlank()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Skin Type",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        profile.skinType,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            if (!profile.skinConcerns.isNullOrEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        "Skin Concerns",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        profile.skinConcerns.joinToString(", "),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.End
                    )
                }
            }
        } else {
            Text(
                "No profile information available. Click the edit button to add your details.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ProfileEditSection(
    firstName: String,
    lastName: String,
    skinType: String,
    skinConcerns: List<String>,
    onFirstNameChange: (String) -> Unit,
    onLastNameChange: (String) -> Unit,
    onSkinTypeChange: (String) -> Unit,
    onSkinConcernsChange: (List<String>) -> Unit,
    onSaveProfile: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "Edit Profile",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )

        OutlinedTextField(
            value = firstName,
            onValueChange = onFirstNameChange,
            label = { Text("First Name") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = lastName,
            onValueChange = onLastNameChange,
            label = { Text("Last Name") },
            modifier = Modifier.fillMaxWidth()
        )

        SkinTypeDropdown(
            options = listOf("Dry", "Oily", "Combination", "Normal", "Sensitive"),
            selected = skinType,
            onSelectionChange = onSkinTypeChange,
            modifier = Modifier.fillMaxWidth()
        )

        MultiSelectSkinConcernDropdown(
            options = listOf("Acne", "Dryness", "Sun Damage", "Hyperpigmentation"),
            selected = skinConcerns,
            onSelectionChange = onSkinConcernsChange,
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = onSaveProfile,
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("Save Changes")
        }
    }
}

@Composable
fun SecuritySection(
    isChangingPassword: Boolean,
    onTogglePasswordChange: () -> Unit
) {
    Column {
        Text(
            "Security",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onTogglePasswordChange,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isChangingPassword) "Cancel" else "Change Password")
            }
        }
    }
}

@Composable
fun PasswordChangeSection(
    currentPassword: String,
    newPassword: String,
    confirmNewPassword: String,
    errorMessage: String?,
    successMessage: String?,
    isLoading: Boolean,
    onCurrentPasswordChange: (String) -> Unit,
    onNewPasswordChange: (String) -> Unit,
    onConfirmNewPasswordChange: (String) -> Unit,
    onUpdatePassword: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = currentPassword,
                onValueChange = onCurrentPasswordChange,
                label = { Text("Current Password") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )

            OutlinedTextField(
                value = newPassword,
                onValueChange = onNewPasswordChange,
                label = { Text("New Password") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )

            OutlinedTextField(
                value = confirmNewPassword,
                onValueChange = onConfirmNewPasswordChange,
                label = { Text("Confirm New Password") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )

            errorMessage?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            successMessage?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Button(
                onClick = onUpdatePassword,
                modifier = Modifier.align(Alignment.End),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Update Password")
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
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selected,
            onValueChange = { /* read only */ },
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
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelectionChange(option)
                        expanded = false
                    }
                )
            }
        }
    }
}