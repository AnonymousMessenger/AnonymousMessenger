package com.dx.anonymousmessenger.ui.view.single_activity;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.AlertDialog;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.view.Display;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;

import androidx.annotation.FloatRange;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.content.ContextCompat;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;
import androidx.transition.AutoTransition;
import androidx.transition.TransitionManager;

import com.alexvasilkov.gestures.Settings;
import com.alexvasilkov.gestures.commons.CropAreaView;
import com.alexvasilkov.gestures.views.GestureImageView;
import com.dx.anonymousmessenger.DxApplication;
import com.dx.anonymousmessenger.R;
import com.dx.anonymousmessenger.db.DbHelper;
import com.dx.anonymousmessenger.file.FileHelper;
import com.dx.anonymousmessenger.messages.MessageSender;
import com.dx.anonymousmessenger.messages.QuotedUserMessage;
import com.dx.anonymousmessenger.ui.custom.FlickDismissLayout;
import com.dx.anonymousmessenger.ui.custom.FlickGestureListener;
import com.dx.anonymousmessenger.ui.custom.GestureTextView;
import com.dx.anonymousmessenger.ui.view.DxActivity;
import com.dx.anonymousmessenger.util.Utils;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.Objects;

public class PictureViewerActivity extends DxActivity implements FlickGestureListener.GestureCallbacks {

    private Drawable activityBackgroundDrawable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
//        setTheme(R.style.AppTheme_TransparentFullscreen);
        super.onCreate(savedInstanceState);
        try{
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        }catch (Exception ignored){}
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        getWindow().requestFeature(Window.FEATURE_SWIPE_TO_DISMISS);
        getWindow().requestFeature(Window.FEATURE_ACTIVITY_TRANSITIONS);
        getWindow().setExitTransition(getWindow().getSharedElementExitTransition());
        setContentView(R.layout.activity_picture_viewer);

//        getWindow().getSharedElementExitTransition()

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
        imageView.setTransitionName("picture");
        FlickDismissLayout fdl = findViewById(R.id.flick);
        FlickGestureListener flickListener = createFlickGestureListener(this);
        flickListener.setContentHeightProvider(new FlickGestureListener.ContentHeightProvider() {
            @Override
            public int getContentHeightForDismissAnimation() {
                return imageView.getMaxHeight();
            }

            @Override
            public int getContentHeightForCalculatingThreshold() {
                return imageView.getMaxHeight();
            }
        });
        flickListener.setOnGestureIntercepter((deltaY) -> false);
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
                    imageView.getController().getSettings().setMaxZoom(3f)
                    .setDoubleTapZoom(2f);
                    try{
                        imageView.setImageBitmap(finalImage);
                    }catch (Exception ignored) {}
                    GestureTextView textCaption = findViewById(R.id.txt_caption_view);
                    textCaption.setOnClickListener(v -> toggleUiVisibility(textCaption));
                    imageView.setOnClickListener(v -> toggleUiVisibility(textCaption));
                    findViewById(R.id.btn_save).setOnClickListener(v -> saveWithAlert());
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
        // if we have a uri then this is a profile image candidate from storage
        else if(getIntent().getStringExtra("uri")!=null && !Objects.equals(getIntent().getStringExtra("uri"), "")){
            new Thread(()->{
                Bitmap image;
                try{
                    InputStream is = FileHelper.getInputStreamFromUri(Uri.parse(getIntent().getStringExtra("uri")),this);

                    image = BitmapFactory.decodeStream(is);
                    if(image.getHeight()>500 && image.getWidth()>500){
                        BitmapFactory.Options options = new BitmapFactory.Options();
                        options.inSampleSize = 4;
                        is = FileHelper.getInputStreamFromUri(Uri.parse(getIntent().getStringExtra("uri")),this);
                        image = BitmapFactory.decodeStream(is,null,options);
                    }
                    image = Utils.rotateBitmap(image, is);
                }catch (Exception e){
                    e.printStackTrace();
                    return;
                }
                if(image==null){
                    return;
                }
                Bitmap finalImage = image;
                new Handler(Looper.getMainLooper()).post(()->{
//                    imageView.getController().getSettings().setZoomEnabled(false);
//                    imageView.getController().getSettings().setDoubleTapEnabled(false);
                    imageView.getController().getSettings()
                            .setFitMethod(Settings.Fit.OUTSIDE)
                            .setFillViewport(true)
                            .setMovementArea(500, 500)
                            .setRotationEnabled(true);
                    imageView.setImageBitmap(finalImage);
                    CropAreaView cropView = findViewById(R.id.image_crop_area);
                    cropView.setVisibility(View.VISIBLE);
                    cropView.setImageView(imageView);
                    cropView.setAspect(1f);
                    cropView.setRounded(true);
                    cropView.update(true);
                    findViewById(R.id.layout_crop_controls).setVisibility(View.VISIBLE);
                    FloatingActionButton done = findViewById(R.id.btn_done);
                    done.setOnClickListener(v -> {
                        try{
                            new Thread(() -> {
                                // get cropped bitmap in a bytearray
                                Bitmap cropped = imageView.crop();
                                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                                if (cropped != null) {
                                    cropped.compress(Bitmap.CompressFormat.PNG, 75, stream);
                                    cropped.recycle();
                                }
                                String path = null;
                                DxApplication app = ((DxApplication)getApplication());
                                //save cropped bitmap in an encrypted file
                                try {
                                    path = FileHelper.saveFile(stream.toByteArray(),app,new Date().getTime()+"profile image");
                                } catch (NoSuchAlgorithmException e) {
                                    e.printStackTrace();
                                }
                                //add reference in the account table
                                if(path==null){
                                    return;
                                }
                                // delete pic from db/file
                                String oldPath = app.getAccount().getProfileImagePath();
                                if(oldPath!=null && !oldPath.equals("")){
                                    FileHelper.deleteFile(oldPath,app);
                                }

                                app.getAccount().changeProfileImage("",app);
                                app.getAccount().changeProfileImage(path,app);
                                //finish activity
                                finish();
                                //send to all
                                QuotedUserMessage qum = new QuotedUserMessage("","",app.getHostname(),"",
                                        app.getAccount().getNickname(),new Date().getTime(),false,"",false,"profile_image",path,"image");
                                MessageSender.sendMediaMessageToAll(qum,app,"",false, true);
                            }).start();
                        }catch (Exception e){
                            e.printStackTrace();
                        }
                    });
                });
            }).start();
        }
        // else image is from local shared storage and the user might send it
        else{
            new Thread(()->{
                Bitmap image;
                try{
                    image = Utils.rotateBitmap(BitmapFactory.decodeFile(getIntent().getStringExtra("path")),getIntent().getStringExtra("path"));
                    Display display = getWindowManager().getDefaultDisplay();
                    Point size = new Point();
                    display.getSize(size);
                    int width = size.x;
                    int height = size.y;

                    if(image.getHeight()>height && image.getWidth()>width){
                        BitmapFactory.Options options = new BitmapFactory.Options();
                        options.inSampleSize = 2;
                        image = Utils.rotateBitmap(BitmapFactory.decodeFile(getIntent().getStringExtra("path"),options),getIntent().getStringExtra("path"));
                    }
                }catch (Exception e){
                    e.printStackTrace();
                    return;
                }
                if(image==null){
                    return;
                }
                Bitmap finalImage = image;
                new Handler(Looper.getMainLooper()).post(()->{
//                img.getController().getSettings().setRotationEnabled(true);
                    imageView.setImageBitmap(finalImage);
                });
            }).start();

            TextInputLayout textInputLayout = findViewById(R.id.txt_layout_caption);
            textInputLayout.setVisibility(View.VISIBLE);
            findViewById(R.id.layout_send_controls).setVisibility(View.VISIBLE);
            TextInputEditText msg = findViewById(R.id.txt_caption);
            msg.setOnFocusChangeListener((v, hasFocus) -> {
                FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT);
                if(hasFocus){
                    params.gravity = Gravity.TOP;
                }else{
                    params.gravity = Gravity.CENTER;
                }
                animateParamsChange(textInputLayout,params,500);
            });
//            InputMethodManager imm = requireNonNull(ContextCompat.getSystemService(this, InputMethodManager.class));
//            imm.hideSoftInputFromWindow(msg.getWindowToken(), 0);
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

    private void toggleUiVisibility(GestureTextView textCaption) {
        if(findViewById(R.id.layout_controls).getVisibility()==View.VISIBLE){
            findViewById(R.id.layout_controls).setVisibility(View.GONE);
            textCaption.setVisibility(View.GONE);
        }else{
            findViewById(R.id.layout_controls).setVisibility(View.VISIBLE);
            if(!textCaption.getText().toString().equals("")){
                textCaption.setVisibility(View.VISIBLE);
            }
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
    protected void onPause() {
        InputMethodManager imm = ContextCompat.getSystemService(this, InputMethodManager.class);
        if (imm != null) {
            imm.hideSoftInputFromWindow(findViewById(R.id.txt_caption).getWindowToken(), 0);
        }
        super.onPause();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return super.onSupportNavigateUp();
    }

    @Override
    public void onBackPressed() {
//        finish();
        finishAfterTransition();
    }

    private void saveToStorage(){
        try{
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    requestPermissions(new String[] { Manifest.permission.WRITE_EXTERNAL_STORAGE }, 1);
                }
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
                String photoUriStr = MediaStore.Images.Media.insertImage(getContentResolver(), image, getString(R.string.anonymous_messenger)+Utils.formatDateTime(getIntent().getLongExtra("time",new Date().getTime())) , "");
                Uri photoUri = Uri.parse(photoUriStr);

                // add datetime
                long now = System.currentTimeMillis() / 1000;
                ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.DATE_ADDED, now);
                values.put(MediaStore.Images.Media.DATE_MODIFIED, now);
                values.put(MediaStore.Images.Media.DATE_TAKEN, now);
                getContentResolver().update(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        values,
                        MediaStore.Images.Media._ID + "=?",
                        new String [] { ContentUris.parseId(photoUri) + "" });

                // call media scanner to refresh gallery
                Intent scanFileIntent = new Intent(
                        Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, photoUri);
                sendBroadcast(scanFileIntent);
                runOnUiThread(()-> Snackbar.make(findViewById(R.id.img_to_send),R.string.saved_to_storage,Snackbar.LENGTH_LONG).show());
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private void animateDimmingOnEntry() {
        View root_layout = findViewById(R.id.imageviewer_root);
        if(root_layout==null || root_layout.getBackground()==null){
            return;
        }
        Drawable back = root_layout.getBackground().mutate();
        this.activityBackgroundDrawable = back;
        back = this.activityBackgroundDrawable;

        root_layout.setBackground(back);
        ValueAnimator var1 = ObjectAnimator.ofFloat(0.5F, 0.0F);
        var1.setDuration(200L);
        var1.setInterpolator(new FastOutSlowInInterpolator());
        var1.addUpdateListener(animation -> updateBackgroundDimmingAlpha((Float) animation.getAnimatedValue()));
//        var1.addUpdateListener((ValueAnimator.AnimatorUpdateListener)(new ImageViewerActivity$animateDimmingOnEntry$$inlined$apply$lambda$1(this)));
        var1.start();
    }

    /**
     * @param targetTransparencyFactor 1f for maximum transparency. 0f for none.
     */
    private void updateBackgroundDimmingAlpha(@FloatRange(from = 0, to = 1) float targetTransparencyFactor) {
        // Increase dimming exponentially so that the background is fully transparent while the image has been moved by half.
        float dimming = 1f - Math.min(1f, targetTransparencyFactor * 2);
        if(activityBackgroundDrawable!=null){
            activityBackgroundDrawable.setAlpha((int) (dimming * 255));
        }
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

    public static void animateConstraintLayout(ConstraintLayout constraintLayout, ConstraintSet set, long duration) {
        AutoTransition trans = new AutoTransition();
        trans.setDuration(duration);
        trans.setInterpolator(new AccelerateDecelerateInterpolator());

        TransitionManager.beginDelayedTransition(constraintLayout, trans);
        set.applyTo(constraintLayout);
    }

    public static void animateParamsChange(TextInputLayout textInputLayout, ViewGroup.LayoutParams params, long duration) {

        AutoTransition trans = new AutoTransition();
        trans.setDuration(duration);
        trans.setInterpolator(new AccelerateDecelerateInterpolator());

        TransitionManager.beginDelayedTransition(textInputLayout, trans);
        textInputLayout.setLayoutParams(params);
    }
}