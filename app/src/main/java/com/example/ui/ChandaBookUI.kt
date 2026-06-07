package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Donation
import com.example.data.model.Expense
import com.example.data.model.Member
import com.example.data.model.Organization
import com.example.utils.CurrencyFormatter
import com.example.utils.DateUtils
import com.example.utils.ShareUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

// Active View Routing Constants
const val ROUTE_SPLASH = "splash"
const val ROUTE_LOGIN = "login"
const val ROUTE_DASHBOARD = "dashboard"
const val ROUTE_ADD_DONATION = "add_donation"
const val ROUTE_RECEIPT = "receipt"
const val ROUTE_DONATIONS_LIST = "donations_list"
const val ROUTE_DONATION_DETAIL = "donation_detail"
const val ROUTE_ADD_EXPENSE = "add_expense"
const val ROUTE_EXPENSES_LIST = "expenses_list"
const val ROUTE_REPORTS = "reports"
const val ROUTE_MEMBERS = "members"
const val ROUTE_SETTINGS = "settings"

@Composable
fun DiyaLogo(modifier: Modifier = Modifier, iconSize: Float = 100f) {
    Canvas(modifier = modifier.size((iconSize * 1.5).dp)) {
        val width = size.width
        val height = size.height

        // 1. Draw elegant orange/golden backdrop ring
        drawCircle(
            color = Color(0xFFFFE0B2),
            radius = width * 0.42f,
            center = Offset(width * 0.5f, height * 0.55f)
        )

        // 2. Draw clay diya base
        val clayPath = Path().apply {
            moveTo(width * 0.15f, height * 0.45f)
            cubicTo(
                width * 0.15f, height * 0.85f,
                width * 0.85f, height * 0.85f,
                width * 0.85f, height * 0.45f
            )
            quadraticTo(width * 0.5f, height * 0.58f, width * 0.15f, height * 0.45f)
            close()
        }
        drawPath(clayPath, color = Color(0xFFD84315)) // Rich brown clay

        // Diya inner depth rim
        val innerPath = Path().apply {
            moveTo(width * 0.15f, height * 0.45f)
            quadraticTo(width * 0.5f, height * 0.58f, width * 0.85f, height * 0.45f)
            quadraticTo(width * 0.5f, height * 0.50f, width * 0.15f, height * 0.45f)
            close()
        }
        drawPath(innerPath, color = Color(0xFFBF360C)) // Deep rim shadow

        // 3. Draw golden flame representing prosperity
        val flamePath = Path().apply {
            moveTo(width * 0.5f, height * 0.12f)
            cubicTo(
                width * 0.32f, height * 0.32f,
                width * 0.35f, height * 0.45f,
                width * 0.5f, height * 0.45f
            )
            cubicTo(
                width * 0.65f, height * 0.45f,
                width * 0.68f, height * 0.32f,
                width * 0.5f, height * 0.12f
            )
            close()
        }
        drawPath(flamePath, color = Color(0xFFFFC107)) // Golden yellow flame core

        // Flame bright inner core
        val innerFlame = Path().apply {
            moveTo(width * 0.5f, height * 0.22f)
            cubicTo(
                width * 0.40f, height * 0.34f,
                width * 0.42f, height * 0.44f,
                width * 0.5f, height * 0.44f
            )
            cubicTo(
                width * 0.58f, height * 0.44f,
                width * 0.60f, height * 0.34f,
                width * 0.5f, height * 0.22f
            )
            close()
        }
        drawPath(innerFlame, color = Color(0xFFFFEA00)) // Golden glow

        // Saffron flame accent tip
        drawCircle(
            color = Color(0xFFFF4E00),
            radius = width * 0.05f,
            center = Offset(width * 0.5f, height * 0.41f)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChandaBookAppContent(viewModel: ChandaBookViewModel) {
    val context = LocalContext.current
    var currentScreen by remember { mutableStateOf(ROUTE_SPLASH) }
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Trigger alerts from StateFlow on the UI
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(
                message = it,
                duration = SnackbarDuration.Short,
                withDismissAction = true
            )
            viewModel.clearError()
        }
    }

    LaunchedEffect(successMessage) {
        successMessage?.let {
            snackbarHostState.showSnackbar(
                message = it,
                duration = SnackbarDuration.Short
            )
            viewModel.clearSuccess()
        }
    }

    // Splash Timer Flow
    if (currentScreen == ROUTE_SPLASH) {
        LaunchedEffect(Unit) {
            delay(2200)
            if (isLoggedIn) {
                currentScreen = ROUTE_DASHBOARD
            } else {
                currentScreen = ROUTE_LOGIN
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        contentWindowInsets = WindowInsets.safeDrawing,
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (currentScreen) {
                ROUTE_SPLASH -> SplashScreen()
                
                ROUTE_LOGIN -> LoginRegistrationScreen(
                    viewModel = viewModel,
                    onLoginSuccess = { currentScreen = ROUTE_DASHBOARD }
                )
                
                ROUTE_DASHBOARD -> DashboardScreen(
                    viewModel = viewModel,
                    onNavigate = { route -> currentScreen = route }
                )
                
                ROUTE_ADD_DONATION -> AddDonationScreen(
                    viewModel = viewModel,
                    onNavigateBack = { currentScreen = ROUTE_DASHBOARD },
                    onReceiptReady = { 
                        currentScreen = ROUTE_RECEIPT 
                    }
                )
                
                ROUTE_RECEIPT -> ReceiptScreen(
                    viewModel = viewModel,
                    onNavigateBack = { currentScreen = ROUTE_DASHBOARD }
                )
                
                ROUTE_DONATIONS_LIST -> DonationsListScreen(
                    viewModel = viewModel,
                    onNavigateBack = { currentScreen = ROUTE_DASHBOARD },
                    onNavigateToDetail = { donation ->
                        viewModel.selectedDonation.value = donation
                        currentScreen = ROUTE_DONATION_DETAIL
                    },
                    onNavigateToAdd = { currentScreen = ROUTE_ADD_DONATION }
                )
                
                ROUTE_DONATION_DETAIL -> DonationDetailScreen(
                    viewModel = viewModel,
                    onNavigateBack = { currentScreen = ROUTE_DONATIONS_LIST },
                    onNavigateToReceipt = { currentScreen = ROUTE_RECEIPT }
                )
                
                ROUTE_ADD_EXPENSE -> AddExpenseScreen(
                    viewModel = viewModel,
                    onNavigateBack = { currentScreen = ROUTE_DASHBOARD }
                )
                
                ROUTE_EXPENSES_LIST -> ExpensesListScreen(
                    viewModel = viewModel,
                    onNavigateBack = { currentScreen = ROUTE_DASHBOARD },
                    onNavigateToAdd = { currentScreen = ROUTE_ADD_EXPENSE }
                )
                
                ROUTE_REPORTS -> ReportsScreen(
                    viewModel = viewModel,
                    onNavigateBack = { currentScreen = ROUTE_DASHBOARD }
                )
                
                ROUTE_MEMBERS -> MembersScreen(
                    viewModel = viewModel,
                    onNavigateBack = { currentScreen = ROUTE_DASHBOARD }
                )

                ROUTE_SETTINGS -> SettingsScreen(
                    viewModel = viewModel,
                    onNavigateBack = { currentScreen = ROUTE_DASHBOARD },
                    onLogout = { currentScreen = ROUTE_LOGIN }
                )
            }

            // Spinner Loading Overlay
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.25f))
                        .pointerInput(Unit) {},
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "Updating Ledger...",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// 1. SPLASH SCREEN
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
@Composable
fun SplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFFFF8A50), Color(0xFFFF6B35)) // Festive orange + deep orange
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            DiyaLogo(iconSize = 130f)
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "ChandaBook",
                color = Color.White,
                fontSize = 38.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Serif,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Digital Festival Ledger App",
                color = Color(0xFFFFE0B2),
                fontSize = 16.sp,
                fontWeight = FontWeight.Light,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// 2. LOGIN & REGISTER SCREEN (With Bottom Sheet Flow)
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginRegistrationScreen(
    viewModel: ChandaBookViewModel,
    onLoginSuccess: () -> Unit
) {
    val context = LocalContext.current
    var showSheet by remember { mutableStateOf(false) }
    var selectedMethod by remember { mutableStateOf("email") } // "email" or "whatsapp"
    var isNewUserRegister by remember { mutableStateOf(false) } // true = register, false = login

    // Fields
    var nameField by remember { mutableStateOf("") }
    var orgNameField by remember { mutableStateOf("") }
    var emailField by remember { mutableStateOf("") }
    var phoneField by remember { mutableStateOf("") }
    var roleField by remember { mutableStateOf("Admin") } // dropdown values
    var otpField by remember { mutableStateOf("") }

    val otpTimer by viewModel.otpTimer.collectAsState()
    val otpSent by viewModel.otpSent.collectAsState()

    var showRoleDropdown by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            DiyaLogo(iconSize = 85f)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "ChandaBook",
                fontSize = 32.sp,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                "Digital Khata Book for your Festival Committee 🪔",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Google Button (Interactive Mock representing real integration callback)
            Card(
                onClick = {
                    viewModel.showSuccess("Google Sign-In Triggered successfully")
                    viewModel.selectGoogleLogin("mock_google_token_9001", onLoginSuccess)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("google_login_button"),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow, // Fallback play icon representing Google
                        contentDescription = "Google Logo",
                        tint = Color(0xFF4285F4),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "Continue with Google",
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF757575),
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Email Button
            Button(
                onClick = {
                    selectedMethod = "email"
                    isNewUserRegister = false
                    viewModel.cancelAuth()
                    showSheet = true
                },
                modifier = Modifier.fillMaxWidth().height(52.dp).testTag("email_login_button"),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Email, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Continue with Email OTP", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // WhatsApp Button
            Button(
                onClick = {
                    selectedMethod = "whatsapp"
                    isNewUserRegister = false
                    viewModel.cancelAuth()
                    showSheet = true
                },
                modifier = Modifier.fillMaxWidth().height(52.dp).testTag("whatsapp_login_button"),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Phone, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Continue with WhatsApp OTP", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            Row(
                modifier = Modifier.clickable {
                    isNewUserRegister = true
                    viewModel.cancelAuth()
                    showSheet = true
                },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("New Committee? ", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                Text("Create free account", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }

        // OTP Bottom Sheet Dialog Simulation to keep building stable
        if (showSheet) {
            AlertDialog(
                onDismissRequest = { showSheet = false },
                title = {
                    Text(
                        if (isNewUserRegister) {
                            "Register as ${if (selectedMethod == "email") "Email" else "WhatsApp"}"
                        } else {
                            "Sign In with ${if (selectedMethod == "email") "Email OTP" else "WhatsApp"}"
                        }
                    )
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (!otpSent) {
                            if (isNewUserRegister) {
                                OutlinedTextField(
                                    value = nameField,
                                    onValueChange = { nameField = it },
                                    label = { Text("Your Name") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = orgNameField,
                                    onValueChange = { orgNameField = it },
                                    label = { Text("Festival Organization / Mandap Name") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                // Simple custom dropdown simulation
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    OutlinedTextField(
                                        value = roleField,
                                        onValueChange = { },
                                        readOnly = true,
                                        label = { Text("Role in Committee") },
                                        trailingIcon = {
                                            IconButton(onClick = { showRoleDropdown = true }) {
                                                Icon(Icons.Default.KeyboardArrowDown, null)
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    DropdownMenu(
                                        expanded = showRoleDropdown,
                                        onDismissRequest = { showRoleDropdown = false }
                                    ) {
                                        listOf("Admin", "Member", "Viewer").forEach { r ->
                                            DropdownMenuItem(
                                                text = { Text(r) },
                                                onClick = {
                                                    roleField = r
                                                    showRoleDropdown = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            if (selectedMethod == "email") {
                                OutlinedTextField(
                                    value = emailField,
                                    onValueChange = { emailField = it },
                                    label = { Text("Email Address") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            } else {
                                OutlinedTextField(
                                    value = phoneField,
                                    onValueChange = {
                                        // Auto-format limit to 10 digits
                                        if (it.length <= 10 && it.all { char -> char.isDigit() }) {
                                            phoneField = it
                                        }
                                    },
                                    label = { Text("10-Digit Mobile Number") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                    singleLine = true,
                                    prefix = { Text("+91 ") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        } else {
                            // OTP SENT, INPUT SCREEN
                            Text(
                                "Enter the 6-digit verification code sent to ${viewModel.authTarget.value}",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            OutlinedTextField(
                                value = otpField,
                                onValueChange = {
                                    if (it.length <= 6 && it.all { char -> char.isDigit() }) {
                                        otpField = it
                                    }
                                },
                                label = { Text("6-Digit OTP Code") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("otp_input_field"),
                                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center)
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (otpTimer > 0) {
                                    Text("Resend code in ${otpTimer}s", fontSize = 13.sp, color = Color.Gray)
                                } else {
                                    Text(
                                        "Resend OTP",
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.clickable {
                                            if (selectedMethod == "email") {
                                                viewModel.requestEmailOtp(emailField, isNewUserRegister, nameField, orgNameField, roleField)
                                            } else {
                                                viewModel.requestWhatsappOtp(phoneField, isNewUserRegister, nameField, orgNameField, roleField)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (!otpSent) {
                                if (selectedMethod == "email") {
                                    if (emailField.isEmpty() || !emailField.contains("@")) {
                                        viewModel.showError("Enter a valid email address")
                                        return@Button
                                    }
                                    viewModel.requestEmailOtp(emailField, isNewUserRegister, nameField, orgNameField, roleField)
                                } else {
                                    if (phoneField.length != 10) {
                                        viewModel.showError("Enter a valid 10-digit mobile number")
                                        return@Button
                                    }
                                    viewModel.requestWhatsappOtp(phoneField, isNewUserRegister, nameField, orgNameField, roleField)
                                }
                            } else {
                                if (otpField.length != 6) {
                                    viewModel.showError("OTP must be exactly 6 digits")
                                    return@Button
                                }
                                if (selectedMethod == "email") {
                                    viewModel.verifyEmailOtp(otpField, isNewUserRegister) {
                                        showSheet = false
                                        onLoginSuccess()
                                    }
                                } else {
                                    viewModel.verifyWhatsappOtp(otpField, isNewUserRegister) {
                                        showSheet = false
                                        onLoginSuccess()
                                    }
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text(if (!otpSent) "Send OTP" else "Verify & Login")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showSheet = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// 3. DASHBOARD SCREEN
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: ChandaBookViewModel,
    onNavigate: (String) -> Unit
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val orgs by viewModel.organizations.collectAsState()
    val activeOrgId by viewModel.activeOrgId.collectAsState()
    val activeOrgDetails by viewModel.activeOrgDetails.collectAsState()
    val summaryTotals by viewModel.summary.collectAsState()

    val filteredDonations by viewModel.filteredDonations.collectAsState()
    val filteredExpenses by viewModel.filteredExpenses.collectAsState()

    var showOrgSwitcher by remember { mutableStateOf(false) }
    var scaffoldState = rememberBottomSheetScaffoldState()
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize()) {
        
        // -------------------------------------------------------------
        // BEAUTIFUL CUSTOM HEADER (Bold Design Scheme with bottom-rounded 32.dp sheet)
        // -------------------------------------------------------------
        Card(
            shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 20.dp)
            ) {
                // Top Row: Brand & Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            modifier = Modifier.size(40.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("🪔", fontSize = 22.sp)
                            }
                        }
                        Column {
                            Text(
                                "ChandaBook",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 21.sp,
                                fontFamily = FontFamily.Serif,
                                letterSpacing = (-0.5).sp
                            )
                            Text(
                                "DIGITAL LEDGER",
                                color = Color(0xFFFFCCBC),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.5.sp
                            )
                        }
                    }

                    // Top Action buttons
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = { viewModel.refreshActiveOrgData() },
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color.White.copy(alpha = 0.15f), CircleShape)
                        ) {
                            Icon(Icons.Default.Refresh, "Refresh", tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                        IconButton(
                            onClick = { onNavigate(ROUTE_SETTINGS) },
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color.White.copy(alpha = 0.15f), CircleShape)
                        ) {
                            Icon(Icons.Default.Settings, "Profile", tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Org Selector Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFBF5028), RoundedCornerShape(16.dp))
                        .clickable { showOrgSwitcher = true }
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            // Saturated pulse dot representation representing live state
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(Color(0xFF4ADE80), CircleShape)
                            )
                            Text(
                                activeOrgDetails?.name ?: "Select Committee",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text("▼", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }

                    // Anchor the dropdown menu
                    DropdownMenu(
                        expanded = showOrgSwitcher,
                        onDismissRequest = { showOrgSwitcher = false }
                    ) {
                        if (orgs.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("+ Click to create organization") },
                                onClick = {
                                    showOrgSwitcher = false
                                    onNavigate(ROUTE_SETTINGS)
                                }
                            )
                        } else {
                            orgs.forEach { org ->
                                DropdownMenuItem(
                                    text = { Text(org.name) },
                                    onClick = {
                                        viewModel.switchOrganization(org.id)
                                        showOrgSwitcher = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // MAIN BODY CONTAINER (Styled after high contrast cream background)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Summary totals logic
            val totalDonations = summaryTotals?.totalAmount ?: 0.0
            val totalExpenses = filteredExpenses.sumOf { it.amount }
            val netBalance = totalDonations - totalExpenses

            // -------------------------------------------------------------
            // FINANCIAL SUMMARY (Bold Typography Focus Grid Layout)
            // -------------------------------------------------------------
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Total Chanda Card
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier
                            .weight(1f)
                            .height(110.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "TOTAL CHANDA",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = CurrencyFormatter.format(totalDonations),
                                style = MaterialTheme.typography.displayMedium,
                                color = Color(0xFF2E7D32)
                            )
                            // Progress highlight line
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(Color(0xFFE8F5E9))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(if (totalDonations > 0) 0.75f else 0f)
                                        .background(Color(0xFF2E7D32))
                                )
                            }
                        }
                    }

                    // Total Expense Card
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier
                            .weight(1f)
                            .height(110.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "TOTAL EXPENSE",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = CurrencyFormatter.format(totalExpenses),
                                style = MaterialTheme.typography.displayMedium,
                                color = Color(0xFFD32F2F)
                            )
                            // Progress highlight line
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(Color(0xFFFCE8E6))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(if (totalDonations > 0) (totalExpenses / totalDonations.coerceAtLeast(1.0)).toFloat().coerceIn(0f, 1f) else 0f)
                                        .background(Color(0xFFD32F2F))
                                )
                            }
                        }
                    }
                }

                // Available Liquidity Card
                Card(
                    shape = RoundedCornerShape(32.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "AVAILABLE BALANCE",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = CurrencyFormatter.format(netBalance),
                                style = MaterialTheme.typography.displayLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(Color(0xFFFFF3E0), RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("📊", fontSize = 24.sp)
                        }
                    }
                }
            }

            // -------------------------------------------------------------
            // QUICK ACTION BOARD GRID (Bold layout style with premium 16.dp rounding)
            // -------------------------------------------------------------
            Text(
                text = "QUICK TRANSACTIONS BOARD",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Button 1: Add Donation
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigate(ROUTE_ADD_DONATION) }
                        .testTag("quick_add_donation_button"),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.size(56.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("➕", fontSize = 22.sp, color = Color.White)
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "ADD CHANDA",
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Button 2: Add Expense
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigate(ROUTE_ADD_EXPENSE) }
                        .testTag("quick_add_expense_button"),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF2E7D32)),
                        modifier = Modifier.size(56.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("₹OUT", fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color.White)
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "ADD BILL",
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Button 3: View Reports
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigate(ROUTE_REPORTS) },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
                        modifier = Modifier.size(56.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("📜", fontSize = 22.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "REPORTS",
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Button 4: Members List
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigate(ROUTE_MEMBERS) },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
                        modifier = Modifier.size(56.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("👥", fontSize = 22.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "TEAM",
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Recent activity lists
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("RECENT DONATIONS (CHANDA)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                TextButton(onClick = { onNavigate(ROUTE_DONATIONS_LIST) }) {
                    Text("View All", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            if (filteredDonations.isEmpty()) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(24.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("🪔 Empty Empty Empty!", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Ready for Ganesh Chaturthi? Add your first donation!", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    filteredDonations.take(4).forEach { donation ->
                        Card(
                            onClick = {
                                viewModel.selectedDonation.value = donation
                                onNavigate(ROUTE_DONATION_DETAIL)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(donation.donorName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("Mode: ${donation.paymentMethod.uppercase()} | ${DateUtils.getRelativeTime(donation.createdAt)}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text(CurrencyFormatter.format(donation.amount), color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("RECENT EXPENSES", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                TextButton(onClick = { onNavigate(ROUTE_EXPENSES_LIST) }) {
                    Text("View All", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            if (filteredExpenses.isEmpty()) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(24.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("No expenses registered yet.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    filteredExpenses.take(4).forEach { expense ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(expense.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("Mode: ${expense.paymentMethod.uppercase()} | ${DateUtils.getRelativeTime(expense.createdAt)}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text(CurrencyFormatter.format(expense.amount), color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                            }
                        }
                    }
                }
            }
        }

        // FOOTER MAIN BOTTOM NAVIGATION BAR
        NavigationBar(
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            NavigationBarItem(
                selected = true,
                onClick = {},
                icon = { Icon(Icons.Default.Home, null) },
                label = { Text("Home", fontSize = 10.sp) }
            )
            NavigationBarItem(
                selected = false,
                onClick = { onNavigate(ROUTE_DONATIONS_LIST) },
                icon = { Icon(Icons.Default.CheckCircle, null) },
                label = { Text("Chanda", fontSize = 10.sp) }
            )
            NavigationBarItem(
                selected = false,
                onClick = { onNavigate(ROUTE_EXPENSES_LIST) },
                icon = { Icon(Icons.Default.Warning, null) }, // Red expense warning / note representation
                label = { Text("Expenses", fontSize = 10.sp) }
            )
            NavigationBarItem(
                selected = false,
                onClick = { onNavigate(ROUTE_REPORTS) },
                icon = { Icon(Icons.Default.DateRange, null) },
                label = { Text("Reports", fontSize = 10.sp) }
            )
            NavigationBarItem(
                selected = false,
                onClick = { onNavigate(ROUTE_SETTINGS) },
                icon = { Icon(Icons.Default.Settings, null) },
                label = { Text("Settings", fontSize = 10.sp) }
            )
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// 4. ADD DONATION SCREEN
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDonationScreen(
    viewModel: ChandaBookViewModel,
    onNavigateBack: () -> Unit,
    onReceiptReady: () -> Unit
) {
    var donorName by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var amountStr by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("donation") }
    var paymentMethod by remember { mutableStateOf("cash") }
    var address by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    var categoryExpanded by remember { mutableStateOf(false) }

    val categories = listOf("donation", "prasad", "decoration", "sound", "pooja", "other")
    val payments = listOf("cash", "upi", "bank_transfer", "cheque", "online")

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Add Donation (Chanda)") },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary, titleContentColor = Color.White)
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = donorName,
                onValueChange = { donorName = it },
                label = { Text("Donor Full Name *") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("donor_name_input")
            )

            OutlinedTextField(
                value = phoneNumber,
                onValueChange = {
                    if (it.length <= 10 && it.all { char -> char.isDigit() }) {
                        phoneNumber = it
                    }
                },
                label = { Text("Phone Number") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                placeholder = { Text("optional, 10-digit") },
                prefix = { Text("+91 ") },
                trailingIcon = {
                    if (phoneNumber.length == 10) {
                        IconButton(onClick = {}) {
                            Icon(Icons.Default.Phone, "WhatsApp Available", tint = Color(0xFF25D366))
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = amountStr,
                onValueChange = { amountStr = it },
                label = { Text("Amount Contributed (₹) *") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                prefix = { Text("₹ ") },
                modifier = Modifier.fillMaxWidth().testTag("donation_amount_input")
            )

            // Category dropdown selector
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = category.uppercase(Locale.getDefault()),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Ledger Category *") },
                    trailingIcon = {
                        IconButton(onClick = { categoryExpanded = true }) {
                            Icon(Icons.Default.KeyboardArrowDown, null)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                DropdownMenu(
                    expanded = categoryExpanded,
                    onDismissRequest = { categoryExpanded = false }
                ) {
                    categories.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat.uppercase()) },
                            onClick = {
                                category = cat
                                categoryExpanded = false
                            }
                        )
                    }
                }
            }

            // Payment chips selector
            Text("Payment Method", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                payments.forEach { method ->
                    FilterChip(
                        selected = paymentMethod == method,
                        onClick = { paymentMethod = method },
                        label = { Text(method.uppercase()) }
                    )
                }
            }

            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                label = { Text("Donor Area / Address") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Special notes / comments") },
                maxLines = 3,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Save & Generate button
            Button(
                onClick = {
                    val amt = amountStr.toDoubleOrNull()
                    if (donorName.isEmpty()) {
                        viewModel.showError("Donor Name is required")
                        return@Button
                    }
                    if (amt == null || amt <= 0) {
                        viewModel.showError("Enter a valid donation amount")
                        return@Button
                    }

                    viewModel.addDonation(
                        donorName = donorName,
                        phone = phoneNumber.ifEmpty { null },
                        amount = amt,
                        category = category,
                        paymentMethod = paymentMethod,
                        address = address.ifEmpty { null },
                        notes = notes.ifEmpty { null }
                    ) {
                        onReceiptReady()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp).testTag("save_and_generate_button"),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("💾 Save & Generate Receipt", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            // Save Only button
            OutlinedButton(
                onClick = {
                    val amt = amountStr.toDoubleOrNull()
                    if (donorName.isEmpty()) {
                        viewModel.showError("Donor Name is required")
                        return@OutlinedButton
                    }
                    if (amt == null || amt <= 0) {
                        viewModel.showError("Enter a valid donation amount")
                        return@OutlinedButton
                    }
                    viewModel.addDonation(
                        donorName = donorName,
                        phone = phoneNumber.ifEmpty { null },
                        amount = amt,
                        category = category,
                        paymentMethod = paymentMethod,
                        address = address.ifEmpty { null },
                        notes = notes.ifEmpty { null }
                    ) {
                        viewModel.showSuccess("Donation saved successfully without creating document")
                        onNavigateBack()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
            ) {
                Text("Save Ledger Record Only", color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// 5. RECEIPT SCREEN
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptScreen(
    viewModel: ChandaBookViewModel,
    onNavigateBack: () -> Unit
) {
    val donation by viewModel.selectedDonation.collectAsState()
    val orgDetails by viewModel.activeOrgDetails.collectAsState()
    val receiptFile by viewModel.generatedReceiptFile.collectAsState()
    val context = LocalContext.current

    val finalDonation = donation ?: return

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Donation Receipt voucher") },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary, titleContentColor = Color.White)
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Receipt card design
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    DiyaLogo(iconSize = 50f)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        orgDetails?.name?.uppercase(Locale.getDefault()) ?: "FESTIVAL COMMITTEE",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text("CHANDABOOK OFFICIAL VOUCHER", fontSize = 10.sp, color = Color.Gray)

                    Spacer(modifier = Modifier.height(20.dp))

                    Text("Receipt No: ${finalDonation.receiptNumber}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Date: ${DateUtils.formatWithTime(finalDonation.createdAt)}", fontSize = 11.sp, color = Color.Gray)

                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Box containing the Amount
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, Color(0xFFC8E6C9))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("DONATION RECEIVED", fontSize = 10.sp, color = Color.Gray)
                            Text(
                                CurrencyFormatter.format(finalDonation.amount),
                                fontSize = 38.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Payment Method: ${finalDonation.paymentMethod.uppercase()}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Ledger table layout
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Received From:", fontSize = 13.sp, color = Color.Gray)
                            Text(finalDonation.donorName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Phone:", fontSize = 13.sp, color = Color.Gray)
                            Text(finalDonation.phoneNumber ?: "N/A", fontSize = 13.sp)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Purpose Category:", fontSize = 13.sp, color = Color.Gray)
                            Text(finalDonation.category.uppercase(), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                        if (!finalDonation.address.isNullOrEmpty()) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Address:", fontSize = 13.sp, color = Color.Gray)
                                Text(finalDonation.address ?: "", fontSize = 13.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text("Thank You for your Contribution! 🙏", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Recorded securely by: ${finalDonation.receivedByName}", fontSize = 10.sp, color = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons
            Button(
                onClick = {
                    receiptFile?.let {
                        ShareUtils.shareFile(context, it, "ChandaBook Receipt ${finalDonation.receiptNumber}")
                    } ?: viewModel.showError("PDF Receipt could not be loaded")
                },
                modifier = Modifier.fillMaxWidth().height(52.dp).testTag("download_pdf_button"),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, null) // Simple download representation
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("📥 Download PDF Receipt", fontWeight = FontWeight.Bold)
                }
            }

            Button(
                onClick = {
                    receiptFile?.let {
                        val textStr = "Assalam-o-Alaikum! Greetings from *${orgDetails?.name}*.\nWe cordially thank *${finalDonation.donorName}* for the contribution of *${CurrencyFormatter.format(finalDonation.amount)}* toward festival celebrations. Attached is the digital Chanda receipt: No. ${finalDonation.receiptNumber}."
                        ShareUtils.shareViaWhatsApp(context, it, textStr)
                    } ?: viewModel.showError("PDF Receipt could not be read")
                },
                modifier = Modifier.fillMaxWidth().height(52.dp).testTag("whatsapp_share_button"),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Share, null)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("📤 Share via WhatsApp", fontWeight = FontWeight.Bold)
                }
            }

            OutlinedButton(
                onClick = onNavigateBack,
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text("Dismiss & Return Home")
            }
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// 6. DONATIONS LIST SCREEN
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DonationsListScreen(
    viewModel: ChandaBookViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (Donation) -> Unit,
    onNavigateToAdd: () -> Unit
) {
    val context = LocalContext.current
    val query by viewModel.donationSearchQuery.collectAsState()
    val activeCategory by viewModel.donationFilterCategory.collectAsState()
    val activePayment by viewModel.donationFilterPayment.collectAsState()
    val list by viewModel.filteredDonations.collectAsState()

    val categories = listOf("All", "donation", "prasad", "decoration", "sound", "pooja", "other")
    val payments = listOf("All", "cash", "upi", "bank_transfer", "cheque", "online")

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAdd,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, "Add donation")
            }
        }
    ) { p ->
        Column(
            modifier = Modifier.fillMaxSize().padding(p)
        ) {
            TopAppBar(
                title = { Text("Donations (Chanda Ledger)") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary, titleContentColor = Color.White)
            )

            // Search Bar at Top
            OutlinedTextField(
                value = query,
                onValueChange = { viewModel.donationSearchQuery.value = it },
                placeholder = { Text("Search by donor name, phone, receipt no") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Filters horizontal chips
            Text("Purpose Category", fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 16.dp, top = 4.dp))
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { cat ->
                    FilterChip(
                        selected = activeCategory == cat,
                        onClick = { viewModel.donationFilterCategory.value = cat },
                        label = { Text(cat.uppercase()) }
                    )
                }
            }

            Text("Payment Method", fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 16.dp, top = 4.dp))
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                payments.forEach { pay ->
                    FilterChip(
                        selected = activePayment == pay,
                        onClick = { viewModel.donationFilterPayment.value = pay },
                        label = { Text(pay.uppercase()) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Count header
            Text("${list.size} Entries Found", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(horizontal = 16.dp))

            // Donations Grid LazyColumn
            LazyColumn(
                modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (list.isEmpty()) {
                    item {
                        Card(modifier = Modifier.fillMaxWidth().padding(top = 24.dp)) {
                            Column(
                                modifier = Modifier.padding(24.dp).fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("No matching donation records found", fontSize = 13.sp, color = Color.Gray)
                            }
                        }
                    }
                } else {
                    items(list) { donation ->
                        Card(
                            onClick = { onNavigateToDetail(donation) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, Color(0xFFECEFF1))
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(donation.donorName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        SuggestionChip(
                                            onClick = { },
                                            label = { Text(donation.paymentMethod.uppercase(), fontSize = 10.sp) }
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(donation.category.uppercase(), fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                    }
                                    Text("Receipt No. ${donation.receiptNumber} | ${DateUtils.formatDate(donation.createdAt)}", fontSize = 11.sp, color = Color.Gray)
                                }
                                Text(
                                    CurrencyFormatter.format(donation.amount),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// 7. DONATION DETAIL SCREEN
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DonationDetailScreen(
    viewModel: ChandaBookViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToReceipt: () -> Unit
) {
    val donation by viewModel.selectedDonation.collectAsState()
    val context = LocalContext.current
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val finalDonation = donation ?: return

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Ledger Slot Details") },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                }
            },
            actions = {
                IconButton(onClick = { showDeleteConfirm = true }) {
                    Icon(Icons.Default.Delete, "Delete record", tint = Color.White)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary, titleContentColor = Color.White)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE0E0E0))
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("LEDGER RECORD ID", fontSize = 10.sp, color = Color.Gray)
                    Text(finalDonation.id, fontSize = 12.sp, color = Color.Gray, fontFamily = FontFamily.Monospace)

                    Divider()

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Donor Name:", fontWeight = FontWeight.Bold)
                        Text(finalDonation.donorName)
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Contribution Amount:")
                        Text(CurrencyFormatter.format(finalDonation.amount), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Ledger Purpose:")
                        Text(finalDonation.category.uppercase(), fontWeight = FontWeight.SemiBold)
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Payment Mode:")
                        Text(finalDonation.paymentMethod.uppercase())
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Receipt Sequence:")
                        Text(finalDonation.receiptNumber)
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Assigned Address:")
                        Text(finalDonation.address ?: "None Recorded")
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Private Comment:")
                        Text(finalDonation.notes ?: "Empty notes")
                    }

                    Divider()

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Created By Author:")
                        Text(finalDonation.receivedByName)
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Created Timestamp:")
                        Text(DateUtils.formatWithTime(finalDonation.createdAt), fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    viewModel.generateDirectPdfReceipt(finalDonation)
                    onNavigateToReceipt()
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("📄 View / Share Official Receipt", fontWeight = FontWeight.Bold)
            }
        }

        if (showDeleteConfirm) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                title = { Text("Delete Entry") },
                text = { Text("Are you absolutely sure you want to remove this donation record from the ledger book? This process is irreversible.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteDonation(finalDonation)
                            showDeleteConfirm = false
                            onNavigateBack()
                        }
                    ) {
                        Text("DELETE", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirm = false }) {
                        Text("CANCEL")
                    }
                }
            )
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// 8. ADD EXPENSE SCREEN
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseScreen(
    viewModel: ChandaBookViewModel,
    onNavigateBack: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var amountStr by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("decoration") }
    var paymentMethod by remember { mutableStateOf("cash") }
    var notes by remember { mutableStateOf("") }

    val categories = listOf("decoration", "pooja_items", "sound", "prasad", "printing", "transport", "other")
    val payments = listOf("cash", "upi", "bank_transfer", "cheque", "online")

    var categoryExpanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Report Committee Expense") },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary, titleContentColor = Color.White)
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Expense Title / Recipient Name *") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("expense_title_input")
            )

            OutlinedTextField(
                value = amountStr,
                onValueChange = { amountStr = it },
                label = { Text("Payout Amount (₹) *") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                prefix = { Text("₹ ") },
                modifier = Modifier.fillMaxWidth().testTag("expense_amount_input")
            )

            // Category selector
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = category.uppercase(Locale.getDefault()).replace("_", " "),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Expense Sector Category *") },
                    trailingIcon = {
                        IconButton(onClick = { categoryExpanded = true }) {
                            Icon(Icons.Default.KeyboardArrowDown, null)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                DropdownMenu(
                    expanded = categoryExpanded,
                    onDismissRequest = { categoryExpanded = false }
                ) {
                    categories.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat.uppercase().replace("_", " ")) },
                            onClick = {
                                category = cat
                                categoryExpanded = false
                            }
                        )
                    }
                }
            }

            // Payment method selector
            Text("Payment Method", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                payments.forEach { pay ->
                    FilterChip(
                        selected = paymentMethod == pay,
                        onClick = { paymentMethod = pay },
                        label = { Text(pay.uppercase()) }
                    )
                }
            }

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Add specific comments") },
                maxLines = 3,
                modifier = Modifier.fillMaxWidth()
            )

            // Simulated Image Uploader
            Card(
                modifier = Modifier.fillMaxWidth().height(100.dp).clickable {
                    viewModel.showSuccess("Bill Cam attachment simulated successfully")
                },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("📸 Upload Receipt / Invoice Image (Simulated)", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    val amt = amountStr.toDoubleOrNull()
                    if (title.isEmpty()) {
                        viewModel.showError("Expense Title is required")
                        return@Button
                    }
                    if (amt == null || amt <= 0) {
                        viewModel.showError("Enter a valid amount")
                        return@Button
                    }

                    viewModel.addExpense(
                        title = title,
                        amount = amt,
                        category = category,
                        paymentMethod = paymentMethod,
                        notes = notes.ifEmpty { null },
                        billImage = "bill_placeholder_uri_simulated"
                    ) {
                        onNavigateBack()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp).testTag("save_expense_button"),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("💾 Save Expense Record", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// 9. EXPENSES LIST SCREEN
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpensesListScreen(
    viewModel: ChandaBookViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToAdd: () -> Unit
) {
    val query by viewModel.expenseSearchQuery.collectAsState()
    val activeCategory by viewModel.expenseFilterCategory.collectAsState()
    val list by viewModel.filteredExpenses.collectAsState()

    val categories = listOf("All", "decoration", "pooja_items", "sound", "prasad", "printing", "transport", "other")

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAdd,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, "Add Expense")
            }
        }
    ) { p ->
        Column(modifier = Modifier.fillMaxSize().padding(p)) {
            TopAppBar(
                title = { Text("Expenditures Statement") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary, titleContentColor = Color.White)
            )

            // Search box
            OutlinedTextField(
                value = query,
                onValueChange = { viewModel.expenseSearchQuery.value = it },
                placeholder = { Text("Search by bill title, particulars") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Horizontal Category Scroll
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { cat ->
                    FilterChip(
                        selected = activeCategory == cat,
                        onClick = { viewModel.expenseFilterCategory.value = cat },
                        label = { Text(cat.uppercase().replace("_", " ")) }
                    )
                }
            }

            Divider()

            val sumAmount = list.sumOf { it.amount }
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                border = BorderStroke(1.dp, Color(0xFFFFCDD2))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("TOTAL OUTFLOWS", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                    Text(CurrencyFormatter.format(sumAmount), fontSize = 22.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.error)
                }
            }

            Text("${list.size} Payments Settled", fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp))

            // Lazy elements lists
            LazyColumn(
                modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (list.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(30.dp), contentAlignment = Alignment.Center) {
                            Text("No expense records listed.", color = Color.Gray)
                        }
                    }
                } else {
                    items(list) { expense ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, Color(0xFFFAFAFA))
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(expense.title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Text("Purpose: ${expense.category.uppercase().replace("_", " ")}", fontSize = 11.sp, color = Color.Gray)
                                    Text("Mode: ${expense.paymentMethod.uppercase()} | ${DateUtils.formatDate(expense.createdAt)}", fontSize = 11.sp, color = Color.Gray)
                                }
                                Text(
                                    CurrencyFormatter.format(expense.amount),
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// 10. REPORTS SCREEN (Includes Vector Canvas Ring Graphs)
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    viewModel: ChandaBookViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val summaryTotals by viewModel.summary.collectAsState()
    val donations by viewModel.filteredDonations.collectAsState()
    val expenses by viewModel.filteredExpenses.collectAsState()

    val totalDons = summaryTotals?.totalAmount ?: 0.0
    val totalExps = expenses.sumOf { it.amount }
    val liquidCash = totalDons - totalExps

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Financial Analytics Board") },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary, titleContentColor = Color.White)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // General math summaries
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("CONSOLIDATED BOOKS STATUS", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total Donations (Chanda):")
                        Text(CurrencyFormatter.format(totalDons), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total Expenses Passed:")
                        Text(CurrencyFormatter.format(totalExps), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                    }
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Net Reserve Balance:")
                        Text(CurrencyFormatter.format(liquidCash), fontWeight = FontWeight.Black, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            // Visual Pie Chart drawn natively inside Canvas
            Text("DONATION BY CATEGORY DENSITY", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val categories = summaryTotals?.byCategory ?: emptyList()
                    if (categories.isEmpty()) {
                        Text("Not enough data to calculate graphs", color = Color.Gray)
                    } else {
                        // Drawing an elegant donut chart
                        Canvas(modifier = Modifier.size(150.dp)) {
                            var startAngle = 0f
                            val colorsList = listOf(
                                Color(0xFFFF6B35), Color(0xFF2E7D32), Color(0xFFFFD54F),
                                Color(0xFF9E9E9E), Color(0xFFE040FB), Color(0xFF00E5FF)
                            )
                            
                            categories.forEachIndexed { idx, item ->
                                val proportion = if (totalDons > 0) (item.total / totalDons).toFloat() else 0f
                                val sweepArc = proportion * 360f
                                drawArc(
                                    color = colorsList[idx % colorsList.size],
                                    startAngle = startAngle,
                                    sweepAngle = sweepArc,
                                    useCenter = false,
                                    style = Stroke(width = 30f, cap = StrokeCap.Round)
                                )
                                startAngle += sweepArc
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Dynamic Custom Legend
                        categories.forEachIndexed { index, item ->
                            val colorIdx = listOf(
                                Color(0xFFFF6B35), Color(0xFF2E7D32), Color(0xFFFFD54F),
                                Color(0xFF9E9E9E), Color(0xFFE040FB), Color(0xFF00E5FF)
                            )[index % 6]

                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(colorIdx))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(item.category.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                }
                                Text("${CurrencyFormatter.format(item.total)} (${item.count} counts)", fontSize = 11.sp, color = Color.Gray)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action: Export Full PDF Report
            Button(
                onClick = {
                    viewModel.exportLedgerReport { file ->
                        ShareUtils.shareFile(context, file, "ChandaBook Statement")
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp).testTag("export_pdf_report_button"),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, null) // Statement file representation
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("📥 Export & Print Full PDF Report", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// 11. MEMBERS SCREEN
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MembersScreen(
    viewModel: ChandaBookViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val orgDetails by viewModel.activeOrgDetails.collectAsState()
    val members by viewModel.members.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Committee Members Directory") },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary, titleContentColor = Color.White)
        )

        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Invite Section with join code
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("ADD MEMBERS", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
                    Text("Members can join using this invitation code on registration sheets.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val joinCode = orgDetails?.organizationCode ?: "CB-MANDAP-99"
                        Text(
                            joinCode, 
                            fontWeight = FontWeight.Black, 
                            fontSize = 20.sp, 
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.primary
                        )
                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("ChandaBook Code", joinCode)
                                clipboard.setPrimaryClip(clip)
                                viewModel.showSuccess("Invitation Code Copied to clipboard!")
                            },
                            modifier = Modifier.testTag("copy_invite_code_button")
                        ) {
                            Icon(Icons.Default.Share, "Copy code", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            Text("REGISTERED VOLUNTEERS REGISTER", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.Gray)

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (members.isEmpty()) {
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column {
                                    Text("You (Admin)", fontWeight = FontWeight.Bold)
                                    Text("kas242024d@gmail.com", fontSize = 11.sp, color = Color.Gray)
                                }
                                AssistChip(onClick = {}, label = { Text("ADMIN") })
                            }
                        }
                    }
                } else {
                    items(members) { member ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, Color(0xFFFAFAFA))
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(member.displayName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Text(member.email ?: member.phone ?: "No contact details", fontSize = 11.sp, color = Color.Gray)
                                }
                                SuggestionChip(
                                    onClick = {},
                                    label = { Text(member.orgRole.uppercase()) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// 12. SETTINGS SCREEN
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: ChandaBookViewModel,
    onNavigateBack: () -> Unit,
    onLogout: () -> Unit
) {
    val user by viewModel.currentUser.collectAsState()
    var newOrgName by remember { mutableStateOf("") }
    var newOrgDesc by remember { mutableStateOf("") }
    var showCreateOrgDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("System Preferences") },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary, titleContentColor = Color.White)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Profile Card
            Text("ACCOUNT PROFILE", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.Gray)
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(52.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(user?.name?.take(1)?.uppercase(Locale.getDefault()) ?: "C", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(user?.name ?: "Operator Account", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(user?.email ?: user?.phone ?: "operator@chandabook.com", fontSize = 12.sp, color = Color.Gray)
                        Text("Auth Pattern: ${user?.auth_method?.uppercase() ?: "INTEGRATED"}", fontSize = 10.sp, color = Color.Gray)
                    }
                }
            }

            // Organization Builder Card
            Text("COMMITTEE ORGANIZATIONS", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.Gray)
            Button(
                onClick = { showCreateOrgDialog = true },
                modifier = Modifier.fillMaxWidth().height(50.dp).testTag("create_org_trigger_button"),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("➕ Create New Festival Committee Or Mandap", fontWeight = FontWeight.Bold)
            }

            // About section
            Text("LEGAL & DOCUMENTATION", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.Gray)
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Version: 1.0.0 (Custom Build)", fontSize = 13.sp)
                    Text("Backend Connection: ACTIVE (api.chandabook.com)", fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                    Text("Privacy & Security protocol active.", fontSize = 12.sp, color = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Logout Button
            Button(
                onClick = {
                    viewModel.logoutUser {
                        onLogout()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth().height(52.dp).testTag("logout_button")
            ) {
                Text("🚪 Log Out From ChandaBook", fontWeight = FontWeight.Bold, color = Color.White)
            }
        }

        // New Org Input Dialogue
        if (showCreateOrgDialog) {
            AlertDialog(
                onDismissRequest = { showCreateOrgDialog = false },
                title = { Text("Create Committee / Mandap") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = newOrgName,
                            onValueChange = { newOrgName = it },
                            label = { Text("Committee Name (e.g. Lalbaug Mandap)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("new_org_name_input")
                        )
                        OutlinedTextField(
                            value = newOrgDesc,
                            onValueChange = { newOrgDesc = it },
                            label = { Text("Description") },
                            maxLines = 2,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newOrgName.isEmpty()) {
                                viewModel.showError("Name is required")
                                return@Button
                            }
                            viewModel.createNewOrg(newOrgName, newOrgDesc.ifEmpty { null }) {
                                showCreateOrgDialog = false
                            }
                        }
                    ) {
                        Text("Create")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCreateOrgDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
