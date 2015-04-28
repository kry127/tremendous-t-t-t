package ru.spb.mobisoft.kry127.ttt;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.view.Display;
import android.view.WindowManager;

import ru.spb.mobisoft.kry127.ttt.R;

public class StaticObjects {
	// изображения представляют из себя статические поля
	// так как их повторная загрузка нагружает лишними операциями CPU.
	// Засчёт этого также уменьшается вероятность вылета игры.
	private static Bitmap field;		// хранит изображение поля
	private static Bitmap xImg;		// хранит изображение X
	private static Bitmap oImg;		// хранит изображение O
	private static Bitmap xAnim;	// хранит анимацию X
	private static Bitmap oAnim;	// хранит анимацию O
	// 29.07.2014 добавляем рисунки зачёркивания
	private static Bitmap blueMainDiagonal;
	private static Bitmap redMainDiagonal;
	private static Bitmap blueSubDiagonal;
	private static Bitmap redSubDiagonal;
	private static Bitmap blueHorizontal;
	private static Bitmap redHorizontal;
	private static Bitmap blueVertical;
	private static Bitmap redVertical;

	//несколько финальных статических параметров
	final private static int X_ANIM_COUNT = 8;
	final private static int O_ANIM_COUNT = 8;
	
	private static boolean playerMutex;
	
	public int screen_height;
	public int screen_width;
	public int screen_min;

	@SuppressWarnings("deprecation")
	@SuppressLint("NewApi")
	StaticObjects(Context ctx) {
		
		playerMutex = false;

	    Resources res = ctx.getResources();
	    
	    // получаем размер монитора
	    WindowManager wm = (WindowManager) ctx.getSystemService(Context.WINDOW_SERVICE);
	    Display display = wm.getDefaultDisplay();
	    // checking SDK version
	    if (android.os.Build.VERSION.SDK_INT >= 13) {
		    Point size = new Point();
	    	display.getSize(size);
	    	screen_height = size.y;
	    	screen_width = size.x;
	    } else {
	    	screen_height = display.getHeight();
	    	screen_width = display.getWidth();
	    }
	    screen_min = (screen_height > screen_width) ? screen_width : screen_height;
	    
	    if (xImg == null)
	    	xImg = decodeSampledBitmapFromResource(res, R.drawable.x,
	    			screen_min / 3, screen_min / 3);
	    if (oImg == null)
	    	oImg = decodeSampledBitmapFromResource(res, R.drawable.o,
	    			screen_min / 3, screen_min / 3);
	    if (field == null)
	    	field = decodeSampledBitmapFromResource(res, R.drawable.field,
    			screen_min, screen_min);
	    if (xAnim == null)
	    	xAnim = decodeSampledBitmapFromResource(res, R.drawable.x_diafilm,
	    		screen_min * X_ANIM_COUNT / 3, screen_min / 3);
	    if (oAnim == null)
	    	oAnim = decodeSampledBitmapFromResource(res, R.drawable.o_diafilm,
		    		screen_min * O_ANIM_COUNT / 3, screen_min / 3);
	    if (blueMainDiagonal == null)
	    	blueMainDiagonal = decodeSampledBitmapFromResource(res, R.drawable.diagonal_straight_blue,
		    		screen_min, screen_min);
	    if (redMainDiagonal == null)
	    	redMainDiagonal = decodeSampledBitmapFromResource(res, R.drawable.diagonal_straight_red,
	    		    		screen_min, screen_min);
	    if (blueSubDiagonal == null)
	    	blueSubDiagonal = decodeSampledBitmapFromResource(res, R.drawable.diagonal_reversed_blue,
	    		    		screen_min, screen_min);
	    if (redSubDiagonal == null)
	    	redSubDiagonal = decodeSampledBitmapFromResource(res, R.drawable.diagonal_reversed_red,
		    		screen_min, screen_min);
	    if (blueHorizontal == null)
	    	blueHorizontal = decodeSampledBitmapFromResource(res, R.drawable.horizontal_blue,
		    		screen_min, screen_min / 12);
	    if (redHorizontal == null)
	    	redHorizontal = decodeSampledBitmapFromResource(res, R.drawable.horizontal_red,
		    		screen_min, screen_min /12);
	    if (blueVertical == null)
	    	blueVertical = decodeSampledBitmapFromResource(res, R.drawable.verticale_blue,
		    		screen_min / 12, screen_min);
	    if (redVertical == null)
	    	redVertical = decodeSampledBitmapFromResource(res, R.drawable.verticale_red,
		    		screen_min / 12, screen_min);
	}
	
	// image getters
	public static Bitmap getField() {
		return field;
	}
	public static Bitmap getxImg() {
		return xImg;
	}
	public static Bitmap getoImg() {
		return oImg;
	}
	public static Bitmap getxAnim() {
		return xAnim;
	}
	public static Bitmap getoAnim() {
		return oAnim;
	}
	public static Bitmap getBlueMainDiagonal() {
		return blueMainDiagonal;
	}
	public static Bitmap getRedMainDiagonal() {
		return redMainDiagonal;
	}
	public static Bitmap getBlueSubDiagonal() {
		return blueSubDiagonal;
	}
	public static Bitmap getRedSubDiagonal() {
		return redSubDiagonal;
	}
	public static Bitmap getBlueHorizontal() {
		return blueHorizontal;
	}
	public static Bitmap getRedHorizontal() {
		return redHorizontal;
	}
	public static Bitmap getBlueVertical() {
		return blueVertical;
	}
	public static Bitmap getRedVertical() {
		return redVertical;
	}
	
	// mutex getter-setter for player service
	public static boolean getPlayerMutex()
	{
		return playerMutex;
	}
	public static void setPlayerMutex(boolean isOccupied)
	{
		playerMutex = isOccupied;
	}
	
	// final static parameter getters
	public static int getXAnimCount() {
		return X_ANIM_COUNT;
	}
	
	public static int getOAnimCount() {
		return O_ANIM_COUNT;
	}
	
	// метод загружает в оперативку битмап нужного размера
	public static Bitmap decodeSampledBitmapFromResource(Resources res, int resId,
			int reqWidth, int reqHeight) {
		
		// getting dimensions
		final BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeResource(res, resId, options);
		
		// calculate inSampleSize
		options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
		
		// decode bitmap with inSampleSize set
		// на самом деле просто в n раз уменьшается изображение))
		options.inJustDecodeBounds = false;
		return BitmapFactory.decodeResource(res, resId, options);
	}
	
	public static int calculateInSampleSize(BitmapFactory.Options options,
			int reqWidth, int reqHeight) {
		final int height = options.outHeight;
		final int width = options.outWidth;
		int inSampleSize = 1;
		
		if (height > reqHeight || width > reqWidth) {
			final int halfHeight = height / 2;
			final int halfWidth = width / 2;
			
			while ((halfHeight / inSampleSize) > reqHeight && 
					(halfWidth / inSampleSize) > reqWidth) {
				inSampleSize++;
			}
		}
		return inSampleSize;
	}
}
