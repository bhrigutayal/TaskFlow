package com.example.taskflow.activities

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.GravityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.example.taskflow.R
import com.example.taskflow.adapters.BoardItemsAdapter
import com.example.taskflow.databinding.ActivityMainBinding
import com.example.taskflow.firebase.FirestoreClass
import com.example.taskflow.model.Board
import com.example.taskflow.model.User
import com.example.taskflow.utils.Constants
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : BaseActivity(), NavigationView.OnNavigationItemSelectedListener {

    // A global variable for User Name
    private lateinit var mUserName: String

    // A global variable for SharedPreferences
    private lateinit var mSharedPreferences: SharedPreferences

    private val profileActivityLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Get the user updated details.
            FirestoreClass().loadUserData(this@MainActivity)
        }else {
            Log.e("Cancelled", "Cancelled")
        }
    }
    private val boardActivityLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK

        ) { // Get the latest boards list.
            FirestoreClass().getBoardsList(this@MainActivity)
        } else {
            Log.e("Cancelled", "Cancelled")
        }
    }


    /**
     * This function is auto created by Android when the Activity Class is created.
     */
    private var binding : ActivityMainBinding? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        //This call the parent constructor
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        // This is used to align the xml view to this class
        setContentView(binding?.root)

        setupActionBar()

        // Assign the NavigationView.OnNavigationItemSelectedListener to navigation view.
        binding!!.navView.setNavigationItemSelectedListener(this)

        mSharedPreferences =
            this.getSharedPreferences(Constants.TASKFLOW_PREFERENCES, Context.MODE_PRIVATE)

        // Variable is used get the value either token is updated in the database or not.
        val tokenUpdated = mSharedPreferences.getBoolean(Constants.FCM_TOKEN_UPDATED, false)

        // Here if the token is already updated than we don't need to update it every time.
        if (tokenUpdated) {
            // Get the current logged in user details.
            // Show the progress dialog.
            showProgressDialog(resources.getString(R.string.please_wait))
            FirestoreClass().loadUserData(this@MainActivity, true)
        } else {
            FirebaseMessaging.getInstance()
                .token.addOnSuccessListener(this@MainActivity) { instanceIdResult ->
                updateFCMToken(instanceIdResult)
            }
        }

        binding!!.appBarMain.fabCreateBoard.setOnClickListener {
            val intent = Intent(this@MainActivity, CreateBoardActivity::class.java)
            intent.putExtra(Constants.NAME, mUserName)
            boardActivityLauncher.launch(intent)
        }

        val backPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding!!.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    binding!!.drawerLayout.closeDrawer(GravityCompat.START)
                } else {
                    // A double back press function is added in Base Activity.
                    doubleBackToExit()
                }
            }
        }

        // Register the callback
        onBackPressedDispatcher.addCallback(this, backPressedCallback)
    }



    override fun onNavigationItemSelected(menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {
            R.id.nav_my_profile -> {

                profileActivityLauncher.launch(
                    Intent(this@MainActivity, MyProfileActivity::class.java)
                )
            }

            R.id.nav_sign_out -> {
                // Here sign outs the user from firebase in this device.
                FirebaseAuth.getInstance().signOut()

                mSharedPreferences.edit().clear().apply()

                // Send the user to the intro screen of the application.
                val intent = Intent(this, IntroActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                finish()
            }
        }
        binding!!.drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    /**
     * A function to setup action bar
     */
    private fun setupActionBar() {

        setSupportActionBar(binding!!.appBarMain.toolbarMainActivity)
        binding!!.appBarMain.toolbarMainActivity.setNavigationIcon(R.drawable.ic_action_navigation_menu)

        binding!!.appBarMain.toolbarMainActivity.setNavigationOnClickListener {
            toggleDrawer()
        }
    }

    /**
     * A function for opening and closing the Navigation Drawer.
     */
    private fun toggleDrawer() {

        if (binding!!.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding!!.drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            binding!!.drawerLayout.openDrawer(GravityCompat.START)
        }
    }

    /**
     * A function to get the current user details from firebase.
     */
    fun updateNavigationUserDetails(user: User, readBoardsList: Boolean) {

        hideProgressDialog()

        mUserName = user.name

        // The instance of the header view of the navigation view.
        val headerView = binding!!.navView.getHeaderView(0)

        // The instance of the user image of the navigation view.
        val navUserImage = headerView.findViewById<ImageView>(R.id.iv_user_image)

        // Load the user image in the ImageView.
        Glide
            .with(this@MainActivity)
            .load(user.image) // URL of the image
            .centerCrop() // Scale type of the image.
            .placeholder(R.drawable.ic_user_place_holder) // A default place holder
            .into(navUserImage) // the view in which the image will be loaded.

        // The instance of the user name TextView of the navigation view.
        val navUsername = headerView.findViewById<TextView>(R.id.tv_username)
        // Set the user name
        navUsername.text = user.name

        if (readBoardsList) {
            // Show the progress dialog.
            showProgressDialog(resources.getString(R.string.please_wait))
            FirestoreClass().getBoardsList(this@MainActivity)
        }
    }

    /**
     * A function to update the user's FCM token into the database.
     */
    private fun updateFCMToken(token: String) {

        val userHashMap = HashMap<String, Any>()
        userHashMap[Constants.FCM_TOKEN] = token

        // Update the data in the database.
        // Show the progress dialog.
        showProgressDialog(resources.getString(R.string.please_wait))
        FirestoreClass().updateUserProfileData(this@MainActivity, userHashMap)
    }

    /**
     * A function to populate the result of BOARDS list in the UI i.e in the recyclerView.
     */
    fun populateBoardsListToUI(boardsList: ArrayList<Board>) {

        hideProgressDialog()

        if (boardsList.size > 0) {

            binding!!.appBarMain.mainContent.rvBoardsList.visibility = View.VISIBLE
            binding!!.appBarMain.mainContent.tvNoBoardsAvailable.visibility = View.GONE

            binding!!.appBarMain.mainContent.rvBoardsList.layoutManager = LinearLayoutManager(this@MainActivity)
            binding!!.appBarMain.mainContent.rvBoardsList.setHasFixedSize(true)

            // Create an instance of BoardItemsAdapter and pass the boardList to it.
            val adapter = BoardItemsAdapter(this@MainActivity, boardsList)
            binding!!.appBarMain.mainContent.rvBoardsList.adapter = adapter // Attach the adapter to the recyclerView.

            adapter.setOnClickListener(object :
                BoardItemsAdapter.OnClickListener {
                override fun onClick(position: Int, model: Board) {
                    val intent = Intent(this@MainActivity, TaskListActivity::class.java)
                    intent.putExtra(Constants.DOCUMENT_ID, model.documentId)
                    startActivity(intent)
                }
            })
        } else {
            binding!!.appBarMain.mainContent.rvBoardsList.visibility = View.GONE
            binding!!.appBarMain.mainContent.tvNoBoardsAvailable.visibility = View.VISIBLE
        }
    }

    /**
     * A function to notify the token is updated successfully in the database.
     */
    fun tokenUpdateSuccess() {

        hideProgressDialog()

        // Here we have added a another value in shared preference that the token is updated in the database successfully.
        // So we don't need to update it every time.
        val editor: SharedPreferences.Editor = mSharedPreferences.edit()
        editor.putBoolean(Constants.FCM_TOKEN_UPDATED, true)
        editor.apply()

        // Get the current logged in user details.
        // Show the progress dialog.
        showProgressDialog(resources.getString(R.string.please_wait))
        FirestoreClass().loadUserData(this@MainActivity, true)
    }
}