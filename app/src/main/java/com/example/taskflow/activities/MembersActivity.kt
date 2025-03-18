package com.example.taskflow.activities

import android.app.Activity
import android.app.Dialog
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.taskflow.R
import com.example.taskflow.adapters.MemberListItemsAdapter
import com.example.taskflow.databinding.ActivityMembersBinding
import com.example.taskflow.firebase.FirestoreClass
import com.example.taskflow.model.Board
import com.example.taskflow.model.User
import com.example.taskflow.utils.Constants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.DataOutputStream
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL

class MembersActivity : BaseActivity() {

    // A global variable for Board Details.
    private lateinit var mBoardDetails: Board

    // A global variable for Assigned Members List.
    private lateinit var mAssignedMembersList: ArrayList<User>

    // A global variable for notifying any changes done or not in the assigned members list.
    private var anyChangesDone: Boolean = false

    private var binding : ActivityMembersBinding? = null
    private lateinit var backCallback : OnBackPressedCallback
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMembersBinding.inflate(layoutInflater)
        setContentView(binding?.root)

        if (intent.hasExtra(Constants.BOARD_DETAIL)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                mBoardDetails =
                    intent.getParcelableExtra(Constants.BOARD_DETAIL, Board::class.java)!!
            }else{
                @Suppress("DEPRECATION")
                mBoardDetails = (intent.getParcelableExtra(Constants.BOARD_DETAIL) as Board?)!!
            }
        }

        setupActionBar()

        // Show the progress dialog.
        showProgressDialog(resources.getString(R.string.please_wait))
        FirestoreClass().getAssignedMembersListDetails(
            this@MembersActivity,
            mBoardDetails.assignedTo
        )

        backCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (anyChangesDone) {
                    setResult(Activity.RESULT_OK)
                }
                finish()
            }
        }

        // Register the callback
        onBackPressedDispatcher.addCallback(this, backCallback)
    }

    /**
     * A function to setup action bar
     */
    private fun setupActionBar() {

        setSupportActionBar(binding!!.toolbarMembersActivity)

        val actionBar = supportActionBar
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setHomeAsUpIndicator(R.drawable.ic_white_color_back_24dp)
        }

        binding!!.toolbarMembersActivity.setNavigationOnClickListener { backCallback.handleOnBackPressed() }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        // Inflate the menu to use in the action bar
        menuInflater.inflate(R.menu.menu_add_member, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle presses on the action bar menu items
        when (item.itemId) {
            R.id.action_add_member -> {

                dialogSearchMember()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    /**
     * A function to setup assigned members list into recyclerview.
     */
    fun setupMembersList(list: ArrayList<User>) {

        mAssignedMembersList = list

        hideProgressDialog()

        binding!!.rvMembersList.layoutManager = LinearLayoutManager(this@MembersActivity)
        binding!!.rvMembersList.setHasFixedSize(true)

        val adapter = MemberListItemsAdapter(this@MembersActivity, list)
        binding!!.rvMembersList.adapter = adapter
    }

    /**
     * Method is used to show the Custom Dialog.
     */
    private fun dialogSearchMember() {
        val dialog = Dialog(this)
        /*Set the screen content from a layout resource.
    The resource will be inflated, adding all top-level views to the screen.*/
        dialog.setContentView(R.layout.dialog_search_member)
        dialog.findViewById<TextView>(R.id.tv_add).setOnClickListener{

            val email = dialog.findViewById<EditText>(R.id.et_email_search_member).text.toString()

            if (email.isNotEmpty()) {
                dialog.dismiss()

                // Show the progress dialog.
                showProgressDialog(resources.getString(R.string.please_wait))
                FirestoreClass().getMemberDetails(this@MembersActivity, email)
            } else {
                showErrorSnackBar("Please enter members email address.")
            }
        }
        dialog.findViewById<TextView>(R.id.tv_cancel).setOnClickListener{
            dialog.dismiss()
        }
        //Start the dialog and display it on screen.
        dialog.show()
    }

    fun memberDetails(user: User) {

        mBoardDetails.assignedTo.add(user.id)

        FirestoreClass().assignMemberToBoard(this@MembersActivity, mBoardDetails, user)
    }

    /**
     * A function to get the result of assigning the members.
     */
    fun memberAssignSuccess(user: User) {

        hideProgressDialog()

        mAssignedMembersList.add(user)

        anyChangesDone = true

        setupMembersList(mAssignedMembersList)

        // TODO (Step 5: Call the AsyncTask class when the board is assigned to the user and based on the users detail send them the notification using the FCM token.)
        // START
        sendNotification(user.fcmToken, mBoardDetails.name, mAssignedMembersList[0].name)
        // END
    }

    // TODO (Step 2: Create a AsyncTask class for sending the notification to user based on the FCM Token.)
    // START
    /**
     * “A nested class marked as inner can access the members of its outer class.
     * Inner classes carry a reference to an object of an outer class:”
     * source: https://kotlinlang.org/docs/reference/nested-classes.html
     *
     * This is the background class is used to execute background task.
     *
     * For Background we have used the AsyncTask
     *
     *
     */

    private fun sendNotification(token: String, boardName: String, assignedBy: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Show progress dialog on main thread
                withContext(Dispatchers.Main) {
                    showProgressDialog("Please wait...")
                }

                val url = URL(Constants.FCM_BASE_URL)
                val connection = url.openConnection() as HttpURLConnection

                connection.apply {
                    doOutput = true
                    doInput = true
                    instanceFollowRedirects = false
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("charset", "utf-8")
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty(
                        Constants.FCM_AUTHORIZATION, "${Constants.FCM_KEY}=${Constants.FCM_SERVER_KEY}"
                    )
                    useCaches = false
                }

                // Create JSON Request
                val jsonRequest = JSONObject().apply {
                    put(Constants.FCM_KEY_TO, token)
                    put(
                        Constants.FCM_KEY_DATA, JSONObject().apply {
                            put(Constants.FCM_KEY_TITLE, "Assigned to the Board $boardName")
                            put(Constants.FCM_KEY_MESSAGE, "You have been assigned to the new board by $assignedBy")
                        }
                    )
                }

                DataOutputStream(connection.outputStream).use { wr ->
                    wr.writeBytes(jsonRequest.toString())
                    wr.flush()
                }

                val responseCode = connection.responseCode
                val result = if (responseCode == HttpURLConnection.HTTP_OK) {
                    connection.inputStream.bufferedReader().use { it.readText() }
                } else {
                    connection.responseMessage
                }

                // Hide progress dialog and log result on main thread
                withContext(Dispatchers.Main) {
                    hideProgressDialog()
                    Log.e("JSON Response Result", result)
                }

            } catch (e: SocketTimeoutException) {
                withContext(Dispatchers.Main) {
                    hideProgressDialog()
                    Log.e("Error", "Connection Timeout")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    hideProgressDialog()
                    Log.e("Error", "Error: ${e.message}")
                }
            }
        }
    }
    // END

}