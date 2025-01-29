

package co.gulf.todvpn.vpn.ui.screens

import android.content.Context
import android.provider.Settings
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

import androidx.navigation.NavController
import co.gulf.todvpn.vpn.ui.theme.BackgroundColor
import co.gulf.todvpn.vpn.ui.theme.ThirdColor
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun ActivationScreen(
  navController: NavController,
  onActivationSuccess: () -> Unit
) {
  val context = LocalContext.current
  var activationCode by remember { mutableStateOf("") }
  var isLoading by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }
  val scope = rememberCoroutineScope()

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(BackgroundColor),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
        Text("Enter your activation code", color = ThirdColor)
    OutlinedTextField(
      value = activationCode,
      onValueChange = { activationCode = it.trim() },
      label = { Text("Activation code") },
         modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      colors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = Color(0xBFFF9800),
        unfocusedBorderColor = Color(0xBFFF9800).copy(alpha = 0.5f),
        focusedLabelColor = Color(0xBFFF9800),
        unfocusedLabelColor = Color(0xBFFF9800).copy(alpha = 0.5f),
        focusedTextColor = Color(0xBFFF9800),
        unfocusedTextColor = Color(0xBFFF9800).copy(alpha = 0.8f)
      ),
    )









    Spacer(modifier = Modifier.height(32.dp))

    // زر التفعيل
    Button(
      onClick = {
        if (activationCode.isBlank()) {
          errorMessage = "Please enter the activation code"
          return@Button
        }

        scope.launch {
          isLoading = true
          errorMessage = null

          val result = checkActivationCode(
            code = activationCode,
            context = context
          )

          if (result) {
            onActivationSuccess()
            navController.navigate("dashboard") {
              popUpTo("activation") { inclusive = true } // مسح شاشة التفعيل من السجل
            }
          } else {
            errorMessage = "The code is invalid or already used"
          }

          isLoading = false
        }
      },
      colors = ButtonDefaults.buttonColors(
        containerColor = Color(0xBFFF9800),
        contentColor = Color.White),
      enabled = !isLoading
    ) {
      if (isLoading) CircularProgressIndicator(Modifier.size(24.dp))
      else Text("Activate")
    }

    // عرض رسائل الخطأ
    errorMessage?.let {
      Spacer(modifier = Modifier.height(8.dp))
      Text(
        text = it,
        color = Color.Red,
        modifier = Modifier.fillMaxWidth()
      )
    }
  }
}

private suspend fun checkActivationCode(code: String, context: Context): Boolean {
  return try {
    val db = FirebaseFirestore.getInstance()
    val deviceId = getDeviceId(context)

    // 1. التحقق من وجود الكود في Firestore
    val query = db.collection("codes")
      .whereEqualTo("code", code)
      .get()
      .await()

    // إذا لم يتم العثور على الكود
    if (query.isEmpty) return false

    val document = query.documents[0]
    val isUsed = document["isUsed"] as? Boolean ?: false
    val storedDeviceId = document["deviceId"] as? String

    // 2. إذا كان الكود مستخدمًا مسبقًا
    if (isUsed) {
      // التحقق من تطابق Device ID
      return storedDeviceId == deviceId
    }

    // 3. إذا كان الكود غير مستخدم
    val durationDays = document["duration"] as? Long ?: return false

    // حساب تاريخ الانتهاء
    val endDate = Calendar.getInstance().apply {
      add(Calendar.DAY_OF_YEAR, durationDays.toInt())
    }.time

    // تحديث البيانات في Firestore
    document.reference.update(
      mapOf(
        "isUsed" to true,
        "deviceId" to deviceId,
        "endDate" to endDate
      )
    ).await()

    // حفظ البيانات محليًا
    saveActivationDataLocally(context, durationDays.toInt(), endDate)

    true
  } catch (e: Exception) {
    Log.e("Activation", "Error: ${e.message}")
    false
  }
}

private fun getDeviceId(context: Context): String {
  return Settings.Secure.getString(
    context.contentResolver,
    Settings.Secure.ANDROID_ID
  )
}

private fun saveActivationDataLocally(
  context: Context,
  durationDays: Int,
  endDate: Date
) {
  val prefs = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
  prefs.edit().apply {
    putInt("DURATION_DAYS", durationDays)
    putLong("END_DATE", endDate.time)
    apply()
  }
}





















//package co.gulf.todvpn.vpn.ui.screens
//
//import android.content.Context
//import android.provider.Settings
//import android.util.Log
//import androidx.compose.foundation.background
//import androidx.compose.foundation.layout.*
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.platform.LocalContext
//import androidx.compose.ui.unit.dp
//import androidx.navigation.NavController
//import co.gulf.todvpn.vpn.ui.theme.BackgroundColor
//import co.gulf.todvpn.vpn.ui.theme.ThirdColor
//import com.google.firebase.Timestamp
//import com.google.firebase.firestore.DocumentSnapshot
//import com.google.firebase.firestore.FirebaseFirestore
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.launch
//import kotlinx.coroutines.tasks.await
//import kotlinx.coroutines.withContext
//import java.util.*
//import java.util.concurrent.TimeUnit
//
//@Composable
//fun ActivationScreen(
//  navController: NavController,
//  onActivationComplete: (durationDays: Int) -> Unit
//) {
//  val context = LocalContext.current
//  var code by remember { mutableStateOf("") }
//  var isLoading by remember { mutableStateOf(false) }
//  var errorMessage by remember { mutableStateOf<String?>(null) }
//  val scope = rememberCoroutineScope()
//
//  // التحقق التلقائي عند فتح الشاشة
//  LaunchedEffect(Unit) {
//    checkExistingActivation(context, onActivationComplete)
//    activateTrial(context, onActivationComplete)
//  }
//  Column(
//    modifier = Modifier
//      .fillMaxSize()
//      .background(BackgroundColor),
//    verticalArrangement = Arrangement.Center,
//    horizontalAlignment = Alignment.CenterHorizontally
//  )
//  {
//    Text("Enter your activation code", color = ThirdColor)
//    TextField(
//      value = code,
//      onValueChange = { code = it },
//      label = { Text("Activation Code") },
//      modifier = Modifier.fillMaxWidth().padding(16.dp)
//    )
//
//    Spacer(modifier = Modifier.height(16.dp))
//
//    if (isLoading) {
//      CircularProgressIndicator()
//    } else {
//      Button(
//        onClick = {
//          if (code.isBlank()) {
//            errorMessage = "يرجى إدخال كود التفعيل"
//            return@Button
//          }
//
//          isLoading = true
//          errorMessage = null
//
//          scope.launch {
//            try {
//              val result = validateActivationCode(code.trim(), context)
//              if (result > 0) {
//                saveActivation(context, result)
//                onActivationComplete(result)
//              } else {
//                errorMessage = "الكود غير صالح"
//              }
//            } catch (e: Exception) {
//              errorMessage = handleFirestoreError(e)
//            } finally {
//              isLoading = false
//            }
//          }
//        },
//
//        colors = ButtonDefaults.buttonColors(
//       containerColor = Color(0xBFFF9800),
//      contentColor = Color.White)
//      ) {
//        Text("Activate")
//      }
//    }
//
//    errorMessage?.let { message ->
//      Spacer(modifier = Modifier.height(8.dp))
//      Text(
//        text = message,
//        color = Color.Red,
//        modifier = Modifier.fillMaxWidth()
//      )
//    }
//  }
//}
//private suspend fun activateTrial(context: Context, onComplete: (Int) -> Unit) {
//  val prefs = context.getSharedPreferences("VPN_PREFS", Context.MODE_PRIVATE)
//
//  if (!prefs.getBoolean("TRIAL_ACTIVATED", false)) {
//    val trialDays = 7
//    val endDate = calculateEndDate(trialDays)
//
//    prefs.edit().apply {
//      putLong("END_DATE", endDate.time)
//      putBoolean("TRIAL_ACTIVATED", true)
//      apply()
//    }
//
//    onComplete(trialDays)
//  }
//}
//
//private suspend fun checkExistingActivation(context: Context, onComplete: (Int) -> Unit) {
//  val prefs = context.getSharedPreferences("VPN_PREFS", Context.MODE_PRIVATE)
//  val endDateMillis = prefs.getLong("END_DATE", 0)
//
//  if (endDateMillis > System.currentTimeMillis()) {
//    val remainingDays = TimeUnit.MILLISECONDS.toDays(endDateMillis - System.currentTimeMillis()).toInt()
//    onComplete(remainingDays.coerceAtLeast(1))
//  }
//}
//
//private suspend fun activateTrialIfNeeded(context: Context, onComplete: (Int) -> Unit) {
//  val prefs = context.getSharedPreferences("VPN_PREFS", Context.MODE_PRIVATE)
//
//  if (!prefs.contains("TRIAL_ACTIVATED")) {
//    val trialDays = 7
//    val endDate = calculateEndDate(trialDays)
//
//    withContext(Dispatchers.IO) {
//      prefs.edit()
//        .putLong("END_DATE", endDate.time)
//        .putBoolean("TRIAL_ACTIVATED", true)
//        .apply()
//    }
//
//    onComplete(trialDays)
//  }
//}
//
//private suspend fun validateActivationCode(code: String, context: Context): Int {
//  return withContext(Dispatchers.IO) {
//    try {
//      val db = FirebaseFirestore.getInstance()
//      val deviceId = getDeviceId(context)
//
//      // الخطوة 1: البحث عن أي كود مرتبط بنفس الجهاز والكود المدخل
//      val existingCodeQuery = db.collection("codes")
//        .whereEqualTo("code", code)
//        .whereEqualTo("deviceId", deviceId)
//        .get()
//        .await()
//
//      if (!existingCodeQuery.isEmpty) {
//        val existingCodeDoc = existingCodeQuery.documents.first()
//        val endDate = existingCodeDoc.getDate("endDate")
//
//        // إذا الكود لا يزال صالحًا
//        if (endDate != null && endDate.after(Date())) {
//          return@withContext (existingCodeDoc.getLong("duration")?.toInt() ?: 0)
//        }
//      }
//
//      // الخطوة 2: البحث عن كود عام غير مستخدم
//      val generalCodeQuery = db.collection("codes")
//        .whereEqualTo("code", code)
//        .whereEqualTo("isUsed", false)
//        .get()
//        .await()
//
//      val generalCodeDoc = generalCodeQuery.documents.firstOrNull {
//        it.getString("deviceId").isNullOrEmpty()
//      }
//
//      generalCodeDoc?.let {
//        return@withContext activateCode(it, deviceId)
//      }
//
//      // الخطوة 3: رفض الكود إذا لم يستوف الشروط
//      0
//    } catch (e: Exception) {
//      Log.e("ActivationError", "Firebase operation failed", e)
//      0
//    }
//  }
//}
//private suspend fun checkAndUpdateCode(code: String, context: Context): Int {
//  return try {
//    val db = FirebaseFirestore.getInstance()
//    val deviceId = getDeviceId(context)
//
//    // 1. التحقق من وجود تفعيل سابق للجهاز
//    val existingActivation = db.collection("codes")
//      .whereEqualTo("deviceId", deviceId)
//      .whereGreaterThanOrEqualTo("endDate", Timestamp.now())
//      .get()
//      .await()
//
//    if (!existingActivation.isEmpty) {
//      Log.d("Activation", "الجهاز مفعل مسبقًا")
//      return (existingActivation.documents.first()["duration"] as Long).toInt()
//    }
//
//    // 2. التحقق من صحة الكود الجديد
//    val codeQuery = db.collection("codes")
//      .whereEqualTo("code", code)
//      .whereEqualTo("isUsed", false)
//      .get()
//      .await()
//
//    if (codeQuery.isEmpty) {
//      Log.d("Activation", "الكود غير صالح")
//      return 0
//    }
//
//    // 3. تحديث بيانات الكود
//    val codeDoc = codeQuery.documents.first()
//    val durationDays = (codeDoc["duration"] as Long).toInt()
//    val endDate = calculateEndDate(durationDays)
//
//    codeDoc.reference.update(
//      mapOf(
//        "isUsed" to true,
//        "deviceId" to deviceId,
//        "endDate" to Timestamp(endDate.time / 1000, (endDate.time % 1000).toInt())
//      )
//    ).await()
//
//    Log.d("Activation", "تم تفعيل الكود بنجاح")
//    durationDays
//  } catch (e: Exception) {
//    Log.e("Activation", "فشل التفعيل: ${e.message}")
//    0
//  }
//}
//
//private suspend fun activateCode(document: DocumentSnapshot, deviceId: String): Int {
//  val duration = document.getLong("duration")?.toInt() ?: 0
//
//  // إذا كان الكود مخصصًا للجهاز ولم يتم استخدامه بعد
//  if (document.getString("deviceId") == deviceId && document.getBoolean("isUsed") == false) {
//    val updates = mapOf(
//      "isUsed" to true,
//      "activationDate" to Date(),
//      "endDate" to calculateEndDate(duration)
//    )
//    document.reference.update(updates).await()
//    return duration
//  }
//
//  // إذا كان كودًا عامًا
//  if (document.getString("deviceId").isNullOrEmpty()) {
//    val updates = mapOf(
//      "isUsed" to true,
//      "deviceId" to deviceId,
//      "activationDate" to Date(),
//      "endDate" to calculateEndDate(duration)
//    )
//    document.reference.update(updates).await()
//    return duration
//  }
//
//  return 0
//}
//private fun calculateEndDate(days: Int): Date {
//  return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
//    add(Calendar.DAY_OF_YEAR, days)
//    set(Calendar.HOUR_OF_DAY, 0)
//    set(Calendar.MINUTE, 0)
//    set(Calendar.SECOND, 0)
//    set(Calendar.MILLISECOND, 0)
//  }.time
//}
//
//private fun getDeviceId(context: Context): String {
//  return Settings.Secure.getString(
//    context.contentResolver,
//    Settings.Secure.ANDROID_ID
//  )
//}
//
//private fun saveActivation(context: Context, durationDays: Int) {
//  val endDate = calculateEndDate(durationDays)
//  context.getSharedPreferences("VPN_PREFS", Context.MODE_PRIVATE).edit().apply {
//    putLong("END_DATE", endDate.time)
//    apply()
//  }
//}
//
//private fun handleFirestoreError(e: Exception): String {
//  return when {
//    e.message?.contains("index") == true -> "مشكلة في الإعدادات، يرجى تحديث التطبيق"
//    e is java.net.UnknownHostException -> "لا يوجد اتصال بالإنترنت"
//    else -> "فشل في عملية التفعيل: ${e.localizedMessage}"
//  }
//}
//
//private fun saveEndDateLocally(context: Context, endDate: Date) {
//  val prefs = context.getSharedPreferences("VPN_PREFS", Context.MODE_PRIVATE)
//  prefs.edit().apply {
//    putLong("END_DATE", endDate.time)
//    apply()
//  }
//  Log.d("Activation", "تم حفظ تاريخ الانتهاء: $endDate")
//}
//
//

















/*
package co.gulf.todvpn.vpn.ui.screens

import android.content.Context
import android.provider.Settings
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.*

@Composable
fun ActivationScreen(
  navController: NavController,
  onActivationComplete: (durationDays: Int) -> Unit
) {
  val context = LocalContext.current
  var code by remember { mutableStateOf("") }
  var isLoading by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }
  val scope = rememberCoroutineScope()

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(BackgroundColor),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    TextField(
      value = code,
      onValueChange = { code = it },
      label = { Text("Enter your activation code") },
      modifier = Modifier.fillMaxWidth().padding(16.dp)
    )

    Spacer(modifier = Modifier.height(16.dp))

    if (isLoading) {
      CircularProgressIndicator()
    } else {
      Button(
        onClick = {
          if (code.isBlank()) {
            errorMessage = "يرجى إدخال كود التفعيل"
            return@Button
          }

          isLoading = true
          errorMessage = null

          scope.launch {
            try {
              val result = validateActivationCode(code.trim(), context)
              if (result > 0) {
                saveActivation(context, result)
                onActivationComplete(result)
              } else {
                errorMessage = "الكود غير صالح"
              }
            } catch (e: Exception) {
              errorMessage = handleFirestoreError(e)
            } finally {
              isLoading = false
            }
          }
        },
         colors = ButtonDefaults.buttonColors(
       containerColor = Color(0xBFFF9800),
      contentColor = Color.White)
      ) {
        Text("تفعيل")
      }
    }

    errorMessage?.let { message ->
      Spacer(modifier = Modifier.height(8.dp))
      Text(
        text = message,
        color = Color.Red,
        modifier = Modifier.fillMaxWidth()
      )
    }
  }
}

private suspend fun validateActivationCode(code: String, context: Context): Int {
  return withContext(Dispatchers.IO) {
    try {
      val db = FirebaseFirestore.getInstance()
      val deviceId = getDeviceId(context)

      // الخطوة 1: البحث عن كود مخصص للجهاز
      val deviceSpecificCode = db.collection("codes")
        .whereEqualTo("code", code)
        .whereEqualTo("deviceId", deviceId)
        .get()
        .await()
        .documents
        .firstOrNull()

      deviceSpecificCode?.let {
        if (it.getBoolean("isUsed") == true) return@withContext 0
        return@withContext activateCode(it, deviceId)
      }

      // الخطوة 2: البحث عن كود عام غير مستخدم
      val generalCodes = db.collection("codes")
        .whereEqualTo("code", code)
        .whereEqualTo("isUsed", false)
        .get()
        .await()
        .documents

      val validGeneralCode = generalCodes.firstOrNull { doc ->
        !doc.contains("deviceId") || doc.getString("deviceId").isNullOrEmpty()
      }

      validGeneralCode?.let {
        return@withContext activateCode(it, deviceId)
      }

      // إذا لم يتم العثور على أي كود صالح
      0
    } catch (e: Exception) {
      Log.e("ActivationError", "Firebase operation failed", e)
      0
    }
  }
}

private suspend fun checkAndUpdateCode(code: String, context: Context): Int {
  return try {
    val db = FirebaseFirestore.getInstance()
    val deviceId = getDeviceId(context)

    // 1. التحقق من وجود تفعيل سابق للجهاز
    val existingActivation = db.collection("codes")
      .whereEqualTo("deviceId", deviceId)
      .whereGreaterThanOrEqualTo("endDate", Timestamp.now())
      .get()
      .await()

    if (!existingActivation.isEmpty) {
      Log.d("Activation", "الجهاز مفعل مسبقًا")
      return (existingActivation.documents.first()["duration"] as Long).toInt()
    }

    // 2. التحقق من صحة الكود الجديد
    val codeQuery = db.collection("codes")
      .whereEqualTo("code", code)
      .whereEqualTo("isUsed", false)
      .get()
      .await()

    if (codeQuery.isEmpty) {
      Log.d("Activation", "الكود غير صالح")
      return 0
    }

    // 3. تحديث بيانات الكود
    val codeDoc = codeQuery.documents.first()
    val durationDays = (codeDoc["duration"] as Long).toInt()
    val endDate = calculateEndDate(durationDays)

    codeDoc.reference.update(
      mapOf(
        "isUsed" to true,
        "deviceId" to deviceId,
        "endDate" to Timestamp(endDate.time / 1000, (endDate.time % 1000).toInt())
      )
    ).await()

    Log.d("Activation", "تم تفعيل الكود بنجاح")
    durationDays
  } catch (e: Exception) {
    Log.e("Activation", "فشل التفعيل: ${e.message}")
    0
  }
}

private suspend fun activateCode(document: DocumentSnapshot, deviceId: String): Int {
  val durationDays = (document["duration"] as? Long)?.toInt() ?: 0
  if (durationDays <= 0) return 0

  val updates = mapOf(
    "isUsed" to true,
    "deviceId" to deviceId,
    "activationDate" to Date(),
    "endDate" to calculateEndDate(durationDays)
  )

  document.reference.update(updates).await()
  return durationDays
}

private fun calculateEndDate(days: Int): Date {
  return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
    add(Calendar.DAY_OF_YEAR, days)
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
  }.time
}

private fun getDeviceId(context: Context): String {
  return Settings.Secure.getString(
    context.contentResolver,
    Settings.Secure.ANDROID_ID
  )
}

private fun saveActivation(context: Context, durationDays: Int) {
  val endDate = calculateEndDate(durationDays)
  context.getSharedPreferences("VPN_PREFS", Context.MODE_PRIVATE).edit().apply {
    putLong("END_DATE", endDate.time)
    apply()
  }
}

private fun handleFirestoreError(e: Exception): String {
  return when {
    e.message?.contains("index") == true -> "مشكلة في الإعدادات، يرجى تحديث التطبيق"
    e is java.net.UnknownHostException -> "لا يوجد اتصال بالإنترنت"
    else -> "فشل في عملية التفعيل: ${e.localizedMessage}"
  }
}

private fun saveEndDateLocally(context: Context, endDate: Date) {
  val prefs = context.getSharedPreferences("VPN_PREFS", Context.MODE_PRIVATE)
  prefs.edit().apply {
    putLong("END_DATE", endDate.time)
    apply()
  }
  Log.d("Activation", "تم حفظ تاريخ الانتهاء: $endDate")
}


 */












/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
////package co.gulf.todvpn.vpn.ui.screens
////
////import android.content.Context
////import android.os.Build
////import android.util.Log
////import android.widget.Toast
////import androidx.compose.foundation.background
////import androidx.compose.foundation.layout.*
////import androidx.compose.material3.*
////import androidx.compose.material3.ButtonDefaults.buttonColors
////import androidx.compose.runtime.*
////import androidx.compose.ui.Alignment
////import androidx.compose.ui.Modifier
////import androidx.compose.ui.graphics.Color
////import androidx.compose.ui.platform.LocalContext
////import androidx.compose.ui.unit.dp
////import androidx.navigation.NavHostController
////import co.gulf.todvpn.vpn.ui.theme.BackgroundColor
////import co.gulf.todvpn.vpn.ui.theme.ThirdColor
////import co.gulf.todvpn.vpn.ui.widget.ErrorScreen
////import com.google.firebase.Timestamp
////import com.google.firebase.auth.FirebaseAuth
////import com.google.firebase.firestore.DocumentReference
////import com.google.firebase.firestore.DocumentSnapshot
////import com.google.firebase.firestore.FieldValue
////import com.google.firebase.firestore.FirebaseFirestore
////import java.util.*
////import kotlinx.coroutines.delay
////import kotlinx.coroutines.tasks.await
////
////// ActivationScreen.kt
////@Composable
////fun ActivationScreen(navController: NavHostController, onActivationComplete: () -> Unit) {
////  var activationCode by remember { mutableStateOf("") }
////  var attempts by remember { mutableIntStateOf(0) }
////  var cooldown by remember { mutableStateOf(false) }
////  val db = FirebaseFirestore.getInstance()
////  val context = LocalContext.current
////  val deviceId = remember { getOrCreateDeviceId(context) }
////
////  LaunchedEffect(attempts) {
////    if (attempts >= MAX_ATTEMPTS) {
////      cooldown = true
////      delay(COOLDOWN_DURATION)
////      cooldown = false
////      attempts = 0
////    }
////  }
////
////  Column(
////    modifier = Modifier
////      .fillMaxSize()
////      .background(BackgroundColor),
////    horizontalAlignment = Alignment.CenterHorizontally,
////    verticalArrangement = Arrangement.Center
////  ) {
////    Text("أدخل كود التفعيل", color = ThirdColor)
////    OutlinedTextField(
////      value = activationCode,
////      onValueChange = { activationCode = it.trim() },
////      label = { Text("كود التفعيل") },
////      modifier = Modifier
////        .fillMaxWidth()
////        .padding(16.dp),
////      colors = textFieldColors()
////    )
////
////    Button(
////      onClick = {
////        if (validateCodeFormat(activationCode)) {
////          attempts++
////          validateCode(
////            code = activationCode,
////            deviceId = deviceId,
////            db = db,
////            context = context,
////            onSuccess = {
////              storeLocalActivation(context, it)
////              onActivationComplete()
////            }
////          )
////        } else {
////          Toast.makeText(context, "صيغة الكود غير صحيحة", Toast.LENGTH_SHORT).show()
////        }
////      },
////      enabled = !cooldown && attempts < MAX_ATTEMPTS,
////      colors = buttonColors()
////    ) {
////      Text(if (cooldown) "انتظر ${COOLDOWN_DURATION / 1000 - (attempts * 10)} ثانية" else "تفعيل")
////    }
////  }
////}
////
////private fun validateCode(
////  code: String,
////  deviceId: String,
////  db: FirebaseFirestore,
////  context: Context,
////  onSuccess: (Date) -> Unit
////) {
////  db.collection("codes").document(code).get()
////    .addOnSuccessListener { document ->
////      if (document.exists()) {
////        handleValidCode(document, deviceId, db, context, onSuccess)
////      } else {
////        Toast.makeText(context, "كود التفعيل غير صحيح", Toast.LENGTH_SHORT).show()
////      }
////    }
////    .addOnFailureListener { e ->
////      Toast.makeText(context, "خطأ: ${e.message}", Toast.LENGTH_SHORT).show()
////    }
////}
////
////private fun handleValidCode(
////  document: DocumentSnapshot,
////  deviceId: String,
////  db: FirebaseFirestore,
////  context: Context,
////  onSuccess: (Date) -> Unit
////) {
////  when {
////    document.getBoolean("isUsed") == true ->
////      Toast.makeText(context, "الكود مستخدم سابقاً", Toast.LENGTH_SHORT).show()
////    document.getLong("durationDays")?.toInt() ?: 0 <= 0 ->
////      Toast.makeText(context, "مدة التفعيل غير صالحة", Toast.LENGTH_SHORT).show()
////    else -> activateUser(
////      deviceId = deviceId,
////      durationDays = document.getLong("durationDays")!!.toInt(),
////      codeRef = document.reference,
////      db = db,
////      context = context,
////      onSuccess = onSuccess
////    )
////  }
////}
////
////// FirestoreOperations.kt
////private fun activateUser(
////  deviceId: String,
////  durationDays: Int,
////  codeRef: DocumentReference,
////  db: FirebaseFirestore,
////  context: Context,
////  onSuccess: (Date) -> Unit
////) {
////  val endDate = Calendar.getInstance().apply {
////    add(Calendar.DAY_OF_YEAR, durationDays)
////  }.time
////
////  val deviceInfo = getDeviceInfo(context).toMutableMap().apply {
////    put("firstActivation", Timestamp.now())
////    put("lastSeen", FieldValue.serverTimestamp())
////  }
////
////  db.runBatch { batch ->
////    batch.update(
////      db.collection("users").document(deviceId),
////      mapOf(
////        "activated" to true,
////        "activationEnd" to Timestamp(endDate),
////        "devices" to FieldValue.arrayUnion(deviceInfo)
////      )
////    )
////    batch.update(
////      codeRef,
////      mapOf(
////        "isUsed" to true,
////        "usedBy" to deviceId,
////        "usedAt" to FieldValue.serverTimestamp()
////      )
////    )
////  }.addOnCompleteListener {
////    if (it.isSuccessful) {
////      onSuccess(endDate)
////    } else {
////      Toast.makeText(context, "فشل في التفعيل", Toast.LENGTH_SHORT).show()
////    }
////  }
////}
////
////// DeviceIdManager.kt
////private fun getOrCreateDeviceId(context: Context): String {
////  val alias = "secure_device_id"
////  val prefs = context.getSharedPreferences("device_prefs", Context.MODE_PRIVATE)
////
////  return try {
////    val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
////
////    if (!keyStore.containsAlias(alias)) {
////      createSecureKey(alias)
////      generateAndStoreId(context, alias, prefs)
////    } else {
////      retrieveStoredId(alias, prefs)
////    }
////  } catch (e: Exception) {
////    Log.e("DeviceId", "Error getting device ID", e)
////    generateFallbackId(context)
////  }
////}
////
////private fun createSecureKey(alias: String) {
////  KeyGenerator.getInstance(
////    KeyProperties.KEY_ALGORITHM_AES,
////    "AndroidKeyStore"
////  ).init(
////    KeyGenParameterSpec.Builder(
////      alias,
////      KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
////    ).apply {
////      setBlockModes(KeyProperties.BLOCK_MODE_GCM)
////      setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
////      setKeySize(256)
////    }.build()
////  ).generateKey()
////}
////
////private fun generateAndStoreId(
////  context: Context,
////  alias: String,
////  prefs: SharedPreferences
////): String {
////  val deviceId = UUID.randomUUID().toString()
////  val cipher = Cipher.getInstance("AES/GCM/NoFormat").apply {
////    init(Cipher.ENCRYPT_MODE, getKey(alias))
////  }
////
////  prefs.edit().apply {
////    putString("iv", Base64.encodeToString(cipher.iv, Base64.DEFAULT))
////    putString("device_id", Base64.encodeToString(cipher.doFinal(deviceId.toByteArray()), Base64.DEFAULT))
////    apply()
////  }
////
////  return deviceId
////}
////
////private fun retrieveStoredId(alias: String, prefs: SharedPreferences): String {
////  val iv = Base64.decode(prefs.getString("iv", ""), Base64.DEFAULT)
////  val encrypted = Base64.decode(prefs.getString("device_id", ""), Base64.DEFAULT)
////
////  val cipher = Cipher.getInstance("AES/GCM/NoFormat").apply {
////    init(Cipher.DECRYPT_MODE, getKey(alias), GCMParameterSpec(128, iv))
////  }
////
////  return String(cipher.doFinal(encrypted))
////}
////
////private fun getKey(alias: String): SecretKey {
////  return KeyStore.getInstance("AndroidKeyStore")
////    .apply { load(null) }
////    .getKey(alias, null) as SecretKey
////}
////
////// ActivationStatus.kt
////private fun checkLocalActivation(context: Context): Boolean {
////  val prefs = context.getSharedPreferences("activation_prefs", Context.MODE_PRIVATE)
////  return prefs.getLong("activation_end", 0) > System.currentTimeMillis()
////}
////
////private fun storeLocalActivation(context: Context, endDate: Date) {
////  context.getSharedPreferences("activation_prefs", Context.MODE_PRIVATE)
////    .edit()
////    .putLong("activation_end", endDate.time)
////    .apply()
////}
////
////// MainActivity.kt
////@AndroidEntryPoint
////class MainActivity : AppCompatActivity() {
////
////  override fun onCreate(savedInstanceState: Bundle?) {
////    super.onCreate(savedInstanceState)
////    setFullScreen()
////    setContent {
////      BasedVPNTheme {
////        val navController = rememberNavController()
////        val context = LocalContext.current
////        var isLoading by remember { mutableStateOf(true) }
////        var isActivated by remember { mutableStateOf(false) }
////
////        DisposableEffect(Unit) {
////          val job = CoroutineScope(Dispatchers.IO).launch {
////            val deviceId = getOrCreateDeviceId(context)
////            isActivated = checkLocalActivation(context) || checkRemoteActivation(deviceId)
////            isLoading = false
////          }
////
////          onDispose { job.cancel() }
////        }
////
////        if (isLoading) {
////          SplashScreen()
////        } else {
////          NavHost(
////            navController = navController,
////            startDestination = if (isActivated) Destination.Dashboard else Destination.Activation
////          ) {
////            composable(Destination.Dashboard) { DashboardScreen() }
////            composable(Destination.Activation) {
////              ActivationScreen(navController) {
////                isActivated = true
////                navController.navigate(Destination.Dashboard) {
////                  popUpTo(Destination.Activation) { inclusive = true }
////                }
////              }
////            }
////          }
////        }
////      }
////    }
////  }
////
////  private suspend fun checkRemoteActivation(deviceId: String): Boolean {
////    return try {
////      val doc = FirebaseFirestore.getInstance()
////        .collection("users")
////        .document(deviceId)
////        .get()
////        .await()
////
////      doc.exists() && doc.getTimestamp("activationEnd")?.toDate()?.after(Date()) == true
////    } catch (e: Exception) {
////      false
////    }
////  }
////}
////
////// Constants.kt
////const val MAX_ATTEMPTS = 3
////const val COOLDOWN_DURATION = 30000L
////val BackgroundColor = Color(0xFF1A1A1A)
////val ThirdColor = Color(0xFFD4AF37)
////
////
//
//
//
//
//
//
//
//
//package co.gulf.todvpn.vpn.ui.screens
//
//import android.content.Context
//import android.util.Log
//import android.widget.Toast
//import androidx.compose.foundation.background
//import androidx.compose.foundation.layout.*
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.platform.LocalContext
//import androidx.compose.ui.unit.dp
//import androidx.navigation.NavHostController
//import co.gulf.todvpn.vpn.ui.theme.BackgroundColor
//import co.gulf.todvpn.vpn.ui.theme.ThirdColor
//import com.google.firebase.auth.FirebaseAuth
//import com.google.firebase.firestore.FirebaseFirestore
//import java.time.Instant
//import java.time.temporal.ChronoUnit
//import java.util.*
//import kotlinx.coroutines.CoroutineScope
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.launch
//
//@Composable
//fun ActivationScreen(navController: NavHostController, onActivationComplete: () -> Unit) {
//  val context = LocalContext.current
//  val currentUser = FirebaseAuth.getInstance().currentUser
//  val db = FirebaseFirestore.getInstance()
//  var userState by remember { mutableStateOf<UserState?>(null) }
//  var showTrialScreen by remember { mutableStateOf(false) }
//  val deviceId = getDeviceId(context)
//  var isLoading by remember { mutableStateOf(false) }
//
//  LaunchedEffect(Unit) {
//    isLoading = true
//    try {
//      db.collection("users")
//        .whereEqualTo("deviceId", deviceId)
//        .get()
//        .addOnSuccessListener { querySnapshot ->
//          if (querySnapshot.isEmpty) {
//            FirebaseAuth.getInstance().signInAnonymously()
//              .addOnCompleteListener { task ->
//                isLoading = false
//                if (task.isSuccessful) {
//                  val newUser = FirebaseAuth.getInstance().currentUser
//                  newUser?.uid?.let { userId ->
//                    handleUserFirstLaunch(
//                      userId = userId,
//                      deviceId = deviceId,
//                      db = db,
//                      context = context
//                    ) { userStateResult ->
//                      userState = userStateResult
//                      checkTrialStatus(userState) { isTrialActive ->
//                        showTrialScreen = isTrialActive
//                      }
//                    }
//                  }
//                } else {
//                  Toast.makeText(context, "Anonymous sign-in failed.", Toast.LENGTH_SHORT).show()
//                  showTrialScreen = false
//                }
//              }
//          } else {
//            isLoading = false
//            val userId = querySnapshot.documents.firstOrNull()?.id
//            if (userId != null) {
//              loadUserData(userId, db, context) { data ->
//                userState = data
//                checkTrialStatus(userState) { isTrialActive ->
//                  showTrialScreen = isTrialActive
//                }
//              }
//            }
//          }
//        }
//        .addOnFailureListener {
//          isLoading = false
//          Toast.makeText(context, "Failed to load user data.", Toast.LENGTH_SHORT).show()
//          showTrialScreen = false
//        }
//    } catch (e: Exception) {
//      isLoading = false
//      Log.e("ActivationScreen", "Error loading user data", e)
//      Toast.makeText(context, "An error occurred. Please try again.", Toast.LENGTH_SHORT).show()
//    }
//  }
//
//  if (isLoading) {
//    LoadingIndicator()
//  } else {
//    if (showTrialScreen) {
//      TrialScreen(navController)
//    } else {
//      ActivationCodeScreen(
//        userId = currentUser?.uid ?: "",
//        onActivationComplete = {
//          CoroutineScope(Dispatchers.Main).launch {
//            kotlinx.coroutines.delay(1000)
//            navController.navigate("dashboard")
//          }
//        }
//      )
//    }
//  }
//}
//
//@Composable
//fun TrialScreen(navController: NavHostController) {
//  LaunchedEffect(Unit) {
//    kotlinx.coroutines.delay(5000)
//    navController.navigate(Destination.Dashboard) {
//      popUpTo(Destination.Activation) { inclusive = true }
//    }
//  }
//
//  Column(
//    modifier = Modifier
//      .fillMaxSize()
//      .background(BackgroundColor),
//    horizontalAlignment = Alignment.CenterHorizontally,
//    verticalArrangement = Arrangement.Center
//  ) {
//    Text("You are on a 7-day trial!", color = ThirdColor)
//  }
//}
//
//@Composable
//fun ActivationCodeScreen(userId: String, onActivationComplete: () -> Unit) {
//  var activationCode by remember { mutableStateOf("") }
//  val db = FirebaseFirestore.getInstance()
//  val context = LocalContext.current
//  var isLoading by remember { mutableStateOf(false) }
//
//  Column(
//    modifier = Modifier
//      .fillMaxSize()
//      .background(BackgroundColor),
//    horizontalAlignment = Alignment.CenterHorizontally,
//    verticalArrangement = Arrangement.Center
//  ) {
//    Text("Enter your activation code", color = ThirdColor)
//    OutlinedTextField(
//      value = activationCode,
//      onValueChange = { activationCode = it.trim() },
//      label = { Text("Activation Code") },
//      modifier = Modifier
//        .fillMaxWidth()
//        .padding(16.dp),
//      colors = OutlinedTextFieldDefaults.colors(
//        focusedBorderColor = Color(0xFFD4AF37),
//        unfocusedBorderColor = Color(0xFFD4AF37).copy(alpha = 0.5f),
//        focusedLabelColor = Color(0xFFD4AF37),
//        unfocusedLabelColor = Color(0xFFD4AF37).copy(alpha = 0.5f),
//        focusedTextColor = Color(0xFFD4AF37),
//        unfocusedTextColor = Color(0xFFD4AF37).copy(alpha = 0.8f)
//      )
//    )
//    Spacer(modifier = Modifier.height(16.dp))
//    Button(
//      onClick = {
//        if (activationCode.isNotEmpty()) {
//          isLoading = true
//          try {
//            db.collection("codes")
//              .whereEqualTo("code", activationCode)
//              .get()
//              .addOnSuccessListener { querySnapshot ->
//                if (!querySnapshot.isEmpty) {
//                  val document = querySnapshot.documents[0]
//                  val isUsed = document.getBoolean("isUsed") ?: false
//                  val duration = document.getLong("duration")?.toInt() ?: 0
//
//                  if (!isUsed) {
//                    val startDate = Date()
//                    val activationEndDate = Calendar.getInstance().apply {
//                      time = startDate
//                      add(Calendar.DAY_OF_YEAR, duration)
//                    }.time
//
//                    document.reference.update(
//                      "isUsed", true,
//                      "EndDate", activationEndDate,
//                      "userId", userId
//                    ).addOnSuccessListener {
//                      // تأكد من أن userId غير فارغ قبل استخدامه
//                      if (userId.isNotEmpty()) {
//                        db.collection("users").document(userId).update(
//                          "trialActive", false
//                        ).addOnSuccessListener {
//                          isLoading = false
//                          Toast.makeText(context, "Activation successful!", Toast.LENGTH_SHORT).show()
//                          onActivationComplete()
//                        }.addOnFailureListener { e ->
//                          isLoading = false
//                          Toast.makeText(context, "Failed to update user data: ${e.message}", Toast.LENGTH_SHORT).show()
//                        }
//                      } else {
//                        isLoading = false
//                        Toast.makeText(context, "Invalid user ID.", Toast.LENGTH_SHORT).show()
//                      }
//                    }.addOnFailureListener { e ->
//                      isLoading = false
//                      Toast.makeText(context, "Failed to update code status: ${e.message}", Toast.LENGTH_SHORT).show()
//                    }
//                  } else {
//                    isLoading = false
//                    Toast.makeText(context, "This code is already used.", Toast.LENGTH_SHORT).show()
//                  }
//                } else {
//                  isLoading = false
//                  Toast.makeText(context, "Code not found.", Toast.LENGTH_SHORT).show()
//                }
//              }.addOnFailureListener { e ->
//                isLoading = false
//                Toast.makeText(context, "Failed to validate the code: ${e.message}", Toast.LENGTH_SHORT).show()
//              }
//          } catch (e: Exception) {
//            isLoading = false
//            Log.e("ActivationCodeScreen", "Error validating code", e)
//            Toast.makeText(context, "An error occurred. Please try again.", Toast.LENGTH_SHORT).show()
//          }
//        } else {
//          Toast.makeText(context, "Please enter a valid code.", Toast.LENGTH_SHORT).show()
//        }
//      },
//      colors = ButtonDefaults.buttonColors(
//        containerColor = Color(0xBFFF9800),
//        contentColor = Color.White
//      )
//    ) {
//      Text("Activate")
//    }
//  }
//}
//
//@Composable
//fun LoadingIndicator() {
//  Box(
//    modifier = Modifier
//      .fillMaxSize()
//      .background(Color.Black.copy(alpha = 0.5f)),
//    contentAlignment = Alignment.Center
//  ) {
//
//
//  CircularProgressIndicator(color = Color.White)
//  }
//}
//
//private fun handleUserFirstLaunch(
//  userId: String,
//  deviceId: String,
//  db: FirebaseFirestore,
//  context: Context,
//  onComplete: (UserState?) -> Unit
//) {
//  val userDocRef = db.collection("users").document(userId)
//  userDocRef.get().addOnSuccessListener { document ->
//    if (!document.exists()) {
//      val userState = UserState(
//        startDate = Date(),
//        activationEndDate = Date.from(Instant.now().plus(7, ChronoUnit.DAYS)),
//        isTrialActive = true,
//        deviceId = deviceId
//      )
//      userDocRef.set(userState).addOnSuccessListener {
//        loadUserData(userId, db, context, onComplete)
//      }.addOnFailureListener { e ->
//        Toast.makeText(context, "Failed to create user data: ${e.message}", Toast.LENGTH_SHORT).show()
//        onComplete(null)
//      }
//    } else {
//      loadUserData(userId, db, context, onComplete)
//    }
//  }.addOnFailureListener { e ->
//    Toast.makeText(context, "Failed to load user data: ${e.message}", Toast.LENGTH_SHORT).show()
//    onComplete(null)
//  }
//}
//
//private fun loadUserData(
//  userId: String,
//  db: FirebaseFirestore,
//  context: Context,
//  onUserDataLoaded: (UserState?) -> Unit
//) {
//  val userDocRef = db.collection("users").document(userId)
//
//  userDocRef.get().addOnSuccessListener { document ->
//    if (document.exists()) {
//      val startDate = document.getTimestamp("startDate")?.toDate()
//      val activationEndDate = document.getTimestamp("activationEndDate")?.toDate()
//      val isTrialActive = document.getBoolean("isTrialActive") ?: true
//      val deviceId = document.getString("deviceId")
//
//      val userState = UserState(
//        startDate = startDate,
//        activationEndDate = activationEndDate,
//        isTrialActive = isTrialActive,
//        deviceId = deviceId
//      )
//
//      onUserDataLoaded(userState)
//    } else {
//      Toast.makeText(context, "User data not found. Please try again.", Toast.LENGTH_SHORT).show()
//      onUserDataLoaded(null)
//    }
//  }.addOnFailureListener { e ->
//    Toast.makeText(context, "Failed to load user data: ${e.message}", Toast.LENGTH_SHORT).show()
//    onUserDataLoaded(null)
//  }
//}
//
//private fun checkTrialStatus(data: UserState?, onStatusChanged: (Boolean) -> Unit) {
//  val trialEndDate = data?.startDate?.let { startDate ->
//    Calendar.getInstance().apply {
//      time = startDate
//      add(Calendar.DAY_OF_YEAR, 7) // إضافة 7 أيام لفترة التجربة
//    }.time
//  }
//
//  if (trialEndDate == null || Date().after(trialEndDate)) {
//    onStatusChanged(false) // إخفاء شاشة الـ trial
//  } else {
//    onStatusChanged(true) // إظهار شاشة الـ trial
//  }
//}
//
//data class UserState(
//  val startDate: Date?,
//  val activationEndDate: Date?,
//  val isTrialActive: Boolean,
//  val deviceId: String? = null
//)
//
//object Destination {
//  const val Dashboard = "dashboard"
//  const val Activation = "activation"
//}
//
//fun getDeviceId(context: Context): String {
//  return android.provider.Settings.Secure.getString(context.contentResolver, android.provider.Settings.Secure.ANDROID_ID)
//}
