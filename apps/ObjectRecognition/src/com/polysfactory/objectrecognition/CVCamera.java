package com.polysfactory.objectrecognition;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.AssetManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.opencv.camera.NativePreviewer;
import com.opencv.camera.NativeProcessor;
import com.opencv.camera.NativeProcessor.PoolCallback;
import com.opencv.jni.image_pool;
import com.opencv.opengl.GL2CameraViewer;
import com.polysfactory.objectrecognition.jni.Processor;

public class CVCamera extends Activity {

    private static final String MENU_STRING = "Detect";

    private static final int DIALOG_TUTORIAL_SURF = 4;

    void toasts(int id) {
        switch (id) {
        case DIALOG_TUTORIAL_SURF:
            Toast.makeText(this, "Detecting and Displaying SURF features", Toast.LENGTH_LONG).show();
            break;
        default:
            break;
        }

    }

    MediaPlayer player;

    /**
     * Avoid that the screen get's turned off by the system.
     */
    public void disableScreenTurnOff() {
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    /**
     * Set's the orientation to landscape, as this is needed by AndAR.
     */
    public void setOrientation() {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
    }

    /**
     * Maximize the application.
     */
    public void setFullscreen() {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }

    public void setNoTitle() {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(MENU_STRING);
        return true;
    }

    private NativePreviewer mPreview;

    private GL2CameraViewer glview;

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        LinkedList<PoolCallback> defaultcallbackstack = new LinkedList<PoolCallback>();
        defaultcallbackstack.addFirst(glview.getDrawCallback());

        if (item.getTitle().equals(MENU_STRING)) {
            defaultcallbackstack.addFirst(new SURFProcessor());
            toasts(DIALOG_TUTORIAL_SURF);
        }

        mPreview.addCallbackStack(defaultcallbackstack);

        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        try {
            Log.d("MyCvCamera", "copy keypoints file");
            copy2Local();
        } catch (IOException e) {
            e.printStackTrace();
        }

        processor = new Processor();

        String base = "/data/data/com.theveganrobot.cvcamera/files/";
        String[] files = {"arashi.jpeg.txt", "horumon.jpeg.txt", "michael.jpeg.txt", "mrchildren.jpeg.txt", "ozaki.jpeg.txt" };

        for (int i = 0; i < files.length; i++) {
            String filename = base + files[i];

            if (!processor.loadDescription(filename)) {
                Log.v("Processor", "load desc error: " + filename);
            }
        }

        setFullscreen();
        disableScreenTurnOff();

        FrameLayout frame = new FrameLayout(this);

        // Create our Preview view and set it as the content of our activity.
        mPreview = new NativePreviewer(getApplication(), 640, 480);

        LayoutParams params = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        params.height = getWindowManager().getDefaultDisplay().getHeight();
        params.width = (int) (params.height * 4.0 / 2.88);

        LinearLayout vidlay = new LinearLayout(getApplication());

        vidlay.setGravity(Gravity.CENTER);
        vidlay.addView(mPreview, params);
        frame.addView(vidlay);

        // make the glview overlay ontop of video preview
        mPreview.setZOrderMediaOverlay(false);

        glview = new GL2CameraViewer(getApplication(), false, 0, 0);
        glview.setZOrderMediaOverlay(true);

        LinearLayout gllay = new LinearLayout(getApplication());

        gllay.setGravity(Gravity.CENTER);
        gllay.addView(glview, params);
        frame.addView(gllay);

        setContentView(frame);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (player != null) {
            player.release();
            player = null;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // clears the callback stack
        mPreview.onPause();
        glview.onPause();

    }

    @Override
    protected void onResume() {
        super.onResume();
        glview.onResume();
        mPreview.setParamsFromPrefs(getApplicationContext());
        // add an initiall callback stack to the preview on resume...
        // this one will just draw the frames to opengl
        LinkedList<NativeProcessor.PoolCallback> cbstack = new LinkedList<PoolCallback>();
        cbstack.add(glview.getDrawCallback());
        mPreview.addCallbackStack(cbstack);
        mPreview.onResume();

    }

    // final processor so taht these processor callbacks can access it
    Processor processor;

    class SURFProcessor implements NativeProcessor.PoolCallback {

        int objId = -1;

        @Override
        public void process(int idx, image_pool pool, long timestamp, NativeProcessor nativeProcessor) {
            int newObjId = processor.detectAndDrawFeatures(idx, pool);
            if (newObjId == objId && (player != null && player.isPlaying())) {
                Log.v("Processor", "objId is same. we continue to play the sound");
                return;
            }
            objId = newObjId;
            Log.v("Processor", "this id is " + objId);
            // TODO
            int resId = -1;
            switch (objId) {
            case 0:
                resId = R.raw.arashi;
                break;
            case 1:
                resId = R.raw.horumon;
                break;
            case 2:
                resId = R.raw.jackson;
                break;
            case 3:
                resId = R.raw.hanabi;
                break;
            case 4:
                resId = R.raw.ozaki;
                break;
            default:
                break;
            }
            if (resId != -1 && objId >= 0) {
                if (player != null) {
                    player.release();
                    player = null;
                }

                try {
                    player = MediaPlayer.create(CVCamera.this, resId);
                    player.start();
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                } catch (IllegalStateException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * assets以下のファイルをアプリのfilesディレクトリにコピーする<br>
     * @throws IOException IO例外
     */
    private void copy2Local() throws IOException {
        // assetsから読み込み、出力する
        String[] fileList = getResources().getAssets().list("keypoints");
        if (fileList == null || fileList.length == 0) {
            return;
        }
        AssetManager as = getResources().getAssets();
        InputStream input = null;
        FileOutputStream fos = null;
        BufferedOutputStream bos = null;

        for (String file : fileList) {
            String outFileName = "keypoints" + "/" + file;
            Log.v("MyCvCamera", "copy file:" + outFileName);
            input = as.open(outFileName);
            fos = openFileOutput(file, Context.MODE_WORLD_READABLE);
            bos = new BufferedOutputStream(fos);

            int DEFAULT_BUFFER_SIZE = 1024 * 4;

            byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
            int n = 0;
            while (-1 != (n = input.read(buffer))) {
                fos.write(buffer, 0, n);
            }
            bos.close();
            fos.close();
            input.close();
        }
    }

}
