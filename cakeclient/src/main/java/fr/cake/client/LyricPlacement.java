package fr.cake.client;

import java.util.ArrayList;
import java.util.List;

/** Fits at most six separate lyric cards inside the forward viewing cone. */
public final class LyricPlacement {
 public record Place(double x,double y,double width,double height){}
 private static double unit(double value){return Double.isFinite(value)?Math.clamp(value,0,1):.5;}
 public static List<Place> arrange(int count,int line,double distance,double size,double spacing,double height,double verticalFov,double aspect){
  if(count<=0)return List.of();count=Math.min(6,count);
  distance=Math.max(1,distance);aspect=Math.clamp(aspect,.5,4);
  double halfY=distance*Math.tan(Math.toRadians(Math.clamp(verticalFov,30,110)/2))*.64;
  double halfX=halfY*aspect;
  int columns=count==1?1:2,rows=(count+columns-1)/columns;
  double gap=.14+unit(spacing)*.7;
  double centerY=(unit(height)-.5)*halfY*.8;
  double wanted=distance*(.68+unit(size)*.85);
  double width=Math.min(wanted,2*halfX/(columns+gap*(columns-1)));
  width=Math.min(width,(2*(halfY-Math.abs(centerY)))/(rows*384./1920+(rows-1)*.13));
  width*=.94;
  double cardHeight=width*384/1920;
  double rowStep=cardHeight+width*.13;
  double slackX=Math.max(0,halfX-width*(columns+gap*(columns-1))/2);
  double shiftX=(Math.floorMod(line,11)/5.-1)*slackX*.85;
  double maxCenter=Math.max(0,halfY-((rows-1)*rowStep+cardHeight)/2-cardHeight*.2);
  centerY=Math.clamp(centerY+(Math.floorMod(line,7)/3.-1)*cardHeight*.65,-maxCenter,maxCenter);
  List<Place> places=new ArrayList<>(count);
  for(int i=0;i<count;i++){
   int row=i/columns,col=i%columns,inRow=Math.min(columns,count-row*columns);
   double x=(col-(inRow-1)/2.)*width*(1+gap);
   if(Math.floorMod(line,5)==4)x=-x;x+=shiftX;
   double y=centerY+((rows-1)/2.-row)*rowStep;
   // Small alternating diagonal; bounded by the same forward viewing cone.
   double stagger=count>1&&Math.floorMod(line,3)!=0?((i%2==0?1:-1)*(Math.floorMod(line,3)==1?1:-1))*cardHeight*.16:0;
   y=Math.clamp(y+stagger,-halfY+cardHeight/2,halfY-cardHeight/2);
   places.add(new Place(x,y,width,cardHeight));
  }
  return List.copyOf(places);
 }
}
