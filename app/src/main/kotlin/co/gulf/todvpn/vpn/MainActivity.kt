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

        // التحقق من حالة المستخدم عند بدء التطبيق
        val currentUser = FirebaseAuth.getInstance().currentUser

        // تحديد الوجهة الابتدائية بناءً على حالة المستخدم
        NavHost(
          navController = navController,
          startDestination = if (currentUser == null) Destination.Activation else Destination.Dashboard,  // التحقق من حالة تسجيل الدخول
          enterTransition = { EnterTransition.None },
          exitTransition = { ExitTransition.None },
        ) {
          composable(Destination.Dashboard) {
            DashboardScreen(
              navigateToCountries = { navController.navigate(Destination.Countries) },
              navigateToSettings = { navController.navigate(Destination.Settings) },
            )
          }
          composable(Destination.Activation) {  // شاشة التفعيل
            ActivationScreen()
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




























//package co.gulf.todvpn.vpn
//
//import android.os.Bundle
//import androidx.activity.ComponentActivity
//import androidx.activity.compose.setContent
//import androidx.appcompat.app.AppCompatActivity
//import androidx.compose.animation.EnterTransition
//import androidx.compose.animation.ExitTransition
//import androidx.core.view.WindowCompat
//import androidx.navigation.NavType
//import androidx.navigation.compose.NavHost
//import androidx.navigation.compose.composable
//import androidx.navigation.compose.rememberNavController
//import androidx.navigation.navArgument
//import co.gulf.todvpn.vpn.ui.screens.cities.CitiesScreen
//import co.gulf.todvpn.vpn.ui.screens.countries.CountriesScreen
//import co.gulf.todvpn.vpn.ui.screens.dashboard.DashboardScreen
//import co.gulf.todvpn.vpn.ui.screens.settings.SettingsScreen
//import co.gulf.todvpn.vpn.ui.screens.split_tunneling.SplitTunnelingScreenScreen
//import co.gulf.todvpn.vpn.ui.theme.BasedVPNTheme
//import dagger.hilt.android.AndroidEntryPoint
//import io.norselabs.vpn.common_logger.logger.FileLogTree
//import io.norselabs.vpn.common_logger.share.ChooserIntent
//import javax.inject.Inject
//
//@AndroidEntryPoint
//class MainActivity : AppCompatActivity() {
//
//  @Inject
//  lateinit var fileLogTree: FileLogTree
//
//  override fun onCreate(savedInstanceState: Bundle?) {
//    super.onCreate(savedInstanceState)
//    setFullScreen()
//    setContent {
//      BasedVPNTheme {
//        val navController = rememberNavController()
//        NavHost(
//          navController = navController,
//          startDestination = Destination.Dashboard,
//          enterTransition = { EnterTransition.None },
//          exitTransition = { ExitTransition.None },
//        ) {
//          composable(Destination.Dashboard) {
//            DashboardScreen(
//              navigateToCountries = { navController.navigate(Destination.Countries) },
//              navigateToSettings = { navController.navigate(Destination.Settings) },
//            )
//          }
//          composable(Destination.Countries) {
//            CountriesScreen(
//              navigateBack = { navController.popBackStack() },
//              navigateToCities = { countryId ->
//                navController.navigate("countries/$countryId/cities")
//              },
//            )
//          }
//          composable(
//            route = Destination.Cities,
//            arguments = listOf(navArgument(Args.CountryId) { type = NavType.StringType }),
//          ) { backStackEntry ->
//            CitiesScreen(
//              countryId = backStackEntry.arguments?.getString(Args.CountryId),
//              navigateBack = { navController.popBackStack() },
//              navigateBackToRoot = {
//                navController.popBackStack(
//                  route = Destination.Dashboard,
//                  inclusive = false,
//                )
//              },
//            )
//          }
//          composable(Destination.Settings) {
//            SettingsScreen(
//              navigateBack = { navController.popBackStack() },
//              navigateToSplitTunneling = { navController.navigate(Destination.SplitTunneling) },
//              shareLogs = ::shareLogs,
//            )
//          }
//          composable(Destination.SplitTunneling) {
//            SplitTunnelingScreenScreen(
//              navigateBack = { navController.popBackStack() },
//            )
//          }
//        }
//      }
//    }
//  }
//
//  private fun shareLogs() {
//    val file = fileLogTree.getLogsFile() ?: return
//    ChooserIntent.start(this, BuildConfig.APPLICATION_ID, file)
//  }
//}
//
//fun ComponentActivity.setFullScreen() {
//  WindowCompat.setDecorFitsSystemWindows(window, false)
//}
//
//object Destination {
//  const val Dashboard = "dashboard"
//  const val Countries = "countries"
//  const val Cities = "countries/{${Args.CountryId}}/cities"
//  const val Settings = "settings"
//  const val SplitTunneling = "splitTunnel"
//}
//
//object Args {
//  const val CountryId = "countryId"
//}
