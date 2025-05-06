package com.example.skn.userinterface

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.MultiChoiceSegmentedButtonRow
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.skn.viewmodel.UserProfileViewModel
import kotlinx.coroutines.delay

@Composable
fun OnBoardingScreen(
    profileViewModel: UserProfileViewModel,
    onFinish: () -> Unit
) {
    val loading by profileViewModel.loading.collectAsStateWithLifecycle()
    val profileUpdateSuccess by profileViewModel.updateSuccess.collectAsStateWithLifecycle()
    val error by profileViewModel.error.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val totalSteps = 5
    var currentStep by remember { mutableIntStateOf(0) }  // Tracks current onboarding step

    // User input states (start empty since it's a new user)
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var skinType by remember { mutableStateOf("") }
    var skinConcerns by remember { mutableStateOf<List<String>>(emptyList()) }

    // to determine if phone or tablet
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val isTablet = screenWidthDp >= 600

    // Only navigate when profile update is successful AND we've reached the final step
    LaunchedEffect(profileUpdateSuccess, currentStep) {
        if (profileUpdateSuccess && currentStep == 4) {
            // Wait briefly to make sure user sees success state
            delay(500)
            profileViewModel.resetUpdateStatus() // Reset the flag
            onFinish() // Navigate to main screen
        }
    }

    // Show error in snackbar if there is one
    LaunchedEffect(error) {
        if (error != null) {
            snackbarHostState.showSnackbar(
                message = error ?: "An error occurred"
            )
        }
    }
    Scaffold(
        topBar ={
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) { LinearProgressIndicator(
                progress = { (currentStep + 1) / totalSteps.toFloat() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .align(Alignment.BottomCenter)
                )
            }
    }
    )    { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ){
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(24.dp))
                if (loading) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Saving your profile...", style = MaterialTheme.typography.bodyMedium)
                } else {
                    when (currentStep) {
                        0 ->Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            WelcomeCard(onNext = { currentStep++ })
                        }
                        1 ->Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ){NameCard(firstName = firstName, lastName = lastName, onFirstNameChange = { firstName = it }, onLastNameChange = { lastName = it }, onNext = { currentStep++ })}
                        2 -> {if(isTablet){SkinTypeCardTablet(skinType = skinType, onSkinTypeChange = { skinType = it }, onNext = { currentStep++ })}
                            else{
                            SkinTypeCard(skinType = skinType, onSkinTypeChange = { skinType = it }, onNext = { currentStep++ })
                            }}
                        3 -> {
                            if (isTablet) {
                                SkinConcernsCardTablet(skinConcerns = skinConcerns,
                                    onSkinConcernsChange = { skinConcerns = it },
                                    onFinish = {
                                        profileViewModel.updateProfile(
                                            firstName = firstName, lastName = lastName,
                                            skinType = skinType, skinConcerns = skinConcerns
                                        )
                                        currentStep++
                                    }
                                )
                            } else {
                                SkinConcernsCard(skinConcerns = skinConcerns,
                                    onSkinConcernsChange = { skinConcerns = it },
                                    onFinish = {
                                        profileViewModel.updateProfile(
                                            firstName = firstName, lastName = lastName,
                                            skinType = skinType, skinConcerns = skinConcerns
                                        )
                                        currentStep++
                                    }
                                )
                            }
                        }
                    }
                }

                // Snackbar for errors
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

@Composable
fun WelcomeCard(onNext: () -> Unit) {
    Card(
        modifier = Modifier.padding(24.dp).fillMaxWidth(0.8f),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Welcome to SK!N", style = MaterialTheme.typography.headlineMedium)
            Text("Let's personalize your experience.", style = MaterialTheme.typography.bodyMedium)
            Button(onClick = onNext) {
                Text("Get Started")
            }
        }
    }
}

@Composable
fun NameCard(
    onNext: () -> Unit,
    firstName: String, lastName: String,
    onFirstNameChange: (String) -> Unit, onLastNameChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.padding(24.dp).widthIn(max = 360.dp).fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("What's your name?", style = MaterialTheme.typography.headlineSmall)

            OutlinedTextField(
                value = firstName,
                onValueChange = onFirstNameChange,
                label = { Text("First Name") },
                singleLine = true
            )

            OutlinedTextField(
                value = lastName,
                onValueChange = onLastNameChange,
                label = { Text("Last Name") },
                singleLine = true
            )

            Button(
                onClick = onNext,
                enabled = firstName.isNotBlank() && lastName.isNotBlank()
            ) {
                Text("Continue")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SkinTypeCard(skinType: String, onSkinTypeChange: (String) -> Unit, onNext: () -> Unit) {
    val skinTypes = listOf("Dry", "Oily", "Combination", "Normal", "Sensitive")
    var selectedIndex by remember { mutableIntStateOf(skinTypes.indexOf(skinType).takeIf { it >= 0 } ?: 0) }
    val primary   = MaterialTheme.colorScheme.primary
    val onPrimary = MaterialTheme.colorScheme.onPrimary

    Card(
        modifier = Modifier.padding(24.dp).fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Choose your skin type", style = MaterialTheme.typography.headlineSmall)
            Text("This helps us tailor recommendations for you.", style = MaterialTheme.typography.bodySmall)
            Text("You can always change this later.", style = MaterialTheme.typography.bodySmall, fontStyle = FontStyle.Italic)

            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ){
                    skinTypes.forEachIndexed {
                        index, type ->
                        val isSelected = index == selectedIndex

                        OutlinedButton(
                            onClick = {
                                selectedIndex = index
                                onSkinTypeChange(type)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(24.dp),
                            border = BorderStroke(
                                width = if(isSelected) 0.dp else 1.dp,
                                color = primary
                            ),
                            colors = ButtonDefaults.outlinedButtonColors(
                                // fill with primary color when selected
                                containerColor = if (isSelected)
                                    MaterialTheme.colorScheme.primary
                                else
                                    Color.Transparent,
                                // text color
                                contentColor = if (isSelected) onPrimary else primary
                            )
,                        ) {
                            Text(type)
                        }
                    }
                }
            }

            Button(
                onClick = onNext,
                enabled = skinType.isNotBlank()
            ) {
                Text("Continue")
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SkinTypeCardTablet(skinType: String, onSkinTypeChange: (String) -> Unit, onNext: () -> Unit) {
    val skinTypes = listOf("Dry", "Oily", "Combination", "Normal", "Sensitive")
    var selectedIndex by remember { mutableIntStateOf(skinTypes.indexOf(skinType).takeIf { it >= 0 } ?: 0) }

    Card(
        modifier = Modifier.padding(24.dp).fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Choose your skin type", style = MaterialTheme.typography.headlineSmall)
            Text("This helps us tailor recommendations for you.", style = MaterialTheme.typography.bodySmall)
            Text("You can always change this later.", style = MaterialTheme.typography.bodySmall, fontStyle = FontStyle.Italic)

            SingleChoiceSegmentedButtonRow {
                skinTypes.forEachIndexed { index, type ->
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(index, skinTypes.size),
                        onClick = {
                            selectedIndex = index
                            onSkinTypeChange(type)
                        },
                        selected = index == selectedIndex,
                        label = { Text(type) }
                    )
                }
            }

            Button(
                onClick = onNext,
                enabled = skinType.isNotBlank()
            ) {
                Text("Continue")
            }
        }
    }
}

@Composable
fun SkinConcernsCard(
    skinConcerns: List<String>,
    onSkinConcernsChange: (List<String>) -> Unit,
    onFinish: () -> Unit
) {
    val skinConcernsList = listOf("Acne", "Dryness", "Sun Damage", "Hyperpigmentation")
    val selectedOptions = remember {
        mutableStateListOf(*skinConcernsList.map { it in skinConcerns }.toTypedArray())
    }
    val primary   = MaterialTheme.colorScheme.primary
    val onPrimary = MaterialTheme.colorScheme.onPrimary

    Card(
        modifier = Modifier.padding(24.dp).fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Select your skin concerns", style = MaterialTheme.typography.headlineSmall)
            Text("Choose all that apply. You can update this later.", style = MaterialTheme.typography.bodySmall, fontStyle = FontStyle.Italic)

            Column(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ){
                skinConcernsList.forEachIndexed { index, concern ->
                    val isSelected = selectedOptions[index]
                    OutlinedButton(
                        onClick = {
                            selectedOptions[index] = !isSelected
                            onSkinConcernsChange(
                                skinConcernsList.filterIndexed{i, _ -> selectedOptions[i]}
                            )
                        },
                        modifier =  Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (isSelected)
                            MaterialTheme.colorScheme.primary
                            else
                                Color.Transparent,
                            contentColor = if (isSelected) onPrimary else primary
                        )
                    ) { Text(concern)}
                }
            }


            Button(onClick = onFinish) {
                Text("Finish")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SkinConcernsCardTablet(
    skinConcerns: List<String>,
    onSkinConcernsChange: (List<String>) -> Unit,
    onFinish: () -> Unit
) {
    val skinConcernsList = listOf("Acne", "Dryness", "Sun Damage", "Hyperpigmentation")
    val selectedOptions = remember {
        mutableStateListOf(*skinConcernsList.map { it in skinConcerns }.toTypedArray())
    }

    Card(
        modifier = Modifier.padding(24.dp).fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Select your skin concerns", style = MaterialTheme.typography.headlineSmall)
            Text("Choose all that apply. You can update this later.", style = MaterialTheme.typography.bodySmall, fontStyle = FontStyle.Italic)

            MultiChoiceSegmentedButtonRow {
                skinConcernsList.forEachIndexed { index, concern ->
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(index, skinConcernsList.size),
                        checked = selectedOptions[index],
                        onCheckedChange = {
                            selectedOptions[index] = !selectedOptions[index]
                            val updated = skinConcernsList.filterIndexed { i, _ -> selectedOptions[i] }
                            onSkinConcernsChange(updated)
                        },
                        icon = { SegmentedButtonDefaults.Icon(selectedOptions[index]) },
                        label = { Text(concern) }
                    )
                }
            }

            Button(onClick = onFinish) {
                Text("Finish")
            }
        }
    }
}