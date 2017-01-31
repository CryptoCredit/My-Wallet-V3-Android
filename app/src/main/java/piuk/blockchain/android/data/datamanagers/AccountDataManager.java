package piuk.blockchain.android.data.datamanagers;

import static piuk.blockchain.android.data.services.AddressInfoService.PARAMETER_FINAL_BALANCE;

import android.support.annotation.Nullable;
import info.blockchain.wallet.multiaddr.MultiAddrFactory;
import info.blockchain.wallet.payload.Account;
import info.blockchain.wallet.payload.LegacyAddress;
import info.blockchain.wallet.payload.Payload;
import info.blockchain.wallet.payload.PayloadManager;
import info.blockchain.wallet.util.DoubleEncryptionFactory;
import info.blockchain.wallet.util.PrivateKeyFactory;
import io.reactivex.Observable;
import io.reactivex.exceptions.Exceptions;
import java.util.List;
import org.bitcoinj.core.Base58;
import org.bitcoinj.core.ECKey;
import piuk.blockchain.android.data.rxjava.RxUtil;
import piuk.blockchain.android.data.services.AddressInfoService;

public class AccountDataManager {

    private PayloadManager payloadManager;
    private MultiAddrFactory multiAddrFactory;
    private AddressInfoService addressInfoService;

    public AccountDataManager(PayloadManager payload, MultiAddrFactory addrFactory, AddressInfoService addressService) {
        payloadManager = payload;
        multiAddrFactory = addrFactory;
        addressInfoService = addressService;
    }

    /**
     * Derives new {@link Account} from the master seed
     *
     * @param accountLabel   A label for the account
     * @param secondPassword An optional double encryption password
     * @return An {@link Observable<Account>} wrapping the newly created Account
     */
    public Observable<Account> createNewAccount(String accountLabel, @Nullable String secondPassword) {
        return Observable.fromCallable(() -> payloadManager.addAccount(accountLabel,
                secondPassword != null ? secondPassword : null))
                .compose(RxUtil.applySchedulersToObservable());
    }

    /**
     * Sets a private key for an associated {@link LegacyAddress} which is already in the {@link
     * Payload} as a watch only address
     *
     * @param key            An {@link ECKey}
     * @param secondPassword An optional double encryption password
     * @return An {@link Observable<Boolean>} representing a successful save
     */
    public Observable<Boolean> setPrivateKey(ECKey key, @Nullable String secondPassword) {
        return Observable.fromCallable(() -> payloadManager.setKeyForLegacyAddress(key, secondPassword))
                .compose(RxUtil.applySchedulersToObservable());
    }

    /**
     * Sets a private key for a {@link LegacyAddress}
     *
     * @param legacyAddress  The {@link LegacyAddress} to which you wish to add the key
     * @param key            The {@link ECKey} for the address
     * @param secondPassword An optional double encryption password
     */
    public void setKeyForLegacyAddress(LegacyAddress legacyAddress, ECKey key, @Nullable String secondPassword) throws Exception {
        // If double encrypted, save encrypted in payload
        if (!payloadManager.getPayload().isDoubleEncrypted()) {
            legacyAddress.setEncryptedKeyBytes(key.getPrivKeyBytes());
        } else {
            String encryptedKey = Base58.encode(key.getPrivKeyBytes());
            String encrypted2 = DoubleEncryptionFactory.getInstance().encrypt(encryptedKey,
                    payloadManager.getPayload().getSharedKey(),
                    secondPassword != null ? secondPassword : null,
                    payloadManager.getPayload().getOptions().getIterations());

            legacyAddress.setEncryptedKey(encrypted2);
        }
    }

    /**
     * Allows you to propagate changes to a {@link LegacyAddress} through the {@link Payload} and
     * the {@link MultiAddrFactory}
     *
     * @param legacyAddress The updated address
     * @return {@link Observable<Boolean>} representing a successful save
     */
    public Observable<Boolean> updateLegacyAddress(LegacyAddress legacyAddress) {
        return createUpdateLegacyAddressObservable(legacyAddress)
                .compose(RxUtil.applySchedulersToObservable());
    }

    private Observable<Boolean> createUpdateLegacyAddressObservable(LegacyAddress address) {
        return Observable.fromCallable(() -> payloadManager.addLegacyAddress(address))
                .flatMap(RxUtil.ternary(
                        Boolean::booleanValue,
                        aBoolean -> addAddressAndUpdate(address).flatMap(total -> Observable.just(true)),
                        aBoolean -> Observable.just(false)));
    }

    private Observable<Long> addAddressAndUpdate(LegacyAddress address) {
        try {
            List<String> legacyAddressList = payloadManager.getPayload().getLegacyAddressStringList();
            multiAddrFactory.refreshLegacyAddressData(legacyAddressList.toArray(new String[legacyAddressList.size()]), false);
        } catch (Exception e) {
            throw Exceptions.propagate(e);
        }
        return addressInfoService.getAddressBalance(address, PARAMETER_FINAL_BALANCE)
                .doOnNext(balance -> {
                    multiAddrFactory.setLegacyBalance(address.getAddress(), balance);
                    multiAddrFactory.setLegacyBalance(MultiAddrFactory.getInstance().getLegacyBalance() + balance);
                });
    }

    public Observable<ECKey> getKeyFromImportedData(String format, String data) {
        return Observable.fromCallable(() -> new PrivateKeyFactory().getKey(format, data))
            .compose(RxUtil.applySchedulersToObservable());
    }
}
