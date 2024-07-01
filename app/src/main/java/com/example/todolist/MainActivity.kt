package com.example.todolist


import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.todolist.Model.ToDoItem
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.UUID


class MainActivity : AppCompatActivity(), ItemRowListener {

    lateinit var mDatabase: DatabaseReference
    var toDoItemList: MutableList<ToDoItem>? = null
    lateinit var adapter: ToDoItemAdapter
    private var listViewItems: ListView? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val fab = findViewById<FloatingActionButton>(R.id.fab)
        listViewItems = findViewById(R.id.items_list)

        val headerView = LayoutInflater.from(this).inflate(R.layout.list_header, listViewItems, false)
        listViewItems!!.addHeaderView(headerView)

        fab.setOnClickListener { view ->
            addNewItemDialog()
        }

        mDatabase = FirebaseDatabase.getInstance().reference
        toDoItemList = mutableListOf()
        adapter = ToDoItemAdapter(this, toDoItemList!!)
        listViewItems!!.adapter = adapter
        mDatabase.child(Constants.FIREBASE_ITEM).orderByKey().addListenerForSingleValueEvent(itemListener)
    }

    private fun addNewItemDialog() {
        val alert = AlertDialog.Builder(this)
        val itemEditText = EditText(this)
        alert.setMessage("Add New Item")
        alert.setTitle("Enter To Do Item Text")
        alert.setView(itemEditText)
        alert.setPositiveButton("Submit") { dialog, positiveButton ->
            val todoItem = ToDoItem.create()
            todoItem.itemText = itemEditText.text.toString()
            todoItem.done = false

            val newItem = mDatabase.child(Constants.FIREBASE_ITEM).push()
            todoItem.objectId = newItem.key

            newItem.setValue(todoItem)
                .addOnSuccessListener {
                    // Add item locally and notify adapter
                    toDoItemList?.add(todoItem)
                    adapter.notifyDataSetChanged()
                    Toast.makeText(this, "Item saved with ID " + todoItem.objectId, Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    // Handle failure
                    Toast.makeText(this, "Failed to add item: ${it.message}", Toast.LENGTH_SHORT).show()
                }

            dialog.dismiss()
        }
        alert.show()
    }

    var itemListener: ValueEventListener = object : ValueEventListener {
        override fun onDataChange(dataSnapshot: DataSnapshot) {
            addDataToList(dataSnapshot)
        }

        override fun onCancelled(databaseError: DatabaseError) {
            Log.w("MainActivity", "loadItem:onCancelled", databaseError.toException())
        }
    }

    private fun addDataToList(dataSnapshot: DataSnapshot) {
        toDoItemList?.clear()
        for (snapshot in dataSnapshot.children) {
            val todoItem = snapshot.getValue(ToDoItem::class.java)
            todoItem?.objectId = snapshot.key
            todoItem?.let { toDoItemList?.add(it) }
        }
        adapter.notifyDataSetChanged()
    }

    override fun modifyItemState(itemObjectId: String, isDone: Boolean) {
        val itemReference = mDatabase.child(Constants.FIREBASE_ITEM).child(itemObjectId)
        itemReference.child("done").setValue(isDone)
    }

    override fun onItemDelete(itemObjectId: String) {
        val itemReference = mDatabase.child(Constants.FIREBASE_ITEM).child(itemObjectId)
        itemReference.removeValue()
            .addOnSuccessListener {
                // Remove item locally and notify adapter
                toDoItemList?.removeAll { it.objectId == itemObjectId }
                adapter.notifyDataSetChanged()
                Toast.makeText(this, "Item deleted with ID $itemObjectId", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                // Handle failure
                Toast.makeText(this, "Failed to delete item: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }
}




