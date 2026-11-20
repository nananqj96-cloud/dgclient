package fr.cake.client;
import java.awt.*;
import java.awt.geom.*;
import java.lang.reflect.*;

/** Small isolated 1080p configuration panel: no per-frame image allocations. */
public final class CakeExtras {
 private CakeExtras(){}
 private static int editor=0;
 private static final String[] KNIVES=KnifeCatalog.LABELS;
 private static String status="Mode FPS inactif";
 public static boolean collectionOpen(int tab){return tab==6&&editor==3;}
 public static UiCanvas.Hover collectionHover(double x,double y){
  if(x>=1640&&x<=1828&&y>=304&&y<=352)return new UiCanvas.Hover(1640,304,188,48);
  if(x<396||y<418)return null;
  int col=(int)((x-396)/356),row=(int)((y-418)/70),index=row*4+col;
  if(col<4&&row<6&&index<KnifeCatalog.COUNT&&x<=722+col*356&&y<=474+row*70)return new UiCanvas.Hover(396+col*356,418+row*70,326,56);
  return null;
 }
 private static final String[] FONTS={"CAKE UI","SANS","SERIF","MONO","DISPLAY"};
 private static Settings s(){return CakeClient.settings;}
 private static double num(String key,double fallback){try{Object value=Settings.class.getField(key).get(s());return value instanceof Boolean b?(b?1:0):((Number)value).doubleValue();}catch(Exception e){return fallback;}}
 private static int integer(String key,int fallback){return (int)num(key,fallback);}
 private static void set(String key,Object value){try{Field f=Settings.class.getField(key);if(value instanceof Integer n)f.setInt(s(),n);else if(value instanceof Boolean b)f.setBoolean(s(),b);else f.setDouble(s(),(Double)value);s().save();}catch(Exception ignored){}}
 private static void refresh(){try{Field field=LyricsSky.class.getDeclaredField("previous");field.setAccessible(true);field.set(null,"");}catch(Exception ignored){}}
 public static Font lyricFont(float size,boolean bold){int f=integer("lyricFont",0);if(f==0)return UiCanvas.font(size,bold);String name=switch(f){case 1->"SansSerif";case 2->"Serif";case 3->"Monospaced";default->"Dialog";};return new Font(name,bold?Font.BOLD:Font.PLAIN,1).deriveFont(size);}
 public static Color lyricFill(){return Color.getHSBColor((float)num("lyricHue",.76),.55f,1f);}
 public static Color lyricGlow(){Color c=lyricFill();return new Color(c.getRed(),c.getGreen(),c.getBlue(),220);}
 public static Color lyricOutline(){Color c=lyricFill();return new Color((int)(c.getRed()*.11),(int)(c.getGreen()*.11),(int)(c.getBlue()*.11),230);}
 private static void round(Graphics2D g,int x,int y,int w,int h,int radius,Color c){g.setColor(c);g.fillRoundRect(x,y,w,h,radius,radius);}
 private static void label(Graphics2D g,String v,int x,int y,int size,boolean strong){g.setFont(UiCanvas.font(size,strong));g.setColor(new Color(241,235,255));g.drawString(v,x,y);}
 private static void button(Graphics2D g,int x,int y,int w,int h,String title,boolean active){round(g,x,y,w,h,14,active?new Color(134,86,202,180):new Color(58,50,76,210));label(g,title,x+16,y+h/2+8,21,active);}
 private static void rainbow(Graphics2D g,int x,int y,int w){for(int i=0;i<w;i++){g.setColor(Color.getHSBColor(i/(float)w,.65f,1f));g.fillRect(x+i,y,1,25);}}
 public static void decorate(UiCanvas panel,Graphics2D g,int tab){
  if(tab!=5){editor=0;return;}
  if(tab==6){button(g,1535,194,324,58,"COLLECTION / 22",editor==3);if(editor!=3)return;drawKnives(g);return;}
  if(tab==4){button(g,1535,194,324,58,"TRAIL / JUMP",editor==5);if(editor!=5)return;drawEffects(g);return;}
  if(tab==7){button(g,1170,194,330,58,"POTATO / BOOST FPS",editor==4);if(editor==4){drawFps(g);return;}}
  button(g,1535,194,324,58,tab==5?"POLICE / COULEURS":"AMBIANCE / FOG",editor==(tab==5?1:2));
  if(editor==0)return;
  if((editor==1&&tab!=5)||(editor==2&&tab!=7)){editor=0;return;}
  round(g,350,282,1524,635,24,new Color(17,14,29,246));
  g.setColor(new Color(134,96,212,180));g.setStroke(new BasicStroke(2));g.drawRoundRect(350,282,1524,635,24,24);
  label(g,editor==1?"Typographie des lyrics":"Ambiance colorée et brouillard",395,345,35,true);
  button(g,1650,302,177,48,"RETOUR",false);
  if(editor==1){
   label(g,"Police",400,412,24,true);
   for(int i=0;i<FONTS.length;i++)button(g,395+i*283,430,258,58,FONTS[i],integer("lyricFont",0)==i);
   label(g,"Couleur des paroles",400,563,24,true);
   rainbow(g,395,589,1420);
   int x=(int)(395+num("lyricHue",.76)*1420);g.setColor(Color.WHITE);g.setStroke(new BasicStroke(4));g.drawRect(x-5,583,10,37);
   label(g,"La teinte modifie le texte et sa lueur. Les autres modules restent inchangés.",400,690,23,false);
   label(g,"L'aperçu prend effet sur les nouvelles paroles sans changer la synchronisation.",400,749,23,false);
  }else{
   label(g,"Densité du brouillard",400,431,24,true);
   double fog=num("fogStrength",0);round(g,395,469,1420,16,12,new Color(69,62,91));round(g,395,469,(int)(1420*fog),16,12,new Color(177,134,247));
   label(g,Math.round(fog*100)+" %",1650,453,21,true);
   label(g,"Couleur générale du brouillard",400,574,24,true);
   rainbow(g,395,600,1420);
   int x=(int)(395+num("ambientHue",.72)*1420);g.setColor(Color.WHITE);g.setStroke(new BasicStroke(4));g.drawRect(x-5,594,10,37);
   label(g,"Rendu calculé à la résolution native et appliqué aux pixels du décor.",400,721,22,false);
   label(g,"Le module Optique est activé automatiquement si vous réglez le brouillard.",400,765,22,false);
  }
 }
 public static boolean intercept(UiCanvas panel,double x,double y,int tab){
  if(tab!=5)return false;
  if(tab==6){if(x>=1535&&x<=1859&&y>=194&&y<=252){editor=editor==3?0:3;panel.invalidate();return true;}if(editor==3)return selectKnife(panel,x,y);return false;}
  if(tab==4){if(x>=1535&&x<=1859&&y>=194&&y<=252){editor=editor==5?0:5;panel.invalidate();return true;}if(editor==5)return selectEffects(panel,x,y);return false;}
  if(tab==7){if(x>=1170&&x<=1500&&y>=194&&y<=252){editor=editor==4?0:4;panel.invalidate();return true;}if(editor==4)return selectFps(panel,x,y);}
  int desired=tab==5?1:2;
  if(x>=1535&&x<=1859&&y>=194&&y<=252){editor=editor==desired?0:desired;panel.invalidate();return true;}
  if(editor!=desired)return false;
  if(x>=1650&&x<=1827&&y>=302&&y<=350){editor=0;panel.invalidate();return true;}
  if(editor==1){
   if(y>=430&&y<=488&&x>=395&&x<395+5*283){int f=(int)((x-395)/283);if(x-395-f*283<258){set("lyricFont",f);refresh();panel.invalidate();}return true;}
   if(y>=572&&y<=632&&x>=395&&x<=1815){set("lyricHue",Math.max(0.,Math.min(1.,(x-395)/1420.)));refresh();panel.invalidate();return true;}
  }else{
   if(y>=453&&y<=505&&x>=395&&x<=1815){set("fogStrength",Math.max(0.,Math.min(1.,(x-395)/1420.)));s().atmosphere=true;s().save();panel.invalidate();return true;}
   if(y>=587&&y<=647&&x>=395&&x<=1815){set("ambientHue",Math.max(0.,Math.min(1.,(x-395)/1420.)));panel.invalidate();return true;}
  }
  // Modal intercept blocks invisible underlying controls.
  return x>=350&&x<=1874&&y>=282&&y<=917;
 }
 private static void frame(Graphics2D g,String title){
  round(g,350,282,1524,635,24,new Color(17,14,29,249));g.setStroke(new BasicStroke(2));g.setColor(new Color(134,96,212,180));g.drawRoundRect(350,282,1524,635,24,24);
  label(g,title,392,344,37,true);button(g,1640,304,188,48,"RETOUR",false);
 }
 private static void drawKnives(Graphics2D g){
  frame(g,"COLLECTION // 22 COUTEAUX CS2");label(g,"Modèles et animations CS2 propres à chaque couteau",400,397,22,false);
  int selected=integer("knifeModel",0);
  for(int i=0;i<KNIVES.length;i++){
   int x=396+(i%4)*356,y=418+(i/4)*70;
   button(g,x,y,326,56,(i==selected?"●  ":"")+KNIVES[i],i==selected);
  }
  label(g,"G : inspection  |  Maintenir G : Butterfly  |  Changement immédiat",400,883,21,false);
 }
 private static boolean selectKnife(UiCanvas p,double x,double y){
  if(x>=1640&&x<=1828&&y>=304&&y<=352){editor=0;p.invalidate();return true;}
  if(y>=418&&y<=418+6*70){int row=(int)((y-418)/70),col=(int)((x-396)/356);int i=row*4+col;
   if(col>=0&&col<4&&row>=0&&row<6&&i<KnifeCatalog.COUNT&&x>=396+col*356&&x<=722+col*356&&y<=474+row*70){set("knifeModel",i);p.invalidate();return true;}}
  return x>=350&&x<=1874&&y>=282&&y<=917;
 }
 private static void drawFps(Graphics2D g){
  frame(g,"POTATO // FPS BOOST");label(g,"Profil graphique (appliqué après redémarrage)",399,420,25,false);
  int mode=integer("fpsPreset",0);String[] labels={"STANDARD","RAPIDE","ULTRA FPS"};
  for(int i=0;i<3;i++)button(g,396+i*475,457,447,70,labels[i],mode==i);
  button(g,396,590,1397,72,"Textures Potato 8×8 : "+(s().getClass()!=null&&num("potatoEnabled",0)>0?"ACTIVÉES":"DÉSACTIVÉES"),num("potatoEnabled",0)>0);
  label(g,"186 textures légères. Conservation du pack et des options personnelles.",399,727,23,false);
  label(g,"Ce profil supprime le superflu mais ne garantit aucun FPS chiffré.",399,780,23,false);
  label(g,status.length()>100?status.substring(0,100):status,399,850,21,false);
 }
 private static boolean selectFps(UiCanvas p,double x,double y){
  if(x>=1640&&x<=1828&&y>=304&&y<=352){editor=0;p.invalidate();return true;}
  if(y>=457&&y<=527&&x>=396&&x<1793){int i=(int)((x-396)/475);if(i<=2&&x<396+i*475+447){set("fpsPreset",i);if(i>0){for(String key:new String[]{"motionBlur","atmosphere","sky","fireflies","sakura","motes","embers","snow","pollen"})try{Settings.class.getField(key).setBoolean(s(),false);}catch(Exception ignored){}s().save();}status=CakeOptimize.apply(num("potatoEnabled",0)>0,i);p.invalidate();}return true;}
  if(y>=590&&y<=662&&x>=396&&x<=1793){boolean v=num("potatoEnabled",0)<=0;set("potatoEnabled",v);status=CakeOptimize.apply(v,integer("fpsPreset",0));p.invalidate();return true;}
  return x>=350&&x<=1874&&y>=282&&y<=917;
 }
 private static void drawEffects(Graphics2D g){
  frame(g,"TRAILS // JUMP CIRCLES");label(g,"Effets personnalisables - conservés indépendamment des paroles",399,401,24,false);
  button(g,395,439,681,73,"Trail : "+(num("trailEnabled",0)>0?"ACTIF":"INACTIF"),num("trailEnabled",0)>0);
  button(g,1110,439,683,73,"Jump Circle : "+(num("jumpEnabled",0)>0?"ACTIF":"INACTIF"),num("jumpEnabled",0)>0);
  label(g,"Style Trail",399,571,24,true);label(g,"Style Jump Circle",1110,571,24,true);
  String[] styles={"NEON","ORBITAL","SPARK","AURA"};
  for(int i=0;i<4;i++){button(g,396+(i%2)*345,597+(i/2)*77,326,60,styles[i],integer("trailStyle",0)==i);button(g,1110+(i%2)*345,597+(i/2)*77,326,60,styles[i],integer("jumpStyle",0)==i);}
  label(g,"Effets légers et paramétrables dans le profil Cake Client.",399,870,22,false);
 }
 private static boolean selectEffects(UiCanvas p,double x,double y){
  if(x>=1640&&x<=1828&&y>=304&&y<=352){editor=0;p.invalidate();return true;}
  if(y>=439&&y<=512){if(x>=395&&x<=1076){set("trailEnabled",num("trailEnabled",0)<=0);p.invalidate();return true;}if(x>=1110&&x<=1793){set("jumpEnabled",num("jumpEnabled",0)<=0);p.invalidate();return true;}}
  if(y>=597&&y<=734){int row=(int)((y-597)/77),col=x<1100?(int)((x-396)/345):(int)((x-1110)/345),style=row*2+col;
   if(col>=0&&col<2&&style>=0&&style<4&&y<=657+row*77){if(x>=396&&x<=1067){set("trailStyle",style);p.invalidate();return true;}if(x>=1110&&x<=1781){set("jumpStyle",style);p.invalidate();return true;}}}
  return x>=350&&x<=1874&&y>=282&&y<=917;
 }

}
