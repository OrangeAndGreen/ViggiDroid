package com.Dave.Multitouch;

import android.app.Activity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.ImageView;
import android.widget.TextView;

public class Sandbox_MultitouchActivity extends Activity
{
	private TextView mTextView = null;
	private ImageView mImageView = null;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        mTextView = (TextView) findViewById(R.id.textview);
        mImageView = (ImageView) findViewById(R.id.imageview);
        
        mImageView.setImageResource(R.drawable.miami);
        
        mImageView.setOnTouchListener(new MyTouchListener());
    }
    
    private class MyTouchListener implements OnTouchListener
    {

		@Override
		public boolean onTouch(View view, MotionEvent args)
		{
			int action = args.getAction();
			
			switch(action)
			{
			case MotionEvent.ACTION_DOWN:
				mTextView.setText("Action down");
				break;
			case MotionEvent.ACTION_MOVE:
				mTextView.setText("Action move");
				break;
			case MotionEvent.ACTION_UP:
				mTextView.setText("Action up");
				break;
			case MotionEvent.ACTION_CANCEL:
				mTextView.setText("Action Cancel");
				break;
			case MotionEvent.ACTION_POINTER_UP:
				mTextView.setText("Action Pointer Up");
				break;
			case MotionEvent.ACTION_POINTER_DOWN:
				mTextView.setText("Action Pointer Down");
				break;
			default:
				break;
			}
			
			
			return true;
		}
    	
    }
}