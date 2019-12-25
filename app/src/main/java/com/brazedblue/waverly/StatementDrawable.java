package com.brazedblue.waverly;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;

class StatementDrawable {
	private Bitmap m_Bitmap;
	private Point m_CenterStart = new Point();
	private float m_RotationStart;
	private Point m_CenterEnd = new Point();
	private float m_AnimationFraction = 1;
	private Matrix m_Matrix = new Matrix();
	private boolean m_MatrixInvalid;
	
	StatementDrawable(Bitmap bitmap)
	{
		m_Bitmap = bitmap;
	}
	
	void draw(Canvas canvas, Paint paint)
	{
		if (m_Bitmap != null)
		{
			if (m_MatrixInvalid)
			{
				float centerX = (m_CenterEnd.x - m_CenterStart.x) * m_AnimationFraction + m_CenterStart.x;
				float centerY = (m_CenterEnd.y - m_CenterStart.y) * m_AnimationFraction + m_CenterStart.y;
				m_Matrix.reset();
				m_Matrix.setTranslate(-m_Bitmap.getWidth() / 2, -m_Bitmap.getHeight() / 2);
				m_Matrix.postRotate(m_RotationStart * (1 - m_AnimationFraction));
				m_Matrix.postTranslate(centerX, centerY);
				m_MatrixInvalid = false;
			}
			canvas.drawBitmap(m_Bitmap, m_Matrix, paint);
		}
	}


	void setRotationStart(float rotation) {
		this.m_RotationStart = rotation;
		m_MatrixInvalid = true;
	}

	void setCenterStart(Point centerStart) {
		this.m_CenterStart.set(centerStart.x, centerStart.y);
		m_MatrixInvalid = true;
	}

	void setCenterEnd(Point point) {
		this.m_CenterEnd.set(point.x, point.y);
		m_MatrixInvalid = true;
	}

	public float getAnimationFraction() {
		return m_AnimationFraction;
	}

	void setAnimationFraction(float animationFraction) {
		if (animationFraction != m_AnimationFraction)
		{
			m_MatrixInvalid = true;
		}
		
		this.m_AnimationFraction = animationFraction;
	}
	
	int getWidth()
	{
		int result = 0;
		if (null != m_Bitmap)
		{
			result = m_Bitmap.getWidth();
		}
		return result;
	}

	int getHeight()
	{
		int result = 0;
		if (null != m_Bitmap)
		{
			result = m_Bitmap.getHeight();
		}
		return result;
	}

}
