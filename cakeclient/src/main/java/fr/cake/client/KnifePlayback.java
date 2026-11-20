package fr.cake.client;

/** Original clip timing with alternating cuts. One queued click never restarts a cut. */
public final class KnifePlayback {
 private int mode,next=3,queued=-1;private long started,serial;private double skipped;
 public void play(int action,long now){mode=action;started=now;queued=-1;skipped=0;serial++;}
 public long serial(){return serial;}
 private double elapsed(long now){return Math.max(0,(now-started)/1e9)+skipped;}
 public int mode(){return mode;}
 public double progress(long now,double[] durations){return mode==0?0:Math.clamp(elapsed(now)/Math.max(.01,durations[mode]),0,1);}
 /** Join the source intro to the source outro, omitting its stationary inspection hold. */
 public boolean skipInspectionHold(long now,double begin,double end){if(begin<0||end<=begin||elapsed(now)<begin||elapsed(now)>=end)return false;skipped+=end-begin;serial++;return true;}
 public boolean loopIfFinished(int action,long now,double[] durations){if(mode==0||progress(now,durations)<1)return false;double over=Math.min(.1,Math.max(0,elapsed(now)-durations[mode]));play(action,now-(long)(over*1e9));return true;}
 public void attack(boolean heavy,long now,double[] durations){
  int action=heavy?5:next;if(!heavy)next=next==3?4:3;
  if(mode>=3&&mode<=5&&progress(now,durations)<.85){queued=action;return;}play(action,now);
 }
 public void advance(long now,double[] durations){if(mode!=0&&progress(now,durations)>=1){if(queued>=0){int action=queued;play(action,now);}else{mode=0;started=now;skipped=0;serial++;}}}
 public double seconds(long now,double[] durations,double speed){return mode==0?(elapsed(now)%durations[0])*speed:Math.min(durations[mode],elapsed(now))*speed;}
}
