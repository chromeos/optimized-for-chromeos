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

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    private int mMessagesSent;
    private int mDinosClicked;

    private TextView messageCounterText;
    private TextView clickCounterText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final Button sendButton = findViewById(R.id.button_send);
        ImageView dinoImage1 = findViewById(R.id.image_dino1);
        ImageView dinoImage2 = findViewById(R.id.image_dino2);
        ImageView dinoImage3 = findViewById(R.id.image_dino3);
        ImageView dinoImage4 = findViewById(R.id.image_dino4);

        final EditText messageField = findViewById(R.id.edit_message);

        final TextView messageCounter = findViewById(R.id.text_messages_sent);
        final TextView imageClickCounter = findViewById(R.id.text_dino_clicks);

        // Set up Click Listeners
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMessagesSent++;
                messageCounterText.setText(Integer.toString(mMessagesSent));
                messageField.getText().clear();
            }
        });

        dinoImage1.setOnClickListener(new ImageOnClickListener(imageClickCounter));
        dinoImage2.setOnClickListener(new ImageOnClickListener(imageClickCounter));
        dinoImage3.setOnClickListener(new ImageOnClickListener(imageClickCounter));
        dinoImage4.setOnClickListener(new ImageOnClickListener(imageClickCounter));

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
        }
    }
}
