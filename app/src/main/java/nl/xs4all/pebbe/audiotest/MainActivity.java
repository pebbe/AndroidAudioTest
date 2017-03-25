package nl.xs4all.pebbe.audiotest;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import ca.uol.aig.fftpack.RealDoubleFFT;

public class MainActivity extends Activity {

    int frequency =  8000;
    int channelConfiguration = AudioFormat.CHANNEL_IN_MONO;
    int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;
    private RealDoubleFFT transformer;
    int blockSize = 320;

    Button startStopButton;
    boolean started = false;

    boolean inConfig = false;
    TextView lblRate;
    EditText valRate;
    TextView lblSize;
    EditText valSize;

    RecordAudio recordTask;

    ImageView imageView;
    Bitmap bitmap;
    Canvas canvas;
    Paint paint;

    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;

    // Requesting permission to RECORD_AUDIO
    private boolean permissionToRecordAccepted = false;
    private String [] permissions = {Manifest.permission.RECORD_AUDIO};

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case REQUEST_RECORD_AUDIO_PERMISSION:
                permissionToRecordAccepted  = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                break;
        }
        if (!permissionToRecordAccepted ) finish();

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION);

        setContentView(R.layout.activity_main);

        startStopButton = (Button) findViewById(R.id.start_stop_btn);
        lblRate = (TextView) findViewById(R.id.lbl_rate);
        valRate = (EditText) findViewById(R.id.val_rate);
        lblSize = (TextView) findViewById(R.id.lbl_size);
        valSize = (EditText) findViewById(R.id.val_size);

        lblRate.setVisibility(View.INVISIBLE);
        valRate.setVisibility(View.INVISIBLE);
        lblSize.setVisibility(View.INVISIBLE);
        valSize.setVisibility(View.INVISIBLE);

        imageView = (ImageView) this.findViewById(R.id.imageView1);
        imageView.setVisibility(View.VISIBLE);
        bitmap = Bitmap.createBitmap((int) 800, (int) 1600,
                Bitmap.Config.ARGB_8888);
        canvas = new Canvas(bitmap);
        paint = new Paint();
        paint.setColor(Color.MAGENTA);
        paint.setStrokeWidth(4);

        imageView.setImageBitmap(bitmap);

        started = false;
        inConfig = false;

    }

    public class RecordAudio extends AsyncTask<Void, double[], Void> {

        @Override
        protected Void doInBackground(Void... arg0) {

            try {
                transformer = new RealDoubleFFT(blockSize);

                // int bufferSize = AudioRecord.getMinBufferSize(frequency,
                // AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
                int bufferSize = AudioRecord.getMinBufferSize(frequency,
                        channelConfiguration, audioEncoding);

                AudioRecord audioRecord = new AudioRecord(
                        MediaRecorder.AudioSource.MIC, frequency,
                        channelConfiguration, audioEncoding, bufferSize);

                short[] buffer = new short[blockSize];
                double[] toTransform = new double[blockSize];

                audioRecord.startRecording();

                while (started) {
                    int bufferReadResult = audioRecord.read(buffer, 0,
                            blockSize);

                    for (int i = 0; i < blockSize && i < bufferReadResult; i++) {
                        toTransform[i] = (double) buffer[i] / 32768.0;
                    }
                    transformer.ft(toTransform);
                    publishProgress(toTransform);



                }

                audioRecord.stop();

            } catch (Throwable t) {
                t.printStackTrace();
                Log.e("AudioRecord", "Recording Failed");
            }
            Log.i("MYTAG", "AudioRecord finished");
            return null;
        }

        @Override
        protected void onProgressUpdate(double[]... toTransform) {

            canvas.drawColor(Color.WHITE);

            int m = frequency / blockSize;

            for (int i = 0; i < toTransform[0].length; i++) {
                int x = (800 * i) / blockSize;
                int downy = (int) (800 - (toTransform[0][i] * m * Math.pow(i, .25)));
                int upy = 800;

                canvas.drawLine(x, downy, x, upy, paint);
            }

            imageView.invalidate();

        }

    }

    public void start(View v) {
        if (started) {
            started = false;
            startStopButton.setText("Start");
            //recordTask.cancel(true);
        } else {
            lblRate.setVisibility(View.INVISIBLE);
            valRate.setVisibility(View.INVISIBLE);
            lblSize.setVisibility(View.INVISIBLE);
            valSize.setVisibility(View.INVISIBLE);
            imageView.setVisibility(View.VISIBLE);

            frequency = Integer.parseInt(valRate.getText().toString(), 10);
            if (frequency < 2000 || frequency > 44100) {
                frequency = 8000;
                valRate.setText(""+frequency);
            }

            blockSize = Integer.parseInt(valSize.getText().toString(), 10);
            if (blockSize < 100 || blockSize > frequency) {
                blockSize = 320;
                valSize.setText(""+blockSize);
            }

            started = true;
            inConfig = false;
            startStopButton.setText("Stop");
            recordTask = new RecordAudio();
            recordTask.execute();
        }
    }

    public void config(View v) {
        if (inConfig) {
            lblRate.setVisibility(View.INVISIBLE);
            valRate.setVisibility(View.INVISIBLE);
            lblSize.setVisibility(View.INVISIBLE);
            valSize.setVisibility(View.INVISIBLE);
            imageView.setVisibility(View.VISIBLE);
            inConfig = false;
        } else {
            if (started) {
                started = false;
                startStopButton.setText("Start");
                //recordTask.cancel(true);
            }
            lblRate.setVisibility(View.VISIBLE);
            valRate.setVisibility(View.VISIBLE);
            lblSize.setVisibility(View.VISIBLE);
            valSize.setVisibility(View.VISIBLE);
            imageView.setVisibility(View.INVISIBLE);
            inConfig = true;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        started = false;
    }
}
