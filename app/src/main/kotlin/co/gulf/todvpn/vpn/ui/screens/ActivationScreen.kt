

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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(BackgroundColor),
    contentAlignment = Alignment.Center
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
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
                popUpTo("activation") { inclusive = true }
              }
            } else {
              errorMessage = "The code is invalid or already used"
            }

            isLoading = false
          }
        },
        colors = ButtonDefaults.buttonColors(
          containerColor = Color(0xBFFF9800),
          contentColor = Color.White
        ),
        enabled = !isLoading
      ) {
        if (isLoading) CircularProgressIndicator(Modifier.size(24.dp))
        else Text("Activate")
      }


      errorMessage?.let {
        Spacer(modifier = Modifier.height(16.dp)) // مسافة بين الزرار والرسالة
        Text(
          text = it,
          color = Color.Red,
          fontSize = 18.sp,
          modifier = Modifier
            .fillMaxWidth()
            .wrapContentWidth(Alignment.CenterHorizontally),
          textAlign = TextAlign.Center
        )
      }
    }


  }


}

private suspend fun checkActivationCode(code: String, context: Context): Boolean {
  return try {
    val db = FirebaseFirestore.getInstance()
    val deviceId = getDeviceId(context)


    val query = db.collection("codes")
      .whereEqualTo("code", code)
      .get()
      .await()


    if (query.isEmpty) return false

    val document = query.documents[0]
    val isUsed = document["isUsed"] as? Boolean ?: false
    val storedDeviceId = document["deviceId"] as? String


    if (isUsed) {
      return storedDeviceId == deviceId
    }


    val durationDays = document["duration"] as? Long ?: return false


    val endDate = Calendar.getInstance().apply {
      add(Calendar.DAY_OF_YEAR, durationDays.toInt())
    }.time


    document.reference.update(
      mapOf(
        "isUsed" to true,
        "deviceId" to deviceId,
        "endDate" to endDate
      )
    ).await()


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
