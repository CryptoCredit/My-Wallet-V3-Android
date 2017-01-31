package piuk.blockchain.android.ui.contacts.list;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.util.Log;
import info.blockchain.wallet.contacts.data.Contact;
import info.blockchain.wallet.exceptions.DecryptionException;
import info.blockchain.wallet.payload.PayloadManager;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import piuk.blockchain.android.R;
import piuk.blockchain.android.data.datamanagers.ContactsDataManager;
import piuk.blockchain.android.data.datamanagers.QrCodeDataManager;
import piuk.blockchain.android.data.notifications.FcmCallbackService;
import piuk.blockchain.android.data.rxjava.RxUtil;
import piuk.blockchain.android.injection.Injector;
import piuk.blockchain.android.ui.base.BaseViewModel;
import piuk.blockchain.android.ui.customviews.ToastCustom;
import piuk.blockchain.android.util.PrefsUtil;

@SuppressWarnings("WeakerAccess")
public class ContactsListViewModel extends BaseViewModel {

    private static final String TAG = ContactsListViewModel.class.getSimpleName();

    private DataListener dataListener;
    @Inject QrCodeDataManager qrCodeDataManager;
    @Inject ContactsDataManager contactsDataManager;
    @Inject PayloadManager payloadManager;
    @Inject PrefsUtil prefsUtil;

    interface DataListener {

        Intent getPageIntent();

        void onContactsLoaded(@NonNull List<ContactsListItem> contacts);

        void setUiState(@ContactsListActivity.UiState int uiState);

        void showToast(@StringRes int message, @ToastCustom.ToastType String toastType);

        void showProgressDialog();

        void dismissProgressDialog();

        void showSecondPasswordDialog();

    }

    ContactsListViewModel(DataListener dataListener) {
        Injector.getInstance().getDataManagerComponent().inject(this);
        this.dataListener = dataListener;
    }

    @Override
    public void onViewReady() {
        // Subscribe to notification events
        subscribeToNotifications();

        dataListener.setUiState(ContactsListActivity.LOADING);
        compositeDisposable.add(
                contactsDataManager.loadNodes()
                        .subscribe(
                                success -> {
                                    if (success) {
                                        loadContacts();
                                    } else {
                                        // Not set up, most likely has a second password enabled
                                        if (payloadManager.getPayload().isDoubleEncrypted()) {
                                            dataListener.showSecondPasswordDialog();
                                            dataListener.setUiState(ContactsListActivity.FAILURE);
                                        } else {
                                            initContactsService(null);
                                        }
                                    }
                                }, throwable -> dataListener.setUiState(ContactsListActivity.FAILURE)));

        loadContacts();

        Intent intent = dataListener.getPageIntent();
        if (intent != null && intent.hasExtra(ContactsListActivity.EXTRA_METADATA_URI)) {
            String data = intent.getStringExtra(ContactsListActivity.EXTRA_METADATA_URI);
            handleLink(data);
        }
    }

    void initContactsService(@Nullable String secondPassword) {
        dataListener.setUiState(ContactsListActivity.LOADING);
        compositeDisposable.add(
                contactsDataManager.generateNodes(secondPassword)
                        .andThen(contactsDataManager.getMetadataNodeFactory())
                        .flatMapCompletable(metadataNodeFactory -> contactsDataManager.initContactsService(
                                metadataNodeFactory.getMetadataNode(),
                                metadataNodeFactory.getSharedMetadataNode()))
                        .andThen(contactsDataManager.registerMdid())
                        .andThen(contactsDataManager.publishXpub())
                        .subscribe(
                                this::onViewReady,
                                throwable -> {
                                    dataListener.setUiState(ContactsListActivity.FAILURE);
                                    if (throwable instanceof DecryptionException) {
                                        dataListener.showToast(R.string.password_mismatch_error, ToastCustom.TYPE_ERROR);
                                    } else {
                                        dataListener.showToast(R.string.contacts_error_getting_messages, ToastCustom.TYPE_ERROR);
                                    }
                                }));
    }

    private void loadContacts() {
        dataListener.setUiState(ContactsListActivity.LOADING);
        compositeDisposable.add(
                contactsDataManager.fetchContacts()
                        .andThen(contactsDataManager.getContactList())
                        .toList()
                        .subscribe(
                                this::handleContactListUpdate,
                                throwable -> dataListener.setUiState(ContactsListActivity.FAILURE)));
    }

    // TODO: 19/01/2017 This is pretty gross and I'm certain it can be Rx-ified in the future
    private void handleContactListUpdate(List<Contact> contacts) {
        List<ContactsListItem> list = new ArrayList<>();
        List<Contact> pending = new ArrayList<>();

        compositeDisposable.add(
                contactsDataManager.getContactsWithUnreadPaymentRequests()
                        .toList()
                        .subscribe(actionRequired -> {
                            for (Contact contact : contacts) {
                                list.add(new ContactsListItem(
                                        contact.getId(),
                                        contact.getName(),
                                        contact.getMdid() != null && !contact.getMdid().isEmpty()
                                                ? ContactsListItem.Status.TRUSTED
                                                : ContactsListItem.Status.PENDING,
                                        contact.getCreated(),
                                        isInList(actionRequired, contact)));

                                if (contact.getMdid() == null || contact.getMdid().isEmpty()) {
                                    pending.add(contact);
                                }
                            }

                            checkStatusOfPendingContacts(pending);

                            if (!list.isEmpty()) {
                                dataListener.setUiState(ContactsListActivity.CONTENT);
                                dataListener.onContactsLoaded(list);
                            } else {
                                dataListener.onContactsLoaded(new ArrayList<>());
                                dataListener.setUiState(ContactsListActivity.EMPTY);
                            }

                        }, throwable -> {
                            dataListener.onContactsLoaded(new ArrayList<>());
                            dataListener.setUiState(ContactsListActivity.FAILURE);
                        }));
    }

    private boolean isInList(List<Contact> contacts, Contact toBeFound) {
        for (Contact contact : contacts) {
            if (contact.getId().equals(toBeFound.getId())) {
                return true;
            }
        }
        return false;
    }

    private void checkStatusOfPendingContacts(List<Contact> pending) {
        for (int i = 0; i < pending.size(); i++) {
            final Contact contact = pending.get(i);
            compositeDisposable.add(
                    contactsDataManager.readInvitationSent(contact)
                            .subscribe(
                                    success -> {
                                        // No-op
                                    },
                                    throwable -> {
                                        // No-op
                                    }));
        }
    }

    private void subscribeToNotifications() {
        compositeDisposable.add(
                FcmCallbackService.getNotificationSubject()
                        .compose(RxUtil.applySchedulersToObservable())
                        .subscribe(
                                notificationPayload -> onViewReady(),
                                throwable -> Log.e(TAG, "subscribeToNotifications: ", throwable)));
    }

    private void handleLink(String data) {
        dataListener.showProgressDialog();

        compositeDisposable.add(
                contactsDataManager.acceptInvitation(data)
                        .flatMap(contact -> contactsDataManager.getContactList())
                        .doAfterTerminate(() -> dataListener.dismissProgressDialog())
                        .toList()
                        .subscribe(
                                contacts -> {
                                    handleContactListUpdate(contacts);
                                    dataListener.showToast(R.string.contacts_add_contact_success, ToastCustom.TYPE_GENERAL);
                                }, throwable -> dataListener.showToast(R.string.contacts_add_contact_failed, ToastCustom.TYPE_ERROR)));

    }
}
