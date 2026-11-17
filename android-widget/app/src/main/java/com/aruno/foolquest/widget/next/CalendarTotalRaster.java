package com.aruno.foolquest.widget.next;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.TextPaint;
import android.util.TypedValue;
import android.view.View;
import android.widget.TextView;

/** Draw totals in the app, before a launcher's restricted RemoteViews context
 * can substitute a system font for the bundled font resource. */
final class CalendarTotalRaster {
    private CalendarTotalRaster() {}

    static final class Result {
        final Bitmap bitmap;
        final int width,height,baseline,inkTop,inkBottom;
        Result(Bitmap bitmap,int baseline,int inkTop,int inkBottom) {
            this.bitmap=bitmap;
            width=bitmap.getWidth();height=bitmap.getHeight();
            this.baseline=baseline;this.inkTop=inkTop;this.inkBottom=inkBottom;
        }
    }

    static Result render(Context c,CharSequence styled,TextPaint actual,int color) {
        // Retain code49's native height, baseline, spans and horizontal scale.
        // Only the transparent horizontal bearing is removed below, so the
        // total's slot and the C/T rows keep their existing vertical geometry.
        TextView probe=new TextView(c,null,0,R.style.CalendarDailyAmount);
        probe.setTypeface(actual.getTypeface());
        probe.setText(styled);
        probe.setTextSize(TypedValue.COMPLEX_UNIT_PX,actual.getTextSize());
        probe.setTextScaleX(actual.getTextScaleX());
        probe.setPaintFlags(actual.getFlags());
        probe.setTextColor(color);
        int unspecified=View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED);
        probe.measure(unspecified,unspecified);
        int sourceWidth=Math.max(1,probe.getMeasuredWidth());
        int height=Math.max(1,probe.getMeasuredHeight());
        probe.layout(0,0,sourceWidth,height);

        Bitmap source=Bitmap.createBitmap(sourceWidth,height,Bitmap.Config.ARGB_8888);
        source.setDensity(c.getResources().getDisplayMetrics().densityDpi);
        probe.draw(new Canvas(source));
        int[] pixels=new int[sourceWidth*height];
        source.getPixels(pixels,0,sourceWidth,0,0,sourceWidth,height);
        int left=sourceWidth,right=-1,top=height,bottom=-1;
        for(int y=0;y<height;y++)for(int x=0;x<sourceWidth;x++){
            int alpha=pixels[y*sourceWidth+x]>>>24;
            if(alpha>0){left=Math.min(left,x);right=Math.max(right,x);}
            if(alpha>=160){top=Math.min(top,y);bottom=Math.max(bottom,y);}
        }
        if(right<left){
            // Empty text remains an empty native-height row; it does not
            // acquire an underline merely because the bitmap needs a width.
            Bitmap empty=Bitmap.createBitmap(1,height,Bitmap.Config.ARGB_8888);
            empty.setDensity(source.getDensity());source.recycle();
            return new Result(empty,probe.getBaseline(),0,0);
        }

        // Copy the already drawn pixels. There is no second text measurement
        // or rendering path whose font advances can disagree with the image.
        Bitmap image=Bitmap.createBitmap(right-left+1,height,Bitmap.Config.ARGB_8888);
        image.setDensity(source.getDensity());
        Canvas canvas=new Canvas(image);
        canvas.drawBitmap(source,-left,0,null);
        Paint rule=new Paint();rule.setColor(color);
        canvas.drawRect(0,bottom,image.getWidth(),bottom+1,rule);
        source.recycle();
        return new Result(image,probe.getBaseline(),top,bottom+1);
    }
}
