import com.aruno.foolquest.widget.next.WidgetSpacing;
public class WidgetSpacingTest {
    static void ok(boolean b){if(!b)throw new AssertionError();}
    public static void main(String[] a){
        WidgetSpacing normal=new WidgetSpacing(584,20,24,60,12,14,20,0);
        ok(normal.gap==60&&normal.textGap==12&&normal.top==20&&normal.gauge==20&&!normal.adjusted);
        WidgetSpacing max=new WidgetSpacing(84,128,128,96,48,64,48,13);
        ok(max.adjusted&&max.font>0&&max.gauge>0);
        for(int h:new int[]{49,84,110,180,284,584})for(int gap:new int[]{0,5,18,60,96}){
            WidgetSpacing s=new WidgetSpacing(h,128,128,gap,48,64,48,13);
            float used=s.top+s.bottom+2*s.gap+3*(s.textGap+1.3f*s.font+s.gauge+4)+13;
            ok(used<=h+1.6f);ok(s.gap>=0&&s.font>=1&&s.gauge<=48);
        }
        System.out.println("PASS expanded spacing, exact gaps and small-host fit");
    }
}
