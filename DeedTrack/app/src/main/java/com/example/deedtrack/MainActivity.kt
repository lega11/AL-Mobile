package com.example.deedtrack

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.deedtrack.data.PhotoStore
import com.example.deedtrack.ui.components.MainTab
import com.example.deedtrack.ui.screens.*
import com.example.deedtrack.ui.theme.DeedTrackTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DeedTrackTheme { AgenticLenderApp() }
        }
    }
}

/** Screen addresses used for navigation. */
private object Routes {
    const val LOGIN = "login"
    const val FORGOT = "forgot"
    const val REGISTER = "register"
    const val HOME = "home"
    const val DOCUMENTS = "documents"
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
        MainTab.DOCUMENTS -> navigate(Routes.DOCUMENTS) { popUpTo(Routes.HOME); launchSingleTop = true }
        MainTab.PROFILE -> navigate(Routes.PROFILE) { popUpTo(Routes.HOME); launchSingleTop = true }
    }
}

@Composable
fun AgenticLenderApp(vm: AppViewModel = viewModel()) {
    val nav = rememberNavController()
    val context = LocalContext.current
    val user by vm.user.collectAsStateWithLifecycle()
    val applications by vm.applications.collectAsStateWithLifecycle()
    val form by vm.form.collectAsStateWithLifecycle()

    NavHost(navController = nav, startDestination = Routes.LOGIN) {

        // ---------- 1. Authentication ----------
        composable(Routes.LOGIN) {
            LoginScreen(
                onLogIn = { email, password ->
                    val error = vm.logIn(email, password)
                    if (error == null) nav.goHomeClearingStack()
                    error
                },
                onForgotPassword = { nav.navigate(Routes.FORGOT) },
                onCreateAccount = { nav.navigate(Routes.REGISTER) }
            )
        }
        composable(Routes.FORGOT) {
            ForgotPasswordScreen(onBack = { nav.popBackStack() })
        }
        composable(Routes.REGISTER) {
            CreateAccountScreen(
                onBack = { nav.popBackStack() },
                onCreate = { profile, password ->
                    vm.register(profile, password)
                    nav.goHomeClearingStack()
                }
            )
        }

        // ---------- 2. Home / 3. Profile / Documents tab ----------
        composable(Routes.HOME) {
            HomeScreen(
                user = user,
                applications = applications,
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
                    vm.logOut()
                    PhotoStore.clear(context)
                    nav.navigate(Routes.LOGIN) { popUpTo(nav.graph.id) { inclusive = true } }
                },
                onTab = { nav.switchTab(it) }
            )
        }
        composable(Routes.DOCUMENTS) {
            DocumentChecklistScreen(
                application = applications.firstOrNull(),
                onAddDocuments = vm::addDocuments,
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
                onAddDocuments = vm::addDocuments,
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
