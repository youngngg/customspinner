package com.my.scroller;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.widget.TextView;

import com.my.scroller.CustomSpinner.CustomSpinnerListener;

public class Launcher extends Activity {

	private static final String TAG = "TEST";

	CustomSpinner customSpinner = null;
	CustomSpinner customSpinner2 = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		setContentView(R.layout.custom_spinner);

		customSpinner = (CustomSpinner) findViewById(R.id.custom_spinner);

		List<TextView> views = new ArrayList<TextView>();
		for (int i = 0; i <= 50; i++) {
			TextView element = new TextView(this);
			element.setText("" + i);
			element.setTextSize(40);
			element.setTextColor(Color.DKGRAY);
			element.setGravity(Gravity.CENTER);
			views.add(element);
		}
		customSpinner.setViews(views, this);

		customSpinner.setCurrentChildChangedListener(new CustomSpinnerListener() {
			@Override
			public void onScrollChanged(int currentChild) {
				Log.d(TAG, "Spinner Changed to - " + currentChild);
			}
		});

		customSpinner2 = (CustomSpinner) findViewById(R.id.custom_spinner2);
		views = new ArrayList<TextView>();
		for (int i = 0; i <= 10; i++) {
			TextView element = new TextView(this);
			element.setText("my text " + i);
			element.setTextSize(20);
			element.setTextColor(Color.BLACK);
			element.setGravity(Gravity.CENTER);
			views.add(element);
		}
		customSpinner2.setViews(views, this);

		customSpinner2.setCurrentChildChangedListener(new CustomSpinnerListener() {
			@Override
			public void onScrollChanged(int currentChild) {
				Log.d(TAG, "Spinner2 Changed to - " + currentChild);
				// uncomment code bellow to see how onScrollChanged works
				// customSpinner2.getCurrentTextView().setText("current");
			}
		});
	}

	@Override
	protected void onResume() {
		super.onResume();
		customSpinner.startController();
		customSpinner2.startController();
	}

	@Override
	protected void onStop() {
		super.onStop();
		customSpinner.stopController();
		customSpinner2.stopController();
	}
}
