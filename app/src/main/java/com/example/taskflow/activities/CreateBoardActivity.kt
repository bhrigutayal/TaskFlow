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
import com.example.taskflow.databinding.ActivityCreateBoardBinding
import com.example.taskflow.firebase.FirestoreClass
import com.example.taskflow.model.Board
import com.example.taskflow.utils.Constants
import java.io.IOException

class CreateBoardActivity : BaseActivity() {

    // Add a global variable for URI of a selected image from phone storage.
    private var mSelectedImageFileUri: Uri? = null

    // A global variable for Username
    private lateinit var mUserName: String

    // A global variable for a board image URL
    private var mBoardImageURL: String = ""

    private var binding : ActivityCreateBoardBinding? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateBoardBinding.inflate(layoutInflater)
        setContentView(binding?.root)

        setupActionBar()

        if (intent.hasExtra(Constants.NAME)) {
            mUserName = intent.getStringExtra(Constants.NAME)!!
        }

        binding!!.ivBoardImage.setOnClickListener {
            checkPermissionAndPickImage()
        }

        binding!!.btnCreate.setOnClickListener {

                // Here if the image is not selected then update the other details of user.
                if (mSelectedImageFileUri != null) {

                    uploadBoardImage()
                } else {

                    showProgressDialog(resources.getString(R.string.please_wait))

                    // Call a function to update create a board.
                    createBoard()
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
                    Glide.with(this@CreateBoardActivity)
                        .load(mSelectedImageFileUri)
                        .centerCrop()
                        .placeholder(R.drawable.ic_user_place_holder)
                        .into(binding!!.ivBoardImage)
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

        setSupportActionBar(binding!!.toolbarCreateBoardActivity)

        val actionBar = supportActionBar
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setHomeAsUpIndicator(R.drawable.ic_white_color_back_24dp)
        }

        binding!!.toolbarCreateBoardActivity.setNavigationOnClickListener { backPressedCallback.handleOnBackPressed()}
    }

    /**
     * A function to upload the Board Image to storage and getting the downloadable URL of the image.
     */
    private fun uploadBoardImage() {
        showProgressDialog(resources.getString(R.string.please_wait))

        //getting the storage reference
        val sRef: StorageReference = FirebaseStorage.getInstance().reference.child(
                "BOARD_IMAGE" + System.currentTimeMillis() + "."
                        + Constants.getFileExtension(this@CreateBoardActivity, mSelectedImageFileUri)
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
                                mBoardImageURL = uri.toString()

                                // Call a function to create the board.
                                createBoard()
                            }
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(
                            this@CreateBoardActivity,
                            exception.message,
                            Toast.LENGTH_LONG
                    ).show()

                    hideProgressDialog()
                }
    }

    /**
     * A function to make an entry of a board in the database.
     */
    private fun createBoard() {

        //  A list is created to add the assigned menu_members.
        //  This can be modified later on as of now the user itself will be the member of the board.
        val assignedUsersArrayList: ArrayList<String> = ArrayList()
        assignedUsersArrayList.add(getCurrentUserID()) // adding the current user id.

        // Creating the instance of the Board and adding the values as per parameters.
        val board = Board(
            binding!!.etBoardName.text.toString(),
                mBoardImageURL,
                mUserName,
                assignedUsersArrayList
        )

        FirestoreClass().createBoard(this@CreateBoardActivity, board)
    }

    /**
     * A function for notifying the board is created successfully.
     */
    fun boardCreatedSuccessfully() {

        hideProgressDialog()

        setResult(Activity.RESULT_OK)
        finish()
    }
}