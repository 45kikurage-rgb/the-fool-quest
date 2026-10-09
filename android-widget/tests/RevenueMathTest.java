import com.aruno.foolquest.widget.RevenueMath;
import com.aruno.foolquest.widget.WidgetLayout;
import java.time.Instant;

public class RevenueMathTest {
    static int checks;
    static void eq(Object actual,Object expected){checks++;if(!actual.equals(expected))throw new AssertionError(actual+" != "+expected);}
    static void ok(boolean value){checks++;if(!value)throw new AssertionError("check failed");}
    static void invalid(String value,boolean positive){checks++;try{RevenueMath.yen(value,positive);throw new AssertionError("accepted: "+value);}catch(IllegalArgumentException expected){}}
    public static void main(String[] args){
        eq(RevenueMath.money(1000000),"¥1,000,000");eq(RevenueMath.money(9999999),"¥9,999,999");eq(RevenueMath.money(15),"¥15");eq(RevenueMath.money(-1),"—");
        eq(RevenueMath.percent(1000000,1000000),"100.00%");eq(RevenueMath.percent(1234567,1000000),"123.46%");eq(RevenueMath.percent(10000000,1),"1000000000.00%");eq(RevenueMath.percent(-1,500000),"—%");
        eq(RevenueMath.progress(1000001,1000000),1f);eq(RevenueMath.progress(500000,1000000),.5f);eq(RevenueMath.progress(-1,1000000),0f);
        eq(RevenueMath.total(500000,302990),802990L);eq(RevenueMath.total(-1,302990),-1L);eq(RevenueMath.total(RevenueMath.MAX_YEN,1),-1L);
        eq(RevenueMath.month(Instant.parse("2026-09-30T14:59:59Z").toEpochMilli()),"2026-09");eq(RevenueMath.month(Instant.parse("2026-09-30T15:00:00Z").toEpochMilli()),"2026-10");
        long ninth=Instant.parse("2026-10-09T04:00:00Z").toEpochMilli();
        eq(RevenueMath.targetThroughToday("2026-10",1200000,ninth),348388L);
        eq(RevenueMath.targetThroughToday("2026-10",600000,ninth),174194L);
        eq(RevenueMath.targetThroughToday("2026-10",1,ninth),1L);
        eq(RevenueMath.targetThroughToday("2026-10",-1,ninth),-1L);
        eq(RevenueMath.targetThroughToday("2026-10",0,ninth),-1L);
        eq(RevenueMath.targetThroughToday("2026-09",1200000,ninth),-1L);
        eq(RevenueMath.targetThroughToday("2026-10",1000000,Instant.parse("2026-09-30T15:00:00Z").toEpochMilli()),32259L);
        eq(RevenueMath.targetThroughToday("2026-10",1000000,Instant.parse("2026-10-31T14:59:59Z").toEpochMilli()),1000000L);
        eq(RevenueMath.targetThroughToday("2026-10",RevenueMath.MAX_YEN,Instant.parse("2026-10-31T14:59:59Z").toEpochMilli()),RevenueMath.MAX_YEN);
        eq(RevenueMath.targetThroughToday("2028-02",2900,Instant.parse("2028-02-29T00:00:00Z").toEpochMilli()),2900L);
        eq(RevenueMath.targetThroughToday("2026-02",2800,Instant.parse("2026-02-28T00:00:00Z").toEpochMilli()),2800L);
        // Integer revenues surrounding the exact site 50% and 100% daily pace boundaries.
        eq(RevenueMath.pace(348388,1200000,"2026-10",ninth),RevenueMath.PACE_ON_TRACK);
        eq(RevenueMath.pace(348387,1200000,"2026-10",ninth),RevenueMath.PACE_BEHIND);
        eq(RevenueMath.pace(174194,1200000,"2026-10",ninth),RevenueMath.PACE_BEHIND);
        eq(RevenueMath.pace(174193,1200000,"2026-10",ninth),RevenueMath.PACE_LOW);
        eq(RevenueMath.pace(0,1200000,"2026-10",ninth),RevenueMath.PACE_LOW);
        eq(RevenueMath.pace(2000000,1200000,"2026-10",ninth),RevenueMath.PACE_ON_TRACK);
        eq(RevenueMath.pace(-1,1200000,"2026-10",ninth),RevenueMath.PACE_UNKNOWN);
        eq(RevenueMath.pace(1,-1,"2026-10",ninth),RevenueMath.PACE_UNKNOWN);
        eq(RevenueMath.pace(1,0,"2026-10",ninth),RevenueMath.PACE_UNKNOWN);
        eq(RevenueMath.pace(1000000,1200000,"2026-09",ninth),RevenueMath.PACE_UNKNOWN);
        eq(RevenueMath.pace(0,1,"2026-10",ninth),RevenueMath.PACE_LOW);
        eq(RevenueMath.pace(1,1,"2026-10",ninth),RevenueMath.PACE_ON_TRACK);
        long midnight=Instant.parse("2026-10-09T15:00:00Z").toEpochMilli();
        eq(RevenueMath.pace(350000,1200000,"2026-10",ninth),RevenueMath.PACE_ON_TRACK);
        eq(RevenueMath.pace(350000,1200000,"2026-10",midnight),RevenueMath.PACE_BEHIND);
        for(String date:new String[]{"2026-10-01T00:00:00Z","2026-10-31T00:00:00Z","2028-02-29T00:00:00Z","2026-02-28T00:00:00Z"}){
            long time=Instant.parse(date).toEpochMilli();String month=RevenueMath.month(time);
            long goal=RevenueMath.MAX_YEN,target=RevenueMath.targetThroughToday(month,goal,time);
            eq(RevenueMath.pace(target,goal,month,time),RevenueMath.PACE_ON_TRACK);
            eq(RevenueMath.pace(target-1,goal,month,time),RevenueMath.PACE_BEHIND);
            eq(RevenueMath.pace(0,goal,month,time),RevenueMath.PACE_LOW);
        }
        eq(RevenueMath.yen("0",false),0L);eq(RevenueMath.yen("9007199254740991",false),9007199254740991L);
        invalid("9007199254740992",false);invalid("-1",false);invalid("NaN",false);invalid("1.5",false);invalid("1e5",false);invalid("0",true);invalid(null,false);
        ok(RevenueMath.validMonth("2026-10"));ok(!RevenueMath.validMonth("2026-13"));
        for(int width:new int[]{280,320,360,400,500})for(int height:new int[]{100,110,126,150,200}){
            WidgetLayout l=new WidgetLayout(width,height,8,8,6,6,5);
            ok(l.left<l.labelEnd&&l.labelEnd<l.amountEnd&&l.amountEnd<l.right);ok(l.rowHeight>0);ok(l.amountWidth()>0&&l.percentWidth()>0);
            ok(l.currentEnd<l.slashCenter&&l.slashCenter<l.goalEnd);ok(l.moneyWidth()>0);
            // Cash amounts never participate in column allocation.
            WidgetLayout same=new WidgetLayout(width,height,8,8,6,6,5);eq(l.amountEnd,same.amountEnd);
        }
        WidgetLayout tiny=new WidgetLayout(120,65,32,32,24,24,18);ok(tiny.adjustedPadding);ok(tiny.rowHeight>0);
        System.out.println("PASS "+checks+" display/math/layout checks");
    }
}
