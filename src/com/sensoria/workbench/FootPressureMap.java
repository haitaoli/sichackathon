package com.sensoria.workbench;

import java.security.InvalidParameterException;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.*;

final class SensorMap {
    public Point[] InternalPath;
    public Point[] ExternalPath;
    public int LowCap;
    public int HighCap;
    public int Value;
}

public class FootPressureMap extends View {
	public static int RIGHT_FOOT = 1;
	public static int LEFT_FOOT = 0;
	
	// These constants don't have anything to do with actual sensor indexes
	private final int HEEL = 0;
	private final int MTB1 = 1;
	private final int MTB5 = 2;
	
	private Bitmap mRightFootImage;
	private Bitmap mLeftFootImage;
	private Paint mPaint;
	
	private int mCurrentWidth;
	private int mCurrentHeight;
	private float mCurrentScaleFactor;
	private RectF mRectArea;
	
	private Boolean mIsRightFoot = true;
	
	// Sensor Maps
	private SensorMap[] mSensorMaps;
	private int[] mSensorColors;
			
	// Sensor Paths
	
	Path mInternalLayers[];
	Path mExternalLayers[];
		
	public FootPressureMap(Context context) {
		super(context);
		init(context, null);
	}
	public FootPressureMap(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context, attrs);
	}
	
	private void init(Context context, AttributeSet attrs) {		
		// Preset sensor maps
		mSensorMaps = new SensorMap[3];
		for (int i = 0; i < 3; i++) {
			mSensorMaps[i] = new SensorMap();
		}
		
        // Set from Attributes
        if (attrs != null) {
        	TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.FootPressureMap, 0, 0);
        	try {
        		mSensorMaps[HEEL].LowCap = a.getInt(R.styleable.FootPressureMap_heelLowCap, 10);         		
        		mSensorMaps[HEEL].HighCap = a.getInt(R.styleable.FootPressureMap_heelLowCap, 100);  
        		mSensorMaps[MTB1].LowCap = a.getInt(R.styleable.FootPressureMap_mtb1LowCap, 10);         		
        		mSensorMaps[MTB1].HighCap = a.getInt(R.styleable.FootPressureMap_mtb1LowCap, 100);  
        		mSensorMaps[MTB5].LowCap = a.getInt(R.styleable.FootPressureMap_mtb5LowCap, 10);         		
        		mSensorMaps[MTB5].HighCap = a.getInt(R.styleable.FootPressureMap_mtb5LowCap, 100);  
        		
        		mIsRightFoot = a.getInteger(R.styleable.FootPressureMap_foot, 1) == 1;
        		
        		mSensorMaps[HEEL].Value = a.getInt(R.styleable.FootPressureMap_heelValue, 0);
        		mSensorMaps[MTB1].Value = a.getInt(R.styleable.FootPressureMap_mtb1Value, 0);
        		mSensorMaps[MTB5].Value = a.getInt(R.styleable.FootPressureMap_mtb5Value, 0);
        	} finally {
        		a.recycle();
        	}
        }
		
		// Preset colors and other canvas objects needed during OnDraw
        Bitmap palette = BitmapFactory.decodeResource(getResources(), R.drawable.palette);
        palette = palette.copy(Bitmap.Config.ARGB_8888, true);
        mSensorColors = new int[256];
        palette.getPixels(mSensorColors, 0, 256, 0, 0, 256, 1);
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

		// Preset foot images       
        mRightFootImage = BitmapFactory.decodeResource(getResources(), R.drawable.foot_map_right);
        mLeftFootImage = BitmapFactory.decodeResource(getResources(), R.drawable.foot_map_left);
        
        presetSensorMap(mIsRightFoot);
	}
	
	void presetSensorMap(Boolean isRightFoot) {
		if (mInternalLayers == null) {
			mInternalLayers = new Path[3];
			mExternalLayers = new Path[3];
		}
		
		String prefix = isRightFoot ? "right_" : "left_";
		mInternalLayers[HEEL] = getSensorPathFromFootSvg(prefix + "heel_layer3");
		mInternalLayers[MTB1] = getSensorPathFromFootSvg(prefix + "mtb_layer3_right");
		mInternalLayers[MTB5] = getSensorPathFromFootSvg(prefix + "mtb_layer3_left");
		mExternalLayers[HEEL] = getSensorPathFromFootSvg(prefix + "heel_layer2");
		mExternalLayers[MTB1] = getSensorPathFromFootSvg(prefix + "mtb_layer2_right");
		mExternalLayers[MTB5] = getSensorPathFromFootSvg(prefix + "mtb_layer2_left");
	}
	
	Path getSensorPathFromFootSvg(String tag) {
		try {
			InputSource is = new InputSource(getResources().openRawResource(R.raw.foot_sensors));  
			String expression = "//path[@id='" + tag + "']";			
			XPath xpath = XPathFactory.newInstance().newXPath();			
			
			NodeList nodeList = (NodeList) xpath.evaluate(expression, is, XPathConstants.NODESET);
			
	        if(nodeList != null && nodeList.getLength() > 0) {
	            // Actually just one node expected
	            Node node = nodeList.item(0);	            
	            String pathSpec = node.getAttributes().getNamedItem("d").getNodeValue(); 	            
	            Path path = parsePath(pathSpec);	            

	            return path;
	        }
		} catch (XPathExpressionException e) {
			// Something really broken if we get here
			e.printStackTrace();
		}
		return new Path();
	}
	
	Path parsePath(String pathSpec) {
		Path path = new Path();
		
		try {
			String[] tokens = pathSpec.split("[, ]");
			
			int i = 0;
			char command = '\0';
			
			while (i < tokens.length) {
				// If we don't find one of the commands, the previous command is used
				if (tokens[i].matches("[mMlLzZhHvVcCsSqQtTaA]")) {
					command = tokens[i++].charAt(0);		
				}
				i += emitPathCommand(path, command, tokens, i);
			}
		}
		catch (Exception e) {
			// Bug or malformed/unsupported SVG path spec
			Log.d("FootMapPressure", e.getMessage());
		}
		
		return path;
	}
	
	int emitPathCommand(Path path, char command, String[] tokens, int pos) {
		switch (command) {
			case 'M':
				path.moveTo(Float.parseFloat(tokens[pos]), 
							Float.parseFloat(tokens[pos+1]));
				return 2;
			case 'm':
				path.rMoveTo(Float.parseFloat(tokens[pos]), 
							 Float.parseFloat(tokens[pos+1]));
				return 2;
			case 'L':
				path.lineTo(Float.parseFloat(tokens[pos]), 
					 	  	Float.parseFloat(tokens[pos+1]));
				return 2;
			case 'l':
				path.rLineTo(Float.parseFloat(tokens[pos]), 
							 Float.parseFloat(tokens[pos+1]));
				return 2;
			case 'C':
				path.cubicTo(Float.parseFloat(tokens[pos]), 
							 Float.parseFloat(tokens[pos+1]), 
							 Float.parseFloat(tokens[pos+2]), 
							 Float.parseFloat(tokens[pos+3]),
							 Float.parseFloat(tokens[pos+4]), 
							 Float.parseFloat(tokens[pos+5]));
				return 6;
			case 'c':
				path.rCubicTo(Float.parseFloat(tokens[pos]), 
						 	  Float.parseFloat(tokens[pos+1]), 
							  Float.parseFloat(tokens[pos+2]), 
							  Float.parseFloat(tokens[pos+3]),
							  Float.parseFloat(tokens[pos+4]), 
							  Float.parseFloat(tokens[pos+5]));
				return 6;
			case 'S':
			case 's':
				// Not supported by java path in a simple way - ignore for now (would need to keep history of cursor)
				// Move cursor ahead
				return 4;
			case 'Q':
				path.quadTo(Float.parseFloat(tokens[pos]), 
						 	Float.parseFloat(tokens[pos+1]), 
							Float.parseFloat(tokens[pos+2]), 
							Float.parseFloat(tokens[pos+3]));
				return 4;
			case '1':
				path.rQuadTo(Float.parseFloat(tokens[pos]), 
						  	 Float.parseFloat(tokens[pos+1]), 
							 Float.parseFloat(tokens[pos+2]), 
							 Float.parseFloat(tokens[pos+3]));
				return 4;
			case 'T':
			case 't':
				// Not supported by java path in a simple way - ignore for now (would need to keep history of cursor)
				// Move cursor ahead
				return 2;				
			case 'H':
			case 'h':
			case 'V':
			case 'v':
				// Not supported by java path in a simple way - ignore for now (would need to keep history of cursor)
				// Move cursor ahead
				return 1;
			case 'A':
			case 'a':
				// Not supported by java path in a simple way - ignore for now (would need to keep history of cursor)
				// Move cursor ahead
				return 7;								
			case 'Z':
			case 'z':
				path.close();
				return 0;				
			default:
				// Unknown command: if we returned 0 it would enter infinite loop
				// return 1 to deplete the coordinated we don't know what to do about
				return 1;
		}
	}
	
	// Foot setter/getter
	public int getFoot() {
		return (mIsRightFoot ? RIGHT_FOOT : LEFT_FOOT);
	}
	
	public void setFoot(int foot) {
		if (foot == RIGHT_FOOT) {
			mIsRightFoot = true;
			presetSensorMap(true);
			requestLayout();
			invalidate();
		} else if (foot == LEFT_FOOT) {
			mIsRightFoot = false;
			presetSensorMap(false);
			requestLayout();
			invalidate();
		} else {
			throw new InvalidParameterException();
		}
	}
	
	public void swapFoot() {
		setFoot(mIsRightFoot ? LEFT_FOOT : RIGHT_FOOT);
	}
	
	// Caps
	public void setSensorCaps(int heelLowCap, int heelHighCap, int mtb1LowCap, int mtb1HighCap, int mtb5LowCap, int mtb5HighCap) {
		if ((heelLowCap < 0) || (heelLowCap >= heelHighCap) || (heelHighCap >= 1024)) {
			throw new InvalidParameterException("Heel cap values are invalid");
		}
		if ((mtb1LowCap < 0) || (mtb1LowCap >= mtb1HighCap) || (mtb1HighCap >= 1024)) {
			throw new InvalidParameterException("MTB 1 cap values are invalid");
		}
		if ((mtb5LowCap < 0) || (mtb5LowCap >= mtb5HighCap) || (mtb5HighCap >= 1024)) {
			throw new InvalidParameterException("MTB 5 cap values are invalid");
		}
		
		mSensorMaps[HEEL].LowCap = heelLowCap;         		
		mSensorMaps[HEEL].HighCap = heelHighCap;  
		mSensorMaps[MTB1].LowCap = mtb1LowCap;         		
		mSensorMaps[MTB1].HighCap = mtb1HighCap;  
		mSensorMaps[MTB5].LowCap = mtb5LowCap;         		
		mSensorMaps[MTB5].HighCap = mtb5HighCap;
		
		requestLayout();
		invalidate();
	}
	
	// Values
	public void setSensors(int heelValue, int mtb1Value, int mtb5Value) {
		if ((heelValue < 0) || (heelValue >= 1024)) {
			throw new InvalidParameterException("Heel value is invalid");
		}
		if ((mtb1Value < 0) || (mtb1Value >= 1024)) {
			throw new InvalidParameterException("MTB 1 value is invalid");
		}
		if ((mtb5Value < 0) || (mtb5Value >= 1024)) {
			throw new InvalidParameterException("MTB 5 value is invalid");
		}
		
		mSensorMaps[HEEL].Value = heelValue;
		mSensorMaps[MTB1].Value = mtb1Value;
		mSensorMaps[MTB5].Value = mtb5Value;
		
		requestLayout();
		invalidate();
	}
	
	@Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mCurrentHeight = h;
        mCurrentWidth = w;
        if ((mCurrentHeight > 0) && (mCurrentWidth > 0)) {
        	mCurrentScaleFactor = ((float)mCurrentWidth / 315.0f);
        }
        
        mRectArea = new RectF(0, 0, w, w);
	}
	
	@Override 
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
			
		if ((mCurrentHeight > 0) && (mCurrentWidth > 0)) {	
			canvas.drawBitmap(mIsRightFoot ? mRightFootImage : mLeftFootImage, null, mRectArea, null);
			
			canvas.scale(mCurrentScaleFactor, mCurrentScaleFactor);
			
			drawHeatPoint(canvas, HEEL);
			drawHeatPoint(canvas, MTB1);
			drawHeatPoint(canvas, MTB5);
		}
	}
	
	private void drawHeatPoint(Canvas canvas, int sensorIndex) {
        int internalCrown;
        int externalCrown;

        double lowCap = mSensorMaps[sensorIndex].LowCap;
        double highCap = mSensorMaps[sensorIndex].HighCap;
        int intensity = mSensorMaps[sensorIndex].Value;
        
        double adjustedIntensity = (intensity > highCap ? highCap : (intensity < lowCap ? lowCap : intensity));

        double targetIntensity = ((adjustedIntensity - lowCap) / (highCap - lowCap)) * 255;

        if (targetIntensity < 50)
        {
            externalCrown = 0;
            internalCrown = 50;
        }
        else
        {
            externalCrown = (int)(targetIntensity - 50);
            internalCrown = (int)targetIntensity;
        }
        
        int internalColor = mSensorColors[internalCrown];
        int externalColor = mSensorColors[externalCrown];
                
        mPaint.setColor(mSensorColors[externalCrown]);
        canvas.drawPath(mExternalLayers[sensorIndex], mPaint);
        
        mPaint.setColor(mSensorColors[internalCrown]);
        canvas.drawPath(mInternalLayers[sensorIndex], mPaint);

	}

}
