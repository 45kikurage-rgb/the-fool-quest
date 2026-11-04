import com.aruno.foolquest.widget.next.CalendarDateMath;

public class CalendarDateMathTest {
    private static int checks=0;
    private static void eq(Object a,Object b){checks++;if(!a.equals(b))throw new AssertionError(a+" != "+b);}
    private static void eqNull(Object a){checks++;if(a!=null)throw new AssertionError("expected null: "+a);}
    public static void main(String[] args){
        eq(CalendarDateMath.daysInMonth("2026-02"),28);
        eq(CalendarDateMath.daysInMonth("2028-02"),29);
        eq(CalendarDateMath.daysInMonth("2026-11"),30);
        eq(CalendarDateMath.daysInMonth("2026-12"),31);
        eq(CalendarDateMath.firstWeekday("2026-11"),0);
        eq(CalendarDateMath.firstWeekday("2026-10"),4);
        eq(CalendarDateMath.normalizeCsvDate("2026/11/01"),"2026-11-01");
        eq(CalendarDateMath.normalizeCsvDate("2026-11-1 12:34"),"2026-11-01");
        eq(CalendarDateMath.normalizeCsvDate("2028/2/29"),"2028-02-29");
        eqNull(CalendarDateMath.normalizeCsvDate("2026/02/29"));
        eqNull(CalendarDateMath.normalizeCsvDate("2026/13/01"));
        eqNull(CalendarDateMath.normalizeCsvDate("no date"));
        System.out.println("PASS "+checks+" calendar/date checks");
    }
}
