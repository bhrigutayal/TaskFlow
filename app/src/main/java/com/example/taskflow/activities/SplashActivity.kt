package com.example.taskflow.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.WindowInsets
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.example.taskflow.databinding.ActivitySplashBinding
import com.example.taskflow.firebase.FirestoreClass

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    /**
     * This function is auto created by Android when the Activity Class is created.
     */
    private var binding : ActivitySplashBinding? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        //This call the parent constructor
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        // This is used to align the xml view to this class
        setContentView(binding?.root)

        // This is used to hide the status bar and make the splash screen as a full screen activity.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.hide(WindowInsets.Type.statusBars())
        } else {
            @Suppress("DEPRECATION")
            window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )
        }
        // This is used to get the file from the assets folder and set it to the title textView.
        val typeface: Typeface =
            Typeface.createFromAsset(assets, "carbon bl.ttf")
        binding!!.tvAppName.typeface = typeface

        // Adding the handler to after the a task after some delay.
        Handler(Looper.getMainLooper()).postDelayed({

            // Here if the user is signed in once and not signed out again from the app. So next time while coming into the app
            // we will redirect him to MainScreen or else to the Intro Screen as it was before.

            // Get the current user id
            val currentUserID = FirestoreClass().getCurrentUserID()
            // Start the Intro Activity

            if (currentUserID.isNotEmpty()) {
                // Start the Main Activity
                startActivity(Intent(this@SplashActivity, MainActivity::class.java))
            } else {
                // Start the Intro Activity
                startActivity(Intent(this@SplashActivity, IntroActivity::class.java))
            }
            finish() // Call this when your activity is done and should be closed.
        }, 2500) // Here we pass the delay time in milliSeconds after which the splash activity will disappear.
    }
}