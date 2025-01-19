package co.gulf.todvpn.vpn

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.core.view.WindowCompat
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import co.gulf.todvpn.vpn.ui.screens.cities.CitiesScreen
import co.gulf.todvpn.vpn.ui.screens.countries.CountriesScreen
import co.gulf.todvpn.vpn.ui.screens.dashboard.DashboardScreen
import co.gulf.todvpn.vpn.ui.screens.settings.SettingsScreen
import co.gulf.todvpn.vpn.ui.screens.split_tunneling.SplitTunnelingScreenScreen
import co.gulf.todvpn.vpn.ui.screens.ActivationScreen  // إضافة شاشة التفعيل
import co.gulf.todvpn.vpn.ui.theme.BasedVPNTheme
import dagger.hilt.android.AndroidEntryPoint
import io.norselabs.vpn.common_logger.logger.FileLogTree
import io.norselabs.vpn.common_logger.share.ChooserIntent
import com.google.firebase.auth.FirebaseAuth
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

  @Inject
  lateinit var fileLogTree: FileLogTree

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setFullScreen()
    setContent {
      BasedVPNTheme {
        val navController = rememberNavController()
        val currentUser = FirebaseAuth.getInstance().currentUser

        // التحقق من وجود معلومات المستخدم وحالة التفعيل
        val isActivated = getActivationStatus()

        // تحديد الوجهة الابتدائية بناءً على حالة التفعيل أو التجربة
        NavHost(
          navController = navController,
          startDestination = if (currentUser == null || !isActivated) Destination.Activation else Destination.Dashboard,
          enterTransition = { EnterTransition.None },
          exitTransition = { ExitTransition.None },
        ) {
          composable(Destination.Dashboard) {
            DashboardScreen(
              navigateToCountries = { navController.navigate(Destination.Countries) },
              navigateToSettings = { navController.navigate(Destination.Settings) },
            )
          }
          composable(Destination.Activation) {
            ActivationScreen(navController = navController, onActivationComplete = {
              // الانتقال إلى الداشبورد عند الانتهاء من التفعيل
              navController.navigate("dashboard") {
                popUpTo("activation") { inclusive = true } // حذف شاشة التفعيل من الـBackStack
              }
            })
          }


          composable(Destination.Countries) {
            CountriesScreen(
              navigateBack = { navController.popBackStack() },
              navigateToCities = { countryId ->
                navController.navigate("countries/$countryId/cities")
              },
            )
          }
          composable(
            route = Destination.Cities,
            arguments = listOf(navArgument(Args.CountryId) { type = NavType.StringType }),
          ) { backStackEntry ->
            CitiesScreen(
              countryId = backStackEntry.arguments?.getString(Args.CountryId),
              navigateBack = { navController.popBackStack() },
              navigateBackToRoot = {
                navController.popBackStack(
                  route = Destination.Dashboard,
                  inclusive = false,
                )
              },
            )
          }
          composable(Destination.Settings) {
            SettingsScreen(
              navigateBack = { navController.popBackStack() },
              navigateToSplitTunneling = { navController.navigate(Destination.SplitTunneling) },
              shareLogs = ::shareLogs,
            )
          }
          composable(Destination.SplitTunneling) {
            SplitTunnelingScreenScreen(
              navigateBack = { navController.popBackStack() },
            )
          }
        }
      }
    }
  }

  private fun shareLogs() {
    val file = fileLogTree.getLogsFile() ?: return
    ChooserIntent.start(this, BuildConfig.APPLICATION_ID, file)
  }

  private fun getActivationStatus(): Boolean {
    // استرجاع حالة التفعيل من SharedPreferences أو قاعدة بيانات Firebase
    val sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)
    return sharedPreferences.getBoolean("isActivated", false)
  }

  private fun setActivationStatus(isActivated: Boolean) {
    // حفظ حالة التفعيل في SharedPreferences
    val sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)
    sharedPreferences.edit().putBoolean("isActivated", isActivated).apply()
  }
}

fun ComponentActivity.setFullScreen() {
  WindowCompat.setDecorFitsSystemWindows(window, false)
}

object Destination {
  const val Dashboard = "dashboard"
  const val Countries = "countries"
  const val Cities = "countries/{${Args.CountryId}}/cities"
  const val Settings = "settings"
  const val SplitTunneling = "splitTunnel"
  const val Activation = "activation"  // إضافة شاشة التفعيل
}

object Args {
  const val CountryId = "countryId"
}
