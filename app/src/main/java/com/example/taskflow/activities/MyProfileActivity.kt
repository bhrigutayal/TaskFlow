package com.example.taskflow.activities

import android.Manifest
import android.app.Activity
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.bumptech.glide.Glide
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.example.taskflow.R
import com.example.taskflow.databinding.ActivityMyProfileBinding
import com.example.taskflow.firebase.FirestoreClass
import com.example.taskflow.model.User
import com.example.taskflow.utils.Constants
import java.io.IOException

class MyProfileActivity : BaseActivity() {

    // Add a global variable for URI of a selected image from phone storage.
    private var mSelectedImageFileUri: Uri? = null

    // A global variable for user details.
    private lateinit var mUserDetails: User

    // A global variable for a user profile image URL
    private var mProfileImageURL: String = ""

    private var binding : ActivityMyProfileBinding? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMyProfileBinding.inflate(layoutInflater)
        setContentView(binding?.root)

        setupActionBar()

        FirestoreClass().loadUserData(this@MyProfileActivity)

        binding!!.ivProfileUserImage.setOnClickListener {
            checkPermissionAndPickImage()
        }

        binding!!.btnUpdate.setOnClickListener {

            // Here if the image is not selected then update the other details of user.
            if (mSelectedImageFileUri != null) {

                uploadUserImage()
            } else {

                showProgressDialog(resources.getString(R.string.please_wait))

                // Call a function to update user details in the database.
                updateUserProfileData()
            }
        }
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                pickImageLauncher.launch("image/*")
            } else {
                Toast.makeText(this, "Storage permission denied!", Toast.LENGTH_SHORT).show()
            }
        }

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri != null) {
                mSelectedImageFileUri = uri
                try {
                    Glide.with(this@MyProfileActivity)
                        .load(mSelectedImageFileUri)
                        .centerCrop()
                        .placeholder(R.drawable.ic_user_place_holder)
                        .into(binding!!.ivProfileUserImage)
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }

    private fun checkPermissionAndPickImage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ only needs READ_MEDIA_IMAGES
            requestPermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            // Android 12 and below needs READ_EXTERNAL_STORAGE
            requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    /**
     * A function to setup action bar
     */
    private fun setupActionBar() {

        setSupportActionBar(binding!!.toolbarMyProfileActivity)

        val actionBar = supportActionBar
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setHomeAsUpIndicator(R.drawable.ic_white_color_back_24dp)
            actionBar.title = resources.getString(R.string.my_profile)
        }

        binding!!.toolbarMyProfileActivity.setNavigationOnClickListener { backPressedCallback.handleOnBackPressed() }
    }

    /**
     * A function to set the existing details in UI.
     */
    fun setUserDataInUI(user: User) {

        // Initialize the user details variable
        mUserDetails = user

        Glide
                .with(this@MyProfileActivity)
                .load(user.image)
                .centerCrop()
                .placeholder(R.drawable.ic_user_place_holder)
                .into(binding!!.ivProfileUserImage)

        binding!!.etName.setText(user.name)
        binding!!.etEmail.setText(user.email)
        if (user.mobile != 0L) {
            val mobile = user.mobile.toString()
            binding!!.etMobile.setText(mobile)
        }
    }


    /**
     * A function to upload the selected user image to firebase cloud storage.
     */
    private fun uploadUserImage() {

        showProgressDialog(resources.getString(R.string.please_wait))

        if (mSelectedImageFileUri != null) {

            //getting the storage reference
            val sRef: StorageReference = FirebaseStorage.getInstance().reference.child(
                    "USER_IMAGE" + System.currentTimeMillis() + "."
                            + Constants.getFileExtension(this@MyProfileActivity, mSelectedImageFileUri)
            )

            //adding the file to reference
            sRef.putFile(mSelectedImageFileUri!!)
                    .addOnSuccessListener { taskSnapshot ->
                        // The image upload is success
                        Log.e(
                                "Firebase Image URL",
                                taskSnapshot.metadata!!.reference!!.downloadUrl.toString()
                        )

                        // Get the downloadable url from the task snapshot
                        taskSnapshot.metadata!!.reference!!.downloadUrl
                                .addOnSuccessListener { uri ->
                                    Log.e("Downloadable Image URL", uri.toString())

                                    // assign the image url to the variable.
                                    mProfileImageURL = uri.toString()

                                    // Call a function to update user details in the database.
                                    updateUserProfileData()
                                }
                    }
                    .addOnFailureListener { exception ->
                        Toast.makeText(
                                this@MyProfileActivity,
                                exception.message,
                                Toast.LENGTH_LONG
                        ).show()

                        hideProgressDialog()
                    }
        }
    }

    /**
     * A function to update the user profile details into the database.
     */
    private fun updateUserProfileData() {

        val userHashMap = HashMap<String, Any>()

        if (mProfileImageURL.isNotEmpty() && mProfileImageURL != mUserDetails.image) {
            userHashMap[Constants.IMAGE] = mProfileImageURL
        }

        if (binding!!.etName.text.toString() != mUserDetails.name) {
            userHashMap[Constants.NAME] = binding!!.etName.text.toString()
        }

        if (binding!!.etMobile.text.toString() != mUserDetails.mobile.toString()) {
            userHashMap[Constants.MOBILE] = binding!!.etMobile.text.toString().toLong()
        }

        // Update the data in the database.
        FirestoreClass().updateUserProfileData(this@MyProfileActivity, userHashMap)
    }

    /**
     * A function to notify the user profile is updated successfully.
     */
    fun profileUpdateSuccess() {

        hideProgressDialog()

        Toast.makeText(this@MyProfileActivity, "Profile updated successfully!", Toast.LENGTH_SHORT).show()

        setResult(Activity.RESULT_OK)
        finish()
    }
}