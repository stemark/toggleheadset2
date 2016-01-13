# Introduction #

This page describes how to install/use this widget.  This widget is meant to specifically address the problems with the 3.5mm audio jack on audio adapters (specifically the HTC 5 in 1 adapter,) the G1 and the donut release.  It hopefully won't be necessary and probably won't work after the next release.

## Tested Configurations ##
For a full list of tested configurations, please see [Tested Configurations](http://code.google.com/p/toggleheadset2/wiki/TestedConfigurations)

# Downloading #
## Downloading From The Project Page (old version)... not recommended ##
  * Make sure your settings allow install of non-market applications
    * Menu->Settings->Applications->Unknown sources = check.
  * Download the .apk file to your phone and install by selecting the file in a file browser application OR use your phone's browser to click on the download link on the project homepage.

## Downloading from the Play Store ##
  * Download and install like a regular application through the Google Play store.  You can find it at [this link](https://play.google.com/store/apps/details?id=com.dwalkes.android.toggleheadset2&hl=en)

# Installing the Widget #

This application only runs as a widget, there is no user application or menu available.  You must complete this step to install the application as a widget somewhere on your home screen.

## Andriod 3 and later ##
After clicking the "All Apps" button to list all applications, You should see a "Widgets" tab at the top left of the screen.  Click "Widgets" and find toggleheadset2.  Hold and drag to place on your homescreen.

## Android 2 and earlier ##

  * From the home screen, press the "Menu" button
  * Select "Add"
  * Select "Widgets"
  * Select "Toggle Headset 2"
  * If you'd like to force routing to speaker on startup, click the check box displayed when placing the icon and press the OK button.  This will override the headset detect switch and always force audio to the speaker at startup.  If you don't want to use this behavior, leave the box un-checked.
  * Place somewhere on your home screen.

# Using #

  * As long as the service is running (hasn't been stopped due to low resources) the 3.5 mm jack should automatically be selected as output when plugged in on an HTC 5 in 1 adapter.  When you disconnect the htc adapter the audio output should automatically switch back to speakerphone.

  * If you'd like to switch between speaker phone or headset routing manually, click on the widget icon on the home screen.  Audio output should toggle on each click.

  * The icon status should show whether headphone or speaker routing is currently enabled