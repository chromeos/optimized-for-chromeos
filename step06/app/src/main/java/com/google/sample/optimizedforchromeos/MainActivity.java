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

import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.TooltipCompat;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayDeque;

public class MainActivity extends AppCompatActivity {

    private int mMessagesSent;
    private int mDinosClicked;

    private TextView messageCounterText;
    private TextView clickCounterText;

    private static final int UNDO_MESSAGE_SENT = 1;
    private static final int UNDO_DINO_CLICKED = 2;
    private ArrayDeque<Integer> undoQueue = new ArrayDeque<Integer>();
    private ArrayDeque<Integer> redoQueue = new ArrayDeque<Integer>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mMessagesSent = 0;
        mDinosClicked = 0;

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

        // Set up Click Listeners
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMessagesSent++;
                messageCounterText.setText(Integer.toString(mMessagesSent));
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

    }

    class ImageOnClickListener implements View.OnClickListener {
        TextView mClickCounter;

        public ImageOnClickListener(TextView mClickCounter) {
            this.mClickCounter = mClickCounter;
        }

        @Override
        public void onClick(View v) {
            mDinosClicked++;
            mClickCounter.setText(Integer.toString(mDinosClicked));
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
                        mMessagesSent--;
                        messageCounterText.setText(Integer.toString(mMessagesSent));
                        break;

                    case UNDO_DINO_CLICKED:
                        mDinosClicked--;
                        clickCounterText.setText(Integer.toString(mDinosClicked));
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
                        mMessagesSent++;
                        messageCounterText.setText(Integer.toString(mMessagesSent));
                        break;

                    case UNDO_DINO_CLICKED:
                        mDinosClicked++;
                        clickCounterText.setText(Integer.toString(mDinosClicked));
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
}
