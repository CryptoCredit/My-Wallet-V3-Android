package piuk.blockchain.android.ui.contacts.pairing;

import android.content.Intent;
import android.support.annotation.StringRes;
import info.blockchain.wallet.contacts.data.Contact;
import javax.inject.Inject;
import piuk.blockchain.android.R;
import piuk.blockchain.android.data.datamanagers.ContactsDataManager;
import piuk.blockchain.android.injection.Injector;
import piuk.blockchain.android.ui.base.BaseViewModel;
import piuk.blockchain.android.ui.customviews.ToastCustom;


@SuppressWarnings("WeakerAccess")
public class ContactsInvitationBuilderViewModel extends BaseViewModel {

    private DataListener dataListener;
    private Contact recipient;
    private Contact sender;
    private String uri;
    @Inject ContactsDataManager contactManager;

    interface DataListener {

        void showProgressDialog();

        void dismissProgressDialog();

        void showToast(@StringRes int message, @ToastCustom.ToastType String toastType);

        void onLinkGenerated(Intent intent);

        void onUriGenerated(String uri, String recipientName);

        void finishPage();

    }

    ContactsInvitationBuilderViewModel(DataListener dataListener) {
        Injector.getInstance().getDataManagerComponent().inject(this);
        this.dataListener = dataListener;
    }

    @Override
    public void onViewReady() {
        // No-op
    }

    void setNameOfSender(String nameOfSender) {
        sender = new Contact();
        sender.setName(nameOfSender);
    }

    void setNameOfRecipient(String nameOfRecipient) {
        recipient = new Contact();
        recipient.setName(nameOfRecipient);
    }

    void onQrCodeSelected() {
        if (uri == null) {
            dataListener.showProgressDialog();

            compositeDisposable.add(
                    contactManager.createInvitation(sender, recipient)
                            .map(Contact::createURI)
                            .doAfterTerminate(() -> dataListener.dismissProgressDialog())
                            .subscribe(
                                    uri -> {
                                        this.uri = uri;
                                        dataListener.onUriGenerated(uri, recipient.getName());
                                    },
                                    throwable -> dataListener.showToast(R.string.unexpected_error, ToastCustom.TYPE_ERROR)));
        } else {
            // Prevents contact being added more than once, as well as unnecessary web calls
            dataListener.onUriGenerated(uri, recipient.getName());
        }
    }

    void onLinkClicked() {
        if (uri == null) {
            dataListener.showProgressDialog();

            compositeDisposable.add(
                    contactManager.createInvitation(sender, recipient)
                            .map(Contact::createURI)
                            .doAfterTerminate(() -> dataListener.dismissProgressDialog())
                            .subscribe(
                                    uri -> {
                                        this.uri = uri;
                                        generateIntent(uri);
                                    },
                                    throwable -> dataListener.showToast(R.string.unexpected_error, ToastCustom.TYPE_ERROR)));
        } else {
            // Prevents contact being added more than once, as well as unnecessary web calls
            generateIntent(uri);
        }
    }

    // TODO: 16/01/2017 Maybe this should happen silently? It's a stopgap for now because the notification
    // service still isn't working at all
    void onDoneSelected() {
        if (uri == null) {
            dataListener.finishPage();
        } else {
            // Check status of sent invitation
            dataListener.showProgressDialog();
            compositeDisposable.add(
                    contactManager.readInvitationSent(recipient)
                            .doAfterTerminate(() -> dataListener.dismissProgressDialog())
                            .subscribe(
                                    success -> dataListener.finishPage(),
                                    throwable -> dataListener.finishPage()));
        }
    }

    private void generateIntent(String uri) {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_TEXT, uri);
        intent.setType("text/plain");
        dataListener.onLinkGenerated(intent);
    }
}
