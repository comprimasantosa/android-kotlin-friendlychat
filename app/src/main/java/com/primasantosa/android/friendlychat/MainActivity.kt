package com.primasantosa.android.friendlychat

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.auth.AuthUI
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference

class MainActivity : AppCompatActivity() {

    companion object {
        // Logging purpose
        private val TAG = "MainActivity"
    }

    private var username: String? = ANONYMOUS
    private lateinit var friendlyMessageData: MutableList<FriendlyMessage>

    private lateinit var progressBar: ProgressBar
    private lateinit var photoPickerButton: ImageButton
    private lateinit var messageEditText: EditText
    private lateinit var sendButton: Button
    private lateinit var messageRecyclerView: RecyclerView

    // Firebase
    private lateinit var firebaseDatabase: FirebaseDatabase
    private lateinit var databaseReference: DatabaseReference
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firebaseStorage: FirebaseStorage
    private lateinit var storageReference: StorageReference
    private var authStateListener: FirebaseAuth.AuthStateListener? = null
    private var childEventListener: ChildEventListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize Firebase Components
        firebaseDatabase = FirebaseDatabase.getInstance()
        firebaseAuth = FirebaseAuth.getInstance()
        firebaseStorage = FirebaseStorage.getInstance()

        databaseReference = firebaseDatabase.reference.child("messages")
        storageReference = firebaseStorage.reference.child("chat_photos")

        // Initialize references to Views
        progressBar = findViewById(R.id.progressBar)
        photoPickerButton = findViewById(R.id.photoPickerButton)
        messageEditText = findViewById(R.id.messageEditText)
        sendButton = findViewById(R.id.sendButton)

        // Initialize RecyclerView and its adapter
        friendlyMessageData = mutableListOf()
        messageRecyclerView = findViewById(R.id.messageRecyclerView)
        messageRecyclerView.apply {
            adapter = FriendlyMessageAdapter(friendlyMessageData)
        }

        // Initialize Progress Bar
        progressBar.visibility = View.INVISIBLE

        // ImagePickerButton shows an image picker to upload a image for a message
        photoPickerButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "image/jpeg"
            intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true)
            startActivityForResult(
                Intent.createChooser(intent, "Complete action using"),
                RC_PHOTO_PICKER
            )
        }

        // Enable Send button when there's text to send
        messageEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                sendButton.isEnabled = s.toString().trim().isNotEmpty()
            }
        })

        // Length filter
        messageEditText.filters += InputFilter.LengthFilter(DEFAULT_MSG_LENGTH_LIMIT)

        // Send button sends a message and clears the EditText
        sendButton.setOnClickListener {
            val message = FriendlyMessage(messageEditText.text.toString(), username, null)
            databaseReference.push().setValue(message)

            // Clear input box
            messageEditText.text.clear()
        }

        // Firebase Auth
        authStateListener = FirebaseAuth.AuthStateListener {
            val user = firebaseAuth.currentUser
            if (user != null) {
                // user is signed in
                onSignedInInitialize(user.displayName)
            } else {
                // user is signed out
                onSignedOutCleanup()

                val providers = arrayListOf(
                    AuthUI.IdpConfig.EmailBuilder().build(),
                    AuthUI.IdpConfig.GoogleBuilder().build()
                )

                startActivityForResult(
                    AuthUI.getInstance()
                        .createSignInIntentBuilder()
                        .setIsSmartLockEnabled(false)
                        .setAvailableProviders(providers)
                        .build(),
                    RC_SIGN_IN
                )
            }
        }

    }

    private fun onSignedOutCleanup() {
        username = ANONYMOUS
        friendlyMessageData.clear()
        detachDatabaseReadListener()
    }

    private fun detachDatabaseReadListener() {
        childEventListener?.let { databaseReference.removeEventListener(it) }
        childEventListener = null
    }

    private fun onSignedInInitialize(username: String?) {
        this.username = username
        attachDatabaseReadListener()
    }

    private fun attachDatabaseReadListener() {
        // Database Read
        if (childEventListener == null) {
            childEventListener = object : ChildEventListener {
                override fun onCancelled(p0: DatabaseError) {}
                override fun onChildMoved(p0: DataSnapshot, p1: String?) {}
                override fun onChildChanged(p0: DataSnapshot, p1: String?) {}
                override fun onChildRemoved(p0: DataSnapshot) {}
                override fun onChildAdded(p0: DataSnapshot, p1: String?) {
                    val message = p0.getValue(FriendlyMessage::class.java)
                    friendlyMessageData.add(message!!)
                    messageRecyclerView.smoothScrollToPosition(friendlyMessageData.count() - 1)
                    messageRecyclerView.adapter?.notifyDataSetChanged()
                }
            }
            databaseReference.addChildEventListener(childEventListener!!)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            if (resultCode == Activity.RESULT_OK) {
                Toast.makeText(this, "Signed in!", Toast.LENGTH_SHORT).show()
            } else if (resultCode == Activity.RESULT_CANCELED) {
                Toast.makeText(this, "Sign in canceled.", Toast.LENGTH_SHORT).show()
                finish()
            }
        } else if (requestCode == RC_PHOTO_PICKER && resultCode == Activity.RESULT_OK) {
            val selectedImageUri = data?.data

            // Get reference to store file at chat_photos/<FILENAME>
            val photoRef = storageReference.child(selectedImageUri?.lastPathSegment!!)

            // Upload file to Firebase Storage
            photoRef.putFile(selectedImageUri).continueWithTask { task ->
                if (!task.isSuccessful) {
                    task.exception?.let {
                        throw task.exception!!
                    }
                }
                // Continue with the task to get the download URL
                photoRef.downloadUrl
            }.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val downloadUri = task.result
                    val friendlyMessage = FriendlyMessage(null, username, downloadUri.toString())
                    databaseReference.push().setValue(friendlyMessage)
                } else {
                    // Handle failures
                    // ...
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.sign_out_menu -> {
                AuthUI.getInstance().signOut(this)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onResume() {
        super.onResume()
        firebaseAuth.addAuthStateListener(authStateListener!!)
    }

    override fun onPause() {
        super.onPause()
        if (authStateListener != null) {
            firebaseAuth.removeAuthStateListener(authStateListener!!)
        }
        detachDatabaseReadListener()
        friendlyMessageData.clear()
    }
}
