package com.daram.keyboard

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.daram.keyboard.settings.SettingsActivity

/**
 * 런처 진입점.
 * 앱 설치 후 키보드 활성화 안내 및 설정으로의 진입을 제공한다.
 * 이 Activity는 단순한 안내 화면이므로 IME 서비스와 독립적으로 동작한다.
 */
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(64, 64, 64, 64)
        }

        val title = TextView(this).apply {
            text = getString(R.string.app_name)
            textSize = 24f
            setPadding(0, 0, 0, 32)
        }

        val statusText = TextView(this).apply {
            textSize = 15f
            setPadding(0, 0, 0, 24)
        }

        val enableButton = Button(this).apply {
            text = "키보드 활성화 (시스템 설정)"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
            }
        }

        val settingsButton = Button(this).apply {
            text = getString(R.string.settings_title)
            setOnClickListener {
                startActivity(Intent(this@MainActivity, SettingsActivity::class.java))
            }
        }

        layout.addView(title)
        layout.addView(statusText)
        layout.addView(enableButton)
        layout.addView(settingsButton)
        setContentView(layout)

        // 활성화 상태 표시
        updateStatus(statusText)
    }

    override fun onResume() {
        super.onResume()
        val statusText = (window.decorView.rootView as? LinearLayout)
            ?.getChildAt(1) as? TextView
        statusText?.let { updateStatus(it) }
    }

    private fun updateStatus(textView: TextView) {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        val enabledMethods = imm.enabledInputMethodList
        val isEnabled = enabledMethods.any {
            it.packageName == packageName
        }
        textView.text = if (isEnabled) {
            "✓ 다람 키보드가 활성화되어 있습니다."
        } else {
            "다람 키보드가 아직 활성화되지 않았습니다.\n아래 버튼을 눌러 시스템 설정에서 활성화하세요."
        }
    }
}
