package piuk.blockchain.android.data.datamanagers;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.TestCase.assertEquals;

import android.graphics.Bitmap;
import io.reactivex.observers.TestObserver;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import piuk.blockchain.android.BlockchainTestApplication;
import piuk.blockchain.android.BuildConfig;
import piuk.blockchain.android.RxTest;

@Config(sdk = 23, constants = BuildConfig.class, application = BlockchainTestApplication.class)
@RunWith(RobolectricTestRunner.class)
public class QrCodeDataManagerTest extends RxTest {

    private QrCodeDataManager mSubject;
    private static final String TEST_URI = "bitcoin://1234567890";

    @Before
    public void setUp() throws Exception {
        super.setUp();
        mSubject = new QrCodeDataManager();
    }

    @Test
    public void generateQrCode() throws Exception {
        // Arrange

        // Act
        TestObserver<Bitmap> observer = mSubject.generateQrCode(TEST_URI, 100).test();
        // Assert
        Bitmap bitmap = observer.values().get(0);
        assertNotNull(bitmap);
        assertEquals(100, bitmap.getWidth());
        assertEquals(100, bitmap.getHeight());
        observer.assertComplete();
        observer.assertNoErrors();
    }

    @Test
    public void generateQrCodeNullUri() throws Exception {
        // Arrange

        // Act
        TestObserver<Bitmap> observer = mSubject.generateQrCode(null, 100).test();
        // Assert
        observer.assertError(Throwable.class);
    }

    @Test
    public void generateQrCodeInvalidDimensions() throws Exception {
        // Arrange

        // Act
        TestObserver<Bitmap> observer = mSubject.generateQrCode(TEST_URI, -1).test();
        // Assert
        observer.assertError(Throwable.class);
    }

}