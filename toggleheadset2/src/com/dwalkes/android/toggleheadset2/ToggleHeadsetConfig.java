/*
 * Copyright (C) 2011 Dan Walkes, rabinkarki@gmail.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dwalkes.android.toggleheadset2;

import com.dwalkes.android.toggleheadset2.ToggleHeadsetAppWidgetProvider.ToggleHeadsetService;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;

public class ToggleHeadsetConfig extends Activity {
	Button mConfigOkButton;
	CheckBox mForceEarpieceCheck;
	CheckBox mRouteSpeakerOnCallAnswerCheck;
	int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
	private static final String TAG = "ToggleHeadsetConfig";
	public static final String PREF_KEY_ROUTE_SPEAKER_ON_CALL_ANSWER = "route_speaker_on_call_answer";
	public static final String PREF_KEY_FORCE_SPEAKER_ON_BOOT = "force_earpiece";
	
	@Override
	/**
	 * Create a dialog that prompts the user to select whether to force headset routing at boot 
	 */
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setResult(RESULT_CANCELED);

	    setContentView(R.layout.toggle_config);
	  
	    mForceEarpieceCheck = (CheckBox)findViewById(R.id.checkBox1);
	    mRouteSpeakerOnCallAnswerCheck = (CheckBox)findViewById(R.id.checkBoxRouteToSpeakerOnCallAnswer);
	    mConfigOkButton = (Button)findViewById(R.id.button1);
	    mConfigOkButton.setOnClickListener(configOkButtonOnClickListener);
		SharedPreferences prefs = getSharedPreferences(ToggleHeadsetService.PREF_FILE, 0);
	    mForceEarpieceCheck.setChecked(prefs.getBoolean(PREF_KEY_FORCE_SPEAKER_ON_BOOT, false));
	    mRouteSpeakerOnCallAnswerCheck.setChecked(prefs.getBoolean(PREF_KEY_ROUTE_SPEAKER_ON_CALL_ANSWER, false));
	    
	    
	  
	    Intent intent = getIntent();
	    Bundle extras = intent.getExtras();
	    if (extras != null) {
	        mAppWidgetId = extras.getInt(
	                AppWidgetManager.EXTRA_APPWIDGET_ID,
	                AppWidgetManager.INVALID_APPWIDGET_ID);
	    }
	  
	    // If they gave us an intent without the widget id, just bail.
	    if (mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
	        finish();
	    }
	}
	
	private Button.OnClickListener configOkButtonOnClickListener
	= new Button.OnClickListener(){

	/**
	 * Get the result of the dialog and set preferences accordingly.
	 */
	@Override
	public void onClick(View arg0) {
		 //save preference to a file, so that we can retrieve it later.
		SharedPreferences.Editor prefEditor = getSharedPreferences(ToggleHeadsetService.PREF_FILE, 0).edit();
		prefEditor.putBoolean(PREF_KEY_FORCE_SPEAKER_ON_BOOT, mForceEarpieceCheck.isChecked());
		prefEditor.putBoolean(PREF_KEY_ROUTE_SPEAKER_ON_CALL_ANSWER, mRouteSpeakerOnCallAnswerCheck.isChecked());
		
		if ( !prefEditor.commit() ) {
			Log.w(TAG, "Failed to commit preference setting force_earpiece");
		}
		Intent resultValue = new Intent();
		resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
		setResult(RESULT_OK, resultValue);
		finish();
	}};
}
