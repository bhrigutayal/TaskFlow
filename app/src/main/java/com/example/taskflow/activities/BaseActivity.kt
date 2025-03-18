package com.example.taskflow.activities

import android.app.Dialog
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PersistableBundle
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.example.taskflow.R

open class BaseActivity : AppCompatActivity() {

    private var doubleBackToExitPressedOnce = false
    open var backPressedCallback: OnBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            finish()
        }
    }
    /**
     * This is a progress dialog instance which we will initialize later on.
     */
    private lateinit var mProgressDialog: Dialog

    /**
     * This function is used to show the progress dialog with the title and message to user.
     */
    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onCreate(savedInstanceState, persistentState)

        // Initialize the callback and store its reference

        // Register the callback
        onBackPressedDispatcher.addCallback(this, backPressedCallback)
    }
    fun showProgressDialog(text: String) {
        mProgressDialog = Dialog(this)

        /*Set the screen content from a layout resource.
        The resource will be inflated, adding all top-level views to the screen.*/
        mProgressDialog.setContentView(R.layout.dialog_progress)

        mProgressDialog.findViewById<TextView>(R.id.tv_progress_text).text = text

        //Start the dialog and display it on screen.
        mProgressDialog.show()
    }
    /**
     * This function is used to dismiss the progress dialog if it is visible to user.
     */
    fun hideProgressDialog() {
        mProgressDialog.dismiss()
    }

    fun getCurrentUserID(): String {
        return FirebaseAuth.getInstance().currentUser!!.uid
    }

    fun doubleBackToExit() {
        if (doubleBackToExitPressedOnce) {
            backPressedCallback.handleOnBackPressed()
            return
        }

        this.doubleBackToExitPressedOnce = true
        Toast.makeText(
                this,
                resources.getString(R.string.please_click_back_again_to_exit),
                Toast.LENGTH_SHORT
        ).show()

        Handler(Looper.getMainLooper()).postDelayed({ doubleBackToExitPressedOnce = false }, 2000)
    }

    fun showErrorSnackBar(message: String) {
        val snackBar =
                Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG)
        val snackBarView = snackBar.view
        snackBarView.setBackgroundColor(
                ContextCompat.getColor(
                        this@BaseActivity,
                        R.color.snackbar_error_color
                )
        )
        snackBar.show()
    }
}
// END