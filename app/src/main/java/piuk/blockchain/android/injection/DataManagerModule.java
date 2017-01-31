package piuk.blockchain.android.injection;

import android.content.Context;
import dagger.Module;
import dagger.Provides;
import info.blockchain.wallet.api.AddressInfo;
import info.blockchain.wallet.api.Settings;
import info.blockchain.wallet.api.TransactionDetails;
import info.blockchain.wallet.api.Unspent;
import info.blockchain.wallet.api.WalletPayload;
import info.blockchain.wallet.multiaddr.MultiAddrFactory;
import info.blockchain.wallet.payload.PayloadManager;
import info.blockchain.wallet.payment.Payment;
import piuk.blockchain.android.data.access.AccessState;
import piuk.blockchain.android.data.datamanagers.AccountDataManager;
import piuk.blockchain.android.data.datamanagers.AccountEditDataManager;
import piuk.blockchain.android.data.datamanagers.AuthDataManager;
import piuk.blockchain.android.data.datamanagers.QrCodeDataManager;
import piuk.blockchain.android.data.datamanagers.SendDataManager;
import piuk.blockchain.android.data.datamanagers.SettingsDataManager;
import piuk.blockchain.android.data.datamanagers.TransactionListDataManager;
import piuk.blockchain.android.data.datamanagers.TransferFundsDataManager;
import piuk.blockchain.android.data.fingerprint.FingerprintAuthImpl;
import piuk.blockchain.android.data.services.AddressInfoService;
import piuk.blockchain.android.data.services.PaymentService;
import piuk.blockchain.android.data.services.SettingsService;
import piuk.blockchain.android.data.services.TransactionDetailsService;
import piuk.blockchain.android.data.services.UnspentService;
import piuk.blockchain.android.data.services.WalletPayloadService;
import piuk.blockchain.android.data.stores.TransactionListStore;
import piuk.blockchain.android.ui.fingerprint.FingerprintHelper;
import piuk.blockchain.android.ui.receive.WalletAccountHelper;
import piuk.blockchain.android.ui.swipetoreceive.SwipeToReceiveHelper;
import piuk.blockchain.android.ui.transactions.TransactionHelper;
import piuk.blockchain.android.util.AESUtilWrapper;
import piuk.blockchain.android.util.AppUtil;
import piuk.blockchain.android.util.ExchangeRateFactory;
import piuk.blockchain.android.util.PrefsUtil;
import piuk.blockchain.android.util.StringUtils;

@Module
public class DataManagerModule {

    @Provides
    @ViewModelScope
    protected AuthDataManager provideAuthDataManager(PayloadManager payloadManager,
                                                     PrefsUtil prefsUtil,
                                                     AppUtil appUtil,
                                                     AESUtilWrapper aesUtilWrapper,
                                                     AccessState accessState,
                                                     StringUtils stringUtils) {
        return new AuthDataManager(
                payloadManager,
                prefsUtil,
                new WalletPayloadService(new WalletPayload()),
                appUtil,
                aesUtilWrapper,
                accessState,
                stringUtils);
    }

    @Provides
    @ViewModelScope
    protected QrCodeDataManager provideReceiveDataManager() {
        return new QrCodeDataManager();
    }

    @Provides
    @ViewModelScope
    protected WalletAccountHelper provideWalletAccountHelper(PayloadManager payloadManager,
                                                             PrefsUtil prefsUtil,
                                                             StringUtils stringUtils,
                                                             ExchangeRateFactory exchangeRateFactory,
                                                             MultiAddrFactory multiAddrFactory) {
        return new WalletAccountHelper(payloadManager, prefsUtil, stringUtils, exchangeRateFactory, multiAddrFactory);
    }

    @Provides
    @ViewModelScope
    protected TransactionListDataManager provideTransactionListDataManager(PayloadManager payloadManager,
                                                                           TransactionListStore transactionListStore) {
        return new TransactionListDataManager(
                payloadManager,
                new TransactionDetailsService(new TransactionDetails()),
                transactionListStore);
    }

    @Provides
    @ViewModelScope
    protected TransferFundsDataManager provideTransferFundsDataManager(PayloadManager payloadManager,
                                                                       MultiAddrFactory multiAddrFactory) {
        return new TransferFundsDataManager(payloadManager, multiAddrFactory, new Unspent(), new Payment());
    }

    @Provides
    @ViewModelScope
    protected TransactionHelper provideTransactionHelper(PayloadManager payloadManager,
                                                         MultiAddrFactory multiAddrFactory) {
        return new TransactionHelper(payloadManager, multiAddrFactory);
    }

    @Provides
    @ViewModelScope
    protected AccountDataManager provideAccountDataManager(PayloadManager payloadManager,
                                                           MultiAddrFactory multiAddrFactory) {
        return new AccountDataManager(payloadManager, multiAddrFactory, new AddressInfoService(new AddressInfo()));
    }

    @Provides
    @ViewModelScope
    protected FingerprintHelper provideFingerprintHelper(Context applicationContext,
                                                         PrefsUtil prefsUtil) {
        return new FingerprintHelper(applicationContext, prefsUtil, new FingerprintAuthImpl());
    }

    @Provides
    @ViewModelScope
    protected SettingsDataManager provideSettingsDataManager() {
        return new SettingsDataManager(new SettingsService(new Settings()));
    }

    @Provides
    @ViewModelScope
    protected AccountEditDataManager provideAccountEditDataManager(PayloadManager payloadManager) {
        return new AccountEditDataManager(
                new UnspentService(new Unspent()),
                new PaymentService(new Payment()),
                payloadManager);
    }

    @Provides
    @ViewModelScope
    protected SwipeToReceiveHelper provideSwipeToReceiveHelper(PayloadManager
                                                                payloadManager, MultiAddrFactory
                                                                multiAddrFactory, PrefsUtil prefsUtil) {
        return new SwipeToReceiveHelper(payloadManager, multiAddrFactory, prefsUtil);
    }

    @Provides
    @ViewModelScope
    protected SendDataManager provideSendDatamanager() {
        return new SendDataManager(new PaymentService(new Payment()));
    }
}
