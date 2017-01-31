package piuk.blockchain.android.ui.contacts.pairing;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import javax.inject.Inject;
import piuk.blockchain.android.R;
import piuk.blockchain.android.data.datamanagers.ContactsDataManager;
import piuk.blockchain.android.injection.Injector;
import piuk.blockchain.android.ui.base.BaseViewModel;
import piuk.blockchain.android.ui.customviews.ToastCustom;
import piuk.blockchain.android.util.AppUtil;

public class ContactPairingMethodViewModel extends BaseViewModel {

    private DataListener dataListener;
    @Inject AppUtil appUtil;
    @Inject ContactsDataManager contactManager;

    interface DataListener {

        void showToast(@StringRes int message, @ToastCustom.ToastType String toastType);

        void finishActivityWithResult(int resultCode);

    }

    ContactPairingMethodViewModel(DataListener dataListener) {
        Injector.getInstance().getAppComponent().inject(this);
        this.dataListener = dataListener;
    }

    @Override
    public void onViewReady() {
        // No-op
    }

    void handleScanInput(@NonNull String extra) {
        compositeDisposable.add(
                contactManager.acceptInvitation(extra)
                        .subscribe(
                                contact -> {
                                    dataListener.showToast(R.string.contacts_add_contact_success, ToastCustom.TYPE_OK);
                                    dataListener.finishActivityWithResult(Activity.RESULT_OK);
                                }, throwable -> dataListener.showToast(R.string.contacts_invalid_qr, ToastCustom.TYPE_ERROR)));
    }

    boolean isCameraOpen() {
        return appUtil.isCameraOpen();
    }

}
