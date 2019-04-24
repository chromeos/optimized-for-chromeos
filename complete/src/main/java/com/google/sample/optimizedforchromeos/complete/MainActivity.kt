/*
 *       Copyright (c) 2019 Google Inc. All rights reserved.
 *
 *       Licensed under the Apache License, Version 2.0 (the "License");
 *       you may not use this file except in compliance with the License.
 *       You may obtain a copy of the License at
 *
 *             http://www.apache.org/licenses/LICENSE-2.0
 *
 *       Unless required by applicable law or agreed to in writing, software
 *       distributed under the License is distributed on an "AS IS" BASIS,
 *       WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *       See the License for the specific language governing permissions and
 *       limitations under the License.
 */

package com.google.sample.optimizedforchromeos.complete

import android.content.ClipData
import android.content.ClipDescription
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.util.Log
import android.util.TypedValue
import android.view.*
import android.view.MotionEvent.ACTION_HOVER_ENTER
import android.view.MotionEvent.ACTION_HOVER_EXIT
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.TooltipCompat
import androidx.lifecycle.ViewModelProviders
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_main.*
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.util.*

class MainActivity : AppCompatActivity() {

    var dinosClicked = 0
    var messagesSent = 0

    private var undoStack = ArrayDeque<Int>()
    private var redoStack = ArrayDeque<Int>()

    private val UNDO_MESSAGE_SENT = 1
    private val UNDO_DINO_CLICKED = 2

    lateinit private var dinoModel: DinoViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Set up click Listeners
        button_send.setOnClickListener(SendButtonOnClickListener(text_messages_sent))
        image_dino_1.setOnClickListener(ImageOnClickListener(text_dinos_clicked))
        image_dino_2.setOnClickListener(ImageOnClickListener(text_dinos_clicked))
        image_dino_3.setOnClickListener(ImageOnClickListener(text_dinos_clicked))
        image_dino_4.setOnClickListener(ImageOnClickListener(text_dinos_clicked))

        // Enter key listener
        edit_message.setOnKeyListener(View.OnKeyListener { v, keyCode, keyEvent ->
            if (keyEvent.action == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_ENTER) {
                button_send.performClick()
                return@OnKeyListener true
            }
            false
        })

        // Make controls focusable programmatically
        // Alternative: change the focusable value in the .xml layout files
        button_send.setFocusable(true)
        image_dino_1.setFocusable(true)
        image_dino_2.setFocusable(true)
        image_dino_3.setFocusable(true)
        image_dino_4.setFocusable(true)

        // Add highlighting
        val highlightValue = TypedValue()
        theme.resolveAttribute(R.attr.selectableItemBackground, highlightValue, true)

        image_dino_1.setBackgroundResource(highlightValue.resourceId)
        image_dino_2.setBackgroundResource(highlightValue.resourceId)
        image_dino_3.setBackgroundResource(highlightValue.resourceId)
        image_dino_4.setBackgroundResource(highlightValue.resourceId)

        // Fix next focus targets
        edit_message.nextFocusForwardId = R.id.button_send
        edit_message.nextFocusRightId = R.id.button_send
        button_send.nextFocusForwardId = R.id.image_dino_1
        button_send.nextFocusLeftId = R.id.edit_message
        image_dino_2.nextFocusForwardId = R.id.image_dino_3
        image_dino_3.nextFocusForwardId = R.id.image_dino_4

        // Adjust image highlighting
        image_dino_1.setBackgroundResource(R.drawable.box_border)
        image_dino_2.setBackgroundResource(R.drawable.box_border)
        image_dino_3.setBackgroundResource(R.drawable.box_border)
        image_dino_4.setBackgroundResource(R.drawable.box_border)

        // Context click listeners
        registerForContextMenu(image_dino_1)
        registerForContextMenu(image_dino_2)
        registerForContextMenu(image_dino_3)
        registerForContextMenu(image_dino_4)

        // Add dino tooltips
        TooltipCompat.setTooltipText(image_dino_1, getString(R.string.name_dino_hadrosaur))
        TooltipCompat.setTooltipText(image_dino_2, getString(R.string.name_dino_triceratops))
        TooltipCompat.setTooltipText(image_dino_3, getString(R.string.name_dino_nodosaur))
        TooltipCompat.setTooltipText(image_dino_4, getString(R.string.name_dino_afrovenator))

        // OnHover listeners
        button_send.setOnHoverListener(View.OnHoverListener { v, event ->
            val action = event.actionMasked

            when (action) {
                ACTION_HOVER_ENTER -> {
                    val buttonColorStateList = ColorStateList(
                        arrayOf(intArrayOf()),
                        intArrayOf(Color.argb(127, 0, 255, 0))
                    )
                    button_send.setBackgroundTintList(buttonColorStateList)
                    return@OnHoverListener true
                }

                ACTION_HOVER_EXIT -> {
                    button_send.setBackgroundTintList(null)
                    return@OnHoverListener true
                }
            }

            false
        })

        text_drag.setOnHoverListener(View.OnHoverListener { v, event ->
            val action = event.actionMasked

            when (action) {
                ACTION_HOVER_ENTER -> {
                    text_drag.setBackgroundResource(R.drawable.hand)
                    return@OnHoverListener true
                }

                ACTION_HOVER_EXIT -> {
                    text_drag.setBackgroundResource(0)
                    return@OnHoverListener true
                }
            }

            false
        })

        // Add Drag and Drop listeners
        text_drop.setOnDragListener(DropTargetListener(this))
        text_drag.setOnLongClickListener(TextViewLongClickListener())

        // Get the persistent ViewModel
        dinoModel = ViewModelProviders.of(this).get(DinoViewModel::class.java)

        // Restore the stacks
        undoStack = dinoModel.getUndoStack()
        redoStack = dinoModel.getRedoStack()

        // Set up data observers
        dinoModel.getMessagesSent().observe(this, androidx.lifecycle.Observer { newCount ->
            text_messages_sent.setText(Integer.toString(newCount))
        })

        dinoModel.getDinosClicked().observe(this, androidx.lifecycle.Observer { newCount ->
            text_dinos_clicked.setText(Integer.toString(newCount))
        })

        dinoModel.getDropText().observe(this, androidx.lifecycle.Observer { newString ->
            text_drop.text = newString
        })
    }

    internal inner class SendButtonOnClickListener(private val sentCounter: TextView
    ) : View.OnClickListener {
        override fun onClick(v: View?) {
            dinoModel.setMessagesSent(dinoModel.getMessagesSentInt() + 1)
            edit_message.text.clear()
            undoStack.push(UNDO_MESSAGE_SENT)
            redoStack.clear()
        }
    }

    internal inner class ImageOnClickListener(private val clickCounter: TextView
    ) : View.OnClickListener {
        override fun onClick(v: View) {
            dinoModel.setDinosClicked(dinoModel.getDinosClickedInt() + 1)
            undoStack.push(UNDO_DINO_CLICKED)
            redoStack.clear()
        }
    }

    override fun onCreateContextMenu(
        menu: ContextMenu,
        v: View,
        menuInfo: ContextMenu.ContextMenuInfo?
    ) {
        super.onCreateContextMenu(menu, v, menuInfo)
        val inflater = menuInflater
        inflater.inflate(R.menu.context_menu, menu)
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        if (R.id.menu_item_share_dino == item.itemId) {
            Snackbar.make(findViewById(android.R.id.content),
                getString(R.string.menu_shared_message), Snackbar.LENGTH_SHORT).show()
            return true
        } else {
            return super.onContextItemSelected(item)
        }
    }

    protected inner class DropTargetListener(private val activity: AppCompatActivity
    ) : View.OnDragListener {
        override fun onDrag(v: View, event: DragEvent): Boolean {
            val action = event.action

            when (action) {
                DragEvent.ACTION_DRAG_STARTED -> {
                    // Limit the types of items that can be received
                    if (event.clipDescription.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN) ||
                        event.clipDescription.hasMimeType("application/x-arc-uri-list")) {

                        // Greenify background colour so user knows this is a target
                        v.setBackgroundColor(Color.argb(55, 0, 255, 0))
                        return true
                    }

                    // If dragged item is of an unrecognized type, indicate that not a valid target
                    return false
                }

                DragEvent.ACTION_DRAG_ENTERED -> {
                    // Increase green background colour when item is over top of target
                    v.setBackgroundColor(Color.argb(150, 0, 255, 0))
                    return true
                }

                DragEvent.ACTION_DRAG_EXITED -> {
                    // Less intense green background colour when item not over target
                    v.setBackgroundColor(Color.argb(55, 0, 255, 0))
                    return true
                }

                DragEvent.ACTION_DRAG_ENDED -> {
                    // Restore background colour to transparent
                    v.setBackgroundColor(Color.argb(0, 255, 255, 255))
                    return true
                }

                DragEvent.ACTION_DROP -> {
                    requestDragAndDropPermissions(event) // Allow items from other applications
                    val item = event.clipData.getItemAt(0)
                    val textTarget = v as TextView

                    if (event.clipDescription.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN)) {
                        // If this is a text item, simply display it in a new TextView.
                        textTarget.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
                        dinoModel.setDropText(item.text.toString())

                    } else if (event.clipDescription.hasMimeType("application/x-arc-uri-list")) {
                        // If a file, read the first 200 characters and output them in a new TextView.

                        // Note the use of ContentResolver to resolve the ChromeOS content URI.
                        val contentUri = item.uri
                        val parcelFileDescriptor: ParcelFileDescriptor?
                        try {
                            parcelFileDescriptor =
                                contentResolver.openFileDescriptor(contentUri, "r")
                        } catch (e: FileNotFoundException) {
                            e.printStackTrace()
                            Log.e("OptimizedChromeOS", "Error on drop: File not found.")
                            return false
                        }

                        if (parcelFileDescriptor == null) {
                            textTarget.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
                            dinoModel.setDropText("Error: could not load file: " +
                                    contentUri.toString())
                            return false
                        }

                        val fileDescriptor = parcelFileDescriptor.fileDescriptor

                        val MAX_LENGTH = 5000
                        val bytes = ByteArray(MAX_LENGTH)

                        try {
                            val `in` = FileInputStream(fileDescriptor)
                            try {
                                `in`.read(bytes, 0, MAX_LENGTH)
                            } finally {
                                `in`.close()
                            }
                        } catch (ex: Exception) {
                        }

                        val contents = String(bytes)

                        val CHARS_TO_READ = 200
                        val content_length =
                            if (contents.length > CHARS_TO_READ)
                                CHARS_TO_READ
                            else
                                0

                        textTarget.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
                        dinoModel.setDropText(contents.substring(0, content_length))
                    } else {
                        return false
                    }
                    return true
                }

                else -> {
                    Log.d("OptimizedChromeOS",
                        "Unknown action type received by DropTargetListener.")
                    return false
                }
            }
        }
    }

    protected inner class TextViewLongClickListener : View.OnLongClickListener {
        override fun onLongClick(v: View): Boolean {
            val thisTextView = v as TextView
            val dragContent = "Dragged Text: " + thisTextView.text

            // Set the drag content and type
            val item = ClipData.Item(dragContent)
            val dragData = ClipData(dragContent, arrayOf(ClipDescription.MIMETYPE_TEXT_PLAIN), item)

            // Set the visual look of the dragged object
            // Can be extended and customized. We use the default here.
            val dragShadow = View.DragShadowBuilder(v)

            // Starts the drag, note: global flag allows for cross-application drag
            v.startDragAndDrop(dragData, dragShadow, null, View.DRAG_FLAG_GLOBAL)

            return false
        }
    }

    override fun dispatchKeyShortcutEvent(event: KeyEvent): Boolean {
        // Ctrl-z == Undo
        if (event.keyCode == KeyEvent.KEYCODE_Z && event.hasModifiers(KeyEvent.META_CTRL_ON)) {
            val lastAction = undoStack.poll()
            if (null != lastAction) {
                redoStack.push(lastAction)

                when (lastAction) {
                    UNDO_MESSAGE_SENT -> {
                        dinoModel.setMessagesSent(dinoModel.getMessagesSentInt() - 1)
                    }

                    UNDO_DINO_CLICKED -> {
                        dinoModel.setDinosClicked(dinoModel.getDinosClickedInt() - 1)
                    }

                    else -> Log.d("OptimizedChromeOS", "Error on Ctrl-z: Unknown Action")
                }

                return true
            }
        }

        // Ctrl-Shift-z == Redo
        if (event.keyCode == KeyEvent.KEYCODE_Z &&
            event.hasModifiers(KeyEvent.META_CTRL_ON or KeyEvent.META_SHIFT_ON)) {
            val prevAction = redoStack.poll()
            if (null != prevAction) {
                undoStack.push(prevAction)

                when (prevAction) {
                    UNDO_MESSAGE_SENT -> {
                        dinoModel.setMessagesSent(dinoModel.getMessagesSentInt() + 1)
                    }

                    UNDO_DINO_CLICKED -> {
                        dinoModel.setDinosClicked(dinoModel.getDinosClickedInt() + 1)
                    }

                    else -> Log.d("OptimizedChromeOS", "Error on Ctrl-Shift-z: Unknown Action")
                }

                return true
            }
        }

        return super.dispatchKeyShortcutEvent(event)
    }
}
