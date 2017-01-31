package piuk.blockchain.android.ui.auth;

import static io.reactivex.Observable.just;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static piuk.blockchain.android.ui.auth.CreateWalletFragment.KEY_INTENT_EMAIL;
import static piuk.blockchain.android.ui.auth.CreateWalletFragment.KEY_INTENT_PASSWORD;
import static piuk.blockchain.android.ui.auth.LandingActivity.KEY_INTENT_RECOVERING_FUNDS;
import static piuk.blockchain.android.ui.auth.PinEntryFragment.KEY_VALIDATING_PIN_FOR_RESULT;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.ImageView;
import info.blockchain.wallet.exceptions.AccountLockedException;
import info.blockchain.wallet.exceptions.DecryptionException;
import info.blockchain.wallet.exceptions.HDWalletException;
import info.blockchain.wallet.exceptions.InvalidCredentialsException;
import info.blockchain.wallet.exceptions.PayloadException;
import info.blockchain.wallet.exceptions.ServerConnectionException;
import info.blockchain.wallet.exceptions.UnsupportedVersionException;
import info.blockchain.wallet.payload.Account;
import info.blockchain.wallet.payload.HDWallet;
import info.blockchain.wallet.payload.Payload;
import info.blockchain.wallet.payload.PayloadManager;
import io.reactivex.Completable;
import io.reactivex.Observable;
import java.util.ArrayList;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.spongycastle.crypto.InvalidCipherTextException;
import piuk.blockchain.android.BlockchainTestApplication;
import piuk.blockchain.android.BuildConfig;
import piuk.blockchain.android.data.access.AccessState;
import piuk.blockchain.android.data.datamanagers.AuthDataManager;
import piuk.blockchain.android.injection.ApiModule;
import piuk.blockchain.android.injection.ApplicationModule;
import piuk.blockchain.android.injection.DataManagerModule;
import piuk.blockchain.android.injection.Injector;
import piuk.blockchain.android.injection.InjectorTestUtils;
import piuk.blockchain.android.ui.customviews.ToastCustom;
import piuk.blockchain.android.ui.fingerprint.FingerprintHelper;
import piuk.blockchain.android.util.AESUtilWrapper;
import piuk.blockchain.android.util.AppUtil;
import piuk.blockchain.android.util.DialogButtonCallback;
import piuk.blockchain.android.util.PrefsUtil;
import piuk.blockchain.android.util.SSLVerifyUtil;
import piuk.blockchain.android.util.StringUtils;

@SuppressWarnings("PrivateMemberAccessBetweenOuterAndInnerClass")
@Config(sdk = 23, constants = BuildConfig.class, application = BlockchainTestApplication.class)
@RunWith(RobolectricTestRunner.class)
public class PinEntryViewModelTest {

    private PinEntryViewModel mSubject;

    @Mock private PinEntryViewModel.DataListener mActivity;
    @Mock private AuthDataManager mAuthDataManager;
    @Mock private AppUtil mAppUtil;
    @Mock private PrefsUtil mPrefsUtil;
    @Mock private PayloadManager mPayloadManager;
    @Mock private StringUtils mStringUtils;
    @Mock private FingerprintHelper mFingerprintHelper;
    @Mock private AccessState mAccessState;
    @Mock private SSLVerifyUtil mSslVerifyUtil;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        InjectorTestUtils.initApplicationComponent(
                Injector.getInstance(),
                new MockApplicationModule(RuntimeEnvironment.application),
                new MockApiModule(),
                new MockDataManagerModule());

        ImageView mockImageView = mock(ImageView.class);
        when(mActivity.getPinBoxArray()).thenReturn(new ImageView[]{mockImageView, mockImageView, mockImageView, mockImageView});

        mSubject = new PinEntryViewModel(mActivity);
    }

    @Test
    public void onViewReadyEmailAndPasswordInIntentCreateWalletSuccessful() throws Exception {
        // Arrange
        String email = "example@email.com";
        String password = "1234567890";
        Intent intent = new Intent();
        intent.putExtra(KEY_INTENT_EMAIL, email);
        intent.putExtra(KEY_INTENT_PASSWORD, password);
        when(mActivity.getPageIntent()).thenReturn(intent);
        when(mPrefsUtil.getValue(PrefsUtil.KEY_PIN_IDENTIFIER, "")).thenReturn("");
        when(mFingerprintHelper.getEncryptedData(PrefsUtil.KEY_ENCRYPTED_PIN_CODE)).thenReturn("");
        when(mAuthDataManager.createHdWallet(anyString(), anyString())).thenReturn(just(new Payload()));
        // Act
        mSubject.onViewReady();
        // Assert
        assertEquals(false, mSubject.allowExit());
        verify(mActivity).showProgressDialog(anyInt(), anyString());
        verify(mActivity).dismissProgressDialog();
        verify(mPrefsUtil).setValue(PrefsUtil.KEY_EMAIL, email);
        verify(mPayloadManager).setEmail(email);
        verify(mPayloadManager).setTempPassword(password);
    }

    @Test
    public void onViewReadyValidatingPinForResult() throws Exception {
        // Arrange
        Intent intent = new Intent();
        intent.putExtra(KEY_VALIDATING_PIN_FOR_RESULT, true);
        when(mActivity.getPageIntent()).thenReturn(intent);
        // Act
        mSubject.onViewReady();
        // Assert
        assertEquals(true, mSubject.isForValidatingPinForResult());
    }

    @Test
    public void onViewReadyEmailAndPasswordInIntentCreateWalletThrowsError() throws Exception {
        // Arrange
        String email = "example@email.com";
        String password = "1234567890";
        Intent intent = new Intent();
        intent.putExtra(KEY_INTENT_EMAIL, email);
        intent.putExtra(KEY_INTENT_PASSWORD, password);
        when(mActivity.getPageIntent()).thenReturn(intent);
        when(mPrefsUtil.getValue(PrefsUtil.KEY_PIN_IDENTIFIER, "")).thenReturn("");
        when(mFingerprintHelper.getEncryptedData(PrefsUtil.KEY_ENCRYPTED_PIN_CODE)).thenReturn("");
        when(mAuthDataManager.createHdWallet(anyString(), anyString())).thenReturn(Observable.error(new Throwable()));
        // Act
        mSubject.onViewReady();
        // Assert
        assertEquals(false, mSubject.allowExit());
        verify(mActivity).showProgressDialog(anyInt(), anyString());
        verify(mActivity, times(2)).dismissProgressDialog();
        //noinspection WrongConstant
        verify(mActivity).showToast(anyInt(), anyString());
    }

    @Test
    public void onViewReadyRecoveringFunds() throws Exception {
        // Arrange
        String email = "example@email.com";
        String password = "1234567890";
        Intent intent = new Intent();
        intent.putExtra(KEY_INTENT_EMAIL, email);
        intent.putExtra(KEY_INTENT_PASSWORD, password);
        intent.putExtra(KEY_INTENT_RECOVERING_FUNDS, true);
        when(mActivity.getPageIntent()).thenReturn(intent);
        when(mAuthDataManager.createHdWallet(anyString(), anyString())).thenReturn(just(new Payload()));
        // Act
        mSubject.onViewReady();
        // Assert
        assertEquals(false, mSubject.allowExit());
        verify(mPrefsUtil).setValue(PrefsUtil.KEY_EMAIL, email);
        verify(mPayloadManager).setEmail(email);
        verify(mPayloadManager).setTempPassword(password);
        verifyNoMoreInteractions(mPayloadManager);
    }

    @Test
    public void onViewReadyMaxAttemptsExceeded() throws Exception {
        // Arrange
        when(mActivity.getPageIntent()).thenReturn(new Intent());
        when(mPrefsUtil.getValue(PrefsUtil.KEY_PIN_FAILS, 0)).thenReturn(4);
        when(mPayloadManager.getPayload()).thenReturn(mock(Payload.class));
        when(mPrefsUtil.getValue(PrefsUtil.KEY_PIN_IDENTIFIER, "")).thenReturn("");
        when(mFingerprintHelper.getEncryptedData(PrefsUtil.KEY_ENCRYPTED_PIN_CODE)).thenReturn("");
        // Act
        mSubject.onViewReady();
        // Assert
        assertEquals(true, mSubject.allowExit());
        //noinspection WrongConstant
        verify(mActivity).showToast(anyInt(), anyString());
        verify(mActivity).showMaxAttemptsDialog();
    }

    @Test
    public void checkFingerprintStatusShouldShowDialog() throws Exception {
        // Arrange
        mSubject.mValidatingPinForResult = false;
        mSubject.mRecoveringFunds = false;
        when(mPrefsUtil.getValue(PrefsUtil.KEY_PIN_IDENTIFIER, "")).thenReturn("1234");
        when(mFingerprintHelper.getIfFingerprintUnlockEnabled()).thenReturn(true);
        when(mFingerprintHelper.getEncryptedData(PrefsUtil.KEY_ENCRYPTED_PIN_CODE)).thenReturn(null);
        when(mFingerprintHelper.getEncryptedData(PrefsUtil.KEY_ENCRYPTED_PIN_CODE)).thenReturn("");
        // Act
        mSubject.checkFingerprintStatus();
        // Assert
        verify(mActivity).showFingerprintDialog(anyString());
    }

    @Test
    public void checkFingerprintStatusDontShow() throws Exception {
        // Arrange
        mSubject.mValidatingPinForResult = true;
        // Act
        mSubject.checkFingerprintStatus();
        // Assert
        verify(mActivity).showKeyboard();
    }

    @Test
    public void canShowFingerprintDialog() throws Exception {
        // Arrange
        mSubject.mCanShowFingerprintDialog = true;
        // Act
        boolean value = mSubject.canShowFingerprintDialog();
        // Assert
        assertEquals(true, value);
    }

    @Test
    public void loginWithDecryptedPin() throws Exception {
        // Arrange
        String pincode = new String("1234");
        when(mAuthDataManager.validatePin(pincode)).thenReturn(just("password"));
        // Act
        mSubject.loginWithDecryptedPin(pincode);
        // Assert
        verify(mActivity).getPinBoxArray();
        assertEquals(false, mSubject.canShowFingerprintDialog());
    }

    @Test
    public void onDeleteClicked() throws Exception {
        // Arrange
        mSubject.mUserEnteredPin = "1234";
        // Act
        mSubject.onDeleteClicked();
        // Assert
        assertEquals("123", mSubject.mUserEnteredPin);
        verify(mActivity).getPinBoxArray();
    }

    @Test
    public void padClickedPinAlreadyFourDigits() throws Exception {
        // Arrange
        mSubject.mUserEnteredPin = "0000";
        // Act
        mSubject.onPadClicked("0");
        // Assert
        verifyZeroInteractions(mActivity);
    }

    @Test
    public void padClickedAllZeros() throws Exception {
        // Arrange
        mSubject.mUserEnteredPin = "000";
        // Act
        when(mPrefsUtil.getValue(PrefsUtil.KEY_PIN_IDENTIFIER, "")).thenReturn("");
        when(mFingerprintHelper.getEncryptedData(PrefsUtil.KEY_ENCRYPTED_PIN_CODE)).thenReturn("");
        mSubject.onPadClicked("0");
        // Assert
        verify(mActivity).clearPinBoxes();
        //noinspection WrongConstant
        verify(mActivity).showToast(anyInt(), anyString());
        assertEquals("", mSubject.mUserEnteredPin);
        assertEquals(null, mSubject.mUserEnteredConfirmationPin);
    }

    @Test
    public void padClickedShowCommonPinWarning() throws Exception {
        // Arrange
        mSubject.mUserEnteredPin = "123";
        when(mPrefsUtil.getValue(anyString(), anyString())).thenReturn("");
        // Act
        mSubject.onPadClicked("4");
        // Assert
        verify(mActivity).showCommonPinWarning(any(DialogButtonCallback.class));
    }

    @Test
    public void padClickedShowCommonPinWarningAndClickRetry() throws Exception {
        // Arrange
        mSubject.mUserEnteredPin = "123";
        when(mPrefsUtil.getValue(anyString(), anyString())).thenReturn("");
        doAnswer(invocation -> {
            ((DialogButtonCallback) invocation.getArguments()[0]).onPositiveClicked();
            return null;
        }).when(mActivity).showCommonPinWarning(any(DialogButtonCallback.class));
        // Act
        mSubject.onPadClicked("4");
        // Assert
        verify(mActivity).showCommonPinWarning(any(DialogButtonCallback.class));
        verify(mActivity).clearPinBoxes();
        assertEquals("", mSubject.mUserEnteredPin);
        assertEquals(null, mSubject.mUserEnteredConfirmationPin);
    }

    @Test
    public void padClickedShowCommonPinWarningAndClickContinue() throws Exception {
        // Arrange
        mSubject.mUserEnteredPin = "123";
        when(mPrefsUtil.getValue(anyString(), anyString())).thenReturn("");
        doAnswer(invocation -> {
            ((DialogButtonCallback) invocation.getArguments()[0]).onNegativeClicked();
            return null;
        }).when(mActivity).showCommonPinWarning(any(DialogButtonCallback.class));
        // Act
        mSubject.onPadClicked("4");
        // Assert
        verify(mActivity).showCommonPinWarning(any(DialogButtonCallback.class));
        assertEquals("", mSubject.mUserEnteredPin);
        assertEquals("1234", mSubject.mUserEnteredConfirmationPin);
    }

    @Test
    public void padClickedShowPinReuseWarning() throws Exception {
        // Arrange
        mSubject.mUserEnteredPin = "258";
        when(mPrefsUtil.getValue(anyString(), anyString())).thenReturn("");
        when(mAccessState.getPIN()).thenReturn("2580");
        // Act
        mSubject.onPadClicked("0");
        // Assert
        verify(mActivity).dismissProgressDialog();
        //noinspection WrongConstant
        verify(mActivity).showToast(anyInt(), eq(ToastCustom.TYPE_ERROR));
        verify(mActivity).clearPinBoxes();
    }

    @Test
    public void padClickedVerifyPinValidateCalled() throws Exception {
        // Arrange
        mSubject.mUserEnteredPin = "133";
        when(mPrefsUtil.getValue(PrefsUtil.KEY_PIN_IDENTIFIER, "")).thenReturn("1234567890");
        when(mAuthDataManager.validatePin(anyString())).thenReturn(just(""));
        // Act
        mSubject.onPadClicked("7");
        // Assert
        verify(mActivity).setTitleVisibility(View.INVISIBLE);
        verify(mActivity, times(2)).showProgressDialog(anyInt(), anyString());
        verify(mAuthDataManager).validatePin(anyString());
    }

    @Test
    public void padClickedVerifyPinForResultReturnsValidPassword() throws Exception {
        // Arrange
        mSubject.mUserEnteredPin = "133";
        mSubject.mValidatingPinForResult = true;
        when(mPrefsUtil.getValue(PrefsUtil.KEY_PIN_IDENTIFIER, "")).thenReturn("1234567890");
        when(mAuthDataManager.validatePin(anyString())).thenReturn(just(""));
        // Act
        mSubject.onPadClicked("7");
        // Assert
        verify(mActivity).setTitleVisibility(View.INVISIBLE);
        verify(mActivity).showProgressDialog(anyInt(), anyString());
        verify(mActivity).dismissProgressDialog();
        verify(mAuthDataManager).validatePin(anyString());
        verify(mActivity).finishWithResultOk("1337");
    }

    @Test
    public void padClickedVerifyPinValidateCalledReturnsErrorIncrementsFailureCount() throws Exception {
        // Arrange
        mSubject.mUserEnteredPin = "133";
        when(mPrefsUtil.getValue(PrefsUtil.KEY_PIN_IDENTIFIER, "")).thenReturn("1234567890");
        when(mAuthDataManager.validatePin(anyString())).thenReturn(Observable.error(new InvalidCredentialsException()));
        // Act
        mSubject.onPadClicked("7");
        // Assert
        verify(mActivity).setTitleVisibility(View.INVISIBLE);
        verify(mActivity).showProgressDialog(anyInt(), anyString());
        verify(mAuthDataManager).validatePin(anyString());
        verify(mPrefsUtil).setValue(anyString(), anyInt());
        verify(mPrefsUtil).getValue(anyString(), anyInt());
        //noinspection WrongConstant
        verify(mActivity).showToast(anyInt(), anyString());
        verify(mActivity).restartPageAndClearTop();
    }

    @Test
    public void padClickedVerifyPinValidateCalledReturnsInvalidCipherText() throws Exception {
        // Arrange
        mSubject.mUserEnteredPin = "133";
        when(mPrefsUtil.getValue(PrefsUtil.KEY_PIN_IDENTIFIER, "")).thenReturn("1234567890");
        when(mAuthDataManager.validatePin(anyString())).thenReturn(just(""));
        when(mAuthDataManager.updatePayload(anyString(), anyString(), anyString())).thenReturn(Completable.error(new InvalidCipherTextException()));
        // Act
        mSubject.onPadClicked("7");
        // Assert
        verify(mActivity).setTitleVisibility(View.INVISIBLE);
        verify(mActivity, times(2)).showProgressDialog(anyInt(), anyString());
        verify(mAuthDataManager).validatePin(anyString());
        verify(mAuthDataManager).updatePayload(anyString(), anyString(), anyString());
        verify(mPrefsUtil).setValue(anyString(), anyInt());
        //noinspection WrongConstant
        verify(mActivity).showToast(anyInt(), anyString());
        verify(mAccessState).setPIN(null);
        verify(mAppUtil).clearCredentialsAndRestart();
    }

    @Test
    public void padClickedVerifyPinValidateCalledReturnsGenericException() throws Exception {
        // Arrange
        mSubject.mUserEnteredPin = "133";
        when(mPrefsUtil.getValue(PrefsUtil.KEY_PIN_IDENTIFIER, "")).thenReturn("1234567890");
        when(mAuthDataManager.validatePin(anyString())).thenReturn(just(""));
        when(mAuthDataManager.updatePayload(anyString(), anyString(), anyString())).thenReturn(Completable.error(new Exception()));
        // Act
        mSubject.onPadClicked("7");
        // Assert
        verify(mActivity).setTitleVisibility(View.INVISIBLE);
        verify(mActivity, times(2)).showProgressDialog(anyInt(), anyString());
        verify(mAuthDataManager).validatePin(anyString());
        verify(mAuthDataManager).updatePayload(anyString(), anyString(), anyString());
        verify(mPrefsUtil).setValue(anyString(), anyInt());
        //noinspection WrongConstant
        verify(mActivity).showToast(anyInt(), anyString());
        verify(mAppUtil).clearCredentialsAndRestart();
    }

    @Test
    public void padClickedCreatePinCreateSuccessful() throws Exception {
        // Arrange
        mSubject.mUserEnteredPin = "133";
        mSubject.mUserEnteredConfirmationPin = "1337";
        when(mPrefsUtil.getValue(PrefsUtil.KEY_PIN_IDENTIFIER, "")).thenReturn("");
        when(mAuthDataManager.createPin(anyString(), anyString())).thenReturn(just(true));
        // Act
        mSubject.onPadClicked("7");
        // Assert
        verify(mActivity, times(2)).showProgressDialog(anyInt(), anyString());
        verify(mAuthDataManager).createPin(anyString(), anyString());
        verify(mFingerprintHelper).clearEncryptedData(PrefsUtil.KEY_ENCRYPTED_PIN_CODE);
        verify(mFingerprintHelper).setFingerprintUnlockEnabled(false);
    }

    @Test
    public void padClickedCreatePinCreateFailed() throws Exception {
        // Arrange
        mSubject.mUserEnteredPin = "133";
        mSubject.mUserEnteredConfirmationPin = "1337";
        when(mPrefsUtil.getValue(PrefsUtil.KEY_PIN_IDENTIFIER, "")).thenReturn("");
        when(mAuthDataManager.createPin(anyString(), anyString())).thenReturn(just(false));
        // Act
        mSubject.onPadClicked("7");
        // Assert
        verify(mActivity).showProgressDialog(anyInt(), anyString());
        verify(mAuthDataManager).createPin(anyString(), anyString());
        //noinspection WrongConstant
        verify(mActivity).showToast(anyInt(), anyString());
        verify(mPrefsUtil).clear();
        verify(mAppUtil).restartApp();
    }

    @Test
    public void padClickedCreatePinWritesNewConfirmationValue() throws Exception {
        // Arrange
        mSubject.mUserEnteredPin = "133";
        when(mPrefsUtil.getValue(PrefsUtil.KEY_PIN_IDENTIFIER, "")).thenReturn("");
        when(mAuthDataManager.createPin(anyString(), anyString())).thenReturn(just(true));
        // Act
        mSubject.onPadClicked("7");
        // Assert
        assertEquals("1337", mSubject.mUserEnteredConfirmationPin);
        assertEquals("", mSubject.mUserEnteredPin);
    }

    @Test
    public void padClickedCreatePinMismatched() throws Exception {
        // Arrange
        mSubject.mUserEnteredPin = "133";
        mSubject.mUserEnteredConfirmationPin = "1234";
        when(mPrefsUtil.getValue(PrefsUtil.KEY_PIN_IDENTIFIER, "")).thenReturn("");
        when(mAuthDataManager.createPin(anyString(), anyString())).thenReturn(just(true));
        // Act
        mSubject.onPadClicked("7");
        // Assert
        //noinspection WrongConstant
        verify(mActivity).showToast(anyInt(), anyString());
        verify(mActivity).dismissProgressDialog();
    }

    @Test
    public void clearPinBoxes() throws Exception {
        // Arrange

        // Act
        mSubject.clearPinBoxes();
        // Assert
        verify(mActivity).clearPinBoxes();
        assertEquals("", mSubject.mUserEnteredPin);
    }

    @Test
    public void validatePasswordSuccessful() throws Exception {
        // Arrange
        String password = new String("1234567890");
        when(mAuthDataManager.updatePayload(anyString(), anyString(), anyString())).thenReturn(Completable.complete());
        // Act
        mSubject.validatePassword(password);
        // Assert
        verify(mActivity).showProgressDialog(anyInt(), anyString());
        verify(mAuthDataManager).updatePayload(anyString(), anyString(), anyString());
        verify(mPayloadManager).setTempPassword("");
        verify(mActivity).dismissProgressDialog();
        //noinspection WrongConstant
        verify(mActivity).showToast(anyInt(), anyString());
        verify(mPrefsUtil, times(2)).removeValue(anyString());
        verify(mActivity).restartPageAndClearTop();
    }

    @Test
    public void validatePasswordThrowsGenericException() throws Exception {
        // Arrange
        String password = new String("1234567890");
        when(mAuthDataManager.updatePayload(anyString(), anyString(), anyString())).thenReturn(Completable.error(new Throwable()));
        // Act
        mSubject.validatePassword(password);
        // Assert
        verify(mActivity).showProgressDialog(anyInt(), anyString());
        verify(mAuthDataManager).updatePayload(anyString(), anyString(), anyString());
        verify(mPayloadManager).setTempPassword("");
        verify(mActivity, times(2)).dismissProgressDialog();
        //noinspection WrongConstant
        verify(mActivity).showToast(anyInt(), anyString());
        verify(mActivity).showValidationDialog();
    }

    @Test
    public void validatePasswordThrowsServerConnectionException() throws Exception {
        // Arrange
        String password = new String("1234567890");
        when(mAuthDataManager.updatePayload(anyString(), anyString(), anyString())).thenReturn(Completable.error(new ServerConnectionException()));
        // Act
        mSubject.validatePassword(password);
        // Assert
        verify(mActivity).showProgressDialog(anyInt(), anyString());
        verify(mAuthDataManager).updatePayload(anyString(), anyString(), anyString());
        verify(mPayloadManager).setTempPassword("");
        verify(mActivity).dismissProgressDialog();
        //noinspection WrongConstant
        verify(mActivity).showToast(anyInt(), anyString());
    }

    @Test
    public void validatePasswordThrowsHDWalletExceptionException() throws Exception {
        // Arrange
        String password = new String("1234567890");
        when(mAuthDataManager.updatePayload(anyString(), anyString(), anyString())).thenReturn(Completable.error(new HDWalletException()));
        // Act
        mSubject.validatePassword(password);
        // Assert
        verify(mActivity).showProgressDialog(anyInt(), anyString());
        verify(mAuthDataManager).updatePayload(anyString(), anyString(), anyString());
        verify(mPayloadManager).setTempPassword("");
        verify(mActivity).dismissProgressDialog();
        //noinspection WrongConstant
        verify(mActivity).showToast(anyInt(), anyString());
        verify(mAppUtil).restartApp();
    }

    @Test
    public void validatePasswordThrowsAccountLockedException() throws Exception {
        // Arrange
        String password = new String("1234567890");
        when(mAuthDataManager.updatePayload(anyString(), anyString(), anyString())).thenReturn(Completable.error(new AccountLockedException()));
        // Act
        mSubject.validatePassword(password);
        // Assert
        verify(mActivity).showProgressDialog(anyInt(), anyString());
        verify(mAuthDataManager).updatePayload(anyString(), anyString(), anyString());
        verify(mPayloadManager).setTempPassword("");
        verify(mActivity).dismissProgressDialog();
        verify(mActivity).showAccountLockedDialog();
    }

    @Test
    public void updatePayloadInvalidCredentialsException() throws Exception {
        // Arrange
        when(mAuthDataManager.updatePayload(anyString(), anyString(), anyString())).thenReturn(Completable.error(new InvalidCredentialsException()));
        Payload mockPayload = mock(Payload.class);
        when(mockPayload.getSharedKey()).thenReturn("1234567890");
        when(mPayloadManager.getPayload()).thenReturn(mockPayload);
        // Act
        mSubject.updatePayload("");
        // Assert
        verify(mActivity).showProgressDialog(anyInt(), anyString());
        verify(mAuthDataManager).updatePayload(anyString(), anyString(), anyString());
        verify(mActivity).goToPasswordRequiredActivity();
    }

    @Test
    public void updatePayloadServerConnectionException() throws Exception {
        // Arrange
        when(mAuthDataManager.updatePayload(anyString(), anyString(), anyString())).thenReturn(Completable.error(new ServerConnectionException()));
        Payload mockPayload = mock(Payload.class);
        when(mockPayload.getSharedKey()).thenReturn("1234567890");
        when(mPayloadManager.getPayload()).thenReturn(mockPayload);
        // Act
        mSubject.updatePayload("");
        // Assert
        verify(mActivity).showProgressDialog(anyInt(), anyString());
        verify(mAuthDataManager).updatePayload(anyString(), anyString(), anyString());
        //noinspection WrongConstant
        verify(mActivity).showToast(anyInt(), anyString());
    }

    @Test
    public void updatePayloadDecryptionException() throws Exception {
        // Arrange
        when(mAuthDataManager.updatePayload(anyString(), anyString(), anyString())).thenReturn(Completable.error(new DecryptionException()));
        Payload mockPayload = mock(Payload.class);
        when(mockPayload.getSharedKey()).thenReturn("1234567890");
        when(mPayloadManager.getPayload()).thenReturn(mockPayload);
        // Act
        mSubject.updatePayload("");
        // Assert
        verify(mActivity).showProgressDialog(anyInt(), anyString());
        verify(mAuthDataManager).updatePayload(anyString(), anyString(), anyString());
        verify(mActivity).goToPasswordRequiredActivity();
    }

    @Test
    public void updatePayloadPayloadExceptionException() throws Exception {
        // Arrange
        when(mAuthDataManager.updatePayload(anyString(), anyString(), anyString())).thenReturn(Completable.error(new PayloadException()));
        Payload mockPayload = mock(Payload.class);
        when(mockPayload.getSharedKey()).thenReturn("1234567890");
        when(mPayloadManager.getPayload()).thenReturn(mockPayload);
        // Act
        mSubject.updatePayload("");
        // Assert
        verify(mActivity).showProgressDialog(anyInt(), anyString());
        verify(mAuthDataManager).updatePayload(anyString(), anyString(), anyString());
        //noinspection WrongConstant
        verify(mActivity).showToast(anyInt(), anyString());
        verify(mAppUtil).restartApp();
    }

    @Test
    public void updatePayloadHDWalletException() throws Exception {
        // Arrange
        when(mAuthDataManager.updatePayload(anyString(), anyString(), anyString())).thenReturn(Completable.error(new HDWalletException()));
        Payload mockPayload = mock(Payload.class);
        when(mockPayload.getSharedKey()).thenReturn("1234567890");
        when(mPayloadManager.getPayload()).thenReturn(mockPayload);
        // Act
        mSubject.updatePayload("");
        // Assert
        verify(mActivity).showProgressDialog(anyInt(), anyString());
        verify(mAuthDataManager).updatePayload(anyString(), anyString(), anyString());
        //noinspection WrongConstant
        verify(mActivity).showToast(anyInt(), anyString());
        verify(mAppUtil).restartApp();
    }

    @Test
    public void updatePayloadVersionNotSupported() throws Exception {
        // Arrange
        when(mAuthDataManager.updatePayload(anyString(), anyString(), anyString())).thenReturn(Completable.error(new UnsupportedVersionException()));
        Payload mockPayload = mock(Payload.class);
        when(mockPayload.getSharedKey()).thenReturn("1234567890");
        when(mPayloadManager.getPayload()).thenReturn(mockPayload);
        // Act
        mSubject.updatePayload("");
        // Assert
        verify(mActivity).showProgressDialog(anyInt(), anyString());
        verify(mAuthDataManager).updatePayload(anyString(), anyString(), anyString());
        verify(mActivity).showWalletVersionNotSupportedDialog(anyString());
    }

    @Test
    public void updatePayloadAccountLocked() throws Exception {
        // Arrange
        when(mAuthDataManager.updatePayload(anyString(), anyString(), anyString())).thenReturn(Completable.error(new AccountLockedException()));
        Payload mockPayload = mock(Payload.class);
        when(mockPayload.getSharedKey()).thenReturn("1234567890");
        when(mPayloadManager.getPayload()).thenReturn(mockPayload);
        // Act
        mSubject.updatePayload("");
        // Assert
        verify(mActivity).showProgressDialog(anyInt(), anyString());
        verify(mAuthDataManager).updatePayload(anyString(), anyString(), anyString());
        verify(mActivity).showAccountLockedDialog();
    }

    @Test
    public void updatePayloadSuccessfulSetLabels() throws Exception {
        // Arrange
        when(mAuthDataManager.updatePayload(anyString(), anyString(), anyString())).thenReturn(Completable.complete());
        Payload mockPayload = mock(Payload.class);
        when(mockPayload.getSharedKey()).thenReturn("1234567890");
        HDWallet mockHdWallet = mock(HDWallet.class);
        when(mockPayload.getHdWallet()).thenReturn(mockHdWallet);
        Account mockAccount = mock(Account.class);
        when(mockAccount.getLabel()).thenReturn(null);
        ArrayList<Account> accountsList = new ArrayList<>();
        accountsList.add(mockAccount);
        when(mockHdWallet.getAccounts()).thenReturn(accountsList);
        when(mPayloadManager.getPayload()).thenReturn(mockPayload);
        when(mockPayload.isUpgraded()).thenReturn(true);
        when(mAppUtil.isNewlyCreated()).thenReturn(true);
        // Act
        mSubject.updatePayload("");
        // Assert
        verify(mActivity).showProgressDialog(anyInt(), anyString());
        verify(mAuthDataManager).updatePayload(anyString(), anyString(), anyString());
        verify(mAppUtil).setSharedKey(anyString());
        verify(mPayloadManager, times(5)).getPayload();
        verify(mStringUtils).getString(anyInt());
        verify(mActivity).dismissProgressDialog();
        assertEquals(true, mSubject.mCanShowFingerprintDialog);
    }

    @Test
    public void updatePayloadSuccessfulUpgradeWallet() throws Exception {
        // Arrange
        when(mAuthDataManager.updatePayload(anyString(), anyString(), anyString())).thenReturn(Completable.complete());
        Payload mockPayload = mock(Payload.class);
        when(mockPayload.getSharedKey()).thenReturn("1234567890");
        when(mPayloadManager.getPayload()).thenReturn(mockPayload);
        when(mockPayload.isUpgraded()).thenReturn(false);
        when(mAppUtil.isNewlyCreated()).thenReturn(false);
        // Act
        mSubject.updatePayload("");
        // Assert
        verify(mActivity).showProgressDialog(anyInt(), anyString());
        verify(mAuthDataManager).updatePayload(anyString(), anyString(), anyString());
        verify(mAppUtil).setSharedKey(anyString());
        verify(mActivity).goToUpgradeWalletActivity();
        verify(mActivity).dismissProgressDialog();
        assertEquals(true, mSubject.mCanShowFingerprintDialog);
    }

    @Test
    public void updatePayloadSuccessfulVerifyPin() throws Exception {
        // Arrange
        when(mAuthDataManager.updatePayload(anyString(), anyString(), anyString())).thenReturn(Completable.complete());
        Payload mockPayload = mock(Payload.class);
        when(mockPayload.getSharedKey()).thenReturn("1234567890");
        when(mPayloadManager.getPayload()).thenReturn(mockPayload);
        when(mockPayload.isUpgraded()).thenReturn(true);
        when(mAppUtil.isNewlyCreated()).thenReturn(false);
        // Act
        mSubject.updatePayload("");
        // Assert
        verify(mActivity).showProgressDialog(anyInt(), anyString());
        verify(mAuthDataManager).updatePayload(anyString(), anyString(), anyString());
        verify(mAppUtil).setSharedKey(anyString());
        verify(mAppUtil).restartAppWithVerifiedPin();
        verify(mActivity).dismissProgressDialog();
        assertEquals(true, mSubject.mCanShowFingerprintDialog);
    }

    @Test
    public void incrementFailureCount() throws Exception {
        // Arrange

        // Act
        mSubject.incrementFailureCountAndRestart();
        // Assert
        verify(mPrefsUtil).getValue(anyString(), anyInt());
        verify(mPrefsUtil).setValue(anyString(), anyInt());
        //noinspection WrongConstant
        verify(mActivity).showToast(anyInt(), anyString());
        verify(mActivity).restartPageAndClearTop();
    }

    @Test
    public void resetApp() throws Exception {
        // Arrange

        // Act
        mSubject.resetApp();
        // Assert
        verify(mAppUtil).clearCredentialsAndRestart();
    }

    @Test
    public void allowExit() throws Exception {
        // Arrange

        // Act
        boolean allowExit = mSubject.allowExit();
        // Assert
        assertEquals(mSubject.bAllowExit, allowExit);
    }

    @Test
    public void isCreatingNewPin() throws Exception {
        // Arrange
        when(mPrefsUtil.getValue(anyString(), anyString())).thenReturn("");
        // Act
        boolean creatingNewPin = mSubject.isCreatingNewPin();
        // Assert
        assertEquals(true, creatingNewPin);
    }

    @Test
    public void isNotCreatingNewPin() throws Exception {
        // Arrange
        when(mPrefsUtil.getValue(anyString(), anyString())).thenReturn("1234567890");
        // Act
        boolean creatingNewPin = mSubject.isCreatingNewPin();
        // Assert
        assertEquals(false, creatingNewPin);
    }

    @Test
    public void getAppUtil() throws Exception {
        // Arrange

        // Act
        AppUtil util = mSubject.getAppUtil();
        // Assert
        assertEquals(util, mAppUtil);
    }

    private class MockApplicationModule extends ApplicationModule {

        MockApplicationModule(Application application) {
            super(application);
        }

        @Override
        protected AppUtil provideAppUtil() {
            return mAppUtil;
        }

        @Override
        protected PrefsUtil providePrefsUtil() {
            return mPrefsUtil;
        }

        @Override
        protected StringUtils provideStringUtils() {
            return mStringUtils;
        }

        @Override
        protected AccessState provideAccessState() {
            return mAccessState;
        }
    }

    private class MockApiModule extends ApiModule {

        @Override
        protected PayloadManager providePayloadManager() {
            return mPayloadManager;
        }

        @Override
        protected SSLVerifyUtil provideSSlVerifyUtil(Context context) {
            return mSslVerifyUtil;
        }
    }

    private class MockDataManagerModule extends DataManagerModule {

        @Override
        protected AuthDataManager provideAuthDataManager(PayloadManager payloadManager,
                                                         PrefsUtil prefsUtil,
                                                         AppUtil appUtil,
                                                         AESUtilWrapper aesUtilWrapper,
                                                         AccessState accessState,
                                                         StringUtils stringUtils) {
            return mAuthDataManager;
        }

        @Override
        protected FingerprintHelper provideFingerprintHelper(Context applicationContext, PrefsUtil prefsUtil) {
            return mFingerprintHelper;
        }
    }

}