package co.gulf.todvpn.vpn

import android.content.Context
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.google.firebase.Timestamp
import dagger.hilt.android.AndroidEntryPoint
import io.norselabs.vpn.common_logger.logger.FileLogTree
import io.norselabs.vpn.common_logger.share.ChooserIntent
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import java.util.Calendar
import java.util.Date
import java.util.TimeZone
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.launch
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

        val coroutineScope = rememberCoroutineScope()
        LaunchedEffect(Unit) {
          try {
            val deviceId = co.gulf.todvpn.vpn.getDeviceId(this@MainActivity)
            Log.d("Activation", "معرف الجهاز: $deviceId")

            val userId = checkDeviceId(deviceId)
            isActivated = getActivationStatus(userId, deviceId)

            Log.d("Activation", "حالة التفعيل الأولية: $isActivated")
          } catch (e: Exception) {
            Log.e("Activation", "خطأ في التحقق الأولي: ${e.message}")
            isActivated = false
          } finally {
            isLoading = false
          }
        }

        if (isLoading) {
          SplashScreen()
        } else {
          NavHost(
            navController = navController,
            startDestination = if (isActivated) Destination.Dashboard else Destination.Activation,
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
                ActivationScreen(
                  navController = navController,
                  onActivationSuccess = {
                    // الانتقال إلى الشاشة الرئيسية بعد التفعيل
                    navController.navigate(Destination.Dashboard)
                  }
                )
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

  @Composable
  fun SplashScreen() {
    Box(
      modifier = Modifier.fillMaxSize(),
      contentAlignment = Alignment.Center
    ) {
      Text("Loading...", style = MaterialTheme.typography.bodyLarge)
    }
  }

  private suspend fun checkDeviceId(deviceId: String): String {
    val usersRef = Firebase.firestore.collection("users")
    val query = usersRef.whereEqualTo("deviceId", deviceId).get().await()

    return if (query.isEmpty) {
      val newUser = hashMapOf(
        "deviceId" to deviceId,
        "trialActive" to true,
        "startDate" to Timestamp.now(),
        "endDate" to java.sql.Timestamp(Date().time + 7 * 24 * 3600 * 1000)
      )
      val newUserRef = usersRef.add(newUser).await()
      newUserRef.id
    } else {
      query.documents.first().id
    }
  }

  private suspend fun getActivationStatus(userId: String, deviceId: String): Boolean {
    return try {
      if (userId.isEmpty()) return false

      val userDoc = Firebase.firestore.collection("users")
        .document(userId)
        .get()
        .await()

      if (!userDoc.exists()) return false

      val trialActive = userDoc.getBoolean("trialActive") ?: false
      val endDate = userDoc.getTimestamp("endDate")?.toDate()

      if (trialActive && endDate?.after(Date()) == true) {
        return true
      }

      if (endDate?.before(Date()) == true) {
        userDoc.reference.update("trialActive", false).await()
      }

      checkValidCodes(deviceId)
    } catch (e: Exception) {
      Log.e("getActivationStatus", "Error: ${e.message}")
      false
    }
  }

  private suspend fun checkValidCodes(deviceId: String): Boolean {
    return try {
      val codesQuery = Firebase.firestore.collection("codes")
        .whereEqualTo("deviceId", deviceId)
        .get()
        .await()

      var isValid = false
      for (document in codesQuery) {
        val endDate = document.getTimestamp("endDate")?.toDate()
        if (endDate != null && endDate.after(Date())) {

          isValid = true
        } else {
          document.reference.delete().await()
        }
      }
      isValid
    } catch (e: Exception) {
      Log.e("checkValidCodes", "Error: ${e.message}")
      false
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
  const val Activation = "activation"
}

object Args {
  const val CountryId = "countryId"
}

fun getDeviceId(context: Context): String {
  return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
}





//
//package co.gulf.todvpn.vpn
//
//import android.content.Context
//import android.os.Bundle
//import android.provider.Settings
//import android.util.Log
//import androidx.activity.ComponentActivity
//import androidx.activity.compose.setContent
//import androidx.appcompat.app.AppCompatActivity
//import androidx.compose.animation.EnterTransition
//import androidx.compose.animation.ExitTransition
//import androidx.compose.foundation.layout.Box
//import androidx.compose.foundation.layout.fillMaxSize
//import androidx.compose.material3.MaterialTheme
//import androidx.compose.material3.Text
//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.LaunchedEffect
//import androidx.compose.runtime.getValue
//import androidx.compose.runtime.mutableStateOf
//import androidx.compose.runtime.remember
//import androidx.compose.runtime.rememberCoroutineScope
//import androidx.compose.runtime.setValue
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
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
//import co.gulf.todvpn.vpn.ui.screens.ActivationScreen  // إضافة شاشة التفعيل
//import co.gulf.todvpn.vpn.ui.theme.BasedVPNTheme
//import com.google.firebase.Firebase
//import com.google.firebase.Timestamp
//import dagger.hilt.android.AndroidEntryPoint
//import io.norselabs.vpn.common_logger.logger.FileLogTree
//import io.norselabs.vpn.common_logger.share.ChooserIntent
//import com.google.firebase.auth.FirebaseAuth
//import com.google.firebase.firestore.FieldValue
//import com.google.firebase.firestore.FirebaseFirestore
//import com.google.firebase.firestore.firestore
//import java.util.Calendar
//import java.util.Date
//import java.util.TimeZone
//import java.util.UUID
//import javax.inject.Inject
//import kotlinx.coroutines.launch
//import kotlinx.coroutines.tasks.await
//
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
//        val currentUser = FirebaseAuth.getInstance().currentUser
//        var isLoading by remember { mutableStateOf(true) }
//        var isActivated by remember { mutableStateOf(false) } // تأكيد استخدام mutableStateOf
//
//        val coroutineScope = rememberCoroutineScope()
//        LaunchedEffect(Unit) {
//          try {
//            val deviceId = getDeviceIdString()
//            Log.d("Activation", "معرف الجهاز: $deviceId")
//
//            val userId = checkDeviceId(deviceId)
//            isActivated = getActivationStatus(userId)
//
//            Log.d("Activation", "حالة التفعيل الأولية: $isActivated")
//          } catch (e: Exception) {
//            Log.e("Activation", "خطأ في التحقق الأولي: ${e.message}")
//            isActivated = false
//          } finally {
//            isLoading = false
//          }
//        }
//
//        if (isLoading) {
//          SplashScreen()
//        } else {
//          NavHost(
//            navController = navController,
//            startDestination = if (isActivated) {
//              Destination.Dashboard
//            } else {
//              Destination.Activation},
//
////            startDestination = if (!isActivated) {
////              Destination.Activation
////            } else Destination.Dashboard,
//            enterTransition = { EnterTransition.None },
//            exitTransition = { ExitTransition.None },
//          ) {
//            composable(Destination.Dashboard) {
//              DashboardScreen(
//                navigateToCountries = { navController.navigate(Destination.Countries) },
//                navigateToSettings = { navController.navigate(Destination.Settings) },
//              )
//            }
//            composable(Destination.Activation) {
//              ActivationScreen(
//                navController = navController,
//                onActivationComplete = { durationDays ->
//                  if (durationDays > 0) {
//                    // مباشرةً قم بتحديث الحالة والانتقال دون إعادة التحقق
//                    isActivated = true
//                    navController.navigate(Destination.Dashboard) {
//                      popUpTo(Destination.Activation) { inclusive = true }
//                    }
//                  } else {
//                    Log.d("ActivationFlow", "فشل التفعيل")
//                  }
//                }
//              )
//            }
//
//            composable(Destination.Countries) {
//              CountriesScreen(
//                navigateBack = { navController.popBackStack() },
//                navigateToCities = { countryId ->
//                  navController.navigate("countries/$countryId/cities")
//                },
//              )
//            }
//            composable(
//              route = Destination.Cities,
//              arguments = listOf(navArgument(Args.CountryId) { type = NavType.StringType }),
//            ) { backStackEntry ->
//              CitiesScreen(
//                countryId = backStackEntry.arguments?.getString(Args.CountryId),
//                navigateBack = { navController.popBackStack() },
//                navigateBackToRoot = {
//                  navController.popBackStack(
//                    route = Destination.Dashboard,
//                    inclusive = false,
//                  )
//                },
//              )
//            }
//            composable(Destination.Settings) {
//              SettingsScreen(
//                navigateBack = { navController.popBackStack() },
//                navigateToSplitTunneling = { navController.navigate(Destination.SplitTunneling) },
//                shareLogs = ::shareLogs,
//              )
//            }
//            composable(Destination.SplitTunneling) {
//              SplitTunnelingScreenScreen(
//                navigateBack = { navController.popBackStack() },
//              )
//            }
//          }
//        }
//      }
//    }
//  }
//
//  // شاشة مؤقتة
//  @Composable
//  fun SplashScreen() {
//    Box(
//      modifier = Modifier.fillMaxSize(),
//      contentAlignment = Alignment.Center
//    ) {
//      Text("Loading...", style = MaterialTheme.typography.bodyLarge)
//    }
//  }
//  private suspend fun getActivationStatus(userId: String): Boolean {
//    return try {
//      val deviceId = getDeviceIdString()
//      val activationSnapshot = FirebaseFirestore.getInstance()
//        .collection("codes")
//        .whereEqualTo("deviceId", deviceId)
//        .whereGreaterThanOrEqualTo("endDate", Timestamp.now())
//        .get()
//        .await()
//
//      !activationSnapshot.isEmpty
//    } catch (e: Exception) {
//      Log.e("Activation", "خطأ في التحقق: ${e.message}")
//      false
//    }
//  }
////  private suspend fun getActivationStatus(userId: String): Boolean {
////    return try {
////      if (userId.isEmpty()) return false
////
////      val userDoc = Firebase.firestore.collection("users")
////        .document(userId)
////        .get()
////        .await()
////
////      if (!userDoc.exists()) return false
////
////      val trialActive = userDoc.getBoolean("trialActive") ?: false
////      val endDate = userDoc.getTimestamp("endDate")?.toDate()
////
////      // إذا كانت الفترة التجريبية نشطة ولم تنته
////      if (trialActive && endDate?.after(Date()) == true) {
////        return true
////      }
////
////      // إذا انتهت الفترة التجريبية، تحديث الحالة
////      if (endDate?.before(Date()) == true) {
////        userDoc.reference.update("trialActive", false).await()
////      }
////
////      // التحقق من وجود كود صالح
////      checkValidCodes(userId)
////    } catch (e: Exception) {
////      Log.e("getActivationStatus", "Error: ${e.message}")
////      false
////    }
////  }
//
//
//  private suspend fun checkValidCodes(userId: String): Boolean {
//    val codes = Firebase.firestore.collection("codes")
//      .whereEqualTo("userId", userId)
//      .get()
//      .await()
//
//    val now = Date()
//    for (code in codes.documents) {
//      val codeEndDate = code.getTimestamp("EndDate")?.toDate()
//      if (codeEndDate?.after(now) == true) return true
//      else code.reference.delete().await()
//    }
//    return false
//  }
//
//  private fun getDeviceIdString(): String {
//    return try {
//      Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
//        ?: UUID.randomUUID().toString().take(16)
//    } catch (e: Exception) {
//      UUID.randomUUID().toString().take(16)
//    }
//  }
//  private suspend fun checkDeviceId(deviceId: String): String {
//    return try {
//      val querySnapshot = Firebase.firestore.collection("users")
//        .whereEqualTo("deviceId", deviceId)
//        .get()
//        .await()
//
//      if (querySnapshot.isEmpty) {
//        val userId = Firebase.firestore.collection("users").document().id
//        val startDate = Calendar.getInstance(TimeZone.getTimeZone("UTC")).time
//        val endDate = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
//          add(Calendar.DAY_OF_YEAR, 7)
//        }.time
//
//        val user = hashMapOf(
//          "userId" to userId,
//          "startDate" to Timestamp(startDate),
//          "endDate" to Timestamp(endDate),
//          "deviceId" to deviceId,
//          "trialActive" to true
//        )
//
//        Firebase.firestore.collection("users")
//          .document(userId)
//          .set(user)
//          .await()
//
//        Log.d("NewUser", "تم إنشاء مستخدم جديد مع فترة تجريبية حتى $endDate")
//        userId
//      } else {
//        querySnapshot.documents.first().id
//      }
//    } catch (e: Exception) {
//      Log.e("checkDeviceId", "Error: ${e.message}")
//      ""
//    }
//  }
//
//
//
//
//
//  private fun shareLogs() {
//    val file = fileLogTree.getLogsFile() ?: return
//    ChooserIntent.start(this, BuildConfig.APPLICATION_ID, file)
//  }
//
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
//  const val Activation = "activation"  // إضافة شاشة التفعيل
//}
//
//object Args {
//  const val CountryId = "countryId"
//}
//fun getDeviceId(context: Context): String {
//  return android.provider.Settings.Secure.getString(context.contentResolver, android.provider.Settings.Secure.ANDROID_ID)
//}
//
//




/*

package co.gulf.todvpn.vpn

import android.content.Context
import android.os.Bundle
import android.provider.Settings
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.google.firebase.Timestamp
import dagger.hilt.android.AndroidEntryPoint
import io.norselabs.vpn.common_logger.logger.FileLogTree
import io.norselabs.vpn.common_logger.share.ChooserIntent
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import java.util.Calendar
import java.util.Date
import java.util.TimeZone
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.launch
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
        var isActivated by remember { mutableStateOf(false) } // تأكيد استخدام mutableStateOf

        val coroutineScope = rememberCoroutineScope()
        LaunchedEffect(Unit) {
          try {
            val deviceId = getDeviceIdString()
            Log.d("Activation", "معرف الجهاز: $deviceId")

            val userId = checkDeviceId(deviceId)
            isActivated = getActivationStatus(userId)

            Log.d("Activation", "حالة التفعيل الأولية: $isActivated")
          } catch (e: Exception) {
            Log.e("Activation", "خطأ في التحقق الأولي: ${e.message}")
            isActivated = false
          } finally {
            isLoading = false
          }
        }

        if (isLoading) {
          SplashScreen()
        } else {
          NavHost(
            navController = navController,
            startDestination = if (isActivated) {
              Destination.Dashboard
            } else {
              Destination.Activation},

//            startDestination = if (!isActivated) {
//              Destination.Activation
//            } else Destination.Dashboard,
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
              ActivationScreen(
                navController = navController,
                onActivationComplete = { durationDays ->
                  if (durationDays > 0) {
                    // مباشرةً قم بتحديث الحالة والانتقال دون إعادة التحقق
                    isActivated = true
                    navController.navigate(Destination.Dashboard) {
                      popUpTo(Destination.Activation) { inclusive = true }
                    }
                  } else {
                    Log.d("ActivationFlow", "فشل التفعيل")
                  }
                }
              )
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
  private suspend fun getActivationStatus(userId: String): Boolean {
    return try {
      val deviceId = getDeviceIdString()
      val activationSnapshot = FirebaseFirestore.getInstance()
        .collection("codes")
        .whereEqualTo("deviceId", deviceId)
        .whereGreaterThanOrEqualTo("endDate", Timestamp.now())
        .get()
        .await()

      !activationSnapshot.isEmpty
    } catch (e: Exception) {
      Log.e("Activation", "خطأ في التحقق: ${e.message}")
      false
    }
  }
//  private suspend fun getActivationStatus(userId: String): Boolean {
//    return try {
//      if (userId.isEmpty()) return false
//
//      val userDoc = Firebase.firestore.collection("users")
//        .document(userId)
//        .get()
//        .await()
//
//      if (!userDoc.exists()) return false
//
//      val trialActive = userDoc.getBoolean("trialActive") ?: false
//      val endDate = userDoc.getTimestamp("endDate")?.toDate()
//
//      // إذا كانت الفترة التجريبية نشطة ولم تنته
//      if (trialActive && endDate?.after(Date()) == true) {
//        return true
//      }
//
//      // إذا انتهت الفترة التجريبية، تحديث الحالة
//      if (endDate?.before(Date()) == true) {
//        userDoc.reference.update("trialActive", false).await()
//      }
//
//      // التحقق من وجود كود صالح
//      checkValidCodes(userId)
//    } catch (e: Exception) {
//      Log.e("getActivationStatus", "Error: ${e.message}")
//      false
//    }
//  }


  private suspend fun checkValidCodes(userId: String): Boolean {
    val codes = Firebase.firestore.collection("codes")
      .whereEqualTo("userId", userId)
      .get()
      .await()

    val now = Date()
    for (code in codes.documents) {
      val codeEndDate = code.getTimestamp("EndDate")?.toDate()
      if (codeEndDate?.after(now) == true) return true
      else code.reference.delete().await()
    }
    return false
  }

  private fun getDeviceIdString(): String {
    return try {
      Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
        ?: UUID.randomUUID().toString().take(16)
    } catch (e: Exception) {
      UUID.randomUUID().toString().take(16)
    }
  }
  private suspend fun checkDeviceId(deviceId: String): String {
    return try {
      val querySnapshot = Firebase.firestore.collection("users")
        .whereEqualTo("deviceId", deviceId)
        .get()
        .await()

      if (querySnapshot.isEmpty) {
        val userId = Firebase.firestore.collection("users").document().id
        val startDate = Calendar.getInstance(TimeZone.getTimeZone("UTC")).time
        val endDate = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
          add(Calendar.DAY_OF_YEAR, 7)
        }.time

        val user = hashMapOf(
          "userId" to userId,
          "startDate" to Timestamp(startDate),
          "endDate" to Timestamp(endDate),
          "deviceId" to deviceId,
          "trialActive" to true
        )

        Firebase.firestore.collection("users")
          .document(userId)
          .set(user)
          .await()

        Log.d("NewUser", "تم إنشاء مستخدم جديد مع فترة تجريبية حتى $endDate")
        userId
      } else {
        querySnapshot.documents.first().id
      }
    } catch (e: Exception) {
      Log.e("checkDeviceId", "Error: ${e.message}")
      ""
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
fun getDeviceId(context: Context): String {
  return android.provider.Settings.Secure.getString(context.contentResolver, android.provider.Settings.Secure.ANDROID_ID)
}



 */


//
//package co.gulf.todvpn.vpn
//
//import android.content.Context
//import android.os.Bundle
//import android.util.Log
//import androidx.activity.ComponentActivity
//import androidx.activity.compose.setContent
//import androidx.appcompat.app.AppCompatActivity
//import androidx.compose.animation.EnterTransition
//import androidx.compose.animation.ExitTransition
//import androidx.compose.foundation.layout.Box
//import androidx.compose.foundation.layout.fillMaxSize
//import androidx.compose.material3.MaterialTheme
//import androidx.compose.material3.Text
//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.DisposableEffect
//import androidx.compose.runtime.getValue
//import androidx.compose.runtime.mutableStateOf
//import androidx.compose.runtime.remember
//import androidx.compose.runtime.rememberCoroutineScope
//import androidx.compose.runtime.setValue
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
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
//import co.gulf.todvpn.vpn.ui.screens.ActivationScreen  // إضافة شاشة التفعيل
//import co.gulf.todvpn.vpn.ui.theme.BasedVPNTheme
//import com.google.firebase.Firebase
//import dagger.hilt.android.AndroidEntryPoint
//import io.norselabs.vpn.common_logger.logger.FileLogTree
//import io.norselabs.vpn.common_logger.share.ChooserIntent
//import com.google.firebase.auth.FirebaseAuth
//import com.google.firebase.firestore.firestore
//import java.util.Date
//import javax.inject.Inject
//import kotlinx.coroutines.launch
//import kotlinx.coroutines.tasks.await
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
//        val currentUser = FirebaseAuth.getInstance().currentUser
//        var isLoading by remember { mutableStateOf(true) }
//        var isActivated by remember { mutableStateOf(false) }
//
//
//        val coroutineScope = rememberCoroutineScope()
//        DisposableEffect(Unit) {
//          val job = coroutineScope.launch {
//            isActivated = getActivationStatus()
//            isLoading = false
//            Log.e("Activation Status", "isActivated: $isActivated")
//          }
//          onDispose { job.cancel() }
//        }
//
//        if (isLoading) {
//          SplashScreen()
//        } else {
//          NavHost(
//            navController = navController,
//            startDestination = if (currentUser == null || !isActivated){
//              Destination.Activation
//
//            } else Destination.Dashboard,
//            enterTransition = { EnterTransition.None },
//            exitTransition = { ExitTransition.None },
//          ) {
//            composable(Destination.Dashboard) {
//              DashboardScreen(
//                navigateToCountries = { navController.navigate(Destination.Countries) },
//                navigateToSettings = { navController.navigate(Destination.Settings) },
//              )
//            }
//            composable(Destination.Activation) {
//              ActivationScreen(navController = navController, onActivationComplete = {
//                if (isActivated) {
//                  navController.navigate(Destination.Dashboard) {
//                    popUpTo(Destination.Activation) { inclusive = true }
//                  }
//                }
//              })
//            }
//            composable(Destination.Countries) {
//              CountriesScreen(
//                navigateBack = { navController.popBackStack() },
//                navigateToCities = { countryId ->
//                  navController.navigate("countries/$countryId/cities")
//                },
//              )
//            }
//            composable(
//              route = Destination.Cities,
//              arguments = listOf(navArgument(Args.CountryId) { type = NavType.StringType }),
//            ) { backStackEntry ->
//              CitiesScreen(
//                countryId = backStackEntry.arguments?.getString(Args.CountryId),
//                navigateBack = { navController.popBackStack() },
//                navigateBackToRoot = {
//                  navController.popBackStack(
//                    route = Destination.Dashboard,
//                    inclusive = false,
//                  )
//                },
//              )
//            }
//            composable(Destination.Settings) {
//              SettingsScreen(
//                navigateBack = { navController.popBackStack() },
//                navigateToSplitTunneling = { navController.navigate(Destination.SplitTunneling) },
//                shareLogs = ::shareLogs,
//              )
//            }
//            composable(Destination.SplitTunneling) {
//              SplitTunnelingScreenScreen(
//                navigateBack = { navController.popBackStack() },
//              )
//            }
//          }
//        }
//      }
//    }
//  }
//
//  // شاشة مؤقتة
//  @Composable
//  fun SplashScreen() {
//    Box(
//      modifier = Modifier.fillMaxSize(),
//      contentAlignment = Alignment.Center
//    ) {
//      Text("Loading...", style = MaterialTheme.typography.bodyLarge)
//    }
//  }
//
//  private suspend fun getActivationStatus(): Boolean {
//    return try {
//      val userId = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
//      if (userId.isEmpty()) {
//        return false
//      }
//
//
//      val userDocumentSnapshot = Firebase.firestore.collection("users")
//        .document(userId)
//        .get()
//        .await()
//
//      if (!userDocumentSnapshot.exists()) {
//        return false
//      }
//
//
//      var trialActive = userDocumentSnapshot.getBoolean("trialActive") ?: false
//      val activationEndDate = userDocumentSnapshot.getTimestamp("activationEndDate")?.toDate()
//
//      if (activationEndDate?.before(Date()) == true) {
//        // إذا انتهت الفترة التجريبية، قم بتحديث الحقل إلى false
//        Firebase.firestore.collection("users").document(userId)
//          .update("trialActive", false)
//          .await()
//        trialActive = false
//      }
//
//
//      if (trialActive && activationEndDate?.after(Date()) == true) {
//        return true
//      }
//
//
//      val codeQuerySnapshot = Firebase.firestore.collection("codes")
//        .whereEqualTo("userId", userId)
//        .get()
//        .await()
//
//      val currentDate = Date()
//      val codeDocument = codeQuerySnapshot.documents.firstOrNull()
//      val endDate = codeDocument?.getDate("EndDate")
//
//      if (endDate == null || endDate.before(currentDate)) {
//        codeDocument?.reference?.delete()?.await()
//        return false
//      } else {
//        return true
//      }
//
//
//    } catch (e: Exception) {
//      Log.e("getActivationStatus", "Error fetching activation status", e)
//      return false
//    }
//  }
//
//
//
//
//
//
//  private fun shareLogs() {
//    val file = fileLogTree.getLogsFile() ?: return
//    ChooserIntent.start(this, BuildConfig.APPLICATION_ID, file)
//  }
//
//
//
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
//  const val Activation = "activation"  // إضافة شاشة التفعيل
//}
//
//object Args {
//  const val CountryId = "countryId"
//}
//fun getDeviceId(context: Context): String {
//  return android.provider.Settings.Secure.getString(context.contentResolver, android.provider.Settings.Secure.ANDROID_ID)
//}
