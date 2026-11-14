import com.aruno.foolquest.widget.next.CalendarAmountFormatter;
import com.aruno.foolquest.widget.next.RevenueMath;

public class CalendarAmountFormatterTest {
    private static int assertions=0;
    private static void eq(String actual,String expected) {
        assertions++;
        if(!expected.equals(actual))throw new AssertionError("expected: "+expected+", actual: "+actual);
    }
    public static void main(String[] args) {
        eq(CalendarAmountFormatter.day(-1),"—");
        eq(CalendarAmountFormatter.dailyLoss(0),"");
        eq(CalendarAmountFormatter.dailyLoss(1500),"−1500");
        eq(CalendarAmountFormatter.dayNet(-50,true),"−50");
        eq(CalendarAmountFormatter.dayNet(-1,true),"−1");
        eq(CalendarAmountFormatter.dayNet(-1,false),"—");
        eq(CalendarAmountFormatter.monthNet(-125,true),"−¥125");
        eq(CalendarAmountFormatter.monthNet(RevenueMath.UNKNOWN,false),"—");
        eq(CalendarAmountFormatter.day(0),"0");
        eq(CalendarAmountFormatter.day(9999),"9999");
        eq(CalendarAmountFormatter.day(10000),"10000");
        eq(CalendarAmountFormatter.day(99999),"99999");
        eq(CalendarAmountFormatter.day(100000),"100000");
        eq(CalendarAmountFormatter.day(999999),"999999");
        eq(CalendarAmountFormatter.day(1000000),"100万");
        eq(CalendarAmountFormatter.day(1000001),"≈100万");
        eq(CalendarAmountFormatter.dayWithPrefix("C",999999),"C 999999");
        eq(CalendarAmountFormatter.dayWithPrefix("T",999999),"T 999999");
        eq(CalendarAmountFormatter.month(999999),"¥999,999");
        eq(CalendarAmountFormatter.month(1234567),"¥1,234,567");
        eq(CalendarAmountFormatter.compact(57250,true),"5.7万");
        eq(CalendarAmountFormatter.compact(999000,true),"99.9万");
        eq(CalendarAmountFormatter.compact(999499,true),"99.9万");
        eq(CalendarAmountFormatter.compact(999500,true),"100.0万");
        eq(CalendarAmountFormatter.compact(1,true),"0.0万");
        eq(CalendarAmountFormatter.compact(-1,true),"0.0万");
        eq(CalendarAmountFormatter.compact(-54290,true),"−5.4万");
        eq(CalendarAmountFormatter.compactLoss(110000),"−11.0万");
        eq(CalendarAmountFormatter.compact(0,false),"—");
        eq(CalendarAmountFormatter.compactOrZero(0,false),"0.0万");
        eq(CalendarAmountFormatter.compactOrZero(-1,false),"0.0万");
        eq(CalendarAmountFormatter.compactOrZero(RevenueMath.UNKNOWN,true),"0.0万");
        eq(CalendarAmountFormatter.compactOrZero(0,true),"0.0万");
        eq(CalendarAmountFormatter.compactOrZero(57250,true),"5.7万");
        eq(CalendarAmountFormatter.compactOrZero(-54290,true),"−5.4万");
        eq(CalendarAmountFormatter.forecast(654152,true,9,30),"218万");
        eq(CalendarAmountFormatter.forecast(491050,true,9,30),"163万");
        eq(CalendarAmountFormatter.forecast(163102,true,9,30),"54万");
        eq(CalendarAmountFormatter.forecast(10000,true,1,29),"29万");
        eq(CalendarAmountFormatter.forecast(19999,true,31,31),"1万");
        eq(CalendarAmountFormatter.forecast(-19999,true,31,31),"−1万");
        eq(CalendarAmountFormatter.forecast(10000,true,0,31),"—");
        eq(CalendarAmountFormatter.forecast(0,true,10,30),"0万");
        eq(CalendarAmountFormatter.forecast(10,false,10,30),"—");
        System.out.println("PASS "+assertions+" daily-6-digit formatting checks");
    }
}
