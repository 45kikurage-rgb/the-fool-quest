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
        System.out.println("PASS "+assertions+" daily-6-digit formatting checks");
    }
}
