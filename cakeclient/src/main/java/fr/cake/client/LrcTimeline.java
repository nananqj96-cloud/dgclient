package fr.cake.client;
import java.util.*;
import java.util.regex.*;
/** Immutable LRC timeline; interpolation is line-based, never claimed to be word timing. */
public final class LrcTimeline {
 public record Line(long start,String text){}
 private static final Pattern TIME=Pattern.compile("\\[(\\d{1,3}):(\\d{2})(?:[.:](\\d{1,3}))?\\]");
 public final List<Line> lines;
 private LrcTimeline(List<Line> lines){this.lines=List.copyOf(lines);}
 public static LrcTimeline parse(String lrc){
  long offset=0;Matcher off=Pattern.compile("\\[offset:([+-]?\\d+)\\]",Pattern.CASE_INSENSITIVE).matcher(lrc);if(off.find())try{offset=Long.parseLong(off.group(1));}catch(NumberFormatException ignored){}
  ArrayList<Line> out=new ArrayList<>();
  for(String raw:lrc.split("\\R")){
   Matcher m=TIME.matcher(raw);List<Long> times=new ArrayList<>();int end=0;
   while(m.find()){long mm=Long.parseLong(m.group(1)),ss=Long.parseLong(m.group(2));if(ss>=60)continue;String fraction=m.group(3);long millis=fraction==null?0:Long.parseLong((fraction+"000").substring(0,3));times.add(Math.max(0,mm*60000+ss*1000+millis-offset));end=m.end();}
   if(times.isEmpty())continue;String text=raw.substring(end).replaceAll("<\\d{1,3}:\\d{2}(?:\\.\\d{1,3})?>","").trim();
   if(text.length()>500)text=text.substring(0,500);
   for(long t:times)out.add(new Line(t,text));
  }
  out.sort(Comparator.comparingLong(Line::start));return new LrcTimeline(out);
 }
 public int index(long position){int lo=0,hi=lines.size()-1,result=-1;while(lo<=hi){int m=(lo+hi)>>>1;if(lines.get(m).start<=position){result=m;lo=m+1;}else hi=m-1;}return result;}
 public double progress(int index,long position,long duration){if(index<0||index>=lines.size())return 0;long start=lines.get(index).start;long end=index+1<lines.size()?lines.get(index+1).start:(duration>start?duration:start+5000);return Math.clamp((position-start)/(double)Math.max(250,end-start),0,1);}
}
