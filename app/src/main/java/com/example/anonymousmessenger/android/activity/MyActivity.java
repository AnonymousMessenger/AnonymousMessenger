//package com.example.anonymousmessenger.android.activity;
//
//import android.content.Intent;
//import android.transition.Transition;
//import android.view.Window;
//import android.widget.CheckBox;
//
//import org.briarproject.bramble.api.nullsafety.MethodsNotNullByDefault;
//import org.briarproject.bramble.api.nullsafety.ParametersNotNullByDefault;
//import org.briarproject.briar.R;
//import org.briarproject.briar.android.account.UnlockActivity;
//import org.briarproject.briar.android.controller.BriarController;
//import org.briarproject.briar.android.controller.DbController;
//import org.briarproject.briar.android.controller.handler.UiResultHandler;
//import org.briarproject.briar.android.login.StartupActivity;
//import org.briarproject.briar.android.logout.ExitActivity;
//import org.briarproject.briar.api.android.LockManager;
//
//import java.util.logging.Logger;
//
//import javax.annotation.Nullable;
//import javax.inject.Inject;
//
//import androidx.annotation.RequiresApi;
//import androidx.appcompat.app.ActionBar;
//import androidx.appcompat.app.AlertDialog;
//import androidx.appcompat.widget.Toolbar;
//
//import static android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK;
//import static android.content.Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS;
//import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
//import static android.content.Intent.FLAG_ACTIVITY_NO_ANIMATION;
//import static android.os.Build.VERSION.SDK_INT;
//import static java.util.logging.Level.INFO;
//import static com.example.anonymousmessenger.android.activity.RequestCodes.REQUEST_DOZE_WHITELISTING;
//import static com.example.anonymousmessenger.android.activity.RequestCodes.REQUEST_PASSWORD;
//import static com.example.anonymousmessenger.android.activity.RequestCodes.REQUEST_UNLOCK;
//import static com.example.anonymousmessenger.android.util.UiUtils.excludeSystemUi;
//import static com.example.anonymousmessenger.android.util.UiUtils.getDozeWhitelistingIntent;
//import static com.example.anonymousmessenger.android.util.UiUtils.isSamsung7;
//
//@MethodsNotNullByDefault
//@ParametersNotNullByDefault
//public abstract class MyActivity extends BaseActivity {
//
//    public static final String GROUP_ID = "briar.GROUP_ID";
//    public static final String GROUP_NAME = "briar.GROUP_NAME";
//
//    private static final Logger LOG =
//            Logger.getLogger(BriarActivity.class.getName());
//
//    @Inject
//    BriarController briarController;
//    @Deprecated
//    @Inject
//    DbController dbController;
//    @Inject
//    protected LockManager lockManager;
//
//    @Override
//    public void onStart() {
//        super.onStart();
//        lockManager.onActivityStart();
//    }
//
//    @Override
//    protected void onActivityResult(int request, int result,
//                                    @Nullable Intent data) {
//        super.onActivityResult(request, result, data);
//        if (request == REQUEST_PASSWORD) {
//            // Recreate the activity so any DB tasks that failed before
//            // signing in can be retried
//            if (result == RESULT_OK) {
//                if (LOG.isLoggable(INFO)) {
//                    LOG.info("Recreating " + getClass().getSimpleName()
//                            + " after signing in");
//                }
//                recreate();
//            }
//        } else if (request == REQUEST_UNLOCK && result != RESULT_OK) {
//            // We arrive here, if the user presses 'back'
//            // in the Keyguard unlock screen, because UnlockActivity finishes.
//            // If we don't finish here, isFinishing will be false in onResume()
//            // and we launch a new UnlockActivity causing a loop.
//            supportFinishAfterTransition();
//            // If the result is OK, we don't need to do anything here
//            // and can resume normally.
//        }
//    }
//
//    @Override
//    public void onResume() {
//        super.onResume();
//        if (!briarController.accountSignedIn() && !isFinishing()) {
//            // Also check that the activity isn't finishing already.
//            // This is possible if we finished in onActivityResult().
//            // Launching another StartupActivity would cause a loop.
//            LOG.info("Not signed in, launching StartupActivity");
//            Intent i = new Intent(this, StartupActivity.class);
//            startActivityForResult(i, REQUEST_PASSWORD);
//        } else if (lockManager.isLocked() && !isFinishing()) {
//            // Also check that the activity isn't finishing already.
//            // This is possible if we finished in onActivityResult().
//            // Launching another UnlockActivity would cause a loop.
//            LOG.info("Locked, launching UnlockActivity");
//            Intent i = new Intent(this, UnlockActivity.class);
//            startActivityForResult(i, REQUEST_UNLOCK);
//        } else if (SDK_INT >= 23) {
//            briarController.hasDozed(new UiResultHandler<Boolean>(this) {
//                @Override
//                public void onResultUi(Boolean result) {
//                    if (result) {
//                        showDozeDialog(getString(R.string.warning_dozed,
//                                getString(R.string.app_name)));
//                    }
//                }
//            });
//        }
//    }
//
//    @Override
//    protected void onStop() {
//        super.onStop();
//        lockManager.onActivityStop();
//    }
//
//    protected boolean signedIn() {
//        return briarController.accountSignedIn();
//    }
//
//    /**
//     * Sets the transition animations.
//     * @param enterTransition used to move views into initial positions
//     * @param exitTransition used to move views out when starting a <b>new</b> activity.
//     * @param returnTransition used when window is closing, because the activity is finishing.
//     */
//    @RequiresApi(api = 21)
//    public void setSceneTransitionAnimation(
//            @Nullable Transition enterTransition,
//            @Nullable Transition exitTransition,
//            @Nullable Transition returnTransition) {
//        // workaround for #1007
//        if (isSamsung7()) {
//            return;
//        }
//        if (enterTransition != null) excludeSystemUi(enterTransition);
//        if (exitTransition != null) excludeSystemUi(exitTransition);
//        if (returnTransition != null) excludeSystemUi(returnTransition);
//        Window window = getWindow();
//        window.setEnterTransition(enterTransition);
//        window.setExitTransition(exitTransition);
//        window.setReturnTransition(returnTransition);
//    }
//
//    /**
//     * This should be called after the content view has been added in onCreate()
//     *
//     * @param ownLayout true if the custom toolbar brings its own layout
//     * @return the Toolbar object or null if content view did not contain one
//     */
//    @Nullable
//    protected Toolbar setUpCustomToolbar(boolean ownLayout) {
//        // Custom Toolbar
//        Toolbar toolbar = findViewById(R.id.toolbar);
//        setSupportActionBar(toolbar);
//        ActionBar ab = getSupportActionBar();
//        if (ab != null) {
//            ab.setDisplayShowHomeEnabled(true);
//            ab.setDisplayHomeAsUpEnabled(true);
//            ab.setDisplayShowCustomEnabled(ownLayout);
//            ab.setDisplayShowTitleEnabled(!ownLayout);
//        }
//        return toolbar;
//    }
//
//    protected void showDozeDialog(String message) {
//        AlertDialog.Builder b =
//                new AlertDialog.Builder(this, R.style.BriarDialogTheme);
//        b.setMessage(message);
//        b.setView(R.layout.checkbox);
//        b.setPositiveButton(R.string.fix,
//                (dialog, which) -> {
//                    Intent i = getDozeWhitelistingIntent(BriarActivity.this);
//                    startActivityForResult(i, REQUEST_DOZE_WHITELISTING);
//                    dialog.dismiss();
//                });
//        b.setNegativeButton(R.string.cancel,
//                (dialog, which) -> dialog.dismiss());
//        b.setOnDismissListener(dialog -> {
//            CheckBox checkBox =
//                    ((AlertDialog) dialog).findViewById(R.id.checkbox);
//            if (checkBox.isChecked())
//                briarController.doNotAskAgainForDozeWhiteListing();
//        });
//        b.show();
//    }
//
//    protected void signOut(boolean removeFromRecentApps,
//                           boolean deleteAccount) {
//        if (briarController.accountSignedIn()) {
//            // Don't use UiResultHandler because we want the result even if
//            // this activity has been destroyed
//            briarController.signOut(result -> runOnUiThread(
//                    () -> exit(removeFromRecentApps)), deleteAccount);
//        } else {
//            if (deleteAccount) briarController.deleteAccount();
//            exit(removeFromRecentApps);
//        }
//    }
//
//    private void exit(boolean removeFromRecentApps) {
//        if (removeFromRecentApps) startExitActivity();
//        else finishAndExit();
//    }
//
//    private void startExitActivity() {
//        Intent i = new Intent(this, ExitActivity.class);
//        i.addFlags(FLAG_ACTIVITY_NEW_TASK
//                | FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
//                | FLAG_ACTIVITY_NO_ANIMATION
//                | FLAG_ACTIVITY_CLEAR_TASK);
//        startActivity(i);
//    }
//
//    private void finishAndExit() {
//        if (SDK_INT >= 21) finishAndRemoveTask();
//        else supportFinishAfterTransition();
//        LOG.info("Exiting");
//        System.exit(0);
//    }
//
//    @Deprecated
//    public void runOnDbThread(Runnable task) {
//        dbController.runOnDbThread(task);
//    }
//
//    @Deprecated
//    protected void finishOnUiThread() {
//        runOnUiThreadUnlessDestroyed(this::supportFinishAfterTransition);
//    }
//}
//
