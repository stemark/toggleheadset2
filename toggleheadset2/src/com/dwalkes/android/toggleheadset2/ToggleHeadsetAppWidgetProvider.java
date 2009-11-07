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


import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.IBinder;
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
		// do all updates within a service
        context.startService(new Intent(context, ToggleHeadsetService.class));

	}

	public static class ToggleHeadsetService extends Service {

		private String TAG = "ToggleHeadsetService";
		public static final String INTENT_UPDATE_ICON = "com.dwalkes.android.toggleheadset2.INTENT_UPDATE_ICON";
		public static final String INTENT_TOGGLE_HEADSET = "com.dwalkes.android.toggleheadset2.INTENT_TOGGLE_HEADSET";

		public IBinder onBind(Intent arg0) {
			// TODO Auto-generated method stub
			return null;
		}
		
		ToggleHeadsetBroadcastReceiver headsetReceiver = null;

		/**
		 * Starts a service to monitor headset toggle or updates the current toggle state
		 */
		@Override 
		public void onStart(Intent intent, int startId){
			Log.d(TAG,"onStart");
			Log.d(TAG, "Received " + intent.getAction() );
			
		    // Create an Intent to launch toggle headset
		    Intent toggleIntent = new Intent(this, ToggleHeadsetService.class);
		    toggleIntent.setAction(ToggleHeadsetService.INTENT_TOGGLE_HEADSET);
		    PendingIntent pendingIntent = PendingIntent.getService(this, 0, toggleIntent, 0);

		    // Get the layout for the App Widget and attach an on-click listener to the icon
		    RemoteViews views = new RemoteViews(this.getPackageName(), R.layout.toggle_apwidget);
		    views.setOnClickPendingIntent(R.id.Icon, pendingIntent);

			if(headsetReceiver == null )
			{
				/** Since HEADSET_PLUG uses FLAG_RECIEVER_REGISTERED_ONLY we need to register and
				 * unregister the broadcast receiver in the service
				 */
				headsetReceiver = new ToggleHeadsetBroadcastReceiver();
				IntentFilter filter = new IntentFilter(ToggleHeadsetBroadcastReceiver.HEADSET_PLUG_INTENT);
				registerReceiver(headsetReceiver, filter);
			}
			if( intent != null && intent.getAction() != null ) 
			{
				if( intent.getAction().equals(INTENT_TOGGLE_HEADSET)  )
				{
					// always toggle the headset if this was the received intent
					toggleHeadset();
				}
				else if( intent.getAction().equals(ToggleHeadsetBroadcastReceiver.HEADSET_PLUG_INTENT))
				{
					if( intent.getExtras().getInt("state") == 2 )
					{
						if( !isRoutingHeadset() )
						{
							// only change the headset toggle if not currently routing headset
							toggleHeadset();
						}
					}
				}
			}
			// always update the icon
			updateIcon();



		}
		
		/**
		 * Called when the service is destroyed (low memory conditions).  We may miss
		 * notification of headset plug
		 */
		public void onDestroy() {
			Log.i(TAG,"onDestroy");
			unregisterReceiver(headsetReceiver);
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
	    		Log.d(TAG,"route to speaker"); 
	    		/* see AudioService.setRouting
	    		* Use MODE_INVALID to force headset routing change */
	            manager.setRouting(AudioManager.MODE_INVALID, 0, AudioManager.ROUTE_HEADSET );
	    	}
	    	else 
	    	{
	    		Log.d(TAG,"route to headset"); 
	    		/* see AudioService.setRouting
	    		* Use MODE_INVALID to force headset routing change */
	            manager.setRouting(AudioManager.MODE_INVALID, AudioManager.ROUTE_HEADSET, AudioManager.ROUTE_HEADSET );
	    	}
		}
		
		/**
		 * Checks whether we are currently routing to headset
		 * @return true if routing to headset, false if routing somewhere else
		 */
		public boolean isRoutingHeadset() {
		    AudioManager manager = (AudioManager)this.getSystemService(Context.AUDIO_SERVICE);
			
			int routing = manager.getRouting(AudioManager.MODE_NORMAL);
	    	Log.d(TAG,"getRouting returns " + routing); 
	    	return (routing & AudioManager.ROUTE_HEADSET) != 0; 
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
	        
	        // Push update for this widget to the home screen
	        ComponentName thisWidget = new ComponentName(this, ToggleHeadsetAppWidgetProvider.class);
	        AppWidgetManager manager = AppWidgetManager.getInstance(this);
	        manager.updateAppWidget(thisWidget, view);
		}
		
	}

}
