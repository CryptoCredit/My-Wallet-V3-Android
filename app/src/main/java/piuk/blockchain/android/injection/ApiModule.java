package piuk.blockchain.android.injection;

import android.content.Context;
import dagger.Module;
import dagger.Provides;
import info.blockchain.wallet.api.Notifications;
import info.blockchain.wallet.api.PersistentUrls;
import info.blockchain.wallet.contacts.Contacts;
import info.blockchain.wallet.payload.PayloadManager;
import java.util.concurrent.TimeUnit;
import javax.inject.Named;
import javax.inject.Singleton;
import okhttp3.OkHttpClient;
import piuk.blockchain.android.data.access.AccessState;
import piuk.blockchain.android.data.api.ApiInterceptor;
import piuk.blockchain.android.data.datamanagers.ContactsDataManager;
import piuk.blockchain.android.data.notifications.NotificationTokenManager;
import piuk.blockchain.android.data.services.ContactsService;
import piuk.blockchain.android.data.services.NotificationService;
import piuk.blockchain.android.data.stores.TransactionListStore;
import piuk.blockchain.android.util.PrefsUtil;
import piuk.blockchain.android.util.SSLVerifyUtil;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;


/**
 * Created by adambennett on 08/08/2016.
 */

@SuppressWarnings("WeakerAccess")
@Module
public class ApiModule {

    private static final int API_TIMEOUT = 30;

    @Provides
    protected PayloadManager providePayloadManager() {
        return PayloadManager.getInstance();
    }

    @Provides
    @Singleton
    protected TransactionListStore provideTransactionListStore() {
        return new TransactionListStore();
    }

    @Provides
    @Singleton
    protected NotificationTokenManager provideNotificationTokenManager(AccessState accessState,
                                                                       PayloadManager payloadManager,
                                                                       PrefsUtil prefsUtil) {

        return new NotificationTokenManager(
                new NotificationService(new Notifications()), accessState, payloadManager, prefsUtil);
    }

    @Provides
    @Singleton
    protected ContactsDataManager provideContactsManager(PayloadManager payloadManager) {
        return new ContactsDataManager(new ContactsService(new Contacts()), payloadManager);
    }

    @Provides
    @Singleton
    protected OkHttpClient provideOkHttpClient() {
        return new OkHttpClient.Builder()
                .connectTimeout(API_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(API_TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(API_TIMEOUT, TimeUnit.SECONDS)
                .retryOnConnectionFailure(false)
                .addInterceptor(new ApiInterceptor())
                .build();
    }

    @Provides
    @Singleton
    protected JacksonConverterFactory provideJacksonConverterFactory() {
        return JacksonConverterFactory.create();
    }

    @Provides
    @Singleton
    @Named("api")
    protected Retrofit provideRetrofitApiInstance(OkHttpClient okHttpClient, JacksonConverterFactory converterFactory) {
        return new Retrofit.Builder()
                .baseUrl(PersistentUrls.getInstance().getCurrentBaseApiUrl())
                .client(okHttpClient)
                .addConverterFactory(converterFactory)
                .build();
    }

    @Provides
    @Singleton
    @Named("server")
    protected Retrofit provideRetrofitBlockchainInstance(OkHttpClient okHttpClient, JacksonConverterFactory converterFactory) {
        return new Retrofit.Builder()
                .baseUrl(PersistentUrls.getInstance().getCurrentBaseServerUrl())
                .client(okHttpClient)
                .addConverterFactory(converterFactory)
                .build();
    }

    @Provides
    @Singleton
    protected SSLVerifyUtil provideSSlVerifyUtil(Context context) {
        return new SSLVerifyUtil(context);
    }

}
