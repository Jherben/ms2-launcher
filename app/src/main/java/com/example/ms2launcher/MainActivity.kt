package com.example.ms2launcher

import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private lateinit var pkgField: EditText
    private lateinit var actField: EditText
    private lateinit var pathField: EditText

    private val prefs by lazy { getSharedPreferences("ms2", Context.MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val pad = dp(20)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad * 2, pad, pad)
        }

        root.addView(TextView(this).apply {
            text = "MapleStory 2 Offline"
            textSize = 26f
            setTypeface(null, Typeface.BOLD)
        })
        root.addView(TextView(this).apply {
            text = "One-tap launcher for your Winlator container"
            textSize = 14f
            setPadding(0, dp(4), 0, dp(20))
        })

        val play = Button(this).apply {
            text = "PLAY"
            textSize = 22f
            setOnClickListener { play() }
        }
        root.addView(play, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(88)))

        root.addView(label("Setup"))

        pkgField = field("App package (e.g. com.winlator)", prefs.getString("pkg", "com.winlator")!!)
        root.addView(pkgField)

        root.addView(Button(this).apply {
            text = "Pick from installed apps"
            setOnClickListener { pickApp() }
        })

        root.addView(label("Advanced (optional)"))
        root.addView(TextView(this).apply {
            text = "Leave both empty to just open the app. To jump straight into a " +
                "shortcut, fill in the activity and the shortcut path of your " +
                "Winlator variant."
            textSize = 12f
        })
        actField = field("Activity (e.g. com.winlator.XServerDisplayActivity)",
            prefs.getString("act", "")!!)
        root.addView(actField)
        pathField = field("Shortcut path (.desktop file)", prefs.getString("path", "")!!)
        root.addView(pathField)

        root.addView(Button(this).apply {
            text = "Save"
            setOnClickListener { save(); toast("Saved") }
        })

        setContentView(ScrollView(this).apply { addView(root) })
    }

    private fun play() {
        save()
        val pkg = pkgField.text.toString().trim()
        val act = actField.text.toString().trim()
        val path = pathField.text.toString().trim()

        if (pkg.isEmpty()) { toast("Set the app package first"); return }

        val intent: Intent? = if (act.isNotEmpty()) {
            val cls = if (act.startsWith(".")) pkg + act else act
            Intent().setClassName(pkg, cls).apply {
                if (path.isNotEmpty()) putExtra("shortcut_path", path)
            }
        } else {
            packageManager.getLaunchIntentForPackage(pkg)
        }

        if (intent == null) { toast("App not installed: $pkg"); return }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            toast("Activity not found. Check the name.")
        } catch (e: SecurityException) {
            toast("That activity can't be opened from other apps. Leave Advanced empty.")
        }
    }

    private fun pickApp() {
        val q = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val apps = packageManager.queryIntentActivities(q, 0)
            .sortedBy { it.loadLabel(packageManager).toString().lowercase() }
        val labels = apps.map { it.loadLabel(packageManager).toString() }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Choose Winlator")
            .setItems(labels) { _, i ->
                pkgField.setText(apps[i].activityInfo.packageName)
            }
            .show()
    }

    private fun save() {
        prefs.edit()
            .putString("pkg", pkgField.text.toString().trim())
            .putString("act", actField.text.toString().trim())
            .putString("path", pathField.text.toString().trim())
            .apply()
    }

    private fun field(hint: String, value: String) = EditText(this).apply {
        this.hint = hint
        setText(value)
        setSingleLine()
        inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
    }

    private fun label(text: String) = TextView(this).apply {
        this.text = text
        textSize = 16f
        setTypeface(null, Typeface.BOLD)
        gravity = Gravity.START
        setPadding(0, dp(24), 0, dp(6))
        visibility = View.VISIBLE
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun toast(m: String) = Toast.makeText(this, m, Toast.LENGTH_SHORT).show()
}
