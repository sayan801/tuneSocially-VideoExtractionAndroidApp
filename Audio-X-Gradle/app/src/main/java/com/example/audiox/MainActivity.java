package com.example.audiox;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Date;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

@SuppressLint("UseSparseArrays") public class MainActivity extends Activity implements OnClickListener{

	private static final int chooseVid=1;
	private static final String TAG="MainActivity";
	//private static final boolean VERBOSE=false;
	//private static final int MAX_SAMPLE_SIZE = 256 * 1024;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        //SET UP BUTTON CLICK LISTENERS
        ((Button)findViewById(R.id.chooseVid)).setOnClickListener(this);

    }

    public void onActivityResult(int requestCode,int resultCode,Intent data)
    {
    	if(resultCode==RESULT_OK)
    	{
    		if(requestCode==chooseVid)
    		{
    			Uri uri=data.getData();
    			String path=getPath(uri);
    			Toast.makeText(getApplicationContext(), path, Toast.LENGTH_LONG).show();
                Log.v(TAG, "Path % - "+ path);
    			createNewFolder();
    			
    			boolean res=false;
    			try {
					res=testAudioOnly(path);
				} catch (Exception e) {
					e.printStackTrace();
				}
    			/*
                if(res)
    			{
    				TextView tv=(TextView)findViewById(R.id.audioResultsTextView);
    				tv.setVisibility(1);
    				tv.setText("Success");
    			}
    			else
    			{
    				TextView tv=(TextView)findViewById(R.id.audioResultsTextView);
    				tv.setVisibility(1);
    				tv.setText("Failed");
    			}
    			*/
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    private void createNewFolder()
    {
    	String targetPath=Environment.getExternalStorageDirectory().toString()+"/VidEx";
    	File file = new File(targetPath);
        if (!file.exists()) {
            if (!file.mkdirs()) {
                Toast.makeText(getApplicationContext(), "Cannot access external storage.", Toast.LENGTH_LONG).show();
            }
            else
            {
            	TextView tv=(TextView)findViewById(R.id.audioResultsTextView);
    			tv.setVisibility(1);
    			tv.setText(targetPath);
            }
        }
        else
        {
        	TextView tv=(TextView)findViewById(R.id.audioResultsTextView);
			tv.setVisibility(1);
			tv.setText(targetPath);
        }
    }

	@Override
	public void onClick(View v) {
		switch(v.getId())
		{
		case R.id.chooseVid:	Intent intent=new Intent(Intent.ACTION_PICK,android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
								startActivityForResult(intent,chooseVid);
			break;
		}
		
	}
	
	public boolean testAudioOnly(String source) throws Exception {
        String outputFile = "/storage/emulated/0/VidEx/audioOnly"+new Date().getTime()+".mp4";
        boolean s=false;
        try
        {
        	s=cloneMediaUsingMuxer(source, outputFile, 1, -1);
        }
        catch(Exception e)
        {
        	e.printStackTrace();
        }
        	return s;
    }

    private void decodeLoop(MediaExtractor extractor ,MediaFormat format)
    {

        ByteBuffer[] codecInputBuffers;
        ByteBuffer[] codecOutputBuffers;

        String mime = format.getString(MediaFormat.KEY_MIME);

        // the actual decoder
        MediaCodec codec = MediaCodec.createDecoderByType(mime);
        codec.configure(format, null /* surface */, null /* crypto */, 0 /* flags */);
        codec.start();
        codecInputBuffers = codec.getInputBuffers();
        codecOutputBuffers = codec.getOutputBuffers();

        // get the sample rate to configure AudioTrack
        int sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE);

        // create our AudioTrack instance
       /*
        AudioTrack audioTrack = new AudioTrack(
                AudioManager.USE_DEFAULT_STREAM_TYPE,
                sampleRate,
                AudioFormat.CHANNEL_OUT_STEREO,
                AudioFormat.ENCODING_PCM_16BIT,
                AudioTrack.getMinBufferSize (
                        sampleRate,
                        AudioFormat.CHANNEL_OUT_STEREO,
                        AudioFormat.ENCODING_PCM_16BIT
                ),
                AudioTrack.MODE_STREAM
        );

        // start playing, we will feed you later
        audioTrack.play();
        */
        //extractor.selectTrack(0);

        // start decoding
        final long kTimeOutUs = 10000;
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        boolean sawInputEOS = false;
        boolean sawOutputEOS = false;
        int noOutputCounter = 0;
        int noOutputCounterLimit = 50;

        //previously global varibale
        Boolean doStop = false;
        int inputBufIndex;
        int bufIndexCheck = 0;
        //int lastInputBufIndex;

        while (!sawOutputEOS && noOutputCounter < noOutputCounterLimit && !doStop) {
            //Log.i(LOG_TAG, "loop ");
            noOutputCounter++;

            if (!sawInputEOS) {

                inputBufIndex = codec.dequeueInputBuffer(kTimeOutUs);

                bufIndexCheck++;

                Log.d(TAG, " bufIndexCheck " + bufIndexCheck + " inputBufIndex " + inputBufIndex);

                if (inputBufIndex >= 0) {
                    ByteBuffer dstBuf = codecInputBuffers[inputBufIndex];

                    int sampleSize =
                            extractor.readSampleData(dstBuf, 0 /* offset */);

                    long presentationTimeUs = 0;

                    Log.d(TAG, " sampleSize " + sampleSize + " inputBufIndex " + inputBufIndex);

                    if (sampleSize < 0) {
                        Log.d(TAG, "saw input EOS.");
                        sawInputEOS = true;
                        sampleSize = 0;
                    } else {
                        presentationTimeUs = extractor.getSampleTime();
                    }
                    // can throw illegal state exception (???)

                    codec.queueInputBuffer(
                            inputBufIndex,
                            0 /* offset */,
                            sampleSize,
                            presentationTimeUs,
                            sawInputEOS ? MediaCodec.BUFFER_FLAG_END_OF_STREAM : 0);

                    Log.d(TAG, " sawInputEOS " + sawInputEOS + " presentationTimeUs " + presentationTimeUs);


                    if (!sawInputEOS) {
                        extractor.advance();
                    }
                }
                else
                {
                    Log.e(TAG, "inputBufIndex " +inputBufIndex);
                }
            }

            /*

            int res = codec.dequeueOutputBuffer(info, kTimeOutUs);



            if (res >= 0) {
                //Log.d(LOG_TAG, "got frame, size " + info.size + "/" + info.presentationTimeUs);
                if (info.size > 0) {
                    noOutputCounter = 0;
                }

                int outputBufIndex = res;
                ByteBuffer buf = codecOutputBuffers[outputBufIndex];

                final byte[] chunk = new byte[info.size];
                buf.get(chunk);
                buf.clear();
                if(chunk.length > 0){
                    audioTrack.write(chunk,0,chunk.length);
                    if(this.mState != State.Playing)
                    {
                        mDelegateHandler.onRadioPlayerPlaybackStarted(MP3RadioStreamPlayer.this);
                    }
                    this.mState = State.Playing;
                }
                codec.releaseOutputBuffer(outputBufIndex, false);
                if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    Log.d(LOG_TAG, "saw output EOS.");
                    sawOutputEOS = true;
                }
            } else if (res == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                codecOutputBuffers = codec.getOutputBuffers();

                Log.d(LOG_TAG, "output buffers have changed.");
            } else if (res == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                MediaFormat oformat = codec.getOutputFormat();

                Log.d(LOG_TAG, "output format has changed to " + oformat);
            } else {
                Log.d(LOG_TAG, "dequeueOutputBuffer returned " + res);
            }
            */
        }

        Log.e(TAG, "inputBuf length " + codecInputBuffers.length);

        /*
        Log.d(LOG_TAG, "stopping...");

        relaxResources(true);

        this.mState = State.Stopped;
        doStop = true;

        // attempt reconnect
        if(sawOutputEOS)
        {
            try {
                MP3RadioStreamPlayer.this.play();
                return;
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        */
    }

	 private boolean cloneMediaUsingMuxer(String filePath, String dstMediaPath,int expectedTrackCount, int degrees) throws IOException 
	 {
		 //String COMPRESSED_AUDIO_FILE_MIME_TYPE ;
		 //int COMPRESSED_AUDIO_FILE_BIT_RATE ; // 128kbps
		 //int SAMPLING_RATE;
         String COMPRESSED_AUDIO_FILE_MIME_TYPE = null;// = "audio/mp4a-latm";
         int COMPRESSED_AUDIO_FILE_BIT_RATE = 128000; // 128kbps
         int SAMPLING_RATE = 0;// = 44100;
         int channels = 0;
         long duration = 0;
         final int CODEC_TIMEOUT_IN_MS = 5000;
		 final int BUFFER_SIZE = 88200;
		 boolean suc=false;


         TextView tv=(TextView)findViewById(R.id.videoFilePathTextView);
         tv.setVisibility(1);
         tv.setText(filePath);

         MediaExtractor extractor;

         extractor = new MediaExtractor();
         extractor.setDataSource(filePath);

         int numTracks = extractor.getTrackCount();

         File outputFile = new File(dstMediaPath);
         if (outputFile.exists())
             outputFile.delete();

         MediaMuxer mux = new MediaMuxer(outputFile.getAbsolutePath(), MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);

         MediaFormat outputFormat = MediaFormat.createAudioFormat(COMPRESSED_AUDIO_FILE_MIME_TYPE,SAMPLING_RATE, 1);
         outputFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
         outputFormat.setInteger(MediaFormat.KEY_BIT_RATE, COMPRESSED_AUDIO_FILE_BIT_RATE);

         int audioTrackIdx = mux.addTrack(outputFormat);
         mux.start();

         for (int i = 0; i < numTracks; ++i) {

             MediaFormat format = extractor.getTrackFormat(i);
             COMPRESSED_AUDIO_FILE_MIME_TYPE = format.getString(MediaFormat.KEY_MIME);

             if(COMPRESSED_AUDIO_FILE_MIME_TYPE.startsWith("audio/")) {
                 //COMPRESSED_AUDIO_FILE_BIT_RATE = format.getInteger(MediaFormat.KEY_BIT_RATE);
                 SAMPLING_RATE = format.getInteger(MediaFormat.KEY_SAMPLE_RATE);
                 channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
                 duration = format.getLong(MediaFormat.KEY_DURATION);

                 extractor.selectTrack(i);

                 //decodeLoop(extractor , format);

                 //openmxplayer style

                 MediaCodec codec;

                 // create the actual decoder, using the mime to select
                 codec = MediaCodec.createDecoderByType(COMPRESSED_AUDIO_FILE_MIME_TYPE);
                 // check we have a valid codec instance
                 if (codec == null) {

                     Log.d(TAG, "codec is null.. :-( ");
                     return false;
                 }

                 codec.configure(format, null, null, 0);
                 codec.start();
                 ByteBuffer[] codecInputBuffers  = codec.getInputBuffers();
                 ByteBuffer[] codecOutputBuffers = codec.getOutputBuffers();

                 // configure AudioTrack
                /*
                 int channelConfiguration = channels == 1 ? AudioFormat.CHANNEL_OUT_MONO : AudioFormat.CHANNEL_OUT_STEREO;
                 int minSize = AudioTrack.getMinBufferSize( SAMPLING_RATE, channelConfiguration, AudioFormat.ENCODING_PCM_16BIT);
                 audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, SAMPLING_RATE, channelConfiguration,
                         AudioFormat.ENCODING_PCM_16BIT, minSize, AudioTrack.MODE_STREAM);

                 // start playing, we will feed the AudioTrack later
                 audioTrack.play();
                 extractor.selectTrack(0);
                 */

                 // start decoding
                 final long kTimeOutUs = 1000;
                 MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
                 boolean sawInputEOS = false;
                 boolean sawOutputEOS = false;
                 int noOutputCounter = 0;
                 int noOutputCounterLimit = 10;
                 boolean stop = false;
                 long presentationTimeUs = 0;

                // state.set(PlayerStates.PLAYING);
                 while (!sawOutputEOS && noOutputCounter < noOutputCounterLimit && !stop) {


                     noOutputCounter++;
                     // read a buffer before feeding it to the decoder
                     if (!sawInputEOS) {
                         int inputBufIndex = codec.dequeueInputBuffer(kTimeOutUs);
                         if (inputBufIndex >= 0) {
                             ByteBuffer dstBuf = codecInputBuffers[inputBufIndex];
                             int sampleSize = extractor.readSampleData(dstBuf, 0);
                             if (sampleSize < 0) {
                                 Log.d(TAG, "saw input EOS. Stopping playback");
                                 sawInputEOS = true;
                                 sampleSize = 0;
                             } else {
                                 presentationTimeUs = extractor.getSampleTime();
                                 final int percent =  (duration == 0)? 0 : (int) (100 * presentationTimeUs / duration);
                                 Log.d(TAG, "percent " + percent + " presentationTimeUs " + presentationTimeUs);
                             }

                             codec.queueInputBuffer(inputBufIndex, 0, sampleSize, presentationTimeUs, sawInputEOS ? MediaCodec.BUFFER_FLAG_END_OF_STREAM : 0);

                             if (!sawInputEOS) extractor.advance();

                         } else {
                             Log.e(TAG, "inputBufIndex " +inputBufIndex);
                         }
                     } // !sawInputEOS

                     // decode to PCM and push it to the AudioTrack player
                     int res = codec.dequeueOutputBuffer(info, kTimeOutUs);

                     if (res >= 0) {
                         if (info.size > 0)  noOutputCounter = 0;

                         int outputBufIndex = res;
                         //ByteBuffer buf = codecOutputBuffers[outputBufIndex];
                         mux.writeSampleData(audioTrackIdx, codecOutputBuffers[outputBufIndex], info);

                     /*
                         final byte[] chunk = new byte[info.size];
                         buf.get(chunk);
                         buf.clear();
                         if(chunk.length > 0){
                             audioTrack.write(chunk,0,chunk.length);

                	}*/
                         codec.releaseOutputBuffer(res, false);
                         if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                             Log.d(TAG, "saw output EOS.");
                             sawOutputEOS = true;
                         }
                     } else if (res == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                         codecOutputBuffers = codec.getOutputBuffers();
                         Log.d(TAG, "output buffers have changed.");
                     } else if (res == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                         MediaFormat oformat = codec.getOutputFormat();
                         Log.d(TAG, "output format has changed to " + oformat);
                     } else {
                         Log.d(TAG, "dequeueOutputBuffer returned " + res);
                     }
                 }

                 Log.d(TAG, "stopping...");

                 if(codec != null) {
                     codec.stop();
                     codec.release();
                     codec = null;
                 }


                 // clear source and the other globals
                 /*
                 sourcePath = null;
                 sourceRawResId = -1;
                 duration = 0;
                 mime = null;
                 sampleRate = 0; channels = 0; bitrate = 0;
                 presentationTimeUs = 0; duration = 0;
                 */
             }

             mux.stop();
             mux.release();

             String audioInfo = "Track info: mime:" + COMPRESSED_AUDIO_FILE_MIME_TYPE + " sampleRate:" + SAMPLING_RATE + "" +
                     " channels:" + channels + " bitrate:" + COMPRESSED_AUDIO_FILE_BIT_RATE + " duration:" + duration;
             Log.d(TAG, audioInfo);

             TextView tv1 = (TextView) findViewById(R.id.audioResultsTextView);
             tv1.setVisibility(1);
             tv1.setText( tv1.getText()+ " -- " + audioInfo);
         }
		 /*
		 try {
		        File inputFile = new File(filePath);
		        FileInputStream fis = new FileInputStream(inputFile);

		        File outputFile = new File(dstMediaPath);
		        if (outputFile.exists()) 
		        	outputFile.delete();

		        MediaMuxer mux = new MediaMuxer(outputFile.getAbsolutePath(), MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);

		        MediaFormat outputFormat = MediaFormat.createAudioFormat(COMPRESSED_AUDIO_FILE_MIME_TYPE,SAMPLING_RATE, 1);
		        outputFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
		        outputFormat.setInteger(MediaFormat.KEY_BIT_RATE, COMPRESSED_AUDIO_FILE_BIT_RATE);

		        MediaCodec codec = MediaCodec.createEncoderByType(COMPRESSED_AUDIO_FILE_MIME_TYPE);
		        codec.configure(outputFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
		        codec.start();

		        ByteBuffer[] codecInputBuffers = codec.getInputBuffers(); // Note: Array of buffers
		        ByteBuffer[] codecOutputBuffers = codec.getOutputBuffers();

		        MediaCodec.BufferInfo outBuffInfo = new MediaCodec.BufferInfo();

		        byte[] tempBuffer = new byte[BUFFER_SIZE];
		        boolean hasMoreData = true;
		        double presentationTimeUs = 0;
		        int audioTrackIdx = 0;
		        int totalBytesRead = 0;
		        int percentComplete;

		        do {

		            int inputBufIndex = 0;
		            while (inputBufIndex != -1 && hasMoreData) {
		                inputBufIndex = codec.dequeueInputBuffer(CODEC_TIMEOUT_IN_MS);

		                if (inputBufIndex >= 0) {
		                    ByteBuffer dstBuf = codecInputBuffers[inputBufIndex];
		                    dstBuf.clear();

		                    int bytesRead = fis.read(tempBuffer, 0, dstBuf.limit());
		                    if (bytesRead == -1) { // -1 implies EOS
		                        hasMoreData = false;
		                        codec.queueInputBuffer(inputBufIndex, 0, 0, (long) presentationTimeUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
		                    } else {
		                        totalBytesRead += bytesRead;
		                        dstBuf.put(tempBuffer, 0, bytesRead);
		                        codec.queueInputBuffer(inputBufIndex, 0, bytesRead, (long) presentationTimeUs, 0);
		                        presentationTimeUs = 1000000l * (totalBytesRead / 2) / SAMPLING_RATE;
		                    }
		                }
		            }

		            // Drain audio
		            int outputBufIndex = 0;
		            while (outputBufIndex != MediaCodec.INFO_TRY_AGAIN_LATER) {		            	
		                outputBufIndex = codec.dequeueOutputBuffer(outBuffInfo, CODEC_TIMEOUT_IN_MS);
		                if (outputBufIndex >= 0) {
		                    ByteBuffer encodedData = codecOutputBuffers[outputBufIndex];
		                    encodedData.position(outBuffInfo.offset);
		                    encodedData.limit(outBuffInfo.offset + outBuffInfo.size);

		                    if ((outBuffInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0 && outBuffInfo.size != 0) {
		                        codec.releaseOutputBuffer(outputBufIndex, false);
		                        outBuffInfo.size=0;
		                    } else {
		                        mux.writeSampleData(audioTrackIdx, codecOutputBuffers[outputBufIndex], outBuffInfo);
		                        codec.releaseOutputBuffer(outputBufIndex, false);
		                    }
		                } else if (outputBufIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
		                    outputFormat = codec.getOutputFormat();
		                    Log.v(TAG, "Output format changed - " + outputFormat);
		                    audioTrackIdx = mux.addTrack(outputFormat);
		                    mux.start();
		                } else if (outputBufIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
		                    Log.e(TAG, "Output buffers changed during encode!");
		                } else if (outputBufIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
		                    // NO OP
		                } else {
		                    Log.e(TAG, "Unknown return code from dequeueOutputBuffer - " + outputBufIndex);
		                }
		            }
		            percentComplete = (int) Math.round(((float) totalBytesRead / (float) inputFile.length()) * 100.0);
		            Log.v(TAG, "Conversion % - "+ percentComplete);
		        } while (outBuffInfo.flags != MediaCodec.BUFFER_FLAG_END_OF_STREAM);

		        fis.close();
		        mux.stop();
		        mux.release();
		        Log.v(TAG, "Compression done ...");
		        suc=true;
		    } catch (FileNotFoundException e) {
		        Log.e(TAG, "File not found!", e);
		        suc=false;
		    } catch (IOException e) {
		        Log.e(TAG, "IO exception!", e);
		        suc=false;
		    } catch (Exception ex)
             {
                 Toast.makeText(getApplicationContext(), "MediaExtractor Exception :" + ex.getMessage(), Toast.LENGTH_LONG).show();
             }
            */

		   
		   return suc;
	 }
		 	
		 	/*
	        // Set up MediaExtractor to read from the source.
	        //AssetFileDescriptor srcFd = mResources.openRawResourceFd(srcMedia);
	        MediaExtractor extractor = new MediaExtractor();
	        extractor.setDataSource(srcMedia);
	 
	        int trackCount = extractor.getTrackCount();

	 
	        // Set up MediaMuxer for the destination.
	        MediaMuxer muxer=null;
	        muxer = new MediaMuxer(dstMediaPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
	        muxer.addTrack(MediaFormat.createAudioFormat("audio/mp4a-latm", 48000, 1));
	        // Set up the tracks.
	        HashMap<Integer, Integer> indexMap = new HashMap<Integer, Integer>(trackCount);
	        for (int i = 0; i < trackCount; i++) {
	            extractor.selectTrack(i);
	            MediaFormat format = extractor.getTrackFormat(i);
	            int dstIndex = muxer.addTrack(format);
	            indexMap.put(i, dstIndex);
	        }
	 
	        // Copy the samples from MediaExtractor to MediaMuxer.
	        boolean sawEOS = false;
	        int bufferSize = MAX_SAMPLE_SIZE;
	        int frameCount = 0;
	        int offset = 100;
	 
	        ByteBuffer dstBuf = ByteBuffer.allocate(bufferSize);
	        BufferInfo bufferInfo = new BufferInfo();
	 
	        if (degrees >= 0) {
	            muxer.setOrientationHint(degrees);
	        }
	        muxer.start();
	        while (!sawEOS) {
	            bufferInfo.offset = offset;
	            bufferInfo.size = extractor.readSampleData(dstBuf, offset);
	 
	            if (bufferInfo.size < 0) {
	                if (VERBOSE) {
	                    Log.d(TAG, "saw input EOS.");
	                }
	                sawEOS = true;
	                bufferInfo.size = 0;
	            } else {
	                bufferInfo.presentationTimeUs = extractor.getSampleTime();
	                bufferInfo.flags = extractor.getSampleFlags();
	                int trackIndex = extractor.getSampleTrackIndex();
	 
	                muxer.writeSampleData(indexMap.get(trackIndex), dstBuf,
	                        bufferInfo);
	                extractor.advance();
	 
	                frameCount++;
	                if (VERBOSE) {
	                    Log.d(TAG, "Frame (" + frameCount + ") " +
	                            "PresentationTimeUs:" + bufferInfo.presentationTimeUs +
	                            " Flags:" + bufferInfo.flags +
	                            " TrackIndex:" + trackIndex +
	                            " Size(KB) " + bufferInfo.size / 1024);
	                }
	            }
	        }
	 
	        muxer.stop();
	        muxer.release();
	        suc=true;
	        return suc;
	    }
	 */
	
}
