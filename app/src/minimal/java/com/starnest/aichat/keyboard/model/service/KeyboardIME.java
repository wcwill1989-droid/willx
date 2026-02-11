package com.starnest.aichat.keyboard.model.service;

import android.inputmethodservice.InputMethodService;
import android.view.View;

public final class KeyboardIME extends InputMethodService {
    @Override
    public View onCreateInputView() {
        return new View(this);
    }
}
