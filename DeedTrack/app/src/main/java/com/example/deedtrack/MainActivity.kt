package com.example.deedtrack

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.deedtrack.data.ServiceLocator
import com.example.deedtrack.ui.components.MainTab
import com.example.deedtrack.ui.screens.*
import com.example.deedtrack.ui.theme.DeedTrackTheme

class MainActivity : ComponentActivity() {

    /** Application to open when the borrower taps a status notification. */
    private val openApplicationId: MutableState<String?> = mutableStateOf(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ServiceLocator.init(this)
        StatusNotifications.createChannel(this)
        openApplicationId.value = intent.getStringExtra(StatusNotifications.EXTRA_APPLICATION_ID)
        enableEdgeToEdge()
        setContent {
            DeedTrackTheme {
                AgenticLenderApp(
                    openApplicationId = openApplicationId.value,
                    onOpenedFromNotification = { openApplicationId.value = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.getStringExtra(StatusNotifications.EXTRA_APPLICATION_ID)?.let { openApplicationId.value = it }
    }
}

/** Screen addresses used for navigation. */
private object Routes {
    const val LOGIN = "login"
    const val FORGOT = "forgot"
    const val REGISTER = "register"
    const val HOME = "home"
    const val DOCUMENTS = "documents"
    const val EXPLORE = "explore"
    const val PROFILE = "profile"
    const val APPLY = "apply/{step}"
    const val REVIEW = "review"
    const val SUCCESS = "success/{id}"
    const val DETAIL = "detail/{id}"
    const val CHECKLIST = "checklist/{id}"
    fun apply(step: Int) = "apply/$step"
    fun success(id: String) = "success/$id"
    fun detail(id: String) = "detail/$id"
    fun checklist(id: String) = "checklist/$id"
}

/** Go to Home and clear everything before it (after log in / sign up). */
private fun NavHostController.goHomeClearingStack() =
    navigate(Routes.HOME) { popUpTo(graph.id) { inclusive = true } }

private fun NavHostController.backToHome() {
    popBackStack(Routes.HOME, inclusive = false)
}

private fun NavHostController.switchTab(tab: MainTab) {
    when (tab) {
        MainTab.HOME -> backToHome()
        MainTab.EXPLORE -> navigate(Routes.EXPLORE) { popUpTo(Routes.HOME); launchSingleTop = true }
        MainTab.DOCUMENTS -> navigate(Routes.DOCUMENTS) { popUpTo(Routes.HOME); launchSingleTop = true }
        MainTab.PROFILE -> navigate(Routes.PROFILE) { popUpTo(Routes.HOME); launchSingleTop = true }
    }
}

@Composable
fun AgenticLenderApp(
    openApplicationId: String? = null,
    onOpenedFromNotification: () -> Unit = {},
    vm: AppViewModel = viewModel()
) {
    val nav = rememberNavController()
    val context = LocalContext.current
    val user by vm.user.collectAsStateWithLifecycle()
    val applications by vm.applications.collectAsStateWithLifecycle()
    val form by vm.form.collectAsStateWithLifecycle()
    val uploads by vm.uploads.collectAsStateWithLifecycle()
    val refreshing by vm.refreshing.collectAsStateWithLifecycle()

    // Android 13+: ask once for permission to show status-change notifications.
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(user != null) {
        if (user != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !StatusNotifications.canNotify(context)
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Tapping a status notification opens that application (once the borrower is signed in).
    LaunchedEffect(openApplicationId, user, applications) {
        val id = openApplicationId ?: return@LaunchedEffect
        if (user != null && applications.any { it.id == id }) {
            nav.navigate(Routes.detail(id)) { launchSingleTop = true }
            onOpenedFromNotification()
        }
    }

    // When Firebase reports a logged-in user (after log in, sign up, or reopening the app),
    // leave the login screens and go to Home.
    LaunchedEffect(user) {
        val route = nav.currentDestination?.route
        if (user != null && route in setOf(Routes.LOGIN, Routes.REGISTER, Routes.FORGOT)) {
            nav.goHomeClearingStack()
        }
    }

    NavHost(navController = nav, startDestination = Routes.LOGIN) {

        // ---------- 1. Authentication ----------
        composable(Routes.LOGIN) {
            LoginScreen(
                onLogIn = { email, password -> vm.logIn(email, password) },
                onForgotPassword = { nav.navigate(Routes.FORGOT) },
                onCreateAccount = { nav.navigate(Routes.REGISTER) }
            )
        }
        composable(Routes.FORGOT) {
            ForgotPasswordScreen(
                onBack = { nav.popBackStack() },
                onSendReset = { email -> vm.sendPasswordReset(email) }
            )
        }
        composable(Routes.REGISTER) {
            CreateAccountScreen(
                onBack = { nav.popBackStack() },
                onCreate = { profile, password -> vm.register(profile, password) }
            )
        }

        // ---------- 2. Home / 3. Profile / Documents tab ----------
        composable(Routes.HOME) {
            HomeScreen(
                user = user,
                applications = applications,
                refreshing = refreshing,
                onRefresh = vm::refresh,
                onNewApplication = {
                    vm.startNewApplication()
                    nav.navigate(Routes.apply(0))
                },
                onOpenApplication = { id -> nav.navigate(Routes.detail(id)) },
                onTab = { nav.switchTab(it) }
            )
        }
        composable(Routes.PROFILE) {
            ProfileScreen(
                user = user,
                onSave = vm::updateProfile,
                onLogOut = {
                    vm.logOut() // also clears drafts and document files from the phone
                    nav.navigate(Routes.LOGIN) { popUpTo(nav.graph.id) { inclusive = true } }
                },
                onTab = { nav.switchTab(it) }
            )
        }
        composable(Routes.EXPLORE) {
            ExploreScreen(
                listings = vm.listings,
                onApply = { listing ->
                    vm.startApplicationFor(listing)
                    nav.navigate(Routes.apply(0))
                },
                onTab = { nav.switchTab(it) }
            )
        }
        composable(Routes.DOCUMENTS) {
            DocumentChecklistScreen(
                application = applications.firstOrNull(),
                uploads = uploads,
                onAddDocuments = vm::addDocuments,
                onRetry = vm::retryUpload,
                onRemove = vm::removeDocument,
                onTab = { nav.switchTab(it) }
            )
        }

        // ---------- 4. New application flow ----------
        composable(Routes.APPLY, arguments = listOf(navArgument("step") { type = NavType.IntType })) { entry ->
            ApplicationFormScreen(
                initialStep = entry.arguments?.getInt("step") ?: 0,
                form = form,
                valuation = vm.valuation,
                onUpdate = vm::updateForm,
                onExit = { nav.popBackStack() },
                onSaveDraft = {
                    vm.saveDraft()
                    Toast.makeText(context, "Draft saved", Toast.LENGTH_SHORT).show()
                    nav.backToHome()
                },
                onReview = { nav.navigate(Routes.REVIEW) }
            )
        }

        // ---------- 5. Review & submit ----------
        composable(Routes.REVIEW) {
            ReviewSubmitScreen(
                form = form,
                onBack = { nav.popBackStack() },
                onEditStep = { step -> nav.navigate(Routes.apply(step)) { popUpTo(Routes.HOME) } },
                onSubmit = {
                    val app = vm.submitApplication()
                    nav.navigate(Routes.success(app.id)) { popUpTo(Routes.HOME) }
                }
            )
        }

        // ---------- 7. Submission success ----------
        composable(Routes.SUCCESS, arguments = listOf(navArgument("id") { type = NavType.StringType })) { entry ->
            val id = entry.arguments?.getString("id")
            SubmissionSuccessScreen(
                application = applications.firstOrNull { it.id == id },
                onUploadDocuments = { if (id != null) nav.navigate(Routes.checklist(id)) { popUpTo(Routes.HOME) } },
                onBackToApplications = { nav.backToHome() }
            )
        }

        // ---------- 6. Document upload ----------
        composable(Routes.CHECKLIST, arguments = listOf(navArgument("id") { type = NavType.StringType })) { entry ->
            val id = entry.arguments?.getString("id")
            DocumentChecklistScreen(
                application = applications.firstOrNull { it.id == id },
                uploads = uploads,
                onAddDocuments = vm::addDocuments,
                onRetry = vm::retryUpload,
                onRemove = vm::removeDocument,
                onBack = { nav.popBackStack() }
            )
        }

        // ---------- Application tracking ----------
        composable(Routes.DETAIL, arguments = listOf(navArgument("id") { type = NavType.StringType })) { entry ->
            val id = entry.arguments?.getString("id")
            ApplicationDetailScreen(
                application = applications.firstOrNull { it.id == id },
                onBack = { nav.popBackStack() },
                onDocuments = { if (id != null) nav.navigate(Routes.checklist(id)) },
                onSimulateNextStatus = { if (id != null) vm.simulateNextStatus(id) }
            )
        }
    }
}
