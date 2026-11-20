package fr.cake.client;
import java.util.*;
/** Short readable groups, balanced so three words become a 2 + 1 split. */
public final class LyricChunks {
 public record Chunk(String text,int characters,int cumulative){}
 public static List<Chunk> split(String text){
  String[] words=text.strip().split("\\s+");if(words.length==0||text.isBlank())return List.of();
  int groups=Math.max(2,(int)Math.ceil(words.length/3.));if(words.length==1)groups=1;
  groups=Math.min(groups,6);List<Chunk> result=new ArrayList<>();int at=0,cumulative=0;
  for(int i=0;i<groups;i++){int take=(int)Math.ceil((words.length-at)/(double)(groups-i));String part=String.join(" ",Arrays.copyOfRange(words,at,at+take));at+=take;cumulative+=part.length();result.add(new Chunk(part,part.length(),cumulative));}
  return List.copyOf(result);
 }
}
