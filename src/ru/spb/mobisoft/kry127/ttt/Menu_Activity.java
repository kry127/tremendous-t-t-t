package ru.spb.mobisoft.kry127.ttt;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;

import ru.spb.mobisoft.kry127.ttt.R;

public class Menu_Activity extends Activity {
	
	boolean launchedFirstTime;
	boolean music;
	boolean vibro;
	int lang;
	
	private Intent svc;
	private SharedPreferences sharedPreferences;
	private SharedPreferences.Editor editor;
	private Vibrator vibrator;
	
	private final static int SETTINGS_REQUEST = 1;
	private final static int MID_VIBRATION = 250;
	private final static int SETTINGS_DIALOG = 0;
	private final static int GAMEMODE_DIALOG = 1;
	private final static int END_GAME_DIALOG = 2;
	
	final static float LOGO_STRETCH = (float)0.25;
	final static float BUTTON_STRETCH = (float)0.1;
	
	final static String SHARED_PREFERENCE_FILE = "ru.spb.mobisoft.kry127.ttt.SHARED_PREFERENCE_FILE";
	final static String LFT_KEY = "LAUNCHED_FIRST_TIME";
	final static String MUSIC_KEY = "MUSIC";
	final static String VIBRO_KEY = "VIBRO";
	final static String LANGUAGE_KEY = "LANG";
	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		// убираем заголовок
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main_menu);
		
		LinearLayout bckgr = (LinearLayout)(this.findViewById(R.id.bckgr));
		bckgr.setBackgroundResource(R.drawable.bg);
		
		// получаем вибратор э хха х ха ха
		vibrator = (Vibrator)Menu_Activity.this
				.getSystemService(Context.VIBRATOR_SERVICE);
		
		// starting service
		svc = new Intent(this, BgSoundService.class);
		
		// getting preference
		sharedPreferences = this.getSharedPreferences(SHARED_PREFERENCE_FILE, Context.MODE_PRIVATE);
		editor = sharedPreferences.edit();
		launchedFirstTime = sharedPreferences.getBoolean(LFT_KEY, true);
		music = sharedPreferences.getBoolean(MUSIC_KEY, true);
		vibro = sharedPreferences.getBoolean(VIBRO_KEY, false);
		lang = sharedPreferences.getInt(LANGUAGE_KEY, 0);
		
		int height;
		ImageView logo = (ImageView) findViewById(R.id.logo);
		LayoutParams lp = logo.getLayoutParams();
		height = this.getWallpaper().getIntrinsicHeight();
		lp.height = (int) (height * LOGO_STRETCH);
		logo.setLayoutParams(lp);
		
		// изменение высоты кнопок
		LinearLayout llStart = (LinearLayout) findViewById(R.id.llStart);
		LinearLayout llSettings = (LinearLayout) findViewById(R.id.llSettings);
		LinearLayout llQuit = (LinearLayout) findViewById(R.id.llQuit);
		LinearLayout llMore = (LinearLayout) findViewById(R.id.llMore);
		setButtonHeight(llStart, height, BUTTON_STRETCH);
		setButtonHeight(llSettings, height, BUTTON_STRETCH);
		setButtonHeight(llQuit, height, BUTTON_STRETCH);
		setButtonHeight(llMore, height, BUTTON_STRETCH);
		
		// если приложение запускаетс€ в первый раз, то добавл€ем иконку
		if (launchedFirstTime) {
			this.addShortcut();
			editor.putBoolean(LFT_KEY, false);
			editor.commit();
		}
		
	    new StaticObjects(this);
	}
	
	protected void setButtonHeight(LinearLayout btnHolder, int screenHeight, float percent) {
		LinearLayout.LayoutParams llStartParams = (LinearLayout.LayoutParams)
				btnHolder.getLayoutParams();
		llStartParams.height = (int) (screenHeight * percent);
		btnHolder.setLayoutParams(llStartParams);
	}
	
	@Override
	protected void onDestroy()
	{
		super.onDestroy();
		stopService(svc);
	}
	
	@Override
	protected void onPause()
	{
		super.onPause();
		if (!StaticObjects.getPlayerMutex())
			stopService(svc);
	}
	
	@Override
	protected void onResume()
	{
		// предполагаю, что после закрыти€ диалога вызываетс€ onResume
		super.onResume();
		if (music && !StaticObjects.getPlayerMutex())
			startService(svc);
		StaticObjects.setPlayerMutex(false);
	}

	@Override
	protected Dialog onCreateDialog(int id)
	{
		switch (id) {
		case SETTINGS_DIALOG:
			return createSettingsDialog();
		case GAMEMODE_DIALOG:
			return createGamemodeDialog();
		case END_GAME_DIALOG:
			return endGameDialog();
		default:
			return null;
		}
	}
	
	// SETTINGS_DIALOG creation
	private Dialog createSettingsDialog()
	{

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		LinearLayout root = (LinearLayout) getLayoutInflater().inflate(R.layout.settings, null);
		builder.setView(root);
		
		Spinner s = (Spinner)root.findViewById(R.id.language_spinner);
		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
		        R.array.language, android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		s.setAdapter(adapter);
		
		CheckBox m = (CheckBox)root.findViewById(R.id.sound);
		CheckBox v = (CheckBox)root.findViewById(R.id.vibro);

		s.setSelection(lang);
		m.setChecked(music);
		v.setChecked(vibro);

		s.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1,
					int arg2, long arg3) {
				lang = arg2;
				editor.putInt(LANGUAGE_KEY, lang);
				editor.commit();
				if (vibro)
					vibrator.vibrate(MID_VIBRATION);
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) { }
			
		});
		
		OnClickListener onClickListener = new OnClickListener() {
			@Override
			public void onClick(View v)
			{
				switch (v.getId()) {
				case R.id.sound:
					music = ((CheckBox)v).isChecked();
					editor.putBoolean(MUSIC_KEY, music);
					editor.commit();
					if (music) {
						startService(svc);
					} else {
						stopService(svc);
					}
					break;
				case R.id.vibro:
					vibro = ((CheckBox)v).isChecked();
					editor.putBoolean(VIBRO_KEY, vibro);
					editor.commit();
					break;
				default:
					break;
				}
				if (vibro)
					vibrator.vibrate(MID_VIBRATION);
			}
		};
		
		m.setOnClickListener(onClickListener);
		v.setOnClickListener(onClickListener);
		
		return builder.create();
	}
	
	@SuppressWarnings("deprecation")
	public void onBackPressed() {
		this.showDialog(END_GAME_DIALOG);
	}
	
	// GAMEMODE_DIALOG creation
	private Dialog createGamemodeDialog()
	{
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		LinearLayout root = (LinearLayout) getLayoutInflater().inflate(R.layout.game_type_prompt, null);
		
		OnClickListener ocl = new OnClickListener() {
			@SuppressWarnings("deprecation")
			@Override
			public void onClick(View v) {
				Intent game;
				game = new Intent();
				game.setAction("ru.spb.mobisoft.kry127.ttt.Game_Activity");
				game.putExtra("music", music);
				game.putExtra("vibro", vibro);
				game.putExtra("language", lang);
				game.putExtra("intent", svc);

				if (vibro)
					vibrator.vibrate(MID_VIBRATION);
				
				switch(v.getId()) {
				case R.id.player_vs_cpu:
					game.putExtra("CPU", true);
					break;
				case R.id.player_vs_player:
					game.putExtra("CPU", false);
					break;
				default:
					return;
				}
				
				Menu_Activity.this.dismissDialog(GAMEMODE_DIALOG);
				Menu_Activity.this.startActivityForResult(game, SETTINGS_REQUEST);
				
				StaticObjects.setPlayerMutex(true);
			}
		};
		
		View p_vs_c = (View) root.findViewById(R.id.player_vs_cpu);
		View p_vs_p = (View) root.findViewById(R.id.player_vs_player);
		p_vs_c.setOnClickListener(ocl);
		p_vs_p.setOnClickListener(ocl);
		
		builder.setView(root);
		
		return builder.create();
	}
	
	private AlertDialog endGameDialog() {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			View root = (View) getLayoutInflater().inflate(R.layout.end_game_dialog, null);
			builder.setView(root);
			builder.setOnCancelListener(new OnCancelListener() {

				@Override
				public void onCancel(DialogInterface arg0) {
					finish();
				}
				
			});
			return builder.create();
	}
	
	@SuppressWarnings("deprecation")
	@Override
	protected void onPrepareDialog(int id, Dialog dialog)
	{
		super.onPrepareDialog(id, dialog);
		
		switch (id) {
		case SETTINGS_DIALOG:
			Spinner s = (Spinner)dialog.findViewById(R.id.language_spinner);
			CheckBox m = (CheckBox)dialog.findViewById(R.id.sound);
			CheckBox v = (CheckBox)dialog.findViewById(R.id.vibro);
			
	
			m.setChecked(music);
			v.setChecked(vibro);
			s.setSelection(lang);
			break;
		}
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public boolean onKeyDown(int keycode, KeyEvent e) {
		switch(keycode) {
		case KeyEvent.KEYCODE_MENU:
			showDialog(SETTINGS_DIALOG);
			if (vibro)
				vibrator.vibrate(MID_VIBRATION);
			return true;
		}
		
		return super.onKeyDown(keycode, e);
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode == Activity.RESULT_OK) {
			switch (requestCode) {
			case SETTINGS_REQUEST:
				music = data.getBooleanExtra("music", true);
				vibro = data.getBooleanExtra("vibro", false);
				lang = data.getIntExtra("language", 0);
				break;
			}
		}
	}
	
	// функци€, добавл€юща€ €рлык домой
	private void addShortcut() {
		// интент, который будет выполн€тьс€ при клике
		Intent shortcutIntent = new Intent(this.getApplicationContext(), Menu_Activity.class);
		shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		shortcutIntent.setAction(Intent.ACTION_MAIN);
		
		// интент на сам процесс добавлени€ €рлыка (просим јндрюху сделать €рлычок)
		Intent addIntent = new Intent();
		addIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
		addIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, this.getResources().getString(R.string.app_name));
		addIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
				Intent.ShortcutIconResource.fromContext(getApplicationContext(), R.drawable.logo));
		
		addIntent.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
		this.getApplicationContext().sendBroadcast(addIntent);
	}
	
	public void finish(View v) {
		finish();
	}
	
	@SuppressWarnings("deprecation")
	public void close_quit_dialog(View v) {
		this.dismissDialog(END_GAME_DIALOG);
	}
	
	@SuppressWarnings("deprecation")
	public void startGame(View v)
	{
		if (vibro)
			vibrator.vibrate(MID_VIBRATION);
		showDialog(GAMEMODE_DIALOG);
	}
	
	@SuppressWarnings("deprecation")
	public void settings(View v)
	{
		if (vibro)
			vibrator.vibrate(MID_VIBRATION);
		showDialog(SETTINGS_DIALOG);
	}
	
	@SuppressWarnings("deprecation")
	public void closeSettings(View v)
	{
		dismissDialog(SETTINGS_DIALOG);
	}
	
	@SuppressWarnings("deprecation")
	public void quitGame(View v)
	{
		if (vibro)
			vibrator.vibrate(MID_VIBRATION);
		showDialog(END_GAME_DIALOG);
	}
	
	public void moreGames(View v)
	{
		if (vibro)
			vibrator.vibrate(MID_VIBRATION);
		String uristr = this.getResources().getString(R.string.more_games_uri);
		Intent developerLink = new Intent(Intent.ACTION_VIEW, Uri.parse(uristr));
		startActivity(developerLink);
	}
	
}
