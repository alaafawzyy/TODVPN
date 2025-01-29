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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import co.gulf.todvpn.vpn.ui.screens.ActivationScreen
import co.gulf.todvpn.vpn.ui.theme.BasedVPNTheme
import co.gulf.todvpn.vpn.ui.theme.ThirdColor
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
        var needsActivationChoice by remember { mutableStateOf(false) }
        var deviceId by remember { mutableStateOf<String?>(null) }

        val coroutineScope = rememberCoroutineScope()

        LaunchedEffect(Unit) {
          try {
            val currentDeviceId = getDeviceId(this@MainActivity)
            deviceId = currentDeviceId
            Log.d("Activation", "Device ID: $currentDeviceId")

            val userId = checkDeviceId(currentDeviceId)

            if (userId == null) {
              needsActivationChoice = true
              isLoading = false
              return@LaunchedEffect
            } else {
              isActivated = getActivationStatus(userId, currentDeviceId)
              Log.d("Activation", "Initial activation status: $isActivated")
            }
          } catch (e: Exception) {
            Log.e("Activation", "Initial check error: ${e.message}")
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
            startDestination = when {
              needsActivationChoice -> Destination.ActivationChoice
              isActivated -> Destination.Dashboard
              else -> Destination.Activation
            },
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
          ) {
            composable(Destination.ActivationChoice) {
              ActivationChoiceScreen(
                onTrialSelected = {
                  coroutineScope.launch {
                    val currentDeviceId = deviceId ?: return@launch
                    try {
                      val userId = createTrialUser(currentDeviceId)
                      isActivated = getActivationStatus(userId, currentDeviceId)
                      if (isActivated) {
                        navController.navigate(Destination.Dashboard) {
                          popUpTo(Destination.ActivationChoice) { inclusive = true }
                        }
                      } else {
                        Toast.makeText(
                          this@MainActivity,
                          "Failed to activate trial",
                          Toast.LENGTH_SHORT
                        ).show()
                      }
                    } catch (e: Exception) {
                      Toast.makeText(
                        this@MainActivity,
                        "Error: ${e.message}",
                        Toast.LENGTH_SHORT
                      ).show()
                    }
                  }
                },
                onCodeSelected = {
                  navController.navigate(Destination.Activation)
                }
              )
            }

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
                  coroutineScope.launch {
                    val currentDeviceId = deviceId ?: return@launch
                    try {
                      createUserWithCode(currentDeviceId)
                      navController.navigate(Destination.Dashboard)
                    } catch (e: Exception) {
                      Toast.makeText(
                        this@MainActivity,
                        "Error creating user: ${e.message}",
                        Toast.LENGTH_SHORT
                      ).show()
                    }
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

  @Composable
  fun SplashScreen() {
    Box(
      modifier = Modifier.fillMaxSize(),
      contentAlignment = Alignment.Center
    ) {
      Text("Loading...", style = MaterialTheme.typography.bodyLarge)
    }
  }

  @Composable
  fun ActivationChoiceScreen(
    onTrialSelected: () -> Unit,
    onCodeSelected: () -> Unit
  ) {
    Box(
      modifier = Modifier.fillMaxSize(),
      contentAlignment = Alignment.Center
    ) {
      Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Button(
          onClick = onCodeSelected,
          modifier = Modifier.fillMaxWidth().padding(start = 30.dp, end = 30.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = ThirdColor,
            contentColor = Color.White
          )
        ) {
          Text("Enter Your Code")
        }
        Spacer(modifier = Modifier.height(32.dp))
        Text(
          text = "TRY 7 Days",
          color = ThirdColor,
          fontSize = 18.sp,
          fontWeight = FontWeight.Bold,
          textAlign = TextAlign.Center
        )
        Button(
          onClick = onTrialSelected,
          modifier = Modifier.fillMaxWidth().padding(start = 30.dp, end = 30.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = ThirdColor,
            contentColor = Color.White
          )
        ) {
          Text("Continue to trial")
        }
      }
    }
  }

  private suspend fun checkDeviceId(deviceId: String): String? {
    val usersRef = Firebase.firestore.collection("users")
    val query = usersRef.whereEqualTo("deviceId", deviceId).get().await()
    return if (query.isEmpty) null else query.documents.first().id
  }

  private suspend fun createTrialUser(deviceId: String): String {
    val usersRef = Firebase.firestore.collection("users")
    // Check if user with deviceId already exists
    val query = usersRef.whereEqualTo("deviceId", deviceId).get().await()

    return if (query.isEmpty) {
      // Create new user if not exists
      val newUser = hashMapOf(
        "deviceId" to deviceId,
        "trialActive" to true,
        "startDate" to Timestamp.now(),
        "endDate" to java.sql.Timestamp(Date().time + 7 * 24 * 3600 * 1000)
      )
      val newUserRef = usersRef.add(newUser).await()
      newUserRef.id
    } else {
      // Update existing user with new trial details
      val existingUserId = query.documents.first().id
      usersRef.document(existingUserId).update(
        mapOf(
          "trialActive" to true,
          "startDate" to Timestamp.now(),
          "endDate" to java.sql.Timestamp(Date().time + 7 * 24 * 3600 * 1000)
        )
      ).await()
      existingUserId
    }
  }

  private suspend fun createUserWithCode(deviceId: String) {
    val usersRef = Firebase.firestore.collection("users")
    // Check if user with deviceId already exists
    val query = usersRef.whereEqualTo("deviceId", deviceId).get().await()

    if (query.isEmpty) {
      // Create new user if not exists
      val newUser = hashMapOf(
        "deviceId" to deviceId,
        "trialActive" to false,
        "startDate" to Timestamp.now(),
        "endDate" to Timestamp.now()
      )
      usersRef.add(newUser).await()
    } else {
      // Update existing user (if needed, adjust fields accordingly)
      val existingUserId = query.documents.first().id
      usersRef.document(existingUserId).update(
        mapOf(
          "trialActive" to false,
          "startDate" to Timestamp.now(),
          "endDate" to Timestamp.now()
        )
      ).await()
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
  const val ActivationChoice = "activationChoice"
}

object Args {
  const val CountryId = "countryId"
}

fun getDeviceId(context: Context): String {
  return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
}



















