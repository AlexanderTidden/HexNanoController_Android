package com.hexairbot.hexmini;


import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

import com.hexairbot.hexmini.HexMiniApplication.AppStage;
import com.hexairbot.hexmini.modal.OSDCommon;
import com.hexairbot.hexmini.modal.Transmitter;

public class MainExActivity extends FragmentActivity implements
		SettingsDialogDelegate, OnTouchListener,
		HudViewControllerDelegate {

	private static final String TAG = "MainExActivity";
	public static final int REQUEST_ENABLE_BT = 1;
	private static final int DIALOG_WIFI_DISABLE = 1000;

	private SettingsDialog settingsDialog;
	private HudExViewController hudVC;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		Log.d(TAG, "----onCreate");

		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

		hudVC = new HudExViewController(this, this);
		hudVC.onCreate();
		hudVC.onResume();
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		
		Log.e("onDestroy", "");

		if (Transmitter.sharedTransmitter().getBleConnectionManager() != null) {
			Transmitter.sharedTransmitter().transmmitSimpleCommand(
					OSDCommon.MSPCommnand.MSP_DISARM);
			Transmitter.sharedTransmitter().getBleConnectionManager().close();
		}

		hudVC.onDestroy();
		hudVC = null;

		Thread destroy = new Thread(new Runnable() {

			@Override
			public void run() {
				// TODO Auto-generated method stub
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				android.os.Process.killProcess(android.os.Process.myPid());
			}
		});
		destroy.start();
	}

	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		// mConnectStateManager.pause();
		super.onPause();
		//hudVC.onPause();
		
		Log.e("onPause", "onPause");
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onPostCreate(savedInstanceState);
	}

	@Override
	protected void onPostResume() {
		// TODO Auto-generated method stub
		super.onPostResume();
	}

	@Override
	protected void onRestart() {
		// TODO Auto-generated method stub
		super.onRestart();
		
		Log.e("onRestart", "onRestart");
	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		HexMiniApplication.sharedApplicaion().setAppStage(AppStage.HUD);
		//hudVC.onResume();
		
		Log.e("onResume", "onResume");
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		// TODO Auto-generated method stub
		super.onSaveInstanceState(outState);
	}

	@Override
	protected void onStart() {
		// TODO Auto-generated method stub
		super.onStart();

		hudVC.viewWillAppear();
		
		Log.e("onStart", "");
	}

	@Override
	protected void onStop() {
		// TODO Auto-generated method stub
		super.onStop();
		HexMiniApplication.sharedApplicaion().setAppStage(AppStage.UNKNOWN);
		
		Log.e("onStop()", "onStop");
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		// TODO Auto-generated method stub
		super.onConfigurationChanged(newConfig);
		Log.d(TAG, "----onConfigurationChanged");
		if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
			// Nothing need to be done here

		} else {
			// Nothing need to be done here
		}
	}

	@Override
	public void prepareDialog(SettingsDialog dialog) {

	}

	@Override
	public void onDismissed(SettingsDialog settingsDialog) {
		
		hudVC.setSettingsButtonEnabled(true);
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		return false;
	}

	@Override
	public void settingsBtnDidClick(View settingsBtn) {
		hudVC.setSettingsButtonEnabled(false);
		showSettingsDialog();
	}

	public ViewController getViewController() {
		return hudVC;
	}

	protected void showSettingsDialog() {
		FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
		ft.addToBackStack(null);

		if (settingsDialog == null) {
			Log.d(TAG, "settingsDialog is null");
			settingsDialog = new SettingsDialog(this, this);
		}

		settingsDialog.show(ft, "settings");
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == REQUEST_ENABLE_BT
				&& resultCode == Activity.RESULT_CANCELED) {
			finish();
			return;
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

}
