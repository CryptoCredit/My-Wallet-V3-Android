package piuk.blockchain.android.util;

import android.annotation.SuppressLint;
import android.content.Context;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import junit.framework.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import piuk.blockchain.android.BlockchainTestApplication;
import piuk.blockchain.android.BuildConfig;

@Config(sdk = 23, constants = BuildConfig.class, application = BlockchainTestApplication.class)
@RunWith(RobolectricTestRunner.class)
public class DateUtilTest {

    @Mock Context mMockContext;

    @Test
    public void dateFormatTest() {
        DateUtil dateUtil = new DateUtil(mMockContext);

        //unit test for 'Today' and 'Yesterday' uses android framework (code unchanged)

        Calendar now = Calendar.getInstance();
        String year = String.valueOf(now.get(Calendar.YEAR));
        // Pass in current year so that tests don't break after new year
        Assert.assertEquals("January 1", dateUtil.formatted(parseDateTime(year + "-01-01 00:00:00")));
        Assert.assertEquals("January 1", dateUtil.formatted(parseDateTime(year + "-01-01 00:00:00")));
        Assert.assertEquals("December 31, 2015", dateUtil.formatted(parseDateTime("2015-12-31 23:59:59")));
        Assert.assertEquals("January 1, 2015", dateUtil.formatted(parseDateTime("2015-01-01 00:00:00")));

        Assert.assertEquals("April 15", dateUtil.formatted(parseDateTime(year + "-04-15 00:00:00")));
        Assert.assertEquals("April 15", dateUtil.formatted(parseDateTime(year + "-04-15 12:00:00")));
        Assert.assertEquals("April 15", dateUtil.formatted(parseDateTime(year + "-04-15 23:00:00")));
        Assert.assertEquals("April 15", dateUtil.formatted(parseDateTime(year + "-04-15 23:59:59")));

        Assert.assertEquals("April 15, 2015", dateUtil.formatted(parseDateTime("2015-04-15 00:00:00")));
        Assert.assertEquals("April 15, 2015", dateUtil.formatted(parseDateTime("2015-04-15 12:00:00")));
        Assert.assertEquals("April 15, 2015", dateUtil.formatted(parseDateTime("2015-04-15 23:00:00")));
        Assert.assertEquals("April 15, 2015", dateUtil.formatted(parseDateTime("2015-04-15 23:59:59")));
    }

    @SuppressWarnings("EmptyCatchBlock")
    @SuppressLint("SimpleDateFormat")
    private long parseDateTime(String time) {
        try {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(time).getTime() / 1000;
        } catch (Exception e) {
            // No-op
        }
        return 0;
    }
}