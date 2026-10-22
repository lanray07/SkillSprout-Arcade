package com.skillsprout.arcade

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Preview-only route to exact bundled diagrams. No real explorer, attempts or payments. */
class AccessibilityReviewActivity : ComponentActivity() {
    companion object {
        private const val QA_ID = "voiceview-diagram-qa"
        private val ALLOWED = setOf("dev-maths-y1-0397", "dev-maths-y1-0399",
            "dev-maths-y1-0401", "dev-maths-y2-0404", "dev-maths-y2-0407", "dev-maths-y4-0419")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        check(BuildConfig.DEBUG && BuildConfig.DEVELOPMENT_CONTENT)
        val id = intent.getStringExtra("activityId")
        if (id !in ALLOWED) { finish(); return }
        val item = ContentPacks(this).starter().single { it.id == id }
        // GameScreen persists session position. Delete this synthetic session only.
        clearQaSessions()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContent {
            MaterialTheme(colorScheme=lightColorScheme(primary=Leaf, background=Cream, surface=Cream, onSurface=Forest)) {
                Surface {
                    Column(Modifier.fillMaxSize().systemBarsPadding()) {
                        Text("QA diagram review: ${item.id}", Modifier.padding(12.dp))
                        GameScreen(Child(QA_ID, "QA Explorer", year=item.year, voice=false),
                            Game.MATHS, listOf(item), emptyList(), null,
                            speak={}, silence={}, paused=false, onPause={}, onHome={finish()},
                            onAttempt={}, onComplete={})
                    }
                }
            }
        }
    }

    private fun clearQaSessions() {
        val preferences = getSharedPreferences("sessions", 0)
        val edit = preferences.edit()
        preferences.all.keys.filter { it.startsWith("$QA_ID-") }.forEach { edit.remove(it) }
        edit.commit()
    }

    override fun onDestroy() {
        clearQaSessions()
        super.onDestroy()
    }
}
