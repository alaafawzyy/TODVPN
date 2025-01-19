package co.gulf.todvpn.vpn.ui.screens

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

@Composable
fun ActivationScreen(navController: NavHostController, onActivationComplete: () -> Unit) {
  val context = LocalContext.current
  val currentUser = FirebaseAuth.getInstance().currentUser
  val db = FirebaseFirestore.getInstance()
  var userState by remember { mutableStateOf<UserState?>(null) }
  var isLoading by remember { mutableStateOf(true) }

  LaunchedEffect(currentUser) {
    if (currentUser == null) {
      FirebaseAuth.getInstance().signInAnonymously()
        .addOnCompleteListener { task ->
          if (task.isSuccessful) {
            val newUser = FirebaseAuth.getInstance().currentUser
            newUser?.uid?.let { userId ->
              // تحقق إذا كانت البيانات موجودة في Firestore
              val userDocRef = db.collection("users").document(userId)
              userDocRef.get().addOnSuccessListener { document ->
                if (!document.exists()) {
                  // لو البيانات مش موجودة، هنضيف بيانات جديدة
                  val userState = UserState(
                    startDate = Date(),
                    activationEndDate = null,
                    isTrialActive = true
                  )
                  userDocRef.set(userState)
                }
                // تحميل بيانات المستخدم بعد إضافتها أو التأكد من وجودها
                loadUserData(userId, db, context) { data ->
                  userState = data
                  isLoading = false
                }
              }.addOnFailureListener {
                Toast.makeText(context, "فشل تحميل بيانات المستخدم. حاول مرة أخرى.", Toast.LENGTH_SHORT).show()
                isLoading = false
              }
            }
          } else {
            Log.e("ActivationScreen", "فشل تسجيل الدخول المجهول", task.exception)
            Toast.makeText(context, "فشل تسجيل الدخول المجهول.", Toast.LENGTH_SHORT).show()
            isLoading = false
          }
        }
    } else {
      currentUser.uid?.let { userId ->
        loadUserData(userId, db, context) { data ->
          userState = data
          isLoading = false
        }
      }
    }
  }

  if (isLoading) {
    Text("Loading...")
  } else {
    userState?.let {
      val trialEndDate = it.startDate?.let { startDate ->
        Calendar.getInstance().apply {
          time = startDate
          add(Calendar.DAY_OF_YEAR, 7) // Adding 7 days for trial period
        }.time
      }

      if (trialEndDate != null && Date().after(trialEndDate)) {
        // إذا انتهت فترة التجربة، عرض شاشة التفعيل
        ActivationCodeScreen(currentUser?.uid ?: "")
      } else {
        // إذا كانت فترة التجربة لا تزال فعالة
        TrialScreen(navController)
      }
    }
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

              // Update the user data with the activation end date and set trial as inactive
              db.collection("users").document(userId).update(
                "activationEndDate", activationEndDate,
                "isTrialActive", false
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

fun createActivationCode(db: FirebaseFirestore, code: String, duration: Int) {
  val activationCodeData = hashMapOf(
    "code" to code,  // Ensure this code is unique and not empty
    "duration" to duration,
    "isUsed" to false,
    "createdAt" to Timestamp.now()
  )
  db.collection("codes").document(code).set(activationCodeData)
}

fun extendActivationCode(db: FirebaseFirestore, code: String, newDuration: Int) {
  db.collection("codes").document(code).update("duration", newDuration)
}

fun deleteActivationCode(db: FirebaseFirestore, code: String) {
  db.collection("codes").document(code).delete()
}

fun getActivationCodesStats(db: FirebaseFirestore, onStatsLoaded: (Int, Int) -> Unit) {
  val codesRef = db.collection("codes")
  codesRef.get().addOnSuccessListener { querySnapshot ->
    val totalCodes = querySnapshot.size()
    val unusedCodes = querySnapshot.filter { it.getBoolean("isUsed") == false }.size
    onStatsLoaded(totalCodes, unusedCodes)
  }
}

@Composable
fun TrialScreen(navController: NavHostController) {
  Column(
    modifier = Modifier.fillMaxSize(),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
  ) {
    Text("You are on a 7-day trial!")
    Button(
      onClick = {
        // الانتقال إلى الداشبورد
        navController.navigate("dashboard")
      }
    ) {
      Text("Proceed")
    }
  }
}

@Composable
fun NavigationGraph(navController: NavHostController) {
  NavHost(navController = navController, startDestination = "activation") {
    composable("activation") {
      ActivationScreen(navController = navController, onActivationComplete = {
        // الانتقال إلى الداشبورد عند الانتهاء من التفعيل
        navController.navigate("dashboard")
      })
    }
    composable("dashboard") {
      // شاشة الداشبورد الخاصة بك
      DashboardScreen()
    }
  }
}

@Composable
fun DashboardScreen() {
  // محتوى شاشة الداشبورد
  Text("Welcome to the Dashboard!")
}
