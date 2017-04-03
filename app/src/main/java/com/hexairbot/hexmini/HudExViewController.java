package com.hexairbot.hexmini;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.opengl.GLSurfaceView;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.FrameLayout;

import com.hexairbot.hexmini.HexMiniApplication.AppStage;
import com.hexairbot.hexmini.ble.BleConnectinManager;
import com.hexairbot.hexmini.gestures.EnhancedGestureDetector;
import com.hexairbot.hexmini.modal.ApplicationSettings;
import com.hexairbot.hexmini.modal.Channel;
import com.hexairbot.hexmini.modal.OSDCommon;
import com.hexairbot.hexmini.modal.Transmitter;
import com.hexairbot.hexmini.sensors.DeviceOrientationChangeDelegate;
import com.hexairbot.hexmini.sensors.DeviceOrientationManager;
import com.hexairbot.hexmini.sensors.DeviceSensorManagerWrapper;
import com.hexairbot.hexmini.ui.Button;
import com.hexairbot.hexmini.ui.Image;
import com.hexairbot.hexmini.ui.Image.SizeParams;
import com.hexairbot.hexmini.ui.Indicator;
import com.hexairbot.hexmini.ui.Sprite;
import com.hexairbot.hexmini.ui.Sprite.Align;
import com.hexairbot.hexmini.ui.Text;
import com.hexairbot.hexmini.ui.ToggleButton;
import com.hexairbot.hexmini.ui.UIRenderer;
import com.hexairbot.hexmini.ui.joystick.AcceleratorJoystick;
import com.hexairbot.hexmini.ui.joystick.AnalogueJoystick;
import com.hexairbot.hexmini.ui.joystick.JoystickBase;
import com.hexairbot.hexmini.ui.joystick.JoystickFactory;
import com.hexairbot.hexmini.ui.joystick.JoystickFactory.JoystickType;
import com.hexairbot.hexmini.ui.joystick.JoystickListener;
import com.hexairbot.hexmini.util.FontUtils;

import java.util.ArrayList;


public class HudExViewController extends ViewController
	implements OnTouchListener,
			   OnGestureListener,
			   SettingsViewControllerDelegate, DeviceOrientationChangeDelegate
{
	private static final String TAG = "HudExViewController";

	private static final int LOGO                 = 0;
	private static final int JOY_ID_LEFT          = 1;
	private static final int JOY_ID_RIGHT         = 2;
	private static final int MIDLLE_BG_ID         = 3;
	private static final int TAKE_OFF_BTN_ID      = 4;
	private static final int STOP_BTN_ID          = 5;
	private static final int SETTINGS_BTN_ID      = 6;
	private static final int ALT_HOLD_TOGGLE_BTN  = 7;
	private static final int BATTERY_INDICATOR_ID = 8;
	private static final int HELP_BTN             = 9;

	private static final int BLE_INDICATOR       	   = 23;
	private static final int WEB_ADDRESS			   = 24;
	
    private static final int DEBUG_TEXT_VIEW      = 25;

	private final float  BEGINNER_ELEVATOR_CHANNEL_RATIO  = 0.5f;
	private final float  BEGINNER_AILERON_CHANNEL_RATIO   = 0.5f;
	private final float  BEGINNER_RUDDER_CHANNEL_RATIO    = 0.0f;
	private final float  BEGINNER_THROTTLE_CHANNEL_RATIO  = 0.8f;
	
	private final float  AUTO_ALT_HOLD_MIN_THROTTLE = -0.6f;
	private final float  AUTO_ALT_HOLD_MAX_THROTTLE = 0.4f;
	
	
	private Button stopBtn;
	private Button takeOffBtn;
	private Button settingsBtn;
	private ToggleButton altHoldToggleBtn;
	
	private boolean isAltHoldMode;
	private boolean isAccMode;
	
	private Button[] buttons;
	
	private Indicator batteryIndicator;
	private Indicator bleIndicator;

	private GLSurfaceView glView;
	
	private JoystickBase[] joysticks;   //[0]roll and pitch, [1]rudder and throttle
	private float joypadOpacity;
	private GestureDetector gestureDetector;
	
	private UIRenderer renderer;
	
    private HudViewControllerDelegate delegate;
    
    private boolean isLeftHanded;
    private JoystickListener rollPitchListener;
    private JoystickListener rudderThrottleListener;
    
    private ApplicationSettings settings;
    
    private Channel aileronChannel;
    private Channel elevatorChannel;
    private Channel rudderChannel;
    private Channel throttleChannel;
    private Channel aux1Channel;
    private Channel aux2Channel;
    private Channel aux3Channel;
    private Channel aux4Channel;
    
    private DeviceOrientationManager deviceOrientationManager;
    private static final float ACCELERO_TRESHOLD = (float) Math.PI / 180.0f * 2.0f;
    private static final int PITCH = 1;
    private static final int ROLL = 2;
    private float pitchBase;
    private float rollBase;
    private boolean rollAndPitchJoystickPressed;
    
    private Image middleBg;
    
    private Text debugTextView;
	private ArrayList gameControllerList;

	private String debugStr;

	//get list of connected game controllers
	public ArrayList getGameControllerIds() {

		ArrayList gameControllerDeviceIds = new ArrayList();
		int[] deviceIds = InputDevice.getDeviceIds();
		for (int deviceId : deviceIds) {
			InputDevice dev = InputDevice.getDevice(deviceId);
			int sources = dev.getSources();

			// Verify that the device has gamepad buttons, control sticks, or both.
			if (((sources & InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD)
					|| ((sources & InputDevice.SOURCE_JOYSTICK)
					== InputDevice.SOURCE_JOYSTICK)) {
				// This device is a game controller. Store its device ID.
				if (!gameControllerDeviceIds.contains(deviceId)) {
					gameControllerDeviceIds.add(deviceId);
				}
			}
		}
		return gameControllerDeviceIds;
	}
    
	public HudExViewController(Activity context, HudViewControllerDelegate delegate)
	{
		this.delegate = delegate;
		this.context = context;

		Transmitter.sharedTransmitter().setBleConnectionManager(new BleConnectinManager(context));      
		settings = ((HexMiniApplication)context.getApplication()).getAppSettings();
		
	    joypadOpacity = settings.getInterfaceOpacity();
	    isLeftHanded  = settings.isLeftHanded();

		gestureDetector = new EnhancedGestureDetector(context, this);
		
		joysticks = new JoystickBase[2];

		glView = new GLSurfaceView(context);
		glView.setEGLContextClientVersion(2);
		
		context.setContentView(R.layout.hud_view_controller_framelayout);
		
		FrameLayout mainFrameLayout = (FrameLayout)context.findViewById(R.id.mainFrameLaytout);
		
		//let glView to be transparent
		glView.setZOrderOnTop(true);
		glView.getHolder().setFormat(PixelFormat.TRANSLUCENT);
		glView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
		 
		mainFrameLayout.addView(glView);
		
		//context.setContentView(glView);
		renderer = new UIRenderer(context, null);
	
		initGLSurfaceView();

		Resources res = context.getResources();

		middleBg = new Image(res, R.drawable.main_background, Align.CENTER);
		middleBg.setAlpha(0.5f);
		middleBg.setVisible(true);
		middleBg.setSizeParams(SizeParams.REPEATED, SizeParams.REPEATED);
		middleBg.setAlphaEnabled(true);		

		Image logo = new Image(res, R.drawable.logo_new, Align.BOTTOM_LEFT);
		logo.setMargin(0, 0, (int)res.getDimension(R.dimen.main_logo_margin_bottom), (int)res.getDimension(R.dimen.main_logo_margin_left));

		Image web_address = new Image(res, R.drawable.web_address, Align.BOTTOM_RIGHT);
		web_address.setMargin(0, (int)res.getDimension(R.dimen.main_web_address_margin_right), (int)res.getDimension(R.dimen.main_web_address_margin_bottom), 0);

		Button helpBtn = new Button(res, R.drawable.btn_help_normal, R.drawable.btn_help_hl, Align.BOTTOM_RIGHT);
		helpBtn.setMargin(0, (int)res.getDimension(R.dimen.help_btn_settings_margin_right), (int)res.getDimension(R.dimen.help_btn_settings_margin_bottom), 0);

		takeOffBtn = new Button(res, R.drawable.btn_unlock_normal, R.drawable.btn_unlock_press, Align.BOTTOM_CENTER);
		takeOffBtn.setAlphaEnabled(true);

		stopBtn = new Button(res, R.drawable.btn_lock_normal, R.drawable.btn_lock_press, Align.TOP_CENTER);
		stopBtn.setAlphaEnabled(true);

		int batteryIndicatorRes[] = {R.drawable.device_battery_ac,
				R.drawable.device_battery_0,
				R.drawable.device_battery_1,
				R.drawable.device_battery_2,
				R.drawable.device_battery_3
		};

		batteryIndicator = new Indicator(res, batteryIndicatorRes, Align.TOP_RIGHT);
		batteryIndicator.setMargin((int)res.getDimension(R.dimen.main_device_battery_margin_top), (int)res.getDimension(R.dimen.main_device_battery_margin_right), 0, 0);
		
		altHoldToggleBtn = new ToggleButton(res, R.drawable.btn_hold_off_normal, R.drawable.btn_hold_off_press,
                R.drawable.btn_hold_on_normal, R.drawable.btn_hold_on_press,
                R.drawable.btn_hold_on_normal, Align.TOP_LEFT);
		
		altHoldToggleBtn.setMargin(res.getDimensionPixelOffset(R.dimen.hud_alt_hold_toggle_btn_margin_top), 0, 0, res.getDimensionPixelOffset(R.dimen.hud_alt_hold_toggle_btn_margin_left));
		altHoldToggleBtn.setChecked(settings.isAltHoldMode());
		altHoldToggleBtn.setVisible(false);
		
		settingsBtn = new Button(res, R.drawable.btn_settings_normal1, R.drawable.btn_settings_normal1_press, Align.TOP_RIGHT);
		settingsBtn.setMargin((int)res.getDimension(R.dimen.main_btn_settings_margin_top), (int)res.getDimension(R.dimen.main_btn_settings_margin_right), 0, 0);
		
		int bleIndicatorRes[] = {
				R.drawable.ble_indicator_opened,
				R.drawable.ble_indicator_closed		
		};
		bleIndicator = new Indicator(res, bleIndicatorRes, Align.TOP_RIGHT);
		bleIndicator.setMargin((int)res.getDimension(R.dimen.main_ble_margin_top), (int)res.getDimension(R.dimen.main_ble_margin_right), 0, 0);
		bleIndicator.setValue(1);
	
		buttons = new Button[5];
		buttons[0] = settingsBtn;
		buttons[1] = takeOffBtn;
		buttons[2] = stopBtn;
		buttons[3] = altHoldToggleBtn;
		buttons[4] = helpBtn;

		String debugStr = "000, 000, 000, 0.0";
		debugTextView = new Text(context, debugStr, Align.TOP_LEFT);
		debugTextView.setMargin((int)res.getDimension(R.dimen.hud_state_text_margin_top) * 2, 0, 0, 0);
		debugTextView.setTextColor(Color.WHITE);
		debugTextView.setTypeface(FontUtils.TYPEFACE.Helvetica(context));
		debugTextView.setTextSize(res.getDimensionPixelSize(R.dimen.hud_state_text_size) * 2 / 3);

		HexMiniApplication.sharedApplicaion().setDebugTextView(debugTextView);

		renderer.addSprite(MIDLLE_BG_ID, middleBg);				
		renderer.addSprite(LOGO, logo);	
		renderer.addSprite(WEB_ADDRESS, web_address);	
		renderer.addSprite(BATTERY_INDICATOR_ID, batteryIndicator);
		renderer.addSprite(TAKE_OFF_BTN_ID, takeOffBtn);
		renderer.addSprite(STOP_BTN_ID, stopBtn);
		renderer.addSprite(SETTINGS_BTN_ID, settingsBtn);
		renderer.addSprite(ALT_HOLD_TOGGLE_BTN, altHoldToggleBtn);
		renderer.addSprite(BLE_INDICATOR, bleIndicator);
		renderer.addSprite(DEBUG_TEXT_VIEW, debugTextView);
		renderer.addSprite(HELP_BTN, helpBtn);
		
		
		isAccMode = settings.isAccMode();
		deviceOrientationManager = new DeviceOrientationManager(new DeviceSensorManagerWrapper(this.context), this);
		deviceOrientationManager.onCreate();
		
		
		initJoystickListeners();
		
		helpBtn.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(HudExViewController.this.context, HelpActivity.class);
				HudExViewController.this.context.startActivity(intent);
			}
		});
		
		
		if (isAccMode) {
			initJoysticks(JoystickType.ACCELERO);
		}
		else{
			initJoysticks(JoystickType.ANALOGUE);
		}
		
		initListeners();
		
		initChannels();
		
		if (settings.isHeadFreeMode()) {
			aux1Channel.setValue(1);
		}
		else {
			aux1Channel.setValue(-1);
		}
		
		if (settings.isAltHoldMode()) {
			aux2Channel.setValue(1);
		}
		else{
			aux2Channel.setValue(-1);
		}
		
	    if (settings.isBeginnerMode()) {	       
			new AlertDialog.Builder(context)
			.setIcon(android.R.drawable.ic_dialog_alert).setTitle(R.string.dialog_title_info)
			.setMessage(R.string.beginner_mode_info)
			.setPositiveButton(R.string.dialog_btn_ok, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					
				}
			}).show();
	    }

		gameControllerList = getGameControllerIds();

		if (gameControllerList.isEmpty()) {
			new AlertDialog.Builder(context)
					.setIcon(android.R.drawable.ic_dialog_alert).setTitle(R.string.dialog_title_info)
					.setMessage("No game controller connected")
					.setPositiveButton(R.string.dialog_btn_ok, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {

						}
					}).show();
		}
		else
		{            new AlertDialog.Builder(context)
				.setIcon(android.R.drawable.ic_dialog_alert).setTitle(R.string.dialog_title_info)
				.setMessage("Game controller detected")
				.setPositiveButton(R.string.dialog_btn_ok, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {

					}
				}).show();
		}
	}
	
	private void initChannels() {
	    aileronChannel  = settings.getChannel(Channel.CHANNEL_NAME_AILERON);
	    elevatorChannel = settings.getChannel(Channel.CHANNEL_NAME_ELEVATOR);
	    rudderChannel   = settings.getChannel(Channel.CHANNEL_NAME_RUDDER);
	    throttleChannel = settings.getChannel(Channel.CHANNEL_NAME_THROTTLE);
	    aux1Channel     = settings.getChannel(Channel.CHANNEL_NAME_AUX1);
	    aux2Channel     = settings.getChannel(Channel.CHANNEL_NAME_AUX2);
	    aux3Channel     = settings.getChannel(Channel.CHANNEL_NAME_AUX3);
	    aux4Channel     = settings.getChannel(Channel.CHANNEL_NAME_AUX4);
	    
	    aileronChannel.setValue(0.0f);
	    elevatorChannel.setValue(0.0f);
	    rudderChannel.setValue(0.0f);
	    throttleChannel.setValue(-1);
	}
	
	private void setAltHoldMode(Boolean isAltHoldMode){
	    if(isAltHoldMode) {
	        if ((((int)aux2Channel.getValue()) != 1)) {
	            aux2Channel.setValue(1);
	        }
	    }
	    else{
	        if ((((int)aux2Channel.getValue()) != -1)) {
	        	aux2Channel.setValue(-1);
	        }
	    }
	}
	
	private void initJoystickListeners()
    {
	        rollPitchListener = new JoystickListener()
	        {
	            public void onChanged(JoystickBase joy, float x, float y)
	            {
	            	if(HexMiniApplication.sharedApplicaion().getAppStage() == AppStage.SETTINGS
	            			|| HexMiniApplication.sharedApplicaion().getAppStage() == AppStage.UNKNOWN){
	            		return;
	            	}
	            	
	            	if (isAccMode == false && rollAndPitchJoystickPressed == true) {
		        		if (settings.isBeginnerMode()) {
		        			aileronChannel.setValue(x * BEGINNER_AILERON_CHANNEL_RATIO);
		        			elevatorChannel.setValue(y * BEGINNER_ELEVATOR_CHANNEL_RATIO);
						}
		        		else{
			                aileronChannel.setValue(x);
			                elevatorChannel.setValue(y);
		        		}
					}
	            }

	            @Override
	            public void onPressed(JoystickBase joy)
	            {
	            	rollAndPitchJoystickPressed = true;
	            }

	            @Override
	            public void onReleased(JoystickBase joy)
	            {
	            	rollAndPitchJoystickPressed = false;
	            	
	                aileronChannel.setValue(0.0f);
	                elevatorChannel.setValue(0.0f);
	            }
	        };

	        rudderThrottleListener = new JoystickListener()
	        {
	            public void onChanged(JoystickBase joy, float x, float y)
	            {
	            	if(HexMiniApplication.sharedApplicaion().getAppStage() == AppStage.SETTINGS){
	            		return;
	            	}
	            	
	        		if (settings.isBeginnerMode()) {
	        			rudderChannel.setValue(x * BEGINNER_RUDDER_CHANNEL_RATIO);
		        		throttleChannel.setValue((BEGINNER_THROTTLE_CHANNEL_RATIO - 1) + y * BEGINNER_THROTTLE_CHANNEL_RATIO);

					}else{
		        		rudderChannel.setValue(x);
		        		throttleChannel.setValue(y);
					}
	            }

	            @Override
	            public void onPressed(JoystickBase joy)
	            {
	            	
	            }

	            @Override
	            public void onReleased(JoystickBase joy)
	            {
	        		rudderChannel.setValue(0.0f);
	        		throttleChannel.setValue(joy.getYValue());
	        		
	                if(settings.isAutoAltHoldMode()){	                    
	                    if((throttleChannel.getValue() >= AUTO_ALT_HOLD_MIN_THROTTLE)
	                       && (throttleChannel.getValue() <= AUTO_ALT_HOLD_MAX_THROTTLE)
	                       &&(HexMiniApplication.sharedApplicaion().getCurrentAlt() < 200)){
	                    	HudExViewController.this.setAltHoldMode(true);
	                    }
	                    else{
	                        HudExViewController.this.setAltHoldMode(false);
	                    }
	                }
	                else{
	                	HudExViewController.this.setAltHoldMode(false);
	                }
	            }
	        };
    }
	
	private void initListeners() {
		settingsBtn.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				
				if (delegate != null) {
					delegate.settingsBtnDidClick(arg0);
				}

			}
		});
		
		takeOffBtn.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
			    throttleChannel.setValue(-1);
			    getRudderAndThrottleJoystick().setYValue(-1);
				Transmitter.sharedTransmitter().transmmitSimpleCommand(OSDCommon.MSPCommnand.MSP_ARM);
			}
		});
		
		stopBtn.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				Transmitter.sharedTransmitter().transmmitSimpleCommand(OSDCommon.MSPCommnand.MSP_DISARM);
			}
		});
		
		
		altHoldToggleBtn.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				isAltHoldMode = !isAltHoldMode;
				settings.setIsAltHoldMode(isAltHoldMode);
				settings.save();
				
				altHoldToggleBtn.setChecked(isAltHoldMode);
				
				if (isAltHoldMode) {
					aux2Channel.setValue(1);
				}
				else{
					aux2Channel.setValue(-1);
				}
			}
		});
	}
		
	private void initGLSurfaceView() {
		if (glView != null) {
			glView.setEGLConfigChooser(8 , 8, 8, 8, 16, 0);
			glView.setRenderer(renderer);
			glView.setOnTouchListener(this);
		}
	}
	
	 private void initJoysticks(JoystickType rollAndPitchType)
	    {
	        JoystickBase rollAndPitchJoystick      = getRollAndPitchJoystick(); 
	        JoystickBase rudderAndThrottleJoystick = getRudderAndThrottleJoystick();
	        
	        if (rollAndPitchType == JoystickType.ANALOGUE) {
	            if (rollAndPitchJoystick == null || !(rollAndPitchJoystick instanceof AnalogueJoystick)) {
	            	rollAndPitchJoystick = JoystickFactory.createAnalogueJoystick(this.getContext(), false, rollPitchListener, true);
	            	rollAndPitchJoystick.setXDeadBand(settings.getAileronDeadBand());
	            	rollAndPitchJoystick.setYDeadBand(settings.getElevatorDeadBand());
	            } 
	            else {
	            	rollAndPitchJoystick.setOnAnalogueChangedListener(rollPitchListener);
	            }
			}
	        else if(rollAndPitchType == JoystickType.ACCELERO){
	            if (rollAndPitchJoystick == null || !(rollAndPitchJoystick instanceof AcceleratorJoystick)) {
	            	rollAndPitchJoystick = JoystickFactory.createAcceleroJoystick(this.getContext(), false, rollPitchListener, true);
	            } 
	            else {
	            	rollAndPitchJoystick.setOnAnalogueChangedListener(rollPitchListener);
	            }
	        }
	        
	        if (rudderAndThrottleJoystick == null || !(rudderAndThrottleJoystick instanceof AnalogueJoystick)) {
	        	rudderAndThrottleJoystick = JoystickFactory.createAnalogueJoystick(this.getContext(), false, rudderThrottleListener, false);
	        	rudderAndThrottleJoystick.setXDeadBand(settings.getRudderDeadBand());
	        } 
	        else {
	        	rudderAndThrottleJoystick.setOnAnalogueChangedListener(rudderThrottleListener);
	        }
	        
	        rollAndPitchJoystick.setIsRollPitchJoystick(true);
	        rudderAndThrottleJoystick.setIsRollPitchJoystick(false);
	        
	        joysticks[0] = rollAndPitchJoystick;
	        joysticks[1] = rudderAndThrottleJoystick;
	        
	        setJoysticks();
	        
	        getRudderAndThrottleJoystick().setYValue(-1);
	    }
	
	public void setJoysticks()
	{
		JoystickBase rollAndPitchJoystick = joysticks[0];
		JoystickBase rudderAndThrottleJoystick = joysticks[1];
		
		if (rollAndPitchJoystick != null) 
		{
			if (isLeftHanded) {
			    joysticks[0].setAlign(Align.BOTTOM_RIGHT);
			    joysticks[0].setAlpha(joypadOpacity);
			}else{
				joysticks[0].setAlign(Align.BOTTOM_LEFT);
				joysticks[0].setAlpha(joypadOpacity);
			}
			
			rollAndPitchJoystick.setNeedsUpdate();
		}
	
		if (rudderAndThrottleJoystick != null)	{
			if (isLeftHanded) {
			    joysticks[1].setAlign(Align.BOTTOM_LEFT);
			    joysticks[1].setAlpha(joypadOpacity);
			}else{
			    joysticks[1].setAlign(Align.BOTTOM_RIGHT);
			    joysticks[1].setAlpha(joypadOpacity);
			}
			
			rudderAndThrottleJoystick.setNeedsUpdate();
		}
		
		for (int i=0; i<joysticks.length; ++i) {
		    JoystickBase joystick = joysticks[i];
		    
			if (joystick != null) {
				joystick.setInverseYWhenDraw(true);

				int margin = context.getResources().getDimensionPixelSize(R.dimen.hud_joy_margin);
				
				joystick.setMargin(0, margin, 48 + margin, margin);
			}
		}
		
		renderer.removeSprite(JOY_ID_LEFT);
		renderer.removeSprite(JOY_ID_RIGHT);

		if (rollAndPitchJoystick != null) {
			if (isLeftHanded) {
				renderer.addSprite(JOY_ID_RIGHT, rollAndPitchJoystick);
			}
			else{
				renderer.addSprite(JOY_ID_LEFT, rollAndPitchJoystick);
			}
		}
		
		if (rudderAndThrottleJoystick != null) {
			if (isLeftHanded) {
				renderer.addSprite(JOY_ID_LEFT, rudderAndThrottleJoystick);
			}
			else{
				renderer.addSprite(JOY_ID_RIGHT, rudderAndThrottleJoystick);
			}
		}
	}
	
	public JoystickBase getRollAndPitchJoystick()
	{
		return joysticks[0];
	}
	
	public JoystickBase getRudderAndThrottleJoystick()
	{
			return joysticks[1];
	}
	
	public void setInterfaceOpacity(float opacity)
	{
		if (opacity < 0 || opacity > 100.0f) {
			Log.w(TAG, "Can't set interface opacity. Invalid value: " + opacity);
			return;
		}
		
		joypadOpacity = opacity / 100f;
		
		Sprite joystick = renderer.getSprite(JOY_ID_LEFT);
		joystick.setAlpha(joypadOpacity);
		
		joystick = renderer.getSprite(JOY_ID_RIGHT);
		joystick.setAlpha(joypadOpacity);
	}

	public void setSettingsButtonEnabled(boolean enabled)
	{
		settingsBtn.setEnabled(enabled);
	}
	
	public void onPause()
	{
		if (glView != null) {
			glView.onPause();
		}
		
		deviceOrientationManager.pause();
	}
	
	public void onResume()
	{
		if (glView != null) {
			glView.onResume();
		}
		
		deviceOrientationManager.resume();
	}

    //glView onTouch Event handler
	public boolean onTouch(View v, MotionEvent event)
	{
		boolean result = false;
		
		for (int i=0; i<buttons.length; ++i) {
			if (buttons[i].processTouch(v, event)) {
				result = true;
				break;
			}
		}
		
		if (result != true) {	
			gestureDetector.onTouchEvent(event);
			
			for (int i=0; i<joysticks.length; ++i) {
				JoystickBase joy = joysticks[i];
				if (joy != null) {
					if (joy.processTouch(v, event)) {
						result = true;
					}
				}
			}
		}
		
		return result;
	}
	
	public void onDestroy()
	{
	    renderer.clearSprites();
	    deviceOrientationManager.destroy();
	}

	public boolean onDown(MotionEvent e) 
	{
		return false;
	}

	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
			float velocityY) 
	{
		return false;
	}

	public void onLongPress(MotionEvent e) 
	{
    	// Left unimplemented	
	}

	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
			float distanceY) 
	{
		return false;
	}

	public void onShowPress(MotionEvent e) 
	{
	    // Left unimplemented
	}

	public boolean onSingleTapUp(MotionEvent e) 
	{
		return false;
	}
	
	@Override
	public void interfaceOpacityValueDidChange(float newValue) {
		setInterfaceOpacity(newValue);
	}

	@Override
	public void leftHandedValueDidChange(boolean isLeftHanded) {
		this.isLeftHanded = isLeftHanded;

		setJoysticks();
		
		Log.e(TAG, "THRO:" + throttleChannel.getValue());
		
		getRudderAndThrottleJoystick().setYValue(throttleChannel.getValue());
	}

	@Override
	public void accModeValueDidChange(boolean isAccMode) {
		this.isAccMode = isAccMode;
		
		initJoystickListeners();
		
		if (isAccMode) {
			initJoysticks(JoystickType.ACCELERO);
		}
		else{
			initJoysticks(JoystickType.ANALOGUE);
		}
	}
	
    
	@Override
	public void headfreeModeValueDidChange(boolean isHeadfree) {
		if (settings.isHeadFreeMode()) {
			aux1Channel.setValue(1);
		}
		else {
			aux1Channel.setValue(-1);
		}
	}
	
	@Override
	public void autoAltHoldModeValueDidChange(boolean isAutoAltHoldMode){
	
	}
	
	@Override
	public void aileronAndElevatorDeadBandValueDidChange(float newValue) {
	    JoystickBase rollAndPitchJoyStick  = getRollAndPitchJoystick();
        
	    rollAndPitchJoyStick.setXDeadBand(newValue);
	    rollAndPitchJoyStick.setYDeadBand(newValue);
	}

	@Override
	public void rudderDeadBandValueDidChange(float newValue) {
	    JoystickBase rudderAndThrottleStick  = getRudderAndThrottleJoystick();
        
	    rudderAndThrottleStick.setXDeadBand(newValue);
	}

	@Override
	public void onDeviceOrientationChanged(float[] orientation,
			float magneticHeading, int magnetoAccuracy) {
		  if (rollAndPitchJoystickPressed == false) {
	            pitchBase = orientation[PITCH];
	            rollBase = orientation[ROLL];
                aileronChannel.setValue(0.0f);
                elevatorChannel.setValue(0.0f);
	      }
		  else {
	            float x = (orientation[PITCH] - pitchBase);
	            float y = (orientation[ROLL] - rollBase);

	            if (isAccMode) {
					Log.d(TAG, "ROLL:" + (-x) + ",PITCH:" + y);
					
					if (Math.abs(x) > ACCELERO_TRESHOLD || Math.abs(y) > ACCELERO_TRESHOLD) {
			            if (settings.isBeginnerMode()) {
							aileronChannel.setValue(-x * BEGINNER_AILERON_CHANNEL_RATIO);
			                elevatorChannel.setValue(y * BEGINNER_ELEVATOR_CHANNEL_RATIO);
						}else{
							aileronChannel.setValue(-x);
			                elevatorChannel.setValue(y);
						}
					}
				}
	        }
	}

	@Override
	public void didConnect() {
		bleIndicator.setValue(0);
	}

	@Override
	public void didDisconnect() {
		bleIndicator.setValue(1);
	}

	@Override
	public void didFailToConnect() {
		bleIndicator.setValue(1);
	}

	@Override
	public void beginnerModeValueDidChange(boolean isBeginnerMode) {
		
	}

	@Override
	public void tringToConnect(String target) {
		ApplicationSettings settings = HexMiniApplication.sharedApplicaion().getAppSettings();

		if (target.equals("FlexBLE")) {

			if (settings.getFlexbotVersion().equals("1.5.0") == false) {
				settings.setFlexbotVersion("1.5.0");
				settings.save();
			}
			
			HexMiniApplication.sharedApplicaion().setFullDuplex(true);
		}
			
		else{
			settings.getFlexbotVersion().equals("1.0.0");
			settings.save();
			HexMiniApplication.sharedApplicaion().setFullDuplex(false);
				
		}
	}
}
