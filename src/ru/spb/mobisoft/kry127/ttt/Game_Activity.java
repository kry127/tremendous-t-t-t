package ru.spb.mobisoft.kry127.ttt;

import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import ru.spb.mobisoft.kry127.ttt.R;

public class Game_Activity extends Activity implements OnTouchListener {
	
	// final static parameters 
	private final static int SHORT_VIBRATION = 50;
	private final static int MID_VIBRATION = 250;
	private final static int LONG_VIBRATION = 500;
	private final static int SETTINGS_DIALOG = 0;
	private final static int HELP_DIALOG = 1;
	private final static int FIRST_MOVE_WAITING = 3000;
	private final static int MOVE_WAITING = 50;
	public final static int FRAME_QUANTITY = 8;
	public final static int STRIKE_FRAME_QUANTITY = 12;
	private final static int FRAME_DURATION_MILLISECONDS = 40;
	
	private final static float HEADER_STRETCH = (float)0.06416666;
	
	private final static String SHARED_PREFERENCE_FILE = "com.example.ru.kry127.SHARED_PREFERENCE_FILE";
	private final static String MUSIC_KEY = "MUSIC";
	private final static String VIBRO_KEY = "VIBRO";
	private final static String LANGUAGE_KEY = "LANG";
	
	//0 is null; 1 is X; 2 is y;
	// TODO make some of them static, like Game_Field -- we need only one instance of it
	private byte[] a;
	private byte cpu_turn;
	private int time;
	private int X, Y;
	private int lang;
	private int lastRow;	//1, 2, 3 -- rows; 4, 5, 6 -- cols; 7 -- main, 8 -- sub diagonals
	private byte turn;
	private boolean cpu;
	private boolean music;
	private boolean vibro;
	private boolean animation;
	private boolean toastShowed;
	private Game_Field gf;
	private Vibrator vibrator;
	private Random rand;
	private Intent svc;
	private Toast toast;
	private SharedPreferences sharedPreferences;
	private SharedPreferences.Editor editor;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		// убираем заголовок
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.gamefield);
		
		vibrator = (Vibrator)this.getSystemService(Context.VIBRATOR_SERVICE);
		rand = new Random();
		animation = false;
		toastShowed = false;
		
		Intent intent = this.getIntent();
		cpu = intent.getBooleanExtra("CPU", true);
		cpu_turn = 2;
		// TODO верни значения обратно!
		vibro = intent.getBooleanExtra("vibro", true);
		music = intent.getBooleanExtra("music", false);
		lang = intent.getIntExtra("language", 0);
		svc = intent.getParcelableExtra("intent");
		
		sharedPreferences = this.getSharedPreferences(SHARED_PREFERENCE_FILE, Context.MODE_PRIVATE);
		editor = sharedPreferences.edit();
		
		// создаём рисованное поле крестиков-ноликов
		LinearLayout r = (LinearLayout)findViewById(R.id.cont);
		if (gf == null)	
			gf = new Game_Field(this);
		gf.setOnTouchListener(this);
		r.addView(gf, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		
		// настраиваем высоту строчек "игрок 1" и "игрок 2" на 10% от экрана
		LinearLayout player_x = (LinearLayout)findViewById(R.id.player_x);
		LinearLayout player_o = (LinearLayout)findViewById(R.id.player_o);
		LayoutParams lp = player_x.getLayoutParams();
		int h = this.getWallpaper().getIntrinsicHeight();
		lp.height = (int) Math.floor(h * HEADER_STRETCH);
		player_x.setLayoutParams(lp);
		player_o.setLayoutParams(lp);
		
		// непосредственно игровые параметры
		a = new byte[9];
		for (int i = 0; i<9; i++) {
			a[i] = 0;
		}
		turn = 1;
		Y = X = 0;
		lastRow = 0;
	}
	
	/*
	 * События данного активити
	 */
	
	@SuppressWarnings("deprecation")
	@Override
	public boolean onKeyDown(int keycode, KeyEvent e) {
		switch(keycode) {
		case KeyEvent.KEYCODE_MENU:
			if (vibro)
				vibrator.vibrate(MID_VIBRATION);
			showDialog(SETTINGS_DIALOG);
			return true;
		}
		
		return super.onKeyDown(keycode, e);
	}
	
	@Override
	protected void onResume()
	{
		super.onResume();
		if(!StaticObjects.getPlayerMutex() && music)
			startService(svc);
		StaticObjects.setPlayerMutex(false);
	}

	@Override
	protected void onPause()
	{
		super.onPause();
		if(this.isFinishing()) // ооо, нет, я погибаю
			StaticObjects.setPlayerMutex(true);
		else // а, не) ошибочка
			stopService(svc);
	}


	/*
	 * Блок функций, затрагивающий функционал диалогов
	 */
	@Override
	protected Dialog onCreateDialog(int id)
	{
		switch (id) {
		case SETTINGS_DIALOG:
			return createSettingsDialog();
		case HELP_DIALOG:
			return createHelpDialog();
		default:
			return null;
		}
	}
	
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
				saveResult();
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
					if (music) {
						startService(svc);
					} else {
						stopService(svc);
					}
					break;
				case R.id.vibro:
					vibro = ((CheckBox)v).isChecked();
					if (vibro) {
						Vibrator vibrator = (Vibrator)Game_Activity.this
								.getSystemService(Context.VIBRATOR_SERVICE);
						vibrator.vibrate(MID_VIBRATION);
					}
					break;
				default:
					break;
				}
				saveResult();
			}
		};
		
		m.setOnClickListener(onClickListener);
		v.setOnClickListener(onClickListener);
		
		return builder.create();
	}
	
	private Dialog createHelpDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		
		View root = (View) getLayoutInflater().inflate(R.layout.help, null);
		
		final ImageView iv = (ImageView)root.findViewById(R.id.help_bg_mini);
		final ScrollView sv = (ScrollView)root.findViewById(R.id.help_scroll_view);
		final TextView tv = (TextView)root.findViewById(R.id.help_text_view);
		
		ViewTreeObserver obs = tv.getViewTreeObserver();
		obs.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
			@Override
			public boolean onPreDraw() {
				LayoutParams lp = iv.getLayoutParams();
				lp.height = sv.getHeight();
				iv.setLayoutParams(lp);
				return true;
			}
		});
		
		builder.setView(root);
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
		case HELP_DIALOG:
			break;
		default:
			break;
		}
	}
	
	protected void saveResult()
	{
		editor.putBoolean(MUSIC_KEY, music);
		editor.putBoolean(VIBRO_KEY, vibro);
		editor.putInt(LANGUAGE_KEY, lang);
		editor.commit();
		
		Intent intent = new Intent();
		intent.putExtra("music", music);
		intent.putExtra("vibro", vibro);
		intent.putExtra("language", lang);
		setResult(RESULT_OK, intent);
	}
	
	// функция, определяющая, что сейчас выигрышное положение
	// записывает в переменную lastRow строку, которая "выиграла"
	private int checkWinner()
	{
		for (int i = 0; i<3; i++) {
			if ((a[i*3] == a[i*3 + 1]) && (a[i*3] == a[i*3 + 2])) {
				if (a[i*3] != 0) {
					lastRow = i + 1;
					return a[i*3];
				}
			}
			if ((a[i] == a[i + 3]) && (a[i] == a[i + 6])) {
				if (a[i] != 0) {
					lastRow = i + 4;
					return a[i];
				}
			}
		}
		if ((a[0] == a[4]) && (a[0] == a[8])) {
			if (a[0] != 0) {
				lastRow = 7;
				return a[0];
			}
		}
		if ((a[2] == a[4]) && (a[2] == a[6])) {
			if (a[2] != 0) {
				lastRow = 8;
				return a[2];
			}
		}

		lastRow = 0;
		return 0;
	}
	
	private boolean checkFull() {
		for (int i = 0; i<9; i++) {
			if (a[i] == 0)
				return false;
		}
		return true;
	}
	
	// оцениваем свободные ячейки и выбираем самую выгодную v1.1
	private int getMostProfitMove() {
		// в первый ход просто тыкаем в рандомную клетку, ведь так веселее!
		boolean allEmpty = true;
		for (int i = 0; i < 9; i++)
			if (a[i] != 0)
				allEmpty = false;
		if (allEmpty) {
			return rand.nextInt(9);
		}
		
		int rand = -1;
		int[] maxParams = new int[] {0, 0, 0, 0, 0}; // {два своих, два чужих, один свой, один чужой, пусто}
		for (int i = 0; i < 9; i++) {
			if (a[i] == 0) {
				// получаем индексы рядов, которые содержат данную ячейку
				int[] rows = getRowBunch(i);
				// параметры для текущей ячейки
				int[] params = new int[] {0, 0, 0, 0, 0};
				// будем вычислять параметры, которые запихнём в массив
				for (int j = 0; j < rows.length; j++) {
					int value = getRowValue(getRow(rows[j]));
					switch (value) {
					case -2:
						params[1]++;
						break;
					case -1:
						params[3]++;
						break;
					case 0:
						params[4]++;
						break;
					case 1:
						params[2]++;
						break;
					case 2:
						params[0]++;
						break;
					}
				}
				// сравниваем с первоначальными параметрами, и если больше, то запоминаем текущую ячейку
				if (compareIntArrays(params, maxParams) >= 0) {
					maxParams = params.clone();
					rand = i;
				}
			}
		}
		return rand;
	}
	

	private static int[] getRow(int i) {
		switch (i) {
		case 0:
			return new int[] {0, 1, 2};
		case 1:
			return new int[] {3, 4, 5};
		case 2:
			return new int[] {6, 7, 8};
		case 3:
			return new int[] {0, 3, 6};
		case 4:
			return new int[] {1, 4, 7};
		case 5:
			return new int[] {2, 5, 8};
		case 6:
			return new int[] {0, 4, 8};
		case 7:
			return new int[] {2, 4, 6};
		default:
			return null;
		}
	}
	
	private static int[] getRowBunch(int i) {
		switch (i) {
		case 0:
			return new int[] {0, 3, 6};
		case 1:
			return new int[] {0, 4};
		case 2:
			return new int[] {0, 5, 7};
		case 3:
			return new int[] {1, 3};
		case 4:
			return new int[] {1, 4, 6, 7};
		case 5:
			return new int[] {1, 5};
		case 6:
			return new int[] {2, 3, 7};
		case 7:
			return new int[] {2, 4};
		case 8:
			return new int[] {2, 5, 6};
		default:
			return null;
		}
	}
	
	private int getRowValue(int[] row) {
		int sum = 0;
		for (int i = 0; i < row.length; i++) {
			if (a[row[i]] == cpu_turn)
				sum++;
			else if (a[row[i]] != 0)
				sum--;
		}
		if (sum == 0) {
			for (int i = 0; i < row.length; i++)
				if (a[row[i]] != 0)
					return 100; // нереальное значение
		}
		return sum;
	}
	
	/**
	 * Returns something like a1 - a2
	 * @param a1
	 * @param a2
	 * @return
	 */
	private static int compareIntArrays(int[] a1, int[] a2) {
		int min = (a1.length < a2.length) ? a1.length : a2.length;
		for (int i = 0; i < min; i++) {
			if (a1[i] != a2[i])
				return a1[i] - a2[i];
		}
		return a1.length - a2.length;
	}
	
	// проверяет выигрышную ситуацию. Обработка жёстко вшивается сюда.
	// возвращает true, если можно продолжать ставить элементы
	// когда ничья всё равно возвращаем true -- комьютер сделает свой ход с задержкой
	// когда кто-либо выиграл, то возвращаем false, затем сами обработаем всё после конца анимации
	// зачёркивания
	private boolean check() {
		final int w;
		
		w = checkWinner();
		// если кто-то победил, то сначала надо зачеркнуть ряд
		if (w != 0) {

			// задаём параметры анимации (текущей ячейки нет, передаём lastRow)
			gf.setAnimationParams(-1, turn);
			gf.setStrokeType(lastRow);
			AnimationTimer at = new AnimationTimer(STRIKE_FRAME_QUANTITY, new OnEndAnimationListener() {
				@Override
				public void onEndAnimation() {
					Game_Activity.this.runOnUiThread(new Runnable() {
						@Override
						public void run() {
							Game_Activity.this.onEndStrikeAnimation(w);
						}
						
					});
				}
				
			});
			at.setAnimationRunnable(new Runnable() {
				@Override
				public void run() {
					animateRowStrike();
				}
				
			});
			Timer timer = new Timer();
			timer.schedule(at, 0, FRAME_DURATION_MILLISECONDS);
			return false;
		}
		
		// никто не победил -- ну и ладно. Показываем сообщение, очищаем поле
		if(checkFull()) {
			this.showMessage(0);
			
			if (vibro)
				vibrator.vibrate(LONG_VIBRATION);
			
			reset();
			gf.requestLayout();
		}
		return true;
	}
	
	public void reset() {
		a = new byte[9];
		for (int i = 0; i<9; i++) {
			a[i] = 0;
		}
		gf.setArray(a);
	}

	// если прикоснулись к игровому полю
	@Override
	public boolean onTouch(View arg0, MotionEvent arg1) {
		if (arg1.getActionMasked() == MotionEvent.ACTION_UP) {
			// если на данный момент нет никакой анимации
			if (animation)
				return false;	// обработано (но никак)
			gf.set_X(arg1.getX());
			gf.set_Y(arg1.getY());
			
			int cellNumber = gf.getCellNumber();
			if ((cellNumber >= 0) && (cellNumber < 9) && (a[cellNumber] == 0)) {
				checkCell(cellNumber);
				
				if (vibro)
					vibrator.vibrate(SHORT_VIBRATION);
			}
			return false;	//обработано
		}
		// если "тыкнули"
		if (arg1.getActionMasked() == MotionEvent.ACTION_DOWN) {
			if (toast != null)
				toast.cancel();
		}
		return true;	// не обработано
	}
	
	// отмечает ячейку за игроком, запускает анимацию и блокирует клики по полю
	private void checkCell(int cellNumber) {
		animation = true;
		a[cellNumber] = turn;
		gf.setArrayElementValue(turn, cellNumber);
		
		gf.setAnimationParams(cellNumber, turn);
		
		// создаём таймер анимации
		// первый интерфейс для вызова события конца анимации
		// второй метод -- элементарный метод анимации (то, что вызывается каждый кадр)
		AnimationTimer at = new AnimationTimer(FRAME_QUANTITY, new OnEndAnimationListener() {
			@Override
			public void onEndAnimation() {
				Game_Activity.this.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						Game_Activity.this.onEndElementAnimation();
					}
					
				});
			}
			
		});
		at.setAnimationRunnable(new Runnable() {
			@Override
			public void run() {
				animateCell();
			}
			
		});
		Timer timer = new Timer();
		timer.schedule(at, 0, FRAME_DURATION_MILLISECONDS);
		
		turn %= 2;
		turn += 1;
	}
	
	// событие, вызываемое при окончании анимации прорисовки элемента
	// к сожалению бесполезно, так как вызывается из потока...
	protected void onEndElementAnimation() {
		// проверка
		if (check())
			CPUTurnOrEndAnim();
	}
	
	// вызывается, когда кончается анимация зачёркивания
	protected void onEndStrikeAnimation(int w) {
		this.showMessage(w);
		
		if (vibro)
			vibrator.vibrate(LONG_VIBRATION);
		
		reset();
		//gf.postInvalidate();
		// этот код добавлен после того, как не сработало на планшете
		gf.requestLayout();
		CPUTurnOrEndAnim();
	}

	public void CPUTurnOrEndAnim() {
		// если CPU в теме и начинается его ход, то...
		if (cpu && (turn == cpu_turn)) {
			int moveWaiting = (toastShowed) ? FIRST_MOVE_WAITING : MOVE_WAITING;
			toastShowed = false; // сбрасываем, ведь задержка требуется только один раз.
			Handler hnd = new Handler();
			hnd.postDelayed(new Runnable () {
				@Override
				public void run() {
					int index = getMostProfitMove();
					while (index == -1 || a[index] != 0)
						index = rand.nextInt(9);
					checkCell(index);
				}
				
			}, moveWaiting);
		// иначе все анимации кончены
		} else {
			// полагаю, это исправит его "тормоз" после клика юзера после показа окна)
			toastShowed = false;
			//gf.refreshField();
			gf.postInvalidate();
			animation = false;
		}
	}
	
	public void animateCell() {
		gf.animateCell(time);
	}	
	
	public void animateRowStrike() {
		gf.animateRowStrike(time, lastRow);
	}
	
	public void showMessage(int id) {
		TextView txt;
		toast = new Toast(getApplicationContext());
		toast.setGravity(Gravity.CENTER, 0, 0);
		toast.setDuration(Toast.LENGTH_SHORT);
		View bg = this.getLayoutInflater().inflate(R.layout.winner_message, null);
		ImageView iv = (ImageView)bg.findViewById(R.id.ivWinner);
		toastShowed = true;		// сообщение Toast показано
		switch(id) {
		case 0:
			LayoutParams lp = iv.getLayoutParams();
			lp.width = 0;
			iv = (ImageView)bg.findViewById(R.id.ivWin);
			iv.setImageDrawable(this.getResources().getDrawable(R.drawable.draw));
			
			toast.setView(bg);
			toast.show();
			break;
		case 1:
			X++;
			txt = (TextView) findViewById(R.id.X);
			txt.setText(Integer.toString(X));

			toast.setView(bg);
			toast.show();
			break;
		case 2:
			Y++;
			txt = (TextView) findViewById(R.id.Y);
			txt.setText(Integer.toString(Y));
			
			iv.setImageDrawable(this.getResources().getDrawable(R.drawable.winner_o));
			toast.setView(bg);
			toast.show();
			break;
		default:
			toastShowed = false;	// ошибочка -- нет такого сообщения тост
			break;
		}
	}
	
	public interface OnEndAnimationListener {
		public void onEndAnimation();
	}
	
	public class AnimationTimer extends TimerTask {

		private int duration;
		private OnEndAnimationListener listener;
		private Runnable animationRunnable;
		
		AnimationTimer(int duration, OnEndAnimationListener listener) {
			this.duration = duration;
			Game_Activity.this.time = -3;
			this.listener = listener;
		}
		
		@Override
		public void run() {
			if (++Game_Activity.this.time > duration) {
				this.cancel();
				if (listener != null)
					listener.onEndAnimation();
			} else {
				if (animationRunnable != null)
					animationRunnable.run();
			}
		}
		
		public void setAnimationRunnable(Runnable r) {
			this.animationRunnable = r;
		}
		
	}
	
	// завершает игру и закрывает активити
	public void endGame(View v) {
		this.finish();
	}
	
	@SuppressWarnings("deprecation")
	public void closeSettings(View v) {
		dismissDialog(SETTINGS_DIALOG);
	}
	
	@SuppressWarnings("deprecation")
	public void helpRequested(View v) {
		showDialog(HELP_DIALOG);
	}
}
