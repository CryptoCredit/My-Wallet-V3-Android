package piuk.blockchain.android.ui.base;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.annotation.CallSuper;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager;
import io.reactivex.disposables.CompositeDisposable;
import piuk.blockchain.android.BuildConfig;
import piuk.blockchain.android.R;
import piuk.blockchain.android.data.access.AccessState;
import piuk.blockchain.android.data.rxjava.RxUtil;
import piuk.blockchain.android.util.ApplicationLifeCycle;
import piuk.blockchain.android.util.SSLVerifyUtil;
import piuk.blockchain.android.util.ViewUtils;

/**
 * A base Activity for all activities which need auth timeouts & screenshot prevention
 */
public class BaseAuthActivity extends AppCompatActivity {

    private AlertDialog mAlertDialog;
    private SSLVerifyUtil mSSLVerifyUtil = new SSLVerifyUtil(this);
    private static CompositeDisposable compositeDisposable;

    @CallSuper
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        if (!BuildConfig.DOGFOOD && !BuildConfig.DEBUG) {
            disallowScreenshots();
        }

        compositeDisposable = new CompositeDisposable();

        // Subscribe to SSL pinning events
        compositeDisposable.add(
                mSSLVerifyUtil.getSslPinningSubject()
                        .compose(RxUtil.applySchedulersToObservable())
                        .subscribe(sslEvent -> {
                                    switch (sslEvent) {
                                        case SERVER_DOWN:
                                            showAlertDialog(getString(R.string.ssl_no_connection), false);
                                            break;
                                        case PINNING_FAIL:
                                            showAlertDialog(getString(R.string.ssl_pinning_invalid), true);
                                            break;
                                        case NO_CONNECTION:
                                            showAlertDialog(getString(R.string.ssl_no_connection), false);
                                            break;
                                        case SUCCESS:
                                            // No-op
                                        default:
                                            // No-op
                                    }
                                },
                                Throwable::printStackTrace));
    }

    @CallSuper
    @Override
    protected void onResume() {
        super.onResume();
        stopLogoutTimer();
        ApplicationLifeCycle.getInstance().onActivityResumed();
    }

    @CallSuper
    @Override
    protected void onPause() {
        super.onPause();
        startLogoutTimer();
        ApplicationLifeCycle.getInstance().onActivityPaused();
    }

    @CallSuper
    @Override
    protected void onDestroy() {
        super.onDestroy();
        compositeDisposable.clear();
        if (mAlertDialog != null) {
            mAlertDialog.dismiss();
        }
    }

    /**
     * Displays a {@link Snackbar} on the current page with a clickable action.
     *
     * @param view          The root View of the current activity
     * @param message       The message to be displayed
     * @param length        The length of time for the Snackbar to be displayed
     * @param action        The action to be displayed
     * @param clickListener An OnClickListener to handle action callbacks
     */
    public void showSnackbar(View view, @StringRes int message, @ViewUtils.SnackbarLength int length, @StringRes int action, View.OnClickListener clickListener) {
        makeSnackbar(view, message, length).setAction(action, clickListener).show();
    }

    /**
     * Displays a {@link Snackbar} on the current page.
     *
     * @param view    The root View of the current activity
     * @param message The message to be displayed
     * @param length  The length of time for the Snackbar to be displayed
     */
    public void showSnackbar(View view, @StringRes int message, @ViewUtils.SnackbarLength int length) {
        makeSnackbar(view, message, length).show();
    }

    /**
     * Starts the logout timer. Override in an activity if timeout is not needed.
     */
    protected void startLogoutTimer() {
        AccessState.getInstance().startLogoutTimer(this);
    }

    private void stopLogoutTimer() {
        AccessState.getInstance().stopLogoutTimer(this);
    }

    /**
     * Override if you want a particular activity to be able to be screenshot.
     */
    protected void disallowScreenshots() {
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
    }

    private Snackbar makeSnackbar(View view, @StringRes int message, @ViewUtils.SnackbarLength int length) {
        return Snackbar.make(view, message, length);
    }

    private void showAlertDialog(final String message, final boolean forceExit) {
        if (mAlertDialog != null) mAlertDialog.dismiss();

        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AlertDialogStyle)
                .setMessage(message)
                .setCancelable(false);

        if (!forceExit) {
            builder.setPositiveButton(R.string.retry, (d, id) -> {
                // Retry
                mSSLVerifyUtil.validateSSL();
            });
        }

        builder.setNegativeButton(R.string.exit, (d, id) -> finish());

        mAlertDialog = builder.create();

        if (!isFinishing()) {
            mAlertDialog.show();
        }
    }
}
