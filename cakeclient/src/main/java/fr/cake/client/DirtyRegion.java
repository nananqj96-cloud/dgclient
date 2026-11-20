package fr.cake.client;
import java.util.Arrays;

/** Smallest bounding rectangle containing changed ARGB pixels. */
public record DirtyRegion(int x,int y,int width,int height) {
 public static DirtyRegion between(int[] next,int[] previous,int width,int height){
  if(next.length!=previous.length||next.length!=width*height)throw new IllegalArgumentException("Image size mismatch");
  int minX=width,minY=height,maxX=-1,maxY=-1;
  for(int y=0;y<height;y++){
   int row=y*width,first=Arrays.mismatch(next,row,row+width,previous,row,row+width);if(first<0)continue;
   int last=width-1;while(last>first&&next[row+last]==previous[row+last])last--;
   minX=Math.min(minX,first);maxX=Math.max(maxX,last);minY=Math.min(minY,y);maxY=y;
  }
  return maxX<0?null:new DirtyRegion(minX,minY,maxX-minX+1,maxY-minY+1);
 }
}
