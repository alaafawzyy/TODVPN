package co.gulf.todvpn.vpn.ui.screens

import android.content.Context
import android.content.SharedPreferences
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

@Composable
fun ActivationScreen(navController: NavHostController, onActivationComplete: () -> Unit) {
  val context = LocalContext.current
  val sharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
  val currentUser = FirebaseAuth.getInstance().currentUser
  val db = FirebaseFirestore.getInstance()
  var userState by remember { mutableStateOf<UserState?>(null) }
  var showTrialScreen by remember { mutableStateOf(true) }

  // Directly read the SharedPreferences value
  val isFirstLaunch = sharedPreferences.getBoolean("isFirstLaunch", true)

  LaunchedEffect(currentUser, isFirstLaunch) {
    if (!isFirstLaunch) {
      // Skip the trial screen and navigate directly to the dashboard
      navController.navigate("dashboard") {
        popUpTo("activation") { inclusive = true }
      }
    } else {
      if (currentUser == null) {
        FirebaseAuth.getInstance().signInAnonymously()
          .addOnCompleteListener { task ->
            if (task.isSuccessful) {
              val newUser = FirebaseAuth.getInstance().currentUser
              newUser?.uid?.let { userId ->
                handleUserFirstLaunch(
                  userId = userId,
                  db = db,
                  sharedPreferences = sharedPreferences,
                  context = context,
                  onComplete = {
                    userState = it
                    showTrialScreen = false
                  }
                )
              }
            } else {
              Toast.makeText(context, "Anonymous sign-in failed.", Toast.LENGTH_SHORT).show()
              showTrialScreen = false
            }
          }
      } else {
        currentUser.uid?.let { userId ->
          loadUserData(userId, db, context) { data ->
            userState = data
            showTrialScreen = false
          }
        }
      }
    }
  }

  if (showTrialScreen) {
    TrialScreen(navController, sharedPreferences)
  } else {
    userState?.let {
      val trialEndDate = it.startDate?.let { startDate ->
        Calendar.getInstance().apply {
          time = startDate
          add(Calendar.DAY_OF_YEAR, 7) // Adding 7 days for trial period
        }.time
      }

      if (trialEndDate != null && Date().after(trialEndDate)) {
        // If the trial period has ended, show the activation screen
        ActivationCodeScreen(currentUser?.uid ?: "")
      } else {
        // Trial period is still active
        TrialScreen(navController, sharedPreferences)
      }
    }
  }


    userState?.let {
      val trialEndDate = it.startDate?.let { startDate ->
        Calendar.getInstance().apply {
          time = startDate
          add(Calendar.DAY_OF_YEAR, 7) // Adding 7 days for trial period
        }.time
      }

      if (trialEndDate != null && Date().after(trialEndDate)) {
        // If the trial period has ended, show the activation screen
        ActivationCodeScreen(currentUser?.uid ?: "")
      } else {
        // Trial period is still active
        TrialScreen(navController, sharedPreferences)
      }
    }
  }


fun handleUserFirstLaunch(
  userId: String,
  db: FirebaseFirestore,
  sharedPreferences: SharedPreferences,
  context: Context,
  onComplete: (UserState?) -> Unit
) {
  val userDocRef = db.collection("users").document(userId)
  userDocRef.get().addOnSuccessListener { document ->
    if (!document.exists()) {
      val userState = UserState(
        startDate = Date(),
        activationEndDate = null,
        isTrialActive = true
      )
      userDocRef.set(userState)
    }
    // Mark first launch as complete
    sharedPreferences.edit().putBoolean("isFirstLaunch", false).apply()

    loadUserData(userId, db, context, onComplete)
  }.addOnFailureListener {
    Toast.makeText(context, "Failed to load user data. Please try again.", Toast.LENGTH_SHORT).show()
    onComplete(null)
  }
}

fun loadUserData(
  userId: String,
  db: FirebaseFirestore,
  context: Context,
  onUserDataLoaded: (UserState?) -> Unit
) {
  val userDocRef = db.collection("users").document(userId)

  userDocRef.get().addOnSuccessListener { document ->
    if (document.exists()) {
      val startDate = document.getTimestamp("startDate")?.toDate()
      val activationEndDate = document.getTimestamp("activationEndDate")?.toDate()
      val isTrialActive = document.getBoolean("isTrialActive") ?: true

      val userState = UserState(
        startDate = startDate,
        activationEndDate = activationEndDate,
        isTrialActive = isTrialActive
      )

      onUserDataLoaded(userState)
    } else {
      Toast.makeText(context, "User data not found. Please try again.", Toast.LENGTH_SHORT).show()
      onUserDataLoaded(null)
    }
  }.addOnFailureListener {
    Toast.makeText(context, "Failed to load user data. Please try again.", Toast.LENGTH_SHORT).show()
    onUserDataLoaded(null)
  }
}


@Composable
fun ActivationCodeScreen(userId: String) {
  var activationCode by remember { mutableStateOf("") }
  val db = FirebaseFirestore.getInstance()
  val context = LocalContext.current

  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(16.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
  ) {
    Text("Enter your activation code")
    OutlinedTextField(
      value = activationCode,
      onValueChange = { activationCode = it },
      label = { Text("Activation Code") },
      modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(16.dp))
    Button(
      onClick = {
        val activationDocRef = db.collection("codes").document(activationCode)
        activationDocRef.get().addOnSuccessListener { document ->
          if (document.exists()) {
            val isUsed = document.getBoolean("isUsed") ?: false
            val duration = document.getLong("duration")?.toInt() ?: 0

            if (!isUsed && duration > 0) {
              val activationEndDate = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, duration)
              }.time

              // Update user data and code status
              db.collection("users").document(userId).update(
                "activationEndDate", activationEndDate,
                "trialActive", false
              )
              db.collection("codes").document(activationCode).update("isUsed", true)
              Toast.makeText(context, "Activation successful!", Toast.LENGTH_SHORT).show()
            } else {
              Toast.makeText(context, "Invalid or already used code.", Toast.LENGTH_SHORT).show()
            }
          } else {
            Toast.makeText(context, "Code not found.", Toast.LENGTH_SHORT).show()
          }
        }.addOnFailureListener {
          Toast.makeText(context, "Failed to validate the code. Please try again.", Toast.LENGTH_SHORT).show()
        }
      }
    ) {
      Text("Activate")
    }
  }
}


@Composable
fun TrialScreen(navController: NavHostController, sharedPreferences: SharedPreferences) {
  LaunchedEffect(key1 = true) {
    sharedPreferences.edit().putBoolean("isFirstLaunch", false).apply()
    kotlinx.coroutines.delay(5000)
    navController.navigate("dashboard") {
      popUpTo("activation") { inclusive = true }
    }
  }

  Column(
    modifier = Modifier.fillMaxSize(),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
  ) {
    Text("You are on a 7-day trial!")
  }
}





@Composable
fun NavigationGraph(navController: NavHostController) {
  NavHost(navController = navController, startDestination = "activation") {
    composable("activation") {
      ActivationScreen(navController = navController, onActivationComplete = {
        // Navigate to the dashboard upon activation completion
        navController.navigate("dashboard") {
          popUpTo("activation") { inclusive = true }
        }
      })
    }
    composable("dashboard") {
      DashboardScreen()
    }
  }
}

@Composable
fun DashboardScreen() {
  Column(
    modifier = Modifier.fillMaxSize(),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
  ) {
    Text("Welcome to the Dashboard!")
  }
}
