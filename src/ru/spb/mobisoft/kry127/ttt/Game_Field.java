package ru.spb.mobisoft.kry127.ttt;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.view.View;

public class Game_Field extends View {

	// это коэффициенты, описывающие растяжение клеток полей
	// слева-направо и СНИЗУ-ВВЕРХ!
	// эти магические числа -- следствие дизайна
	final private double[] STRETCH_COEFFICIENTS = {428.0/1244, 382.0/1244, 434.0/1244};
	// те же коэффициенты, выдающие смещение линий
	final private double[] INITIAL_COEFFICIENTS = {0, 428.0/1244, 810.0/1244, 1};
	
	final static double CELL_CONSTRAINT = 0.10;
	
	private Rect[] cells;		// массив прямоуголников описывает клеточки поля
	private byte[] a;			// поле, отображающие что находится в ячейках (ничего - 0, 1 - X, 2 = O)
	private Bitmap field;		// хранит изображение поля
	private Bitmap xImg;		// хранит изображение X
	private Bitmap oImg;		// хранит изображение O
	private Bitmap xAnim;		// хранит анимацию X
	private Bitmap oAnim;		// хранит анимацию O
	private Bitmap strikeImg;	// хранит ссылку на одну из картинок зачёркивания
	private Bitmap cacheBmp;	// буферный точечный рисунок
	private Canvas cacheCnv;	// полотно для рисования в буферном изображении (см. выше)
	private Paint paint;		// буферная переменная для стилей прорисовки
	private Rect mainRect;		// главный прямоугольник. Только в нём будет рисование
	private Rect rSrc;			// хранит прямоугольник, равный размеру Bitmap field
	private Rect rDesc;			// хранит прямоугольник, в который рисуется field
	private float x;			// абсцисса клика
	private float y;			// ордината клика
	private int height;			// высота области рисования
	private int width;			// ширина области рисования
	private int side;			// длинна квадрата игрового поля
	private int padding;		// отбивка. side + padding = max(height, width) является тождеством
	private int XOside;			// размер помещаемых элементов-картинок X и O
	private int lastRow;		// последняя известная линия перечёркнутых элементов
	
	private int invalidateType;	// тип перерисовки View
	
	private int animationTime;	// анимационное время
	private int animTurn;		// тип элемента в анимации ( X или O)
	private int animIndex;		// индекс анимированной ячейки
	private int animDuration;	// время анимации в кадрах

	public Game_Field(Context context) {
		super(context);
		
		invalidateType = -1;
		animDuration = Game_Activity.FRAME_QUANTITY;
		
		paint = new Paint();
		cells = new Rect[9];
		a = new byte[9];

	    rSrc = new Rect();
	    rDesc = new Rect();
	    paint.setStyle(Paint.Style.FILL);
		paint.setAntiAlias(true);
	    
	    field = StaticObjects.getField(); 
	    xImg = StaticObjects.getxImg();
	    oImg = StaticObjects.getoImg();
		xAnim = StaticObjects.getxAnim();
		oAnim = StaticObjects.getoAnim();
	}
	
	// аллокация нового Bitmap необходима для перерисовки поля заново
	@SuppressLint("DrawAllocation")
	@Override
	// вызывается после того, как объект этого класса поместился в раскладку
	protected void onLayout(boolean changed, int left, int top, int right, int bottom)
	{
	    super.onLayout(changed, left, top, right, bottom);
	    acquireDimension();
	    fillCellsArray();
	    
	    cacheBmp = Bitmap.createBitmap(side + padding, side + padding, Config.ARGB_4444);
	    cacheCnv = new Canvas(cacheBmp);
	}
	
	public void animateCell(int time) {
		this.invalidateType = 1;
		this.animationTime = time;
		this.postInvalidate();
	}
	
	public void animateRowStrike(int time, int lastRow) {
		this.invalidateType = 2;
		this.animationTime = time;
		this.lastRow = lastRow;
		this.postInvalidate();
	}
	
	public void setAnimationParams(int index, int turn) {
		this.animIndex = index;		// для анимации прорисовки элемента
		this.animTurn = turn;		// текущий ход. Для обеих анимаций
	}
	
	public void setStrokeType(int lastRow) {
		this.lastRow = lastRow;
		switch (lastRow) {
		case 1:
		case 2:
		case 3:
			this.strikeImg = (animTurn %2 == 1) ? StaticObjects.getBlueHorizontal() :
				StaticObjects.getRedHorizontal();
			break;
		case 4:
		case 5:
		case 6:
			this.strikeImg = (animTurn %2 == 1) ? StaticObjects.getBlueVertical() :
				StaticObjects.getRedVertical();
			break;
		case 7:
			this.strikeImg = (animTurn %2 == 1) ? StaticObjects.getBlueMainDiagonal() :
				StaticObjects.getRedMainDiagonal();
			break;
		case 8:
			this.strikeImg = (animTurn %2 == 1) ? StaticObjects.getBlueSubDiagonal() :
				StaticObjects.getRedSubDiagonal();
			break;
		default:
			this.strikeImg = this.oImg;
			break;
		}
	}
	
	@Override
	protected void onDraw(Canvas canvas){
	    super.onDraw(canvas);
	    switch (invalidateType) {
	    case 1:
	    	animationRedraw(cacheCnv);
	    	break;
	    case 2:
	    	animationStrike(cacheCnv);
	    	break;
	    default:
	    	defaultRedraw(cacheCnv);
	    	break;
	    }
	    canvas.drawBitmap(cacheBmp, 0, 0, paint);
	    invalidateType = -1;
	}
	
	// стандартная прорисовка. Можно вызвать простым invalidate
	private void defaultRedraw(Canvas canvas) {
	    // рисуем поле в виде квадрата
	    paint.setColor(0xFFFFFFFF);
	    rSrc.set(0, 0, field.getWidth(), field.getHeight());
	    rDesc.set((width - side)/2, (height - side)/2, (width + side)/2, (height + side)/2);
	    canvas.drawBitmap(field, rSrc, rDesc, paint);
		
	    // прорисовываем текущий результат игры в виде крестиков-ноликов
	    for (int i = 0; i < 3; i++)
	    	for (int j = 0; j < 3; j++) {
	    		if (invalidateType == -1 || 3 * i + j != this.animIndex) {
		    		rDesc = getXORectAt(3 * i + j);
		    		switch (a[i * 3 + j]) {
		    		case 1:
		    			rSrc.set(0, 0, xImg.getWidth(), xImg.getHeight());
		    			canvas.drawBitmap(xImg, rSrc, rDesc, paint);
		    			break;
		    		case 2:
		    			rSrc.set(0, 0, oImg.getWidth(), oImg.getHeight());
		    			canvas.drawBitmap(oImg, rSrc, rDesc, paint);
		    			break;
		    		default:
		    			break;
		    		}
	    		}
	    	}
		
	}
	
	// покадровая прорисовка анимации выставления элемента
	private void animationRedraw(Canvas canvas) {
		// FIXME reduce code lines here!
		// если X
		if (this.animTurn == 1) {
			int count = StaticObjects.getXAnimCount();
			int w = xAnim.getWidth() / count;
			int progress = (int)Math.floor((double)this.animationTime * (count - 1) / this.animDuration);
			rSrc.set(w * progress, 0, w * (progress + 1), xAnim.getHeight());
			if (progress < count && progress >= 0)
				canvas.drawBitmap(xAnim, rSrc, this.getXORectAt(this.animIndex), paint);
		// если O
		} else if (this.animTurn == 2) {
			int count = StaticObjects.getOAnimCount();
			int w = oAnim.getWidth() / count;
			int progress = (int)Math.floor((double)this.animationTime * (count - 1) / this.animDuration);
			rSrc.set(w * progress, 0, w * (progress + 1), oAnim.getHeight());
			if (progress < count && progress >= 0)
				canvas.drawBitmap(oAnim, rSrc, this.getXORectAt(this.animIndex), paint);
		}
	}
	
	// анимация зачёркивания ряда
	private void animationStrike(Canvas canvas) {
		// TODO реализуй метод ГРАМОТНО!
		paint.setColor(0xFFFFFFFF);
		Rect src = new Rect();
		Rect desc = new Rect();
		Rect r1;
		Rect r2;
		src = new Rect(0, 0, strikeImg.getWidth(), strikeImg.getHeight());
		// для диагоналей и горизонтальных линий
		if((lastRow >= 1 && lastRow <= 3) || lastRow == 7 || lastRow == 8) {
			switch (lastRow) {
			case 1:
			case 2:
			case 3:
				r1 = new Rect(getXORectAt((lastRow - 1) * 3));
				r2 = new Rect(getXORectAt(lastRow * 3 - 1));
				desc = new Rect(r1.left, r1.top, r2.right, r2.bottom);
				desc.inset(0, desc.height() * 3 / 8);
				break;
			case 7:
			case 8:
				desc = new Rect(this.getMainRect());
				break;
			}
			src.right = src.left +
					(int)Math.floor(src.width() * animationTime /
							(float)Game_Activity.STRIKE_FRAME_QUANTITY);
			desc.right = desc.left +
					(int)Math.floor(desc.width() * animationTime /
							(float)Game_Activity.STRIKE_FRAME_QUANTITY);
			canvas.drawBitmap(strikeImg, src, desc, paint);
		// если вертикально
		} else if (lastRow >= 4 && lastRow <= 6) {
			r1 = new Rect(getXORectAt(lastRow - 4));
			r2 = new Rect(getXORectAt(lastRow + 2));
			desc = new Rect(r1.left, r1.top, r2.right, r2.bottom);
			desc.inset(desc.width() * 3 / 8, 0);
			
			src.bottom = src.top +
					(int)Math.floor(src.height() * animationTime /
							(float)Game_Activity.STRIKE_FRAME_QUANTITY);
			desc.bottom = desc.top +
					(int)Math.floor(desc.height() * animationTime /
							(float)Game_Activity.STRIKE_FRAME_QUANTITY);
			canvas.drawBitmap(strikeImg, src, desc, paint);
		}
		

//		точно рабочий вариант
//		canvas.drawText("АНИМАЦИЯ ЗАЧЁРКИВАНИЯ! " + this.animationTime + " frame." ,
//		30, 30 * this.animationTime, paint);*/
//		Rect src = new Rect(0, 0, strikeImg.getWidth(), strikeImg.getHeight());
//		Rect desc = new Rect(this.getMainRect());
//		src.right = src.left +
//				(int)Math.floor(src.width() * animationTime /
//						(float)Game_Activity.STRIKE_FRAME_QUANTITY);
//		desc.right = desc.left +
//				(int)Math.floor(desc.width() * animationTime /
//						(float)Game_Activity.STRIKE_FRAME_QUANTITY);
//		canvas.drawBitmap(strikeImg, src, desc, paint);
		
	}
	
	private void acquireDimension() {
		height = this.getHeight();
		width = this.getWidth();
		// размер квадрата
		side = (height > width) ? width : height;
		// отбивка (св. пространство от края поля
		padding = (int) Math.floor(side * 0.24);
		// пересчитываем сторону
		side -= padding;
		// чтобы X и O по своим размерам смогли вместиться во все клетки
		XOside = (int)(side * STRETCH_COEFFICIENTS[1]);
		// определяем главный прямоуголььник
		mainRect = new Rect();
	    mainRect.set((width - side)/2, (height - side)/2, (width + side)/2, (height + side)/2);
	}
	
	// i=0..2; j=0..2
	// возвращает прямоугольник по коорд. поля
	private Rect getRectAtCoord(int i, int j)
	{
		Point P = new Point((width - side)/2, (height + side)/2);
		P.x += (int)(this.INITIAL_COEFFICIENTS[j] * side);
		P.y -= (int)(this.INITIAL_COEFFICIENTS[3-i] * side);
		
		Point Q = new Point(P.x, P.y);
		Q.x +=(int)(this.STRETCH_COEFFICIENTS[j] * side);
		Q.y +=(int)(this.STRETCH_COEFFICIENTS[2-i] * side);
		
		Rect ret = new Rect();
		ret.set(P.x, P.y, Q.x, Q.y);
		return ret;
	}
	
	// заполняет массив ячеек соответствующими прямоугольниками на канвасе
	private void fillCellsArray()
	{
		for (int i = 0; i < 3; i++)
			for (int j = 0; j < 3; j++) {
				cells[3*i + j] = getRectAtCoord(i, j);
			}
	}
	
	private Rect getXORectAt(int id)
	{
		Rect r = cells[id];
		Rect destination = new Rect();
		int XCent = r.centerX();
		int YCent = r.centerY();
		destination.set(XCent - XOside/2, YCent - XOside/2, XCent + XOside/2, YCent + XOside/2);
		return destination;
		
	}
	
	// тестовая функция прорисовывает поля и показывает, что есть прямоугольник
	@SuppressWarnings("unused")
	private void drawClickField(Canvas canvas) {
	    // рисуем предполагаемое поле кликов
	    paint.setColor(0xFFFFFF00);
	    for (int i = 0; i < 3; i++)
	    	for (int j = 0; j < 3; j++) {
	    		paint.setColor(((i + j) %2 == 0) ? 0x88FF00FF : 0x88FF0000);
	    		Rect r = getRectAtCoord(i, j);
	    		canvas.drawRect(r, paint);
	    		paint.setColor(0xFF000000);
	    		canvas.drawText(i + ";" + j, r.centerX(), r.centerY(), paint);
	    	}
	}
	
	// возвращает номер ячейки по двум координатам
	public int getCellNumber(int x, int y)
	{
		for (int i = 0; i < 9; i++) {
			Rect r = new Rect(cells[i]);
			// уменьшаем прямоугольник на CELL_CONSTRAINT%
			// чтобы не было неоднозначности при клике по границе
			r.inset((int)(XOside*CELL_CONSTRAINT), (int)(XOside*CELL_CONSTRAINT));
			if (r.contains(x, y))
				return i;
		}
		return -1;
	}
	
	// возвращает номер ячейки по последнему отжатию
	public int getCellNumber()
	{
		for (int i = 0; i < 9; i++) {
			Rect r = new Rect(cells[i]);
			// уменьшаем прямоугольник на 5%, чтобы не было неоднозначности при клике по границе
			r.inset((int)(XOside*CELL_CONSTRAINT), (int)(XOside*CELL_CONSTRAINT));
			if (r.contains((int)this.x, (int)this.y))
				return i;
		}
		return -1;
	}
	
	// присвоить элементу с индексом "index" массива "a" новое значение "value"
	// мало того, начинает анимацию
	public void setArrayElementValue(int value, int index)
	{
		try {
			a[index] = (byte) value;
		}
		catch (Exception e)
		{
			return;
		}
	}
	
	// присвоить новый массив "a" объ. данного класса
	public void setArray(byte[] a) {
		this.a = a.clone();
	}
	
	// выдаёт прямоугольник, содержащий ячейку
	public Rect getRect(int index) {
		try {
			return cells[index];
		} catch (Exception e) {
			return null;
		}
	}
	
	// возвращает основной прямоугольник прорисовки
	public Rect getMainRect()
	{
		return mainRect;
	}
	
	public void set_X(float x) {
		this.x = x;
	}
	
	public void set_Y(float y) {
		this.y = y;
	}

}