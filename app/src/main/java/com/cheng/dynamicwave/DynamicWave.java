package com.cheng.dynamicwave;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DrawFilter;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.util.AttributeSet;
import android.view.View;

public class DynamicWave extends View {

	// y = Asin(wx+b)+h
	//b为初始周期点、一般不用管
	private int STRETCH_FACTOR_A = 10;//波动幅度
	private int OFFSET_H = 0;//初始高度
	private float CYCLEFACTOR_W;//波动周期
	private static final int TRANSLATE_X_SPEED_ONE = 6;// 第一条水波移动速度
	private static final int TRANSLATE_X_SPEED_TWO = 4;// 第二条水波移动速度
	private int mTotalWidth, mTotalHeight;//控件总宽高

	private float[] mYPositions;
	private float[] mResetOneYPositions;
	private float[] mResetTwoYPositions;
	private int mXOffsetSpeedOne;
	private int mXOffsetSpeedTwo;
	private int mXOneOffset;
	private int mXTwoOffset;
	private Paint mWavePaint;
	private DrawFilter mDrawFilter;
	private Paint mCirclePaint;
	private PorterDuffXfermode mMode;
	private Context context;
	private AnimationEndListener listener;//监听水位到顶
	private int waterHeight = 0;//水位
	private boolean isStart = false;//动画开始标志
	private boolean isDraw = true;//是否绘制水波动画，跟暂停无关

	public DynamicWave(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.context = context;
		// 将dp转化为px，用于控制不同分辨率上移动速度基本一致
		mXOffsetSpeedOne = DensityUtil.dip2px(context, TRANSLATE_X_SPEED_ONE);
		mXOffsetSpeedTwo = DensityUtil.dip2px(context, TRANSLATE_X_SPEED_TWO);
		// 初始绘制波纹的画笔
		mWavePaint = new Paint();
		mCirclePaint = new Paint();
		// 去除画笔锯齿
		mWavePaint.setAntiAlias(true);
		// 设置风格为实线
		mWavePaint.setStyle(Style.FILL);
		mWavePaint = new Paint();
		// 防抖动
		mWavePaint.setDither(true);
		// 开启图像过滤
		mWavePaint.setFilterBitmap(true);
		// 设置画笔颜色
		mWavePaint.setColor(Color.parseColor("#8bc4ff"));
		mCirclePaint.setColor(Color.parseColor("#ffffff"));
		mDrawFilter = new PaintFlagsDrawFilter(0, Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
		mMode = new PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP);
		mWavePaint.setXfermode(mMode);
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		// 从canvas层面去除绘制时锯齿
		canvas.setDrawFilter(mDrawFilter);
		canvas.saveLayer(0, 0, mTotalWidth, mTotalHeight, null, Canvas.ALL_SAVE_FLAG);
		canvas.drawCircle(mTotalWidth / 2, mTotalWidth / 2, mTotalWidth / 2, mCirclePaint);
		if(isStart) waterHeight += 1;
		if(waterHeight>0){//只有在水位大于0才绘制水波纹
			for (int i = 0; i < mTotalWidth; i++) {
				// 绘制第一条水波纹
				canvas.drawLine(i, mTotalWidth - mResetOneYPositions[i] - waterHeight, i, mTotalWidth, mWavePaint);
				// 绘制第二条水波纹
				canvas.drawLine(i, mTotalWidth - mResetTwoYPositions[i] - waterHeight, i, mTotalWidth, mWavePaint);
			}
			resetPositonY();
			// 改变两条波纹的移动点
			mXOneOffset += mXOffsetSpeedOne;
			mXTwoOffset += mXOffsetSpeedTwo;
			// 如果已经移动到结尾处，则重头记录
			if (mXOneOffset >= mTotalWidth) mXOneOffset = 0;
			if (mXTwoOffset > mTotalWidth) mXTwoOffset = 0;
			// 引发view重绘，一般可以考虑延迟20-30ms重绘，空出时间片
			if (waterHeight <= mTotalHeight + STRETCH_FACTOR_A) {
				if(isDraw) postInvalidateDelayed(30);
			} else {
				if (listener != null)listener.animationEnd();
			}
		}
	}

	private void resetPositonY() {
		// mXOneOffset代表当前第一条水波纹要移动的距离
		int yOneInterval = mYPositions.length - mXOneOffset;
		// 使用System.arraycopy方式重新填充第一条波纹的数据
		System.arraycopy(mYPositions, mXOneOffset, mResetOneYPositions, 0, yOneInterval);
		System.arraycopy(mYPositions, 0, mResetOneYPositions, yOneInterval, mXOneOffset);

		int yTwoInterval = mYPositions.length - mXTwoOffset;
		System.arraycopy(mYPositions, mXTwoOffset, mResetTwoYPositions, 0, yTwoInterval);
		System.arraycopy(mYPositions, 0, mResetTwoYPositions, yTwoInterval, mXTwoOffset);
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		// 记录下view的宽高
		mTotalWidth = w;
		mTotalHeight = h;
		// 用于保存原始波纹的y值
		mYPositions = new float[mTotalWidth];
		// 用于保存波纹一的y值
		mResetOneYPositions = new float[mTotalWidth];
		// 用于保存波纹二的y值
		mResetTwoYPositions = new float[mTotalWidth];
		// 将周期定为view总宽度
		CYCLEFACTOR_W = (float) (2 * Math.PI / mTotalWidth);
		// 根据view总宽度得出所有对应的y值
		for (int i = 0; i < mTotalWidth; i++) {
			mYPositions[i] = (float) (STRETCH_FACTOR_A * Math.sin(CYCLEFACTOR_W * i) + OFFSET_H);
		}
	}

	/**
	 * @category 开始波动
	 */
	public void startWave() {
		if (!isStart) {
			isStart = true;
			isDraw = true;
			if(waterHeight==0) postInvalidateDelayed(30);
		}
	}

	/**
	 * @category 停止波动
	 */
	public void stopWave() {
		if (isStart) {
			isStart = false;
		}
	}

	/**
	 * @category 重置
	 */
	public void resetWave() {
		isStart = false;
		isDraw = false;
		setWaterHeight(0);
		postInvalidateDelayed(30);
	}

	/**
	 * 设置水位高度
	 * @param waterHeight
	 */
	public void setWaterHeight(int waterHeight) {
		this.waterHeight = waterHeight;
	}

	public void setAnimationEndListener(AnimationEndListener listener) {
		this.listener = listener;
	}

	public interface AnimationEndListener {
		void animationEnd();
	}

}