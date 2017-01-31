package piuk.blockchain.android.ui.contacts.pairing;

import android.app.NotificationManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.StringRes;
import javax.inject.Inject;
import piuk.blockchain.android.R;
import piuk.blockchain.android.data.datamanagers.QrCodeDataManager;
import piuk.blockchain.android.data.notifications.FcmCallbackService;
import piuk.blockchain.android.data.notifications.NotificationPayload;
import piuk.blockchain.android.injection.Injector;
import piuk.blockchain.android.ui.base.BaseViewModel;
import piuk.blockchain.android.ui.customviews.ToastCustom;


@SuppressWarnings("WeakerAccess")
public class ContactsQrViewModel extends BaseViewModel {

    private static final int DIMENSION_QR_CODE = 600;

    private DataListener dataListener;
    @Inject QrCodeDataManager qrCodeDataManager;
    @Inject NotificationManager notificationManager;

    interface DataListener {

        Bundle getFragmentBundle();

        void showToast(@StringRes int message, @ToastCustom.ToastType String toastType);

        void onQrLoaded(Bitmap bitmap);

        void updateDisplayMessage(String name);

        void finishPage();

    }

    ContactsQrViewModel(DataListener dataListener) {
        Injector.getInstance().getDataManagerComponent().inject(this);
        this.dataListener = dataListener;
    }

    @Override
    public void onViewReady() {
        subscribeToNotifications();

        if (dataListener.getFragmentBundle() != null) {
            String name = dataListener.getFragmentBundle().getString(ContactsInvitationBuilderQrFragment.ARGUMENT_NAME);
            dataListener.updateDisplayMessage(name);
            String uri = dataListener.getFragmentBundle().getString(ContactsInvitationBuilderQrFragment.ARGUMENT_URI);

            compositeDisposable.add(
                    qrCodeDataManager.generateQrCode(uri, DIMENSION_QR_CODE)
                            .subscribe(
                                    bitmap -> dataListener.onQrLoaded(bitmap),
                                    throwable -> dataListener.showToast(R.string.unexpected_error, ToastCustom.TYPE_ERROR)));

        } else {
            dataListener.showToast(R.string.unexpected_error, ToastCustom.TYPE_ERROR);
        }
    }

    private void subscribeToNotifications() {
        compositeDisposable.add(
                FcmCallbackService.getNotificationSubject()
                        .subscribe(
                                notificationPayload -> {
                                    if (notificationPayload.getType() != null
                                            && notificationPayload.getType().equals(NotificationPayload.NotificationType.CONTACT_REQUEST)) {
                                        // TODO: 31/01/2017 Currently neither of these work for some reason
                                        notificationManager.cancel(FcmCallbackService.ID_FOREGROUND_NOTIFICATION);
                                        dataListener.finishPage();
                                    }
                                },
                                throwable -> {
                                    // No-op
                                }
                        ));
    }
}
