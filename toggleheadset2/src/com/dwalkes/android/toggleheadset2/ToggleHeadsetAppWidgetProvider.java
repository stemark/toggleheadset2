/*
 * Copyright (C) 2009 Dan Walkes
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


import java.lang.reflect.Method;

import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.media.AudioManager;
import android.os.Build;
import android.os.IBinder;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.RemoteViews;


/**
 * 
 *  A toggle headset app widget provider class to create a toggle headset app widget
 *  for the purpose of fixing HTC 3 in 1 adapters before Eclair
 *  See also:
 *  http://code.google.com/p/android/issues/detail?id=2534
 *  https://review.source.android.com/#change,9780
 *
 *  This project is a replacement for toggleheadset which stopped working after 1.6 was released
 * 
 * @author Dan Walkes
 *
 */
public class ToggleHeadsetAppWidgetProvider extends AppWidgetProvider {

	private static String TAG = ToggleHeadsetAppWidgetProvider.class.getName();
	
	/**
	 * Called when appwidget is loaded
	 */
	public void onUpdate( Context context, 
			AppWidgetManager appWidgetManager, 
			int[] appWidgetIds) {
	
		Log.d(TAG,"onUpdate");
		// do all updates within a service (we can keep alive to register headset intents)
        context.startService(new Intent(context, ToggleHeadsetService.class));

	}

	public void onReceive(Context context, Intent intent) 
	{
		Log.d(TAG,"Receive intent " + intent);
		super.onReceive(context, intent);
	}
	
	/**
	 * The ToggleHeadsetService class
	 * A service to run in the background and catch receive headset toggle intents, use these to 
	 * change headset state
	 * @author dan
	 */
	public static class ToggleHeadsetService extends Service implements OnSharedPreferenceChangeListener {

		private String TAG = "ToggleHeadsetService";
		public static final String INTENT_UPDATE_ICON = "com.dwalkes.android.toggleheadset2.INTENT_UPDATE_ICON";
		public static final String INTENT_USER_TOGGLE_REQUEST = "com.dwalkes.android.toggleheadset2.INTENT_TOGGLE_HEADSET";
		public static final String PREF_FILE = "toggleheadset2_prefs";
		private boolean mForceEarpieceOnBoot = false;
		private boolean mRouteSpeakerOnCallAnswer = false;

        /*
         *  Constants determined from AudioSystem source
         */
        private static final int DEVICE_IN_WIRED_HEADSET    = 0x400000;
        private static final int DEVICE_OUT_EARPIECE        = 0x1;
        private static final int DEVICE_OUT_WIRED_HEADSET   = 0x4;
        private static final int DEVICE_STATE_UNAVAILABLE   = 0;
        private static final int DEVICE_STATE_AVAILABLE     = 1;
        
		public IBinder onBind(Intent arg0) {
			// TODO Auto-generated method stub
			return null;
		}
		
		ToggleHeadsetBroadcastReceiver headsetReceiver = null;

		class ToggleHeadsetPhoneStateListener extends PhoneStateListener 
		{
			@Override
			public void onCallStateChanged(int state, String incomingNumber)
			{
				Log.i(TAG,"Call state changed");
				if (state == TelephonyManager.CALL_STATE_OFFHOOK) {
					Log.i(TAG,"Call answered");
					if( isRoutingHeadset() )
					{
						Log.i(TAG,"Toggle to earpiece speaker to take call");
						toggleHeadset();
						updateIcon();
					}
				}
			}
		}
		PhoneStateListener mPhoneStateListener = null;

		synchronized void startPhoneStateListener() 
		{
			if( mPhoneStateListener == null) {
				PhoneStateListener listener  = new ToggleHeadsetPhoneStateListener();
				TelephonyManager manager = (TelephonyManager)getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE);
				manager.listen(listener,PhoneStateListener.LISTEN_CALL_STATE);
				mPhoneStateListener = listener;
			}
		}
		
		synchronized void stopPhoneStateListener()
		{
			if( mPhoneStateListener != null ) {
				TelephonyManager manager = (TelephonyManager)getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE);
				manager.listen(mPhoneStateListener,PhoneStateListener.LISTEN_NONE);
				mPhoneStateListener = null;
			}
		}

		
		public void onCreate() {
			super.onCreate();
			SharedPreferences mPrefs = getSharedPreferences(PREF_FILE, 0);
			mPrefs.registerOnSharedPreferenceChangeListener(this);
			mForceEarpieceOnBoot = mPrefs.getBoolean(ToggleHeadsetConfig.PREF_KEY_FORCE_SPEAKER_ON_BOOT, false);
			mRouteSpeakerOnCallAnswer = mPrefs.getBoolean(ToggleHeadsetConfig.PREF_KEY_ROUTE_SPEAKER_ON_CALL_ANSWER, false);
			Log.i(TAG,"Force earpiece on boot = " + mForceEarpieceOnBoot);
			Log.i(TAG,"Route speaker on call answer = " + mRouteSpeakerOnCallAnswer);
			if( mRouteSpeakerOnCallAnswer )
			{
				startPhoneStateListener();
			}
		}
		
		/**
		 * Starts a service to monitor headset toggle or updates the current toggle state
		 * If this is the first start of the service, registers a broadcast receiver to receive headset plug intent.
		 * If intent for headset plug was received, check whether the state has changed to a value indicating
		 * headset route.  If it has and the headset is not currently routed, route the headset.
		 * If intent for power connected was received, do nothing - but hope this was enough to start the service
		 * in time to catch HEADSET_PLUG intent.  See issue 3 (http://code.google.com/p/toggleheadset2/issues/detail?id=3) 
		 * @param intent The intent to handle, or NULL to simply update the icon
		 * @param startId Not used
		 */
		@Override 
		public void onStart(Intent intent, int startId){
			Log.d(TAG,"onStart " + intent);
			if( intent != null && intent.getAction() != null ) {
				Log.d(TAG, "Received " + intent.getAction() );
			}
			
			if(headsetReceiver == null )
			{
				/** Since HEADSET_PLUG uses FLAG_RECIEVER_REGISTERED_ONLY we need to register and
				 * unregister the broadcast receiver in the service.  
				 * Note - don't include power modes since this would cause the receiver to be fired twice
				 */
				headsetReceiver = new ToggleHeadsetBroadcastReceiver.ToggleHeadsetHeadsetPlugReceiver();
				IntentFilter plugIntentFilter = new IntentFilter(ToggleHeadsetBroadcastReceiver.HEADSET_PLUG_INTENT);
				registerReceiver(headsetReceiver, plugIntentFilter); 
			}
			
			if( intent != null && intent.getAction() != null ) 
			{
				if( intent.getAction().equals(INTENT_USER_TOGGLE_REQUEST)  )
				{
					// always toggle the headset if this was the received intent
					toggleHeadset();
				}
				else if( intent.getAction().equals(ToggleHeadsetBroadcastReceiver.HEADSET_PLUG_INTENT))
				{
					int state = intent.getExtras().getInt("state");
					
					Log.d(TAG,"Headset plug intent recieved, state " + Integer.toString(state));
					/**
					 *  Found by log and source code examine - state 2 is the state on the multi-function adapter where the
					 *  3.5mm audio jack is plugged in 
					 */
					if( state == 2 )
					{
						if( !isRoutingHeadset() )
						{
							/**
							 * Only change the headset toggle if not currently routing headset.
							 * If currently routing headset and the headset was unplugged the OS takes care of this for us.
							 */
							toggleHeadset();
						}
					}
				} 
				else if (intent.getAction().equals(ToggleHeadsetBroadcastReceiver.ACTION_BOOT_COMPLETED))
				{
					if(mForceEarpieceOnBoot) {
						AudioManager manager = (AudioManager)this.getSystemService(Context.AUDIO_SERVICE);
						routeToEarpiece(manager);
						Log.i(TAG,"Force routing on boot");
					}
				}
				else
				{
					/**
					 * Do nothing - but this intent should wake the service up and allow us to catch HEADSET_PLUG or refresh the icon
					 * with current state
					 */
					Log.d(TAG,"Caught " + intent.getAction());
				}
			}
			// always update the icon
			updateIcon();

			/*
			 *  It seems I should be able to stop here, however when I do I can get into states where the headset plug intent
			 *  fires continuously.  I can't understand why.  I was thinking it might be related to registering the broadcast
			 *  receiver for HEADSET_PLUG above and defining HEADSET_PLUG as the intent for the broadcast receiver but when I take
			 *  HEADSET_PLUG out of the manifest I still see the same issue. 
			 */
			//stopSelf(startId);
		}
		
		/**
		 * Called when the service is destroyed (low memory conditions).  We may miss
		 * notification of headset plug
		 */
		public void onDestroy() {
			Log.i(TAG,"onDestroy");
			/*
			 * Fix crash error in the market
			 */
			if( headsetReceiver != null ) {
				unregisterReceiver(headsetReceiver);
			}
			stopPhoneStateListener();
		}
		
		/**
		 * Toggles the current headset setting.  If currently routed headset, routes to
		 * speaker.  If currently routed to speaker routes to headset
		 */
		public void toggleHeadset() {
		    AudioManager manager = (AudioManager)this.getSystemService(Context.AUDIO_SERVICE);
			Log.d(TAG,"toggleHeadset"); 
	    	if( isRoutingHeadset() )
	    	{
	    		routeToEarpiece(manager);
	    	}
	    	else 
	    	{
	    		Log.d(TAG,"route to headset"); 
	    		if( Build.VERSION.SDK_INT == Build.VERSION_CODES.DONUT ) {
		    		/* see AudioService.setRouting
		    		* Use MODE_INVALID to force headset routing change */
		            manager.setRouting(AudioManager.MODE_INVALID, AudioManager.ROUTE_HEADSET, AudioManager.ROUTE_HEADSET );
	    		} else {
	                setDeviceConnectionState(DEVICE_IN_WIRED_HEADSET, DEVICE_STATE_AVAILABLE, "");
	                setDeviceConnectionState(DEVICE_OUT_WIRED_HEADSET, DEVICE_STATE_AVAILABLE, "");
	    		}
	    	}
		}

		/**
		 * Routes audio to earpiece.
		 * @param manager AudioManager instance.
		 */
		private void routeToEarpiece(AudioManager manager) {
			Log.d(TAG,"route to earpiece"); 
			if( Build.VERSION.SDK_INT == Build.VERSION_CODES.DONUT ) {
				/* see AudioService.setRouting
				* Use MODE_INVALID to force headset routing change */
			    manager.setRouting(AudioManager.MODE_INVALID, 0, AudioManager.ROUTE_HEADSET );
			} else {
			    setDeviceConnectionState(DEVICE_IN_WIRED_HEADSET, DEVICE_STATE_UNAVAILABLE, "");
			    setDeviceConnectionState(DEVICE_OUT_WIRED_HEADSET, DEVICE_STATE_UNAVAILABLE, "");
			    setDeviceConnectionState(DEVICE_OUT_EARPIECE, DEVICE_STATE_AVAILABLE, "");
			}
		}
		
		/**
		 * Checks whether we are currently routing to headset
		 * @return true if routing to headset, false if routing somewhere else
		 */
		public boolean isRoutingHeadset() {
			boolean isRoutingHeadset = false;
			
			if( Build.VERSION.SDK_INT == Build.VERSION_CODES.DONUT ) {
				/*
				 * The code that works and is tested for Donut...
				 */
				AudioManager manager = (AudioManager)this.getSystemService(Context.AUDIO_SERVICE);
				
				int routing = manager.getRouting(AudioManager.MODE_NORMAL);
		    	Log.d(TAG,"getRouting returns " + routing); 
		    	isRoutingHeadset = (routing & AudioManager.ROUTE_HEADSET) != 0;
			} else {
				/*
				 * Code for Android 2.1, 2.2, 2.3, maybe others... Thanks Adam King!
				 */
	            try {
	            	/**
	            	 * Use reflection to get headset routing
	            	 */
	                Class<?> audioSystem = Class.forName("android.media.AudioSystem");
	                Method getDeviceConnectionState = audioSystem.getMethod(
	                        "getDeviceConnectionState", int.class, String.class);

	                int retVal = (Integer)getDeviceConnectionState.invoke(audioSystem, DEVICE_IN_WIRED_HEADSET, "");
	                
	                isRoutingHeadset = (retVal == 1);
			    	Log.d(TAG,"getDeviceConnectionState " + retVal); 

	            } catch (Exception e) {
	                Log.e(TAG, "Could not determine status in isRoutingHeadset(): " + e);
	            }
			}
	    	return isRoutingHeadset; 
		}
		
		/**
		 * Updates the icon of the appwidget based on the current status of headphone routing
		 */
		public void updateIcon() {
	    	Log.d(TAG,"updateIcon"); 

	        RemoteViews view = new RemoteViews(this.getPackageName(), R.layout.toggle_apwidget);

	        if( isRoutingHeadset() )
	        {
	        	Log.d(TAG,"Routing Headset"); 
	            view.setImageViewResource(R.id.Icon, R.drawable.headsetroute);
	        }
	        else
	        {
	        	Log.d(TAG,"Not Routing Headset"); 
	            view.setImageViewResource(R.id.Icon, R.drawable.noheadsetroute);
	        }
	        
		    // Create an Intent to launch toggle headset
		    Intent toggleIntent = new Intent(this, ToggleHeadsetService.class);
		    toggleIntent.setAction(ToggleHeadsetService.INTENT_USER_TOGGLE_REQUEST);
		    PendingIntent pendingIntent = PendingIntent.getService(this, 0, toggleIntent, 0);

		    // Get the layout for the App Widget and attach an on-click listener to the icon
		    view.setOnClickPendingIntent(R.id.Icon, pendingIntent);
		    
	        // Push update for this widget to the home screen
	        ComponentName thisWidget = new ComponentName(this, ToggleHeadsetAppWidgetProvider.class);
	        AppWidgetManager manager = AppWidgetManager.getInstance(this);
	        manager.updateAppWidget(thisWidget, view);
		}
		
		
		/**
		 * set device connection state through reflection for Android 2.1, 2.2, 2.3 - 4.0 - later?
		 * Thanks Adam King!
		 * @param device
		 * @param state
		 * @param address
		 */
        private void setDeviceConnectionState(final int device, final int state, final String address) {
            try {
                Class<?> audioSystem = Class.forName("android.media.AudioSystem");
                Method setDeviceConnectionState = audioSystem.getMethod(
                        "setDeviceConnectionState", int.class, int.class, String.class);

                setDeviceConnectionState.invoke(audioSystem, device, state, address);
            } catch (Exception e) {
                Log.e(TAG, "setDeviceConnectionState failed: " + e);
            }
        }

        /**
         * The config activity might happen while the service is running.  Watch for changes to preferences and update boolean values
         * accordingly
         */
		@Override
		public void onSharedPreferenceChanged(
				SharedPreferences sharedPreferences, String key) {
			Log.i(TAG,"shared preference changed for " + key);
			mForceEarpieceOnBoot = sharedPreferences.getBoolean(ToggleHeadsetConfig.PREF_KEY_FORCE_SPEAKER_ON_BOOT, false);
			mRouteSpeakerOnCallAnswer = sharedPreferences.getBoolean(ToggleHeadsetConfig.PREF_KEY_ROUTE_SPEAKER_ON_CALL_ANSWER, false);
			
			if(mRouteSpeakerOnCallAnswer) {
				startPhoneStateListener();
			} else {
				stopPhoneStateListener();
			}
		}
		
	}

}
