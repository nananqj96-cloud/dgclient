package fr.cake.client;

/** A monotonic local clock, anchored to Windows timeline updates rather than every poll. */
public final class PlaybackClock {
 private boolean initialized,playing;private long sampled,lastRaw,lastStamp;private double position,rate=1;
 public long update(long raw,long stamp,long age,long now,boolean active,double playbackRate,long duration,boolean trackChanged){
  double nextRate=Double.isFinite(playbackRate)&&playbackRate>0?playbackRate:1;
  double predicted=position+(playing?Math.max(0,(now-sampled)/1e6)*rate:0);
  double candidate=raw+(active&&(!initialized||trackChanged||stamp!=lastStamp)?Math.clamp(age,0,3_600_000)*nextRate:0);
  if(!initialized||trackChanged){position=candidate;initialized=true;}
  else if(raw!=lastRaw){double error=candidate-predicted;position=Math.abs(error)>1500?candidate:predicted+Math.clamp(error*.18,-60,60);}
  else position=predicted;
  position=duration>0?Math.clamp(position,0,duration):Math.max(0,position);playing=active;sampled=now;lastRaw=raw;lastStamp=stamp;rate=nextRate;return Math.round(position);
 }
 public void reset(){initialized=false;position=0;playing=false;}
}
