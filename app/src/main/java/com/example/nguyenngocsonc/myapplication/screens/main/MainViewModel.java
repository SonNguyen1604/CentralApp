package com.example.nguyenngocsonc.myapplication.screens.main;

import android.databinding.BaseObservable;
import android.databinding.Bindable;

import com.example.nguyenngocsonc.myapplication.BR;

/**
 * Created by nguyen.ngoc.sonc on 10/24/17.
 */

public class MainViewModel extends BaseObservable {

    String text;

    public MainViewModel() {
        text = "1asdasdfqeer";
    }

    @Bindable
    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
        notifyPropertyChanged(BR.text);
    }
}
