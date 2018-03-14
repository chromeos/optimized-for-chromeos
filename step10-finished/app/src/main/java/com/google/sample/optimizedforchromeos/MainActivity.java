/*      Copyright 2018 Google LLC

        Licensed under the Apache License, Version 2.0 (the "License");
        you may not use this file except in compliance with the License.
        You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

        Unless required by applicable law or agreed to in writing, software
        distributed under the License is distributed on an "AS IS" BASIS,
        WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
        See the License for the specific language governing permissions and
        limitations under the License.
*/
package com.google.sample.optimizedforchromeos;

import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.TooltipCompat;
import android.util.Log;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.DragEvent;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayDeque;

import static android.view.MotionEvent.ACTION_HOVER_ENTER;
import static android.view.MotionEvent.ACTION_HOVER_EXIT;
import static android.view.View.DRAG_FLAG_GLOBAL;

public class MainActivity extends AppCompatActivity {
    private DinoViewModel mDinoModel;

    private TextView messageCounterText;
    private TextView clickCounterText;

    private static final int UNDO_MESSAGE_SENT = 1;
    private static final int UNDO_DINO_CLICKED = 2;
    private ArrayDeque<Integer> undoQueue;
    private ArrayDeque<Integer> redoQueue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Get the persistent ViewModel
        mDinoModel = ViewModelProviders.of(this).get(DinoViewModel.class);

        //Restore our queues
        undoQueue = mDinoModel.getUndoQueue();
        redoQueue = mDinoModel.getRedoQueue();

        final Button sendButton = findViewById(R.id.button_send);
        ImageView dinoImage1 = findViewById(R.id.image_dino1);
        ImageView dinoImage2 = findViewById(R.id.image_dino2);
        ImageView dinoImage3 = findViewById(R.id.image_dino3);
        ImageView dinoImage4 = findViewById(R.id.image_dino4);

        final EditText messageField = findViewById(R.id.edit_message);

        messageCounterText = findViewById(R.id.text_messages_sent);
        clickCounterText = findViewById(R.id.text_dino_clicks);

        final TextView dragText = findViewById(R.id.text_drag);
        final TextView dropText = findViewById(R.id.text_drop);

        //Add Drag and Drop listeners
        dragText.setOnLongClickListener(new TextViewLongClickListener());
        dropText.setOnDragListener(new DropTargetListener(this));

        //Adjust image highlighting
        dinoImage1.setBackgroundResource(R.drawable.box_border);
        dinoImage2.setBackgroundResource(R.drawable.box_border);
        dinoImage3.setBackgroundResource(R.drawable.box_border);
        dinoImage4.setBackgroundResource(R.drawable.box_border);

        //Add dino tooltips
        TooltipCompat.setTooltipText(dinoImage1, getString(R.string.name_dino_1));
        TooltipCompat.setTooltipText(dinoImage2, getString(R.string.name_dino_2));
        TooltipCompat.setTooltipText(dinoImage3, getString(R.string.name_dino_3));
        TooltipCompat.setTooltipText(dinoImage4, getString(R.string.name_dino_4));

        //Set up data observers
        final Observer<Integer> messageObserver = new Observer<Integer>() {
            @Override
            public void onChanged(@Nullable final Integer newCount) {
                messageCounterText.setText(Integer.toString(mDinoModel.getMessagesSent().getValue()));
            }
        };
        mDinoModel.getMessagesSent().observe(this, messageObserver);

        final Observer<Integer> dinoClickObserver = new Observer<Integer>() {
            @Override
            public void onChanged(@Nullable final Integer newCount) {
                clickCounterText.setText(Integer.toString(mDinoModel.getDinosClicked().getValue()));
            }
        };
        mDinoModel.getDinosClicked().observe(this, dinoClickObserver);

        final Observer<String> dropTargetObserver = new Observer<String>() {
            @Override
            public void onChanged(@Nullable final String newString) {
                dropText.setText(mDinoModel.getDropText().getValue());
            }
        };
        mDinoModel.getDropText().observe(this, dropTargetObserver);

        // Set up Click Listeners
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDinoModel.setMessagesSent(mDinoModel.getMessagesSent().getValue() + 1);
                messageField.getText().clear();
                undoQueue.push(UNDO_MESSAGE_SENT);
                redoQueue.clear();
            }
        });

        //Single click listeners
        dinoImage1.setOnClickListener(new ImageOnClickListener(clickCounterText));
        dinoImage2.setOnClickListener(new ImageOnClickListener(clickCounterText));
        dinoImage3.setOnClickListener(new ImageOnClickListener(clickCounterText));
        dinoImage4.setOnClickListener(new ImageOnClickListener(clickCounterText));

        //Context click listeners
        registerForContextMenu(dinoImage1);
        registerForContextMenu(dinoImage2);
        registerForContextMenu(dinoImage3);
        registerForContextMenu(dinoImage4);

        //Enter key listener
        messageField.setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent keyEvent) {
                if ((keyEvent.getAction() == KeyEvent.ACTION_DOWN) &&
                        (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    sendButton.performClick();
                    return true;
                }
                return false;
            }
        });

        //OnHover listeners
        sendButton.setOnHoverListener(new View.OnHoverListener() {
            @Override
            public boolean onHover(View v, MotionEvent event) {
                int action = event.getActionMasked();

                switch(action) {
                    case ACTION_HOVER_ENTER:
                        ColorStateList buttonColorStateList = new ColorStateList(new int[][]{{}},
                                new int[]{Color.argb(127, 0, 255, 0)});
                        sendButton.setBackgroundTintList(buttonColorStateList);
                        return true;

                    case ACTION_HOVER_EXIT:
                        sendButton.setBackgroundTintList(null);
                        return true;
                }

                return false;
            }
        });

        dragText.setOnHoverListener(new View.OnHoverListener() {
            @Override
            public boolean onHover(View v, MotionEvent event) {
                int action = event.getActionMasked();

                switch(action) {
                    case ACTION_HOVER_ENTER:
                        dragText.setBackgroundResource(R.drawable.hand);
                        return true;

                    case ACTION_HOVER_EXIT:
                        dragText.setBackgroundResource(0);
                        return true;
                }

                return false;
            }
        });
    }



    class ImageOnClickListener implements View.OnClickListener {
        TextView mClickCounter;

        public ImageOnClickListener(TextView mClickCounter) {
            this.mClickCounter = mClickCounter;
        }

        @Override
        public void onClick(View v) {
            mDinoModel.setDinosClicked(mDinoModel.getDinosClicked().getValue() + 1);
            undoQueue.push(UNDO_DINO_CLICKED);
            redoQueue.clear();
        }
    }

    @Override
    public boolean dispatchKeyShortcutEvent(KeyEvent event) {
        //Ctrl-z == Undo
        if (event.getKeyCode() == KeyEvent.KEYCODE_Z
                && event.hasModifiers(KeyEvent.META_CTRL_ON)) {
            Integer lastAction = undoQueue.poll();
            if (null != lastAction) {
                redoQueue.push(lastAction);

                switch (lastAction) {
                    case UNDO_MESSAGE_SENT:
                        mDinoModel.setMessagesSent(mDinoModel.getMessagesSent().getValue() - 1);
                        break;

                    case UNDO_DINO_CLICKED:
                        mDinoModel.setDinosClicked(mDinoModel.getDinosClicked().getValue() - 1);
                        break;

                    default:
                        Log.d("OptimizedChromeOS", "Error on Ctrl-z: Unknown Action");
                        break;
                }

                return true;
            }
        }

        //Ctrl-Shift-z == Redo
        if ((event.getKeyCode() == KeyEvent.KEYCODE_Z)
                && event.hasModifiers(KeyEvent.META_CTRL_ON | KeyEvent.META_SHIFT_ON)) {
            Integer prevAction = redoQueue.poll();
            if (null != prevAction) {
                undoQueue.push(prevAction);

                switch (prevAction) {
                    case UNDO_MESSAGE_SENT:
                        mDinoModel.setMessagesSent(mDinoModel.getMessagesSent().getValue() + 1);
                        break;

                    case UNDO_DINO_CLICKED:
                        mDinoModel.setDinosClicked(mDinoModel.getDinosClicked().getValue() + 1);
                        break;

                    default:
                        Log.d("OptimizedChromeOS", "Error on Ctrl-Shift-z: Unknown Action");
                        break;
                }

                return true;
            }
        }

        return super.dispatchKeyShortcutEvent(event);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.context_menu, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if (R.id.menu_item_share_dino == item.getItemId()) {
            Snackbar.make(findViewById(android.R.id.content),
                    getString(R.string.menu_shared_message), Snackbar.LENGTH_SHORT).show();
            return true;
        } else {
            return super.onContextItemSelected(item);
        }
    }

    protected class DropTargetListener implements View.OnDragListener {
        private AppCompatActivity mActivity;

        public DropTargetListener(AppCompatActivity mActivity) {
            this.mActivity = mActivity;
        }

        @Override
        public boolean onDrag(View v, DragEvent event) {
            final int action = event.getAction();

            switch(action) {
                case DragEvent.ACTION_DRAG_STARTED:
                    // Limit the types of items that can be received
                    if (event.getClipDescription().hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN)
                            || event.getClipDescription().hasMimeType("application/x-arc-uri-list")) {

                        // Greenify background colour so user knows this is a target
                        v.setBackgroundColor(Color.argb(55,0,255,0));
                        v.invalidate();
                        return true;
                    }

                    //If the dragged item is of an unrecognized type, indicate this is not a valid target
                    return false;

                case DragEvent.ACTION_DRAG_ENTERED:
                    // Increase green background colour when item is over top of target
                    v.setBackgroundColor(Color.argb(150,0,255,0));
                    v.invalidate();
                    return true;

                case DragEvent.ACTION_DRAG_EXITED:
                    // Less intense green background colour when item not over target
                    v.setBackgroundColor(Color.argb(55,0,255,0));
                    v.invalidate();
                    return true;

                case DragEvent.ACTION_DRAG_ENDED:
                    // Restore background colour to transparent
                    v.setBackgroundColor(Color.argb(0,255,255,255));
                    v.invalidate();
                    return true;

                case DragEvent.ACTION_DROP:
                    requestDragAndDropPermissions(event); //Allow items from other applications
                    ClipData.Item item = event.getClipData().getItemAt(0);

                    if (event.getClipDescription().hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN)) {
                        //If this is a text item, simply display it in a new TextView.
                        TextView textTarget = (TextView) v;

                        textTarget.setTextSize(TypedValue.COMPLEX_UNIT_SP,18);
                        textTarget.setText(item.getText());

                    } else if (event.getClipDescription().hasMimeType("application/x-arc-uri-list")) {
                        //If a file, read the first 200 characters and output them in a new TextView.

                        //Note the use of ContentResolver to resolve the ChromeOS content URI.
                        Uri contentUri = item.getUri();
                        ParcelFileDescriptor parcelFileDescriptor;
                        try {
                            parcelFileDescriptor = getContentResolver().openFileDescriptor(contentUri, "r");
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                            Log.e("OptimizedChromeOS", "Error receiving file: File not found.");
                            return false;
                        }

                        FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();

                        final int MAX_LENGTH = 5000;
                        byte[] bytes = new byte[MAX_LENGTH];

                        try {
                            FileInputStream in = new FileInputStream(fileDescriptor);
                            try {
                                in.read(bytes,0, MAX_LENGTH);
                            } finally {
                                in.close();
                            }
                        } catch (Exception ex) {}
                        String contents = new String(bytes);

                        final int CHARS_TO_READ = 200;
                        int content_length = (contents.length() > CHARS_TO_READ) ? CHARS_TO_READ : 0;

                        TextView textTarget = (TextView) v;

                        textTarget.setTextSize(TypedValue.COMPLEX_UNIT_SP,10);
                        textTarget.setText(contents.substring(0,content_length));

                    } else {
                        return false;
                    }
                    return true;
                default:
                    Log.d("OptimizedChromeOS","Unknown action type received by DropTargetListener.");
                    return false;
            }
        }
    }

    protected class TextViewLongClickListener implements View.OnLongClickListener {
        @Override
        public boolean onLongClick(View v) {
            TextView thisTextView = (TextView) v;
            String dragContent = "Dragged Text: " + thisTextView.getText();

            //Set the drag content and type
            ClipData.Item item = new ClipData.Item(dragContent);
            ClipData dragData = new ClipData(dragContent,
                    new String[] {ClipDescription.MIMETYPE_TEXT_PLAIN}, item);

            //Set the visual look of the dragged object
            //Can be extended and customized. We use the default here.
            View.DragShadowBuilder dragShadow = new View.DragShadowBuilder(v);

            // Starts the drag, note: global flag allows for cross-application drag
            v.startDragAndDrop(dragData, dragShadow, null, DRAG_FLAG_GLOBAL);

            return false;
        }
    }
}
