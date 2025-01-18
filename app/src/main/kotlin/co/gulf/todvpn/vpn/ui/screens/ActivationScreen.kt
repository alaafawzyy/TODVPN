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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

@Composable
fun ActivationScreen() {
  val context = LocalContext.current // نحصل على الـ context هنا
  val currentUser = FirebaseAuth.getInstance().currentUser
  val db = FirebaseFirestore.getInstance()
  var userState by remember { mutableStateOf<UserState?>(null) }
  var isLoading by remember { mutableStateOf(true) }

  // التحقق إذا كان المستخدم مسجل دخول أو لا
  LaunchedEffect(currentUser) {
    if (currentUser == null) {
      // تسجيل الدخول المجهول إذا لم يكن هناك مستخدم
      FirebaseAuth.getInstance().signInAnonymously()
        .addOnCompleteListener { task ->
          if (task.isSuccessful) {
            val newUser = FirebaseAuth.getInstance().currentUser
            newUser?.uid?.let { userId ->
              // بعد تسجيل الدخول بنجاح، نستخدم LaunchedEffect لاستدعاء الدالة
              loadUserData(userId, db, context) { data ->
                userState = data
                isLoading = false
              }
            }
          } else {
            // في حالة فشل تسجيل الدخول
            Log.e("ActivationScreen", "Failed to sign in anonymously", task.exception)
            Toast.makeText(context, "Failed to sign in anonymously.", Toast.LENGTH_SHORT).show()
            isLoading = false
          }
        }
    } else {
      // إذا كان هناك مستخدم بالفعل مسجل دخول
      currentUser.uid?.let { userId ->
        loadUserData(userId, db, context) { data ->
          userState = data
          isLoading = false
        }
      }
    }
  }

  // إذا كانت البيانات لا تزال في حالة تحميل
  if (isLoading) {
    // عرض شاشة تحميل أو رسالة للمستخدم
    Text("Loading...")
  } else {
    // بعد تحميل البيانات
    userState?.let {
      if (it.isTrialActive) {
        TrialScreen()  // إظهار الشاشة الرئيسية للتجربة المجانية
      } else {
        ActivationCodeScreen(currentUser?.uid ?: "") // إظهار شاشة إدخال كود التفعيل
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
      val startDate = document.getTimestamp("startDate")
      val activationEndDate = document.getTimestamp("activationEndDate")
      val isTrialActive = document.getBoolean("isTrialActive") ?: true  // إذا كانت القيمة null نستخدم true افتراضيًا

      val userState = UserState(
        startDate = startDate?.toDate(),
        activationEndDate = activationEndDate?.toDate(),
        isTrialActive = isTrialActive
      )

      // تمرير البيانات المحملة
      onUserDataLoaded(userState)
    } else {
      // إذا كانت بيانات المستخدم غير موجودة
      Toast.makeText(context, "User data not found. Please try again.", Toast.LENGTH_SHORT).show()
      onUserDataLoaded(null)
    }
  }.addOnFailureListener {
    // في حالة فشل تحميل البيانات
    Toast.makeText(context, "Failed to load user data. Please try again.", Toast.LENGTH_SHORT).show()
    onUserDataLoaded(null)
  }
}



@Composable
fun TrialScreen() {
  // هذه الشاشة تظهر أثناء فترة التجربة المجانية
  Column(
    modifier = Modifier.fillMaxSize(),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
  ) {
    Text("You are on a 7-day trial!")
    Button(
      onClick = {
        // هنا يمكن إضافة الكود للانتقال إلى شاشة أخرى أو القيام بإجراءات معينة.
      }
    ) {
      Text("Proceed")
    }
  }
}

@Composable
fun ActivationCodeScreen(userId: String) {
  // شاشة إدخال كود التفعيل
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
        // التحقق من الكود في Firestore
        val activationDocRef = db.collection("activationCodes").document(activationCode)
        activationDocRef.get().addOnSuccessListener { document ->
          if (document.exists()) {
            val isActive = document.getBoolean("isActive") ?: false
            if (isActive) {
              // كود التفعيل صحيح، تحديث بيانات المستخدم
              val duration = document.getLong("duration")?.toInt() ?: 0
              val activationEndDate = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, duration)
              }.time

              db.collection("users").document(userId).update(
                "activationEndDate", activationEndDate,
                "isTrialActive", false
              )
              Toast.makeText(context, "Activation successful!", Toast.LENGTH_SHORT).show()
            } else {
              Toast.makeText(context, "Invalid code", Toast.LENGTH_SHORT).show()
            }
          } else {
            Toast.makeText(context, "Code not found", Toast.LENGTH_SHORT).show()
          }
        }
      }
    ) {
      Text("Activate")
    }
  }
}
