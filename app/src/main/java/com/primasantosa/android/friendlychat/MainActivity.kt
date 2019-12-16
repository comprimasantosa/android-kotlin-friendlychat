package com.primasantosa.android.friendlychat

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class MainActivity : AppCompatActivity() {

    companion object {
        // Logging purpose
        private val TAG = "MainActivity"
    }

    private lateinit var userName: String

    private lateinit var progressBar: ProgressBar
    private lateinit var photoPickerButton: ImageButton
    private lateinit var messageEditText: EditText
    private lateinit var sendButton: Button
    private lateinit var messageRecyclerView: RecyclerView

    // Firebase
    private lateinit var firebaseDatabase: FirebaseDatabase
    private lateinit var databaseReference: DatabaseReference
    private lateinit var childEventListener: ChildEventListener
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var authStateListener: FirebaseAuth.AuthStateListener

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        userName = ANONYMOUS

        // Initialize Firebase Components
        firebaseDatabase = FirebaseDatabase.getInstance()
        firebaseAuth = FirebaseAuth.getInstance()

        databaseReference = firebaseDatabase.reference.child("messages")

        // Initialize references to Views
        progressBar = findViewById(R.id.progressBar)
        photoPickerButton = findViewById(R.id.photoPickerButton)
        messageEditText = findViewById(R.id.messageEditText)
        sendButton = findViewById(R.id.sendButton)

        // Initialize RecyclerView and its adapter
        val friendMessageData = mutableListOf<FriendlyMessage?>()

        messageRecyclerView = findViewById(R.id.messageRecyclerView)
        messageRecyclerView.apply {
            adapter = FriendlyMessageAdapter(friendMessageData)
        }

        // Initialize Progress Bar
        progressBar.visibility = View.INVISIBLE

        // ImagePickerButton shows an image picker to upload a image for a message
        photoPickerButton.setOnClickListener {
            // TODO: Fire an intent to show an image picker
        }

        // Enable Send button when there's text to send
        messageEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                sendButton.isEnabled = s.toString().trim().isNotEmpty()
            }
        })

        // TODO: Input filter

        // Send button sends a message and clears the EditText
        sendButton.setOnClickListener {
            val message = FriendlyMessage(messageEditText.text.toString(), userName, null)
            databaseReference.push().setValue(message)

            // Clear input box
            messageEditText.text.clear()
        }

        // Database Read
        childEventListener = object : ChildEventListener {
            override fun onCancelled(p0: DatabaseError) {}
            override fun onChildMoved(p0: DataSnapshot, p1: String?) {}
            override fun onChildChanged(p0: DataSnapshot, p1: String?) {}
            override fun onChildRemoved(p0: DataSnapshot) {}
            override fun onChildAdded(p0: DataSnapshot, p1: String?) {
                val message = p0.getValue(FriendlyMessage::class.java)
                friendMessageData.add(message)
                messageRecyclerView.adapter?.notifyDataSetChanged()
            }
        }
        databaseReference.addChildEventListener(childEventListener)


    }
}
