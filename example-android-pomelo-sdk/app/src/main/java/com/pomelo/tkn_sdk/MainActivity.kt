package com.pomelo.tkn_sdk

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.pomelo.sdk.pushprovisioning.PomeloEnvironment
import com.pomelo.sdk.pushprovisioning.PomeloLogLevel
import com.pomelo.sdk.pushprovisioning.PomeloPushProvisioning
import com.pomelo.tkn_sdk.ui.screens.home.HomeComposable
import com.pomelo.tkn_sdk.ui.theme.Tkn_sdkTheme

class MainActivity : ComponentActivity() {
  private val pushProvisioning = PomeloPushProvisioning(
      environment = PomeloEnvironment.STAGE,
      logLevel = PomeloLogLevel.BODY,
  )

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge(
        statusBarStyle =
            SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT,
            )
    )
    setContent { Tkn_sdkTheme { HomeComposable(pushProvisioning = pushProvisioning) } }
  }
}
