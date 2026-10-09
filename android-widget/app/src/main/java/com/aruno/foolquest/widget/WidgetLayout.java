package com.aruno.foolquest.widget;

/** Fixed columns shared by all rows, independent of the amount's digit count. */
public final class WidgetLayout {
    public final float left,right,top,bottom,labelEnd,amountEnd,percentEnd,rowHeight,footerHeight,currentEnd,slashCenter,goalEnd;
    public final boolean adjustedPadding;
    public WidgetLayout(float width,float height,float l,float r,float t,float b,float gap) {
        left=Math.min(l,width*.08f); right=width-Math.min(r,width*.08f);
        top=Math.min(t,height*.12f); bottom=height-Math.min(b,height*.12f);
        adjustedPadding=left<l||width-right<r||top<t||height-bottom<b;
        float content=right-left; labelEnd=left+content*.16f; amountEnd=left+content*.77f; percentEnd=right;
        // Each amount reserves ten monospace cells: yen + seven digits + two commas.
        float amountStart=labelEnd+3, cell=(amountEnd-2-amountStart)/21;
        currentEnd=amountStart+10*cell; slashCenter=amountStart+10.5f*cell; goalEnd=amountEnd-2;
        footerHeight=height>=100?12:0;
        rowHeight=(bottom-top-footerHeight-Math.min(gap,height*.1f)*2)/3;
    }
    public float labelWidth(){return labelEnd-left-3;}
    public float amountWidth(){return amountEnd-labelEnd-5;}
    public float moneyWidth(){return (goalEnd-labelEnd-3)/21*10;}
    public float percentWidth(){return percentEnd-amountEnd-4;}
}
