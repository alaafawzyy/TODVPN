package co.gulf.todvpn.vpn.ui.screens

import android.content.Context
import android.net.ConnectivityManager
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

@Composable
fun ActivationScreen(navController: NavHostController, onActivationComplete: () -> Unit) {
  val context = LocalContext.current
  val currentUser = FirebaseAuth.getInstance().currentUser
  val db = FirebaseFirestore.getInstance()
  var userState by remember { mutableStateOf<UserState?>(null) }
  var showTrialScreen by remember { mutableStateOf(true) }
  var deviceId by remember { mutableStateOf(getDeviceId(context)) }
  var isLoading by remember { mutableStateOf(false) }

  LaunchedEffect(currentUser) {
    if (currentUser == null) {
      FirebaseAuth.getInstance().signInAnonymously()
        .addOnCompleteListener { task ->
          if (task.isSuccessful) {
            val newUser = FirebaseAuth.getInstance().currentUser
            newUser?.uid?.let { userId ->
              // التحقق من وجود deviceId في قاعدة البيانات
              db.collection("users")
                .whereEqualTo("deviceId", deviceId)
                .get()
                .addOnSuccessListener { querySnapshot ->
                  if (querySnapshot.isEmpty) {
                    // إذا لم يتم العثور على deviceId، يتم تسجيل المستخدم كجديد
                    handleUserFirstLaunch(
                      userId = userId,
                      deviceId = deviceId,
                      db = db,
                      context = context,
                      onComplete = { userStateResult ->
                        userState = userStateResult
                        showTrialScreen = userStateResult?.isTrialActive ?: true
                      }
                    )
                  } else {
                    // إذا تم العثور على deviceId، يتم تحميل بيانات المستخدم
                    loadUserData(userId, db, context) { data ->
                      userState = data
                      showTrialScreen = data?.isTrialActive ?: true

                      // التحقق من انتهاء فترة التجربة
                      val trialEndDate = data?.startDate?.let { startDate ->
                        Calendar.getInstance().apply {
                          time = startDate
                          add(Calendar.DAY_OF_YEAR, 7) // Adding 7 days for trial period
                        }.time
                      }

                      if (trialEndDate != null && Date().after(trialEndDate)) {
                        // إذا انتهت فترة التجربة، انتقل إلى شاشة إدخال الكود
                        showTrialScreen = false
                      }
                    }
                  }
                }
                .addOnFailureListener { e ->
                  Toast.makeText(context, "Failed to check deviceId: ${e.message}", Toast.LENGTH_SHORT).show()
                }
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
          showTrialScreen = data?.isTrialActive ?: true

          // التحقق من انتهاء فترة التجربة
          val trialEndDate = data?.startDate?.let { startDate ->
            Calendar.getInstance().apply {
              time = startDate
              add(Calendar.DAY_OF_YEAR, 7) // Adding 7 days for trial period
            }.time
          }

          if (trialEndDate != null && Date().after(trialEndDate)) {
            // إذا انتهت فترة التجربة، انتقل إلى شاشة إدخال الكود
            showTrialScreen = false
          }
        }
      }
    }
  }

  if (isLoading) {
    LoadingIndicator()
  } else {
    if (showTrialScreen) {
      TrialScreen(navController)
    } else {
      ActivationCodeScreen(
        userId = currentUser?.uid ?: "",
        onActivationComplete = {
          onActivationComplete() // Callback to navigate to the dashboard
        }
      )
    }
  }
}

@Composable
fun LoadingIndicator() {
  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(Color.Black.copy(alpha = 0.5f)),
    contentAlignment = Alignment.Center
  ) {
    CircularProgressIndicator(color = Color.White)
  }
}

@Composable
fun ActivationCodeScreen(userId: String, onActivationComplete: () -> Unit) {
  var activationCode by remember { mutableStateOf("") }
  val db = FirebaseFirestore.getInstance()
  val context = LocalContext.current
  var isLoading by remember { mutableStateOf(false) }

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
      onValueChange = { activationCode = it.trim() }, // إزالة المسافات الزائدة
      label = { Text("Activation Code") },
      modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(16.dp))
    Button(
      onClick = {
        if (activationCode.isNotEmpty()) {
          isLoading = true
          // البحث عن المستند الذي يحتوي على الحقل code الذي يتطابق مع activationCode
          db.collection("codes")
            .whereEqualTo("code", activationCode) // البحث في الحقل code
            .get()
            .addOnSuccessListener { querySnapshot ->
              if (!querySnapshot.isEmpty) {
                val document = querySnapshot.documents[0] // أول مستند متطابق
                val isUsed = document.getBoolean("isUsed") ?: false
                val duration = document.getLong("duration")?.toInt() ?: 0
                val endDate = document.getDate("EndDate")

                // التحقق من صلاحية الكود
                if (!isUsed && duration > 0 && endDate != null && endDate.after(Date())) {
                  // حساب startDate (تاريخ اليوم)
                  val startDate = Date()

                  // حساب activationEndDate (تاريخ اليوم + duration)
                  val activationEndDate = Calendar.getInstance().apply {
                    time = startDate
                    add(Calendar.DAY_OF_YEAR, duration)
                  }.time

                  // تحديث بيانات المستخدم
                  db.collection("users").document(userId).update(
                    "startDate", startDate, // تحديث startDate
                    "activationEndDate", activationEndDate,
                    "trialActive", false
                  ).addOnSuccessListener {
                    // تحديث حالة الكود إلى "مستخدم"
                    document.reference.update("isUsed", true)
                      .addOnSuccessListener {
                        isLoading = false
                        Toast.makeText(context, "Activation successful!", Toast.LENGTH_SHORT).show()
                        onActivationComplete() // الانتقال إلى الشاشة الرئيسية
                      }.addOnFailureListener { e ->
                        isLoading = false
                        Toast.makeText(context, "Failed to update code status: ${e.message}", Toast.LENGTH_SHORT).show()
                      }
                  }.addOnFailureListener { e ->
                    isLoading = false
                    Toast.makeText(context, "Failed to update user data: ${e.message}", Toast.LENGTH_SHORT).show()
                  }
                } else {
                  // إذا انتهت صلاحية الكود، قم بحذفه
                  if (endDate != null && endDate.before(Date())) {
                    deleteExpiredCode(document.id, db, context)
                  }
                  isLoading = false
                  Toast.makeText(context, "Invalid or expired code.", Toast.LENGTH_SHORT).show()
                }
              } else {
                isLoading = false
                Toast.makeText(context, "Code not found.", Toast.LENGTH_SHORT).show()
              }
            }.addOnFailureListener { e ->
              isLoading = false
              Toast.makeText(context, "Failed to validate the code: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
          Toast.makeText(context, "Please enter a valid code.", Toast.LENGTH_SHORT).show()
        }
      }
    ) {
      Text("Activate")
    }
  }
}

@Composable
fun TrialScreen(navController: NavHostController) {
  LaunchedEffect(key1 = true) {
    kotlinx.coroutines.delay(5000)
    navController.navigate(Destination.Dashboard) {
      popUpTo(Destination.Activation) { inclusive = true }
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
fun DashboardScreen() {
  Column(
    modifier = Modifier.fillMaxSize(),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
  ) {
    Text("Welcome to the Dashboard!")
  }
}

private fun deleteExpiredCode(codeId: String, db: FirebaseFirestore, context: Context) {
  db.collection("codes").document(codeId)
    .delete()
    .addOnSuccessListener {
      Log.d("Firestore", "Code deleted successfully.")
    }
    .addOnFailureListener { e ->
      Log.e("Firestore", "Failed to delete code: ${e.message}")
      Toast.makeText(context, "Failed to delete expired code.", Toast.LENGTH_SHORT).show()
    }
}

private fun handleUserFirstLaunch(
  userId: String,
  deviceId: String,
  db: FirebaseFirestore,
  context: Context,
  onComplete: (UserState?) -> Unit
) {
  val userDocRef = db.collection("users").document(userId)
  userDocRef.get().addOnSuccessListener { document ->
    if (!document.exists()) {
      val userState = UserState(
        startDate = Date(),
        activationEndDate = Date.from(Instant.now().plus(7, ChronoUnit.DAYS)),
        isTrialActive = true,
        deviceId = deviceId
      )
      // إضافة deviceId إلى بيانات المستخدم
      userDocRef.set(userState).addOnSuccessListener {
        loadUserData(userId, db, context, onComplete)
      }.addOnFailureListener { e ->
        Toast.makeText(context, "Failed to create user data: ${e.message}", Toast.LENGTH_SHORT).show()
        onComplete(null)
      }
    } else {
      loadUserData(userId, db, context, onComplete)
    }
  }.addOnFailureListener { e ->
    Toast.makeText(context, "Failed to load user data: ${e.message}", Toast.LENGTH_SHORT).show()
    onComplete(null)
  }
}

private fun loadUserData(
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
      val deviceId = document.getString("deviceId")

      val userState = UserState(
        startDate = startDate,
        activationEndDate = activationEndDate,
        isTrialActive = isTrialActive,
        deviceId = deviceId
      )

      onUserDataLoaded(userState)
    } else {
      Toast.makeText(context, "User data not found. Please try again.", Toast.LENGTH_SHORT).show()
      onUserDataLoaded(null)
    }
  }.addOnFailureListener { e ->
    Toast.makeText(context, "Failed to load user data: ${e.message}", Toast.LENGTH_SHORT).show()
    onUserDataLoaded(null)
  }
}

data class UserState(
  val startDate: Date?,
  val activationEndDate: Date?,
  val isTrialActive: Boolean,
  val deviceId: String? = null // إضافة deviceId كحقل اختياري
)

object Destination {
  const val Dashboard = "dashboard"
  const val Activation = "activation"
}

// دالة للحصول على deviceId (يمكن استبدالها بالطريقة المناسبة للحصول على deviceId)
fun getDeviceId(context: Context): String {
  return android.provider.Settings.Secure.getString(context.contentResolver, android.provider.Settings.Secure.ANDROID_ID)
}
