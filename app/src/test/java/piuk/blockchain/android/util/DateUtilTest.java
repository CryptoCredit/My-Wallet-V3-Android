package piuk.blockchain.android.util;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.junit.MatcherAssert.assertThat;

import android.annotation.SuppressLint;
import android.content.Context;
import java.text.SimpleDateFormat;
import java.util.Calendar;
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
        assertThat(dateUtil.formatted(parseDateTime(year + "-01-01 00:00:00")), is("January 1"));
        assertThat(dateUtil.formatted(parseDateTime("2015-12-31 23:59:59")), is("December 31, 2015"));
        assertThat(dateUtil.formatted(parseDateTime("2015-01-01 00:00:00")), is("January 1, 2015"));

        assertThat(dateUtil.formatted(parseDateTime(year + "-04-15 00:00:00")), is("April 15"));
        assertThat(dateUtil.formatted(parseDateTime(year + "-04-15 12:00:00")), is("April 15"));
        assertThat(dateUtil.formatted(parseDateTime(year + "-04-15 23:00:00")), is("April 15"));
        assertThat(dateUtil.formatted(parseDateTime(year + "-04-15 23:59:59")), is("April 15"));

        assertThat(dateUtil.formatted(parseDateTime("2015-04-15 00:00:00")), is("April 15, 2015"));
        assertThat(dateUtil.formatted(parseDateTime("2015-04-15 12:00:00")), is("April 15, 2015"));
        assertThat(dateUtil.formatted(parseDateTime("2015-04-15 23:00:00")), is("April 15, 2015"));
        assertThat(dateUtil.formatted(parseDateTime("2015-04-15 23:59:59")), is("April 15, 2015"));
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