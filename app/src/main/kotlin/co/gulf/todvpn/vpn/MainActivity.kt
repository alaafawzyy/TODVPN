package co.gulf.todvpn.vpn

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.google.firebase.Firebase
import dagger.hilt.android.AndroidEntryPoint
import io.norselabs.vpn.common_logger.logger.FileLogTree
import io.norselabs.vpn.common_logger.share.ChooserIntent
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore
import java.util.Date
import javax.inject.Inject
import kotlinx.coroutines.tasks.await

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
        var isLoading by remember { mutableStateOf(true) }
        var isActivated by remember { mutableStateOf(false) }


        LaunchedEffect(Unit) {
          isActivated = getActivationStatus()
          isLoading = false
          Log.e("Activation Status", "isActivated: $isActivated")
        }


        if (isLoading) {
          SplashScreen()
        } else {

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
                navController.navigate(Destination.Dashboard) {
                  popUpTo(Destination.Activation) { inclusive = true }
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
  }

  // شاشة مؤقتة
  @Composable
  fun SplashScreen() {
    Box(
      modifier = Modifier.fillMaxSize(),
      contentAlignment = Alignment.Center
    ) {
      Text("Loading...", style = MaterialTheme.typography.bodyLarge)
    }
  }

  private suspend fun getActivationStatus(): Boolean {
    return try {
      // الحصول على معرّف المستخدم الحالي
      val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
      if (userId.isEmpty()) {
        return false  // المستخدم غير مسجل
      }

      // الحصول على بيانات المستخدم
      val userDocumentSnapshot = Firebase.firestore.collection("users")
        .document(userId)
        .get()
        .await()

      // التحقق من فترة التجربة
      val trialActive = userDocumentSnapshot.getBoolean("trialActive") ?: false
      val activationEndDate = userDocumentSnapshot.getTimestamp("activationEndDate")?.toDate()
      val isTrialValid = trialActive && (activationEndDate?.after(Date()) == true)

      // إذا كانت فترة التجربة ما زالت نشطة، يُسمح باستخدام التطبيق
      if (isTrialValid) {
        return true
      }

      // إذا انتهت فترة التجربة، يتم التحقق من كود التفعيل الخاص بالمستخدم الحالي
      val codeDocumentSnapshot = Firebase.firestore.collection("codes")
        .whereEqualTo("userId", userId)
        .get()
        .await()

      val currentDate = Date()
      val isCodeValid = codeDocumentSnapshot.size() > 0 &&
        codeDocumentSnapshot.documents[0].getDate("EndDate")!!.after(currentDate)

      // إذا كان الكود صالحًا، يتم تحديث حالة المستخدم
      if (isCodeValid) {
        Firebase.firestore.collection("users").document(userId).update(
          "trialActive", true,
          "activationEndDate", codeDocumentSnapshot.documents[0].getDate("EndDate")
        ).await()
      }

      isCodeValid
    } catch (e: Exception) {
      Log.e("getActivationStatus", "Error fetching activation status", e)
      false
    }
  }


  private fun shareLogs() {
    val file = fileLogTree.getLogsFile() ?: return
    ChooserIntent.start(this, BuildConfig.APPLICATION_ID, file)
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
