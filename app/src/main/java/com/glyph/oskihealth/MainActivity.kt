package com.glyph.oskihealth

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.widget.LinearLayout
import android.widget.RatingBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.preference.PreferenceManager
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.glyph.oskihealth.databinding.ActivityMainBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.patrykandpatrick.vico.core.extension.floor


class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        val securePrefs = EncryptedSharedPreferences(this, "secure_prefs", MasterKey(this))
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val username = securePrefs.getString("name", null)
        val password = securePrefs.getString("password", null)
        if (username == null || password == null) {
            startActivity(Intent(this, OnboardingActivity::class.java))
        } else {
            AuthorisedRequest.USERNAME = username
            AuthorisedRequest.PASSWORD = password
        }
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        binding.bottomNavigation.setupWithNavController(navController)
        appBarConfiguration = AppBarConfiguration(setOf(R.id.chatFragment, R.id.analyticsFragment))
        setupActionBarWithNavController(navController, appBarConfiguration)

        val currentTime = System.currentTimeMillis()
        // One day has passed since you last clicked
        if ((currentTime / 86400000f).floor - (sharedPreferences.getLong("lastTime", 0)/86400000f).floor >= 1) {
            val linearLayout = LinearLayout(this)
            val ratingBar = RatingBar(this)
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

            linearLayout.gravity = Gravity.CENTER
            ratingBar.layoutParams = lp
            ratingBar.numStars = 4
            ratingBar.stepSize = 1f

            linearLayout.addView(ratingBar)

            MaterialAlertDialogBuilder(this)
                .setTitle("How are you feeling today?")
                .setView(linearLayout)
                .setPositiveButton("Done") { dialogInterface: DialogInterface, i: Int ->
                    val checkInData = EncryptedSharedPreferences(
                        this, "checkIn", MasterKey(this)
                    )
                    checkInData.edit().putFloat((currentTime / 86400000f).floor.toString(), ratingBar.rating).apply()
                    sharedPreferences.edit().putLong("lastTime", System.currentTimeMillis()).apply()
                    dialogInterface.dismiss()
                }
                .setCancelable(false)
                .show()
        }

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }
}