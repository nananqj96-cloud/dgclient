package fr.cake.client;
/** One group arrives, the next arrives, then the previous group dissolves. */
public final class LyricSequence {
 private static double smooth(double n){n=Math.clamp(n,0,1);return n*n*(3-2*n);}
 public static double opacity(double progress,double from,double to){
  if(progress<from)return 0;
  double span=Math.max(.02,to-from),enter=Math.min(.04,span*.18);
  double fadeIn=smooth((progress-from)/enter);
  double fadeOut=to>=.999?1:1-smooth((progress-to)/Math.min(.12,span*.4));
  return fadeIn*fadeOut;
 }
 public static double layered(double progress,double from,double hideAt,double fade){
  if(progress<from)return 0;
  return smooth((progress-from)/.035)*(hideAt>=1?1:1-smooth((progress-hideAt)/Math.max(.02,fade)));
 }
}
