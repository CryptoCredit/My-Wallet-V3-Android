package piuk.blockchain.android.data.notifications;

import android.util.Log;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;
import javax.inject.Inject;
import piuk.blockchain.android.injection.Injector;

public class InstanceIdService extends FirebaseInstanceIdService {

    private static final String TAG = InstanceIdService.class.getSimpleName();

    @Inject protected NotificationTokenManager notificationTokenManager;

    {
        Injector.getInstance().getAppComponent().inject(this);
    }

    public InstanceIdService() {
        Log.d(TAG, "InstanceIdService: constructor instantiated");
    }

    @Override
    public void onTokenRefresh() {
        // Get updated InstanceID token.
        String refreshedToken = FirebaseInstanceId.getInstance().getToken();
        Log.d(TAG, "Refreshed token: " + refreshedToken);

        if (refreshedToken != null) {
            notificationTokenManager.storeAndUpdateToken(refreshedToken);
        }
    }
}
