package com.dx.anonymousmessenger.ui.view.single_activity;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.transition.Explode;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.FloatRange;
import androidx.core.content.ContextCompat;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;

import com.alexvasilkov.gestures.views.GestureImageView;
import com.dx.anonymousmessenger.DxApplication;
import com.dx.anonymousmessenger.R;
import com.dx.anonymousmessenger.db.DbHelper;
import com.dx.anonymousmessenger.file.FileHelper;
import com.dx.anonymousmessenger.messages.MessageSender;
import com.dx.anonymousmessenger.messages.QuotedUserMessage;
import com.dx.anonymousmessenger.ui.custom.FlickDismissLayout;
import com.dx.anonymousmessenger.ui.custom.FlickGestureListener;
import com.dx.anonymousmessenger.ui.view.DxActivity;
import com.dx.anonymousmessenger.util.Utils;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.ByteArrayOutputStream;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.Objects;

import static java.util.Objects.requireNonNull;

public class PictureViewerActivity extends DxActivity implements FlickGestureListener.GestureCallbacks {

    private Drawable activityBackgroundDrawable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        try{
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        }catch (Exception ignored){}
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        getWindow().requestFeature(Window.FEATURE_SWIPE_TO_DISMISS);
        getWindow().setExitTransition(new Explode());
        setContentView(R.layout.activity_picture_viewer);


        try{
            setBackEnabled(true);
            if(getIntent().getStringExtra("address")==null || Objects.equals(getIntent().getStringExtra("address"), "")){
                setTitle(R.string.picure_view);
            }else{
                setTitle(Objects.equals(getIntent().getStringExtra("nickname"), "") ? (Objects.equals(getIntent().getStringExtra("address"),
                        ((DxApplication) getApplication()).getHostname()) ?
                        getString(R.string.you)
                        :getIntent().getStringExtra("address"))
                    : (Objects.equals(getIntent().getStringExtra("nickname"), ((DxApplication) getApplication()).getAccount().getNickname()) ?
                        getString(R.string.you)
                        :getIntent().getStringExtra("nickname")));
                if(getIntent().getLongExtra("time",0)!=0){
                    setSubtitle(Utils.formatDateTime(getIntent().getLongExtra("time",0)));
                }
            }
        }catch (Exception ignored){}

        animateDimmingOnEntry();
        GestureImageView imageView = findViewById(R.id.img_to_send);
        FlickDismissLayout fdl = findViewById(R.id.flick);
        FlickGestureListener flickListener = createFlickGestureListener(this);
        flickListener.setContentHeightProvider(new FlickGestureListener.ContentHeightProvider() {
            @Override
            public int getContentHeightForDismissAnimation() {
                return (int) imageView.getMaxHeight();
            }

            @Override
            public int getContentHeightForCalculatingThreshold() {
                return (int) imageView.getMaxHeight();
            }
        });
        flickListener.setOnGestureIntercepter((deltaY) -> {
            return false;
        });
        fdl.setFlickGestureListener(flickListener);

        // if image is from encrypted app storage
        if(getIntent().getBooleanExtra("appData",false)){
            new Thread(()->{
                Bitmap image;
                try{
                    byte[] file = FileHelper.getFile(getIntent().getStringExtra("path"),(DxApplication) getApplication());
                    if(file==null){
                        return;
                    }
                    image = BitmapFactory.decodeByteArray(file, 0, file.length);
                    Display display = getWindowManager().getDefaultDisplay();
                    Point size = new Point();
                    display.getSize(size);
                    int width = size.x;
                    int height = size.y;

                    if(image.getHeight()>height && image.getWidth()>width){
                        BitmapFactory.Options options = new BitmapFactory.Options();
                        options.inSampleSize = 2;
                        image = BitmapFactory.decodeByteArray(file, 0, file.length,options);
                    }


                if(image==null){
                    return;
                }
                Bitmap finalImage = image;
                new Handler(Looper.getMainLooper()).post(()->{

                    imageView.getController().getStateController();
                    imageView.getController().getSettings().setRotationEnabled(true);
                    imageView.getController().getSettings().setRestrictRotation(true);
                    try{
                        imageView.setImageBitmap(finalImage);
                    }catch (Exception ignored) {}
                    TextView textCaption = findViewById(R.id.txt_caption_view);
                    imageView.setOnClickListener(v -> {
                        if(textCaption.getVisibility()==View.VISIBLE){
                            findViewById(R.id.layout_controls).setVisibility(View.GONE);
                            textCaption.setVisibility(View.GONE);
                        }else{
                            findViewById(R.id.layout_controls).setVisibility(View.VISIBLE);
                            textCaption.setVisibility(View.VISIBLE);
                        }
                    });
                    findViewById(R.id.btn_save).setOnClickListener(v ->{
                        saveWithAlert();
                    });
                    findViewById(R.id.layout_controls).setVisibility(View.VISIBLE);
                    if(getIntent().getStringExtra("message")==null || Objects.equals(getIntent().getStringExtra("message"), "")){
                        return;
                    }
                    textCaption.setVisibility(View.VISIBLE);
                    textCaption.setText(getIntent().getStringExtra("message"));
                });
                }catch (Exception e){
                    e.printStackTrace();
                }
            }).start();
        }
        // else image is from local shared storage and the user might send it
        else{
            new Thread(()->{
                Bitmap image;
                try{
                    image = Utils.rotateBitmap(BitmapFactory.decodeFile(getIntent().getStringExtra("path")),getIntent().getStringExtra("path"));
                }catch (Exception e){
                    e.printStackTrace();
                    return;
                }
                if(image==null){
                    return;
                }
                new Handler(Looper.getMainLooper()).post(()->{
//                img.getController().getSettings().setRotationEnabled(true);
                    imageView.setImageBitmap(image);
                });
            }).start();

            TextInputLayout textInputLayout = findViewById(R.id.txt_layout_caption);
            textInputLayout.setVisibility(View.VISIBLE);
            TextInputEditText msg = findViewById(R.id.txt_caption);
            InputMethodManager imm = requireNonNull(
                    ContextCompat.getSystemService(this, InputMethodManager.class));
            imm.hideSoftInputFromWindow(msg.getWindowToken(), 0);
            FloatingActionButton fabSendMedia = findViewById(R.id.btn_send_media);
            fabSendMedia.setVisibility(View.VISIBLE);
            fabSendMedia.setOnClickListener(v -> {
                msg.setEnabled(false);
                new Thread(()->{
                    DxApplication app = (DxApplication) getApplication();
                    //get time ready
                    long time = new Date().getTime();
                    //save bytes encrypted into a file and get path
                    String filename = String.valueOf(time);
                    String path = null;
                    try {
                        Bitmap image = Utils.rotateBitmap(BitmapFactory.decodeFile(getIntent().getStringExtra("path")),getIntent().getStringExtra("path"));
                        if(image==null){
                            return;
                        }
                        ByteArrayOutputStream stream = new ByteArrayOutputStream();
                        image.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                        byte[] byteArray = stream.toByteArray();
                        path = FileHelper.saveFile(byteArray,app,filename);
                    } catch (NoSuchAlgorithmException e) {
                        e.printStackTrace();
                    }
                    if(path==null){
                        return;
                    }
                    String txtMsg = "";
                    if(msg.getText()!=null){
                        txtMsg = msg.getText().toString();
                    }
                    final String fullAddress = DbHelper.getFullAddress(getIntent().getStringExtra(
                            "address"),
                            (DxApplication) getApplication());
                    if(fullAddress == null){
                        return;
                    }
                    //save metadata in encrypted database with reference to encrypted file
                    QuotedUserMessage qum = new QuotedUserMessage("","",app.getHostname(),txtMsg,
                            app.getAccount().getNickname(),time,false,fullAddress,false,filename,path,"image");
                    //send message and get received status
                    MessageSender.sendMediaMessage(qum,app,fullAddress);
                }).start();
                finish();
            });
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        if(getIntent().getBooleanExtra("appData",false)){
            getMenuInflater().inflate(R.menu.picture_menu, menu);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.save_picture) {
            saveWithAlert();
        }
        return super.onOptionsItemSelected(item);
    }

    private void saveWithAlert() {
        new AlertDialog.Builder(this,R.style.AppAlertDialog)
            .setTitle(R.string.save_to_storage)
            .setMessage(R.string.save_to_storage_explain)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setPositiveButton(android.R.string.yes, (dialog, whichButton) -> new Thread(this::saveToStorage
            ).start())
            .setNegativeButton(android.R.string.no, null).show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return super.onSupportNavigateUp();
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    private void saveToStorage(){
        try{
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[] { Manifest.permission.WRITE_EXTERNAL_STORAGE }, 1);
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                    saveToStorage();
                }
                return;
            }
            if(getIntent().getBooleanExtra("appData",false)){
                Bitmap image;
                byte[] file = FileHelper.getFile(getIntent().getStringExtra("path"), (DxApplication) getApplication());
                if(file==null){
                    return;
                }
                image = BitmapFactory.decodeByteArray(file, 0, file.length);
                if(image == null){
                    return;
                }
                MediaStore.Images.Media.insertImage(getContentResolver(), image, getString(R.string.anonymous_messenger)+Utils.formatDateTime(getIntent().getLongExtra("time",new Date().getTime())) , "");
                runOnUiThread(()-> Snackbar.make(findViewById(R.id.img_to_send),R.string.saved_to_storage,Snackbar.LENGTH_LONG).show());
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private void animateDimmingOnEntry() {
        FrameLayout root_layout = findViewById(R.id.imageviewer_root);
        if(root_layout==null || root_layout.getBackground()==null){
            return;
        }
        Drawable back = root_layout.getBackground().mutate();
        this.activityBackgroundDrawable = back;
        back = this.activityBackgroundDrawable;

        root_layout.setBackground(back);
        ValueAnimator var1 = ObjectAnimator.ofFloat(0.5F, 0.0F);
        var1.setDuration(200L);
        var1.setInterpolator((TimeInterpolator)(new FastOutSlowInInterpolator()));
        var1.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                updateBackgroundDimmingAlpha((Float) animation.getAnimatedValue());
            }
        });
//        var1.addUpdateListener((ValueAnimator.AnimatorUpdateListener)(new ImageViewerActivity$animateDimmingOnEntry$$inlined$apply$lambda$1(this)));
        var1.start();
    }

    /**
     * @param targetTransparencyFactor 1f for maximum transparency. 0f for none.
     */
    private void updateBackgroundDimmingAlpha(@FloatRange(from = 0, to = 1) float targetTransparencyFactor) {
        // Increase dimming exponentially so that the background is fully transparent while the image has been moved by half.
        float dimming = 1f - Math.min(1f, targetTransparencyFactor * 2);
        activityBackgroundDrawable.setAlpha((int) (dimming * 255));
    }

    public FlickGestureListener createFlickGestureListener(FlickGestureListener.GestureCallbacks wrappedGestureCallbacks) {
        FlickGestureListener flickListener = new FlickGestureListener(ViewConfiguration.get(getApplicationContext()));
        flickListener.setFlickThresholdSlop(FlickGestureListener.DEFAULT_FLICK_THRESHOLD);
        flickListener.setGestureCallbacks(new FlickGestureListener.GestureCallbacks() {
            @Override
            public void onFlickDismissEnd(long flickAnimationDuration) {
                wrappedGestureCallbacks.onFlickDismissEnd(flickAnimationDuration);
            }

            @Override
            public void onMoveMedia(float moveRatio) {
                wrappedGestureCallbacks.onMoveMedia(moveRatio);

                boolean isImageBeingMoved = moveRatio != 0f;
//                titleDescriptionView.setVisibility(!isImageBeingMoved ? View.VISIBLE : View.INVISIBLE);

                boolean showDimming = !isImageBeingMoved ;//&& titleDescriptionView.streamDimmingRequiredForTitleAndDescription().getValue();
//                imageDimmingView.setVisibility(showDimming ? View.VISIBLE : View.GONE);
            }
        });
        return flickListener;
    }

    @Override
    public void onFlickDismissEnd(long flickAnimationDuration) {
        finish();
    }

    @Override
    public void onMoveMedia(float moveRatio) {
        updateBackgroundDimmingAlpha(Math.abs(moveRatio));
    }
}