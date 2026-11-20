package fr.cake.client;
/** Presets reduce existing work; they never enlarge a player's visibility distances. */
public record FpsPlan(int view,int simulation,double entities,int particles,int weather,boolean fastLeaves,boolean improvedTransparency,boolean ao) {
 public static FpsPlan preset(int level,int currentView,int currentSimulation,double currentEntities){
  int v=level==1?8:level==2?5:3,sim=level==1?6:level==2?5:5;
  double e=level==1?.75:level==2?.5:.5;
  return new FpsPlan(Math.max(2,Math.min(currentView,v)),Math.max(5,Math.min(currentSimulation,sim)),Math.max(.5,Math.min(currentEntities,e)),level==1?1:2,level==1?5:3,true,false,false);
 }
}
