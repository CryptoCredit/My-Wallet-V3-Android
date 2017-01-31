package piuk.blockchain.android.ui.pairing;

import android.app.Application;
import info.blockchain.wallet.payload.PayloadManager;
import junit.framework.Assert;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import piuk.blockchain.android.injection.ApiModule;
import piuk.blockchain.android.injection.ApplicationModule;
import piuk.blockchain.android.injection.DataManagerModule;
import piuk.blockchain.android.injection.Injector;
import piuk.blockchain.android.injection.InjectorTestUtils;
import piuk.blockchain.android.ui.pairing.PairingViewModel.DataListener;
import piuk.blockchain.android.util.AppUtil;
import piuk.blockchain.android.util.PrefsUtil;

public class PairingViewModelTest {

    private PairingViewModel mSubject;

    @Mock
    private DataListener mockListener;

    @Mock private PayloadManager payloadManager;
    @Mock private AppUtil appUtil;
    @Mock private PrefsUtil prefsUtil;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        InjectorTestUtils.initApplicationComponent(
            Injector.getInstance(),
            new MockApplicationModule(RuntimeEnvironment.application),
            new MockApiModule(),
            new DataManagerModule());

        mSubject = new PairingViewModel(mockListener);
    }

    @Test
    public void decode_whenBadString_shouldFail() {

        String qrData_withMissingParts = "1|524b5e9f-72ea-4690-b28c-8c1cfce65ca0MZfQWMPJHjUkAqlEOrm97qIryrXygiXlPNQGh3jppS6GXJZf5mmD2kti0Mf/Bwqw7+OCWWqUf8r19EB+YmgRcWmGxsstWPE2ZR4oJrKpmpo=";

        try {
            Assert.assertNull(mSubject.getQRComponentsFromRawString(qrData_withMissingParts));
            Assert.fail();
        } catch (Exception e) {
            Assert.assertTrue(true);
        }
    }

    @Test
    public void decode_whenGoodString_shouldPass() throws Exception {

        String guid = "a09910d9-1906-4ea1-a956-2508c3fe0661";
        String strGood = "1|"+guid+"|TGbFKLZQ+ZxaAyDwdUcMOAtzolqUYMdkjOYautXPNt41AXqjk67P9aDqRPMM4mmbZ0VPDEpr/xYBSBhjxDCye4L9/MwABu6S3NNV8x+Kn/Q=";

        Pair<String, String> components = mSubject.getQRComponentsFromRawString(strGood);

        Assert.assertEquals(guid, components.getLeft());
    }

    private class MockApplicationModule extends ApplicationModule {

        MockApplicationModule(Application application) {
            super(application);
        }

        @Override
        protected PrefsUtil providePrefsUtil() {
            return prefsUtil;
        }

        @Override
        protected AppUtil provideAppUtil() {
            return appUtil;
        }
    }

    private class MockApiModule extends ApiModule {
        @Override
        protected PayloadManager providePayloadManager() {
            return payloadManager;
        }
    }
}
