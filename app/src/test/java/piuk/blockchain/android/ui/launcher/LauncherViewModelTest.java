package piuk.blockchain.android.ui.launcher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static piuk.blockchain.android.ui.launcher.LauncherViewModel.INTENT_EXTRA_VERIFIED;

import android.app.Application;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import info.blockchain.wallet.payload.Payload;
import info.blockchain.wallet.payload.PayloadManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import piuk.blockchain.android.BlockchainTestApplication;
import piuk.blockchain.android.data.access.AccessState;
import piuk.blockchain.android.injection.ApiModule;
import piuk.blockchain.android.injection.ApplicationModule;
import piuk.blockchain.android.injection.DataManagerModule;
import piuk.blockchain.android.injection.Injector;
import piuk.blockchain.android.injection.InjectorTestUtils;
import piuk.blockchain.android.util.AppUtil;
import piuk.blockchain.android.util.PrefsUtil;

/**
 * Created by adambennett on 09/08/2016.
 */
@Config(sdk = 23, constants = piuk.blockchain.android.BuildConfig.class, application = BlockchainTestApplication.class)
@RunWith(RobolectricTestRunner.class)
public class LauncherViewModelTest {

    private LauncherViewModel mSubject;

    @Mock private LauncherActivity mLauncherActivity;
    @Mock private PrefsUtil mPrefsUtil;
    @Mock private AppUtil mAppUtil;
    @Mock private PayloadManager mPayloadManager;
    @Mock private AccessState mAccessState;
    @Mock private Intent mIntent;
    @Mock private Bundle mExtras;
    @Mock private Payload mPayload;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        InjectorTestUtils.initApplicationComponent(
                Injector.getInstance(),
                new MockApplicationModule(RuntimeEnvironment.application),
                new MockApiModule(),
                new DataManagerModule());

        mSubject = new LauncherViewModel(mLauncherActivity);
    }

    /**
     * Everything is good. Expected output is {@link LauncherActivity#onStartMainActivity()}
     */
    @Test
    public void onViewReadyVerified() throws Exception {
        // Arrange
        when(mLauncherActivity.getPageIntent()).thenReturn(mIntent);
        when(mIntent.getExtras()).thenReturn(mExtras);
        when(mExtras.containsKey(INTENT_EXTRA_VERIFIED)).thenReturn(true);
        when(mExtras.getBoolean(INTENT_EXTRA_VERIFIED)).thenReturn(true);
        when(mPrefsUtil.getValue(anyString(), anyString())).thenReturn("1234567890");
        when(mPrefsUtil.getValue(eq(PrefsUtil.LOGGED_OUT), anyBoolean())).thenReturn(false);
        when(mAppUtil.isSane()).thenReturn(true);
        when(mPayloadManager.getPayload()).thenReturn(mPayload);
        when(mPayload.isUpgraded()).thenReturn(true);
        when(mAccessState.isLoggedIn()).thenReturn(true);
        // Act
        mSubject.onViewReady();
        // Assert
        verify(mLauncherActivity).onStartMainActivity();
    }

    /**
     * Bitcoin URI is found, expected to step into Bitcoin branch and call {@link
     * LauncherActivity#onStartMainActivity()}
     */
    @Test
    public void onViewReadyBitcoinUri() throws Exception {
        // Arrange
        when(mLauncherActivity.getPageIntent()).thenReturn(mIntent);
        when(mIntent.getAction()).thenReturn(Intent.ACTION_VIEW);
        when(mIntent.getScheme()).thenReturn("bitcoin");
        when(mIntent.getData()).thenReturn(Uri.parse("bitcoin uri"));
        when(mIntent.getExtras()).thenReturn(mExtras);
        when(mExtras.containsKey(INTENT_EXTRA_VERIFIED)).thenReturn(true);
        when(mExtras.getBoolean(INTENT_EXTRA_VERIFIED)).thenReturn(true);
        when(mPrefsUtil.getValue(anyString(), anyString())).thenReturn("1234567890");
        when(mPrefsUtil.getValue(eq(PrefsUtil.LOGGED_OUT), anyBoolean())).thenReturn(false);
        when(mAppUtil.isSane()).thenReturn(true);
        when(mPayloadManager.getPayload()).thenReturn(mPayload);
        when(mPayload.isUpgraded()).thenReturn(true);
        when(mAccessState.isLoggedIn()).thenReturn(true);
        // Act
        mSubject.onViewReady();
        // Assert
        verify(mPrefsUtil).setValue(PrefsUtil.KEY_SCHEME_URL, "bitcoin uri");
        verify(mLauncherActivity).onStartMainActivity();
    }

    /**
     * Everything is fine, but PIN not validated. Expected output is {@link
     * LauncherActivity#onRequestPin()}
     */
    @Test
    public void onViewReadyNotVerified() throws Exception {
        // Arrange
        when(mLauncherActivity.getPageIntent()).thenReturn(mIntent);
        when(mIntent.getExtras()).thenReturn(mExtras);
        when(mExtras.containsKey(INTENT_EXTRA_VERIFIED)).thenReturn(false);
        when(mPrefsUtil.getValue(anyString(), anyString())).thenReturn("1234567890");
        when(mPrefsUtil.getValue(eq(PrefsUtil.LOGGED_OUT), anyBoolean())).thenReturn(false);
        when(mAppUtil.isSane()).thenReturn(true);
        when(mPayloadManager.getPayload()).thenReturn(mPayload);
        when(mPayload.isUpgraded()).thenReturn(true);
        when(mAccessState.isLoggedIn()).thenReturn(false);
        // Act
        mSubject.onViewReady();
        // Assert
        verify(mLauncherActivity).onRequestPin();
    }

    /**
     * Everything is fine, but PIN not validated. However, {@link AccessState} returns logged in.
     * Expected output is {@link LauncherActivity#onStartMainActivity()}
     */
    @Test
    public void onViewReadyPinNotValidatedButLoggedInt() throws Exception {
        // Arrange
        when(mLauncherActivity.getPageIntent()).thenReturn(mIntent);
        when(mIntent.getExtras()).thenReturn(mExtras);
        when(mExtras.containsKey(INTENT_EXTRA_VERIFIED)).thenReturn(false);
        when(mPrefsUtil.getValue(anyString(), anyString())).thenReturn("1234567890");
        when(mPrefsUtil.getValue(eq(PrefsUtil.LOGGED_OUT), anyBoolean())).thenReturn(false);
        when(mAppUtil.isSane()).thenReturn(true);
        when(mPayloadManager.getPayload()).thenReturn(mPayload);
        when(mPayload.isUpgraded()).thenReturn(true);
        when(mAccessState.isLoggedIn()).thenReturn(true);
        // Act
        mSubject.onViewReady();
        // Assert
        verify(mAccessState).setIsLoggedIn(true);
        verify(mLauncherActivity).onStartMainActivity();
    }

    /**
     * GUID not found, expected output is {@link LauncherActivity#onNoGuid()}
     */
    @Test
    public void onViewReadyNoGuid() throws Exception {
        // Arrange
        when(mLauncherActivity.getPageIntent()).thenReturn(mIntent);
        when(mIntent.getExtras()).thenReturn(mExtras);
        when(mExtras.containsKey(INTENT_EXTRA_VERIFIED)).thenReturn(false);
        when(mPrefsUtil.getValue(anyString(), anyString())).thenReturn("");
        // Act
        mSubject.onViewReady();
        // Assert
        verify(mLauncherActivity).onNoGuid();
    }

    /**
     * Pin not found, expected output is {@link LauncherActivity#onRequestPin()}
     */
    @Test
    public void onViewReadyNoPin() throws Exception {
        // Arrange
        when(mLauncherActivity.getPageIntent()).thenReturn(mIntent);
        when(mIntent.getExtras()).thenReturn(mExtras);
        when(mExtras.containsKey(INTENT_EXTRA_VERIFIED)).thenReturn(false);
        when(mPrefsUtil.getValue(eq(PrefsUtil.KEY_GUID), anyString())).thenReturn("1234567890");
        when(mPrefsUtil.getValue(eq(PrefsUtil.KEY_PIN_IDENTIFIER), anyString())).thenReturn("");
        // Act
        mSubject.onViewReady();
        // Assert
        verify(mLauncherActivity).onRequestPin();
    }

    /**
     * AppUtil returns not sane. Expected output is {@link LauncherActivity#onCorruptPayload()}
     */
    @Test
    public void onViewReadyNotSane() throws Exception {
        // Arrange
        when(mLauncherActivity.getPageIntent()).thenReturn(mIntent);
        when(mIntent.getExtras()).thenReturn(mExtras);
        when(mExtras.containsKey(INTENT_EXTRA_VERIFIED)).thenReturn(false);
        when(mPrefsUtil.getValue(anyString(), anyString())).thenReturn("1234567890");
        when(mAppUtil.isSane()).thenReturn(false);
        // Act
        mSubject.onViewReady();
        // Assert
        verify(mLauncherActivity).onCorruptPayload();
    }

    /**
     * Everything is fine, but not upgraded. Expected output is {@link
     * LauncherActivity#onRequestUpgrade()}
     */
    @Test
    public void onViewReadyNotUpgraded() throws Exception {
        // Arrange
        when(mLauncherActivity.getPageIntent()).thenReturn(mIntent);
        when(mIntent.getExtras()).thenReturn(mExtras);
        when(mExtras.containsKey(INTENT_EXTRA_VERIFIED)).thenReturn(true);
        when(mExtras.getBoolean(INTENT_EXTRA_VERIFIED)).thenReturn(true);
        when(mPrefsUtil.getValue(anyString(), anyString())).thenReturn("1234567890");
        when(mPrefsUtil.getValue(eq(PrefsUtil.LOGGED_OUT), anyBoolean())).thenReturn(false);
        when(mAppUtil.isSane()).thenReturn(true);
        when(mPayloadManager.getPayload()).thenReturn(mPayload);
        when(mPayload.isUpgraded()).thenReturn(false);
        // Act
        mSubject.onViewReady();
        // Assert
        verify(mLauncherActivity).onRequestUpgrade();
    }

    /**
     * GUID exists, Shared Key exists but user logged out. Expected output is {@link
     * LauncherActivity#onReEnterPassword()}
     */
    @Test
    public void onViewReadyUserLoggedOut() throws Exception {
        // Arrange
        when(mLauncherActivity.getPageIntent()).thenReturn(mIntent);
        when(mIntent.getExtras()).thenReturn(mExtras);
        when(mExtras.containsKey(INTENT_EXTRA_VERIFIED)).thenReturn(false);
        when(mPrefsUtil.getValue(anyString(), anyString())).thenReturn("1234567890");
        when(mPrefsUtil.getValue(eq(PrefsUtil.LOGGED_OUT), anyBoolean())).thenReturn(true);
        // Act
        mSubject.onViewReady();
        // Assert
        verify(mLauncherActivity).onReEnterPassword();
    }

    /**
     * For 100% coverage
     */
    @Test
    public void destroy() {
        // Arrange

        // Act
        mSubject.destroy();
        // Assert
        assertTrue(true);
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
        protected PrefsUtil providePrefsUtil() {
            return mPrefsUtil;
        }

        @Override
        protected AppUtil provideAppUtil() {
            return mAppUtil;
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
    }
}