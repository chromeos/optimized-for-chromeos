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

import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModel;

import java.util.ArrayDeque;

/**
 * Created by hadrosaur on 3/13/18.
 */

public class DinoViewModel extends ViewModel {
    private MutableLiveData<Integer> messagesSent;
    private MutableLiveData<Integer> dinosClicked;
    private MutableLiveData<String> dropText;

    private ArrayDeque<Integer> undoStack;
    private ArrayDeque<Integer> redoStack;

    public ArrayDeque<Integer> getUndoStack() {
        if (this.undoStack == null) {
            this.undoStack = new ArrayDeque<Integer>();
        }
        return undoStack;
    }

    public ArrayDeque<Integer> getRedoStack() {
        if (this.redoStack == null) {
            this.redoStack = new ArrayDeque<Integer>();
        }
        return redoStack;
    }

    public MutableLiveData<Integer> setDinosClicked(int newNumClicks) {
        if (this.dinosClicked == null) {
            this.dinosClicked = new MutableLiveData<Integer>();
        }
        dinosClicked.setValue(newNumClicks);
        return dinosClicked;
    }

    public MutableLiveData<Integer> getDinosClicked() {
        if (this.dinosClicked == null) {
            this.dinosClicked = new MutableLiveData<Integer>();
            this.dinosClicked.setValue(0);
        }
        return dinosClicked;
    }

    public MutableLiveData<Integer> setMessagesSent(int newMessagesSent) {
        if (this.messagesSent == null) {
            this.messagesSent = new MutableLiveData<Integer>();
        }
        messagesSent.setValue(newMessagesSent);
        return messagesSent;
    }

    public MutableLiveData<Integer> getMessagesSent() {
        if (this.messagesSent == null) {
            this.messagesSent = new MutableLiveData<Integer>();
            this.messagesSent.setValue(0);
        }
        return messagesSent;
    }

    public MutableLiveData<String> setDropText(String newDropText) {
        if (this.dropText == null) {
            this.dropText = new MutableLiveData<String>();
        }
        dropText.setValue(newDropText);
        return dropText;
    }

    public MutableLiveData<String> getDropText() {
        if (this.dropText == null) {
            this.dropText = new MutableLiveData<String>();
            this.dropText.setValue("Drop Things Here!");
        }
        return dropText;
    }
}
