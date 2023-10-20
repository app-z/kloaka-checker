package com.example.kloakatester


import android.Manifest.permission.READ_PHONE_NUMBERS
import android.Manifest.permission.READ_PHONE_STATE
import android.Manifest.permission.READ_SMS
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.telephony.TelephonyManager
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class MainActivity : ComponentActivity() {

    lateinit var context: Context

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        context = this

//        val name = if (ActivityCompat.checkSelfPermission(
//                this,
//                Manifest.permission.BLUETOOTH_CONNECT
//            ) != PackageManager.PERMISSION_GRANTED
//        ) {
//            // TODO: Consider calling
//            //    ActivityCompat#requestPermissions
//            // here to request the missing permissions, and then overriding
//            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
//            //                                          int[] grantResults)
//            // to handle the case where the user grants the permission. See the documentation
//            // for ActivityCompat#requestPermissions for more details.
//            return
//        } else {}


        CoroutineScope(Dispatchers.IO).launch {
            Checker.isRejectVebWiew(context).collect{
                val url = if (it) "google.com" else "https://www.geeksforgeeks.org"
                // !!!Only the original thread that created a view hierarchy can touch its views.
                withContext(Dispatchers.Main) {
                    setContent {
                        MainContent(url)
                    }
                }
            }
        }
    }
}

// Creating a composable
// function to display Top Bar
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainContent(url: String) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("GFG | WebView", color = Color.Blue) }) },
        content = { MyContent(url) }
    )
}

// Creating a composable
// function to create WebView
// Calling this function as
// content in the above function
@Composable
fun MyContent(url: String) {

    // Adding a WebView inside AndroidView
    // with layout as full screen
    AndroidView(factory = {
        WebView(it).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            webViewClient = WebViewClient()
            loadUrl(url)
        }
    }, update = {
        it.loadUrl(url)
    })
}

// For displaying preview in
// the Android Studio IDE emulator
@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    MainContent("ya.ru")
}

//val stateTelephony = remember {
//    var simCardAllowed: Boolean = false
//}

// Function will run after click to button
fun GetSimReject(context: Context, result: (Boolean) -> (Unit)) {
    if (ActivityCompat.checkSelfPermission(
            context,
            READ_SMS
        ) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
            context,
            READ_PHONE_NUMBERS
        ) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
            context,
            READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED
    ) {
        // Permission check

        // Create obj of TelephonyManager and ask for current telephone service
        val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        val phoneNumber = telephonyManager.line1Number
//s        phone_number.setText(phoneNumber)
        result(true)
        return
    } else {
        // Ask for permission
        requestPermission(context)
    }
}

private fun requestPermission(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        (context as Activity).requestPermissions(arrayOf<String>(READ_SMS, READ_PHONE_NUMBERS, READ_PHONE_STATE), 100)
    }
}

//fun onRequestPermissionsResult(
//    requestCode: Int,
//    permissions: Array<String?>?,
//    grantResults: IntArray?
//) {
//    when (requestCode) {
//        100 -> {
//            val telephonyManager =
//                this.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
//            if (ActivityCompat.checkSelfPermission(this, READ_SMS) !=
//                PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
//                    this,
//                    READ_PHONE_NUMBERS
//                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
//                    this,
//                    READ_PHONE_STATE
//                ) != PackageManager.PERMISSION_GRANTED
//            ) {
//                return
//            }
//            val phoneNumber = telephonyManager.line1Number
//            //phone_number.setText(phoneNumber)
//
//        }
//
//        else -> throw IllegalStateException("Unexpected value: $requestCode")
//    }
//}