package piuk.blockchain.android.injection;

import dagger.Component;
import info.blockchain.wallet.util.PrivateKeyFactory;
import javax.inject.Singleton;
import piuk.blockchain.android.BlockchainApplication;
import piuk.blockchain.android.data.api.DebugSettings;
import piuk.blockchain.android.data.datamanagers.ContactsDataManager;
import piuk.blockchain.android.data.notifications.InstanceIdService;
import piuk.blockchain.android.data.websocket.WebSocketService;
import piuk.blockchain.android.ui.contacts.pairing.ContactPairingMethodViewModel;
import piuk.blockchain.android.ui.receive.ReceiveCurrencyHelper;
import piuk.blockchain.android.util.AppUtil;
import piuk.blockchain.android.util.ExchangeRateFactory;
import piuk.blockchain.android.util.exceptions.LoggingExceptionHandler;

/**
 * Created by adambennett on 08/08/2016.
 */

@SuppressWarnings("WeakerAccess")
@Singleton
@Component(modules = {
        ApplicationModule.class,
        ApiModule.class
})
public interface ApplicationComponent {

    DataManagerComponent plus(DataManagerModule userModule);

    void inject(AppUtil appUtil);

    void inject(LoggingExceptionHandler loggingExceptionHandler);

    void inject(ExchangeRateFactory exchangeRateFactory);

    void inject(ReceiveCurrencyHelper receiveCurrencyHelper);

    void inject(DebugSettings debugSettings);

    void inject(PrivateKeyFactory privateKeyFactory);

    void inject(InstanceIdService instanceIdService);

    void inject(WebSocketService webSocketService);

    void inject(BlockchainApplication blockchainApplication);

    void inject(ContactsDataManager contactsDataManager);

    void inject(ContactPairingMethodViewModel contactPairingMethodViewModel);
}
