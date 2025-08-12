package com.example.seitasv2

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.seitasv2.ui.common.PeachButton
import com.example.seitasv2.ui.common.PeachScreen
import com.example.seitasv2.ui.theme.Seitasv2Theme

class WelcomeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (SessionManager(this).isLoggedIn()) {
            startActivity(Intent(this, MainActivity::class.java))
            finish(); return
        }

        setContent {
            Seitasv2Theme {
                PeachScreen {
                    Spacer(Modifier.height(24.dp))
                    PeachButton(
                        text = stringResource(R.string.btn_empezar),
                        onClick = { startActivity(Intent(this@WelcomeActivity, LoginActivity::class.java)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(24.dp))
                    Text(stringResource(R.string.welcome_subtitle))
                }
            }
        }
    }
}
