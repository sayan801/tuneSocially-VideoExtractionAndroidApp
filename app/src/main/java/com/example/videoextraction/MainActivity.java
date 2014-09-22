package com.example.videoextraction;

import it.sauronsoftware.jave.AudioAttributes;
import it.sauronsoftware.jave.Encoder;
import it.sauronsoftware.jave.EncoderException;
import it.sauronsoftware.jave.EncodingAttributes;
import it.sauronsoftware.jave.InputFormatException;
import java.io.File;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.TextView;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends Activity implements OnClickListener  {

	private static final int chooseVid=1;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        setUpButtonListeners();
    }

    public void onActivityResult(int requestCode,int resultCode,Intent data)
    {
    	if(resultCode==RESULT_OK)
    	{
    		if(requestCode==chooseVid)
    		{
    			Uri uri=data.getData();
    			String path=getPath(uri);
    			//String videoString=data.getDataString();
    			Toast.makeText(getApplicationContext(), path, Toast.LENGTH_LONG).show();
    			String tpath=path.substring(0, path.lastIndexOf("/")+1);
    			TextView tv=(TextView)findViewById(R.id.textView1);
    			tv.setVisibility(1);
    			tv.setText(tpath);
    			
    			//THE FOLLOWING CODE WORKED ON THE SYSTEM, WITH JAVE LIBRARY ADDED.
    			//I ADDED THE SAME HERE WITH CHANGES TO THE PATH AS IT IS BASICALLY JAVA.
    			File source;
    	        source = new File(path);
    	        File target = new File(tpath+"target.wav");
    	        AudioAttributes audio = new AudioAttributes();
    	        audio.setCodec("pcm_s16le");
    	        EncodingAttributes attrs = new EncodingAttributes();
    	        attrs.setFormat("wav");
    	        attrs.setAudioAttributes(audio);
    	        Encoder encoder = new Encoder();
    	        try {
					encoder.encode(source, target, attrs);
				} catch (IllegalArgumentException e) {
					tv.setText(e.getMessage().toString());
				} catch (InputFormatException e) {
					tv.setText(e.getMessage().toString());
				} catch (EncoderException e) {
					tv.setText(e.getMessage().toString());
				}
    		}
    	}
    }
    
    private String getPath(Uri uri)
    {
    	String[] projection={MediaStore.Video.Media.DATA};
    	Cursor cursor=null;
    	String result="Error";
    	try
    	{
    		cursor=getContentResolver().query(uri,projection,null,null,null);
    		int column_index=cursor.getColumnIndex(MediaStore.Video.Media.DATA);
        	cursor.moveToFirst();
        	result=cursor.getString(column_index).toString();
    	}
    	finally
    	{
    		if(cursor!=null)
    			cursor.close();
    	}
    	return result;
    }

    private void setUpButtonListeners() {
    	((Button)findViewById(R.id.chooseVid)).setOnClickListener(this);
    	((Button)findViewById(R.id.exitButton)).setOnClickListener(this);		
	}


	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }


	@Override
	public void onClick(View v) {
		switch(v.getId())
		{
		case R.id.exitButton:	this.finish();	break;
		case R.id.chooseVid:	
								Intent intent=new Intent(Intent.ACTION_PICK,android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
								startActivityForResult(intent,chooseVid);
								break;
								/*Intent intent=new Intent();
								intent.setType("video/*");
								intent.setAction(Intent.ACTION_GET_CONTENT);
								startActivityForResult(Intent.createChooser(intent, "Select Video"),chooseVid);
								break;*/
		}
		
	}
    
}
