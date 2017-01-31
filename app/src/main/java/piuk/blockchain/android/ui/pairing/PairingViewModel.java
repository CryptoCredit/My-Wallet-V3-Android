package piuk.blockchain.android.ui.pairing;

import android.support.annotation.StringRes;
import android.support.annotation.VisibleForTesting;
import info.blockchain.wallet.api.WalletPayload;
import info.blockchain.wallet.crypto.AESUtil;
import info.blockchain.wallet.payload.PayloadManager;
import io.reactivex.Observable;
import io.reactivex.exceptions.Exceptions;
import java.util.regex.Pattern;
import javax.inject.Inject;
import org.apache.commons.lang3.tuple.Pair;
import org.spongycastle.util.encoders.Hex;
import piuk.blockchain.android.R;
import piuk.blockchain.android.data.rxjava.RxUtil;
import piuk.blockchain.android.injection.Injector;
import piuk.blockchain.android.ui.base.BaseViewModel;
import piuk.blockchain.android.ui.customviews.ToastCustom;
import piuk.blockchain.android.util.AppUtil;
import piuk.blockchain.android.util.PrefsUtil;

public class PairingViewModel extends BaseViewModel {

    @Inject protected AppUtil appUtil;
    @Inject protected PayloadManager payloadManager;
    @Inject protected PrefsUtil prefsUtil;
    private DataListener dataListener;

    interface DataListener {

        void showToast(@StringRes int message, @ToastCustom.ToastType String toastType);

        void startPinEntryActivity();

        void showProgressDialog(@StringRes int message);

        void dismissProgressDialog();

    }

    PairingViewModel(DataListener dataListener) {
        Injector.getInstance().getDataManagerComponent().inject(this);
        this.dataListener = dataListener;
    }

    @Override
    public void onViewReady() {
        // No-op
    }

    void pairWithQR(String raw) {
        dataListener.showProgressDialog(R.string.please_wait);

        appUtil.clearCredentials();

        WalletPayload access = new WalletPayload();

        compositeDisposable.add(
                handleQrCode(raw, access)
                        .compose(RxUtil.applySchedulersToObservable())
                        .subscribe(pairingQRComponents -> {
                            dataListener.dismissProgressDialog();

                            String guid = pairingQRComponents.getLeft();

                            if (guid != null) {
                                prefsUtil.setValue(PrefsUtil.KEY_GUID, guid);
                                prefsUtil.setValue(PrefsUtil.KEY_EMAIL_VERIFIED, true);
                                dataListener.startPinEntryActivity();
                            } else {
                                throw Exceptions.propagate(new Throwable("GUID was null"));
                            }
                        }, throwable -> {
                            dataListener.dismissProgressDialog();
                            dataListener.showToast(R.string.pairing_failed, ToastCustom.TYPE_ERROR);
                            appUtil.clearCredentialsAndRestart();
                        }));
    }

    private Observable<Pair<String, String>> handleQrCode(String data, WalletPayload access) {
        return Observable.fromCallable(() -> {
            Pair<String, String> qrComponents = getQRComponentsFromRawString(data);
            String guid = qrComponents.getLeft();
            String encryptedPairingCode = qrComponents.getRight();

            String encryptionPassword = access.getPairingEncryptionPassword(guid);
            String[] sharedKeyAndPassword = getSharedKeyAndPassword(encryptedPairingCode, encryptionPassword);

            payloadManager.setTempPassword(new String(Hex.decode(sharedKeyAndPassword[1]), "UTF-8"));
            appUtil.setSharedKey(sharedKeyAndPassword[0]);

            return qrComponents;
        });
    }

    @VisibleForTesting
    Pair<String, String> getQRComponentsFromRawString(String rawString) throws Exception {

        if (rawString == null || rawString.length() == 0 || rawString.charAt(0) != '1') {
            throw new Exception("QR string null or empty.");
        }

        String[] components = rawString.split("\\|", Pattern.LITERAL);

        if (components.length != 3) {
            throw new Exception("QR string does not have 3 components.");
        }

        String guid = components[1];
        if (guid.length() != 36) {
            throw new Exception("GUID should be 36 characters in length.");
        }

        String encryptedPairingCode = components[2];

        return Pair.of(guid,encryptedPairingCode);
    }

    @VisibleForTesting
    String[] getSharedKeyAndPassword(String encryptedPairingCode, String encryptionPassword) throws Exception {

        String decryptedPairingCode = AESUtil
            .decrypt(encryptedPairingCode, encryptionPassword, AESUtil.QR_CODE_PBKDF_2ITERATIONS);

        if (decryptedPairingCode == null) {
            throw new Exception("Pairing code decryption failed.");
        }
        String[] sharedKeyAndPassword = decryptedPairingCode.split("\\|", Pattern.LITERAL);

        if (sharedKeyAndPassword.length < 2) {
            throw new Exception("Invalid decrypted pairing code.");
        }

        String sharedKey = sharedKeyAndPassword[0];
        if (sharedKey.length() != 36) {
            throw new Exception("Invalid shared key.");
        }

        return sharedKeyAndPassword;
    }
}
