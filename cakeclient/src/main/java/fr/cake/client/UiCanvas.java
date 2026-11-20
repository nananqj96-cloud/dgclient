package fr.cake.client;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleConsumer;
import net.minecraft.client.Minecraft;

/** Shared 1080p panel for the compact screen and its spatial counterpart. */
public final class UiCanvas {
 public static final int W=1920,H=1080;
 private static final Color BG=new Color(10,12,21,250),CARD=new Color(21,24,39,245),LINE=new Color(160,149,201,28),TEXT=new Color(240,239,249),MUTED=new Color(155,157,177),PURPLE=new Color(178,139,255);
 private static final String[] TABS={"Motion blur","Lucioles","Spatial Panel","Ciel","Ambiance","Lyrics in Sky","Couteaux","Optique","FPS","Fog","Trail","Jump Circle","Silent Aura","Hitbox","Hotbar","Skin","Spotify Visualizer","Hit Particles","Particules","Pack Customizer","Wings & Capes","Realistic Mode","Crosshair","Combat Effects","Knife Materials","Settings"};
 private static final int[][] CATEGORIES={{21,3,7,0,9,22},{4,1,18,10,11,17},{6,24,20,15},{12,13,23},{16,5},{8,14,19,2,25}};
 private static final String[] CATEGORY_NAMES={"Visuels","Atmosphere","Cosmétiques","Combat","Musique","System"};
 private static final BufferedImage[] PREVIEWS=new BufferedImage[KnifeCatalog.COUNT];
 private static int category(int t){for(int i=0;i<CATEGORIES.length;i++)for(int n:CATEGORIES[i])if(n==t)return i;return 0;}
 private final List<Widget> widgets=new ArrayList<>();
 private record Widget(double x,double y,double w,double h,Runnable action,DoubleConsumer slider){}
 private final BufferedImage image=new BufferedImage(W,H,BufferedImage.TYPE_INT_ARGB);
 private static final Font FONT=loadFont();
 private Graphics2D g;
 private SmoothTexture texture;
 private int tab=Edition.PLAN==Edition.Plan.FREE?3:21,drag=-1,hover=-1;
 private long lastRevision=-1,lastStatus=-1;
 private double pointerX=-1,pointerY=-1;
 private boolean dirty=true,spatial;
 private double lastSliderValue=Double.NaN;
 public void setSpatial(boolean value){if(spatial!=value){spatial=value;dirty=true;}}
 public record Hover(double x,double y,double w,double h){}
 public Hover hoverBounds(){if(hover<0||hover>=widgets.size())return null;var w=widgets.get(hover);return new Hover(w.x,w.y,w.w,w.h);}
 private long paints;public long paintCount(){return paints;}
 private static Font loadFont(){try(var in=UiCanvas.class.getResourceAsStream("/assets/cakeclient/font/ui.ttf")){return Font.createFont(Font.TRUETYPE_FONT,in);}catch(Exception e){return new Font("SansSerif",Font.PLAIN,24);}}
 public static Font font(float size,boolean bold){return FONT.deriveFont(bold?Font.BOLD:Font.PLAIN,size);}
 public int tab(){return tab;}
 private Settings s(){return CakeClient.settings;}
 public SmoothTexture texture(){update(pointerX,pointerY);return texture;}
 public void invalidate(){dirty=true;}
 public void update(double mx,double my){
  pointerX=mx;pointerY=my;
  int next=-1;for(int i=0;i<widgets.size();i++){var w=widgets.get(i);if(inside(w,mx,my)){next=i;break;}}
  if(next!=hover){hover=next;if(!spatial)dirty=true;}
  long status=(tab==5||tab==8||tab==16||tab==19||tab==21)?System.currentTimeMillis()/1000:0;
  if(!dirty&&lastRevision==Settings.revision&&lastStatus==status&&texture!=null)return;
  dirty=false;lastRevision=Settings.revision;lastStatus=status;
  if(texture==null)texture=new SmoothTexture("panel",W,H);
  paint();texture.update(image);
 }
 public boolean click(double x,double y){
  if(!Edition.module(Edition.PLAN,tab)&&x>=350&&x<=1874&&y>=130&&y<970)return true;
  if(Edition.musicCustomization(Edition.PLAN)&&CakeExtras.intercept(this,x,y,tab))return true;
  pointerX=x;pointerY=y;
  for(int i=0;i<widgets.size();i++){
   Widget w=widgets.get(i);if(!inside(w,x,y))continue;
   ClientScreen.tap();
   if(w.slider!=null){drag=i;lastSliderValue=Math.clamp((x-w.x)/w.w,0,1);w.slider.accept(lastSliderValue);s().changed();}
   else if(w.action!=null){w.action.run();s().changed();if(!PreviewTrial.active())s().save();CakeOptimize.flush();}
   dirty=true;return true;
  }return x>=0&&x<=W&&y>=0&&y<=H;
 }
 public void drag(double x,double y){if(drag>=0&&drag<widgets.size()){Widget w=widgets.get(drag);if(w.slider!=null){double value=Math.clamp((x-w.x)/w.w,0,1);if(Math.abs(value-lastSliderValue)<.001)return;lastSliderValue=value;w.slider.accept(value);s().changed();dirty=true;}}}

 public void release(){if(drag>=0){drag=-1;CakeOptimize.flush();s().save();dirty=true;}}
 private static boolean inside(Widget w,double x,double y){return x>=w.x&&x<=w.x+w.w&&y>=w.y&&y<=w.y+w.h;}
 private void paint(){
  paints++;
  g=image.createGraphics();g.setComposite(AlphaComposite.Clear);g.fillRect(0,0,W,H);g.setComposite(AlphaComposite.SrcOver);
  g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
  g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
  g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,RenderingHints.VALUE_FRACTIONALMETRICS_ON);
  g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC);
  widgets.clear();
  round(10,10,1900,1060,26,BG);g.setPaint(new GradientPaint(10,10,new Color(125,79,220,24),1150,600,new Color(25,27,42,0)));g.fill(new RoundRectangle2D.Double(10,10,1900,1060,52,52));g.setStroke(new BasicStroke(1.5f));g.setColor(LINE);g.draw(new RoundRectangle2D.Double(10,10,1900,1060,26,26));
  g.setPaint(new GradientPaint(10,120,new Color(91,65,145,35),300,950,new Color(17,20,33,10)));g.fill(new RoundRectangle2D.Double(18,113,283,853,24,24));
  line(10,102,1910,102);line(310,103,310,995);
  label("CAKE",46,60,31,TEXT,true);label("CLIENT",146,60,24,MUTED,false);label(Edition.name().toUpperCase(java.util.Locale.ROOT),240,60,18,PURPLE,true);
  // repack tag (648k.exe) - only change vs the author's source, see REUPLOAD-NOTICE.txt
  label("rebuild · 648k.exe",46,92,14,MUTED,false);
  for(int i=0;i<CATEGORIES.length;i++)navItem(i,350+i*242,27,226,52);

  button(1815,30,60,48,"×",()->{Minecraft mc=Minecraft.getInstance();if(mc.screen instanceof ClientScreen screen)screen.closeAnimated();else SpatialPanel.hide();},false);
  label(I18n.tr(CATEGORY_NAMES[category(tab)]).toUpperCase(java.util.Locale.ROOT),51,163,18,MUTED,true);
  int[] visible=CATEGORIES[category(tab)];
  for(int i=0;i<visible.length;i++){
   final int t=visible[i];double y=198+i*86;
   if(tab==t){round(29,y-2,262,64,10,new Color(148,104,242,24));round(29,y+12,3,34,1,PURPLE);}
   g.setColor(enabled(t)?new Color(155,225,189):new Color(70,72,91));g.fill(new Ellipse2D.Double(269,y+23,6,6));label(TABS[t],51,y+38,23,tab==t?TEXT:MUTED,tab==t);if(!Edition.module(Edition.PLAN,t)){g.setColor(MUTED);g.setStroke(new BasicStroke(2));g.draw(new Line2D.Double(49,y+29,49+g.getFontMetrics().stringWidth(TABS[t]),y+29));}
   widgets.add(new Widget(30,y-2,260,64,()->{tab=t;drag=-1;dirty=true;},null));
  }
  profileCard();
  line(30,976,1890,976);label("Menu · "+MenuKey.name(),46,1026,21,MUTED,false);label("Changes save automatically",390,1026,20,MUTED,false);
  button(1180,995,310,48,"Settings",()->tab=25,false);button(1520,995,354,48,"Enregistrer et fermer",()->{Minecraft mc=Minecraft.getInstance();if(mc.screen instanceof ClientScreen screen)screen.closeAnimated();else SpatialPanel.hide();},true);
  label(TABS[tab],350,188,49,TEXT,true);
  if(tab!=25)label(enabled(tab)?"ACTIVE":"OFF",1560,184,18,enabled(tab)?new Color(155,225,189):MUTED,true);
  String[] sub={"Un flou temporel réglable, appliqué avant l'interface.","Un vol libre dans le décor. Aucun déplacement attaché au joueur.","Posez le panneau dans le monde, puis déplacez-vous autour.","Une ambiance de ciel calculée directement sur la carte graphique.","Quelques particules légères, avec un budget limité.","Des groupes de mots répartis devant vous, à gauche et à droite.","Modèles CS texturés, uniquement avec une épée.","Bloom, ombres de contact et couleurs photographiques.","Réduisez les distances, les particules et les effets coûteux.","Distance, densité et couleur du brouillard.","Une trace légère derrière vos déplacements.","Des cercles au sol à chaque saut.","Aims and hits for you. Your view stays yours.","Contours visuels des entités visibles.","Fonds, cases, sélection et dimensions personnalisables.","Sonic, Patrick, Mario, Luigi and eight more recognizable characters.","Ta musique, sa pochette et sa progression dans une carte déplaçable.","Éclats lumineux après un coup confirmé.","Une ambiance de particules discrète dans le décor.","Textures PvP indépendantes, appliquées sans redémarrer."};
  label(tab==8?Minecraft.getInstance().getFps()+" i/s · Réglages appliqués en jeu":(tab<sub.length?sub[tab]:"Select a look. Tune the details. Make it yours."),351,234,23,MUTED,false);
  int moduleStart=widgets.size();
  switch(tab){case 0->motion();case 1->flies();case 2->spatial();case 3->sky();case 4->ambience();case 5->lyrics();case 6->knives();case 7->optics();case 8->fps();case 9->fog();case 10->effects(false);case 11->effects(true);case 12->follow();case 13->hitboxes();case 14->hotbar();case 15->skins();case 16->musicViz();case 17->particles(false);case 18->particles(true);case 19->packCustomizer();case 20->backs();case 21->realism();case 22->crosshairs();case 23->combat();case 24->materials();case 25->preferences();}
  if(Edition.musicCustomization(Edition.PLAN))CakeExtras.decorate(this,g,tab);
  if(!Edition.module(Edition.PLAN,tab)){widgets.subList(moduleStart,widgets.size()).clear();round(350,282,1524,635,21,new Color(10,8,16,145));label("LOCKED · "+(Edition.module(Edition.Plan.PRO,tab)?"PRO / PREMIUM":"PREMIUM"),390,344,29,PURPLE,true);g.setColor(MUTED);g.setStroke(new BasicStroke(3));g.draw(new Line2D.Double(350,173,350+g.getFontMetrics(font(49,true)).stringWidth(TABS[tab]),173));}
  g.dispose();g=null;
 }
 private void motion(){
  toggle(1724,148,s().motionBlur,()->{s().motionBlur=!s().motionBlur;MotionBlur.reset();});
  card(350,282,925,635);label("Intensité",388,343,28,TEXT,true);
  slider(390,402,840,s().blur,(int)(s().blur*100)+" %",v->s().blur=v);
  label("Faible",390,457,21,MUTED,false);right("Marqué",1230,457,21,MUTED);
  button(390,505,250,61,"Léger · 25 %",()->s().blur=.25,false);
  button(660,505,250,61,"Équilibré · 45 %",()->s().blur=.45,false);
  button(930,505,300,61,"Cinématique · 70 %",()->s().blur=.70,false);
  line(390,620,1230,620);
  label("Les menus et le HUD restent nets.",390,680,25,TEXT,false);
  label("L'exposition est ajustée selon la durée de chaque image.",390,725,22,MUTED,false);
  if(!CakeClient.effectError.isEmpty())label("Le shader a rencontré une erreur. Voir latest.log.",390,825,22,new Color(239,145,150),false);
  trialCard(0,"Essayer le véritable effet","Bougez la caméra pendant 6 secondes.");
 }
 private void flies(){
  toggle(1724,148,s().fireflies,()->s().fireflies=!s().fireflies);
  card(350,282,925,635);
  rowSlider("Quantité",390,328,840,s().density,Fireflies.count()+"",v->s().density=v);
  rowSlider("Vitesse de vol",390,454,840,s().speed,pct(s().speed),v->s().speed=v);
  rowSlider("Rayon d'apparition",390,580,840,s().radius,String.format("%.1f m",4+s().radius*7),v->s().radius=v);
  rowSlider("Éclat",390,706,840,s().glow,pct(s().glow),v->s().glow=v);
  hue(390,849,840,s().hue,v->{s().hue=v;s().rainbow=false;});
  card(1304,282,570,635);label("Trajectoires naturelles",1344,341,28,TEXT,true);
  wrap("Les lucioles conservent leur place dans le monde. Vous pouvez les dépasser sans les tirer derrière vous.",1344,397,485,24,MUTED);
  button(1344,567,490,60,s().rainbow?"Arc-en-ciel activé":"Couleur fixe",()->s().rainbow=!s().rainbow,false);
  button(1344,660,490,65,trialLabel(),()->PreviewTrial.start(1),true);
  wrap("24 insectes maximum. Les plus éloignés s'effacent progressivement.",1344,785,490,22,MUTED);
 }
 private void spatial(){
  toggle(1724,148,s().spatial,()->{s().spatial=!s().spatial;if(!s().spatial)SpatialPanel.hide();});
  card(350,282,925,635);
  rowSlider("Taille du panneau 3D",390,335,840,s().panelSize,pct(s().panelSize),v->s().panelSize=v);
  rowSlider("Distance de pose",390,485,840,s().panelDistance,String.format("%.1f m",1.9+s().panelDistance*1.2),v->s().panelDistance=v);
  rowSlider("Taille du menu classique",390,635,840,(s().menuScale-.42)/.36,(int)(s().menuScale*100)+" % de l'écran",v->s().menuScale=.42+v*.36);
  button(390,803,840,66,(Minecraft.getInstance().level==null?"Disponible dans un monde":"Poser le panneau devant moi"),()->SpatialPanel.placeFromMenu(),true);
  card(1304,282,570,635);label("Une position fixe",1344,341,28,TEXT,true);
  wrap("Marchez, tournez autour du panneau, puis visez un bouton et cliquez. Les curseurs se règlent en maintenant le clic.",1344,400,485,24,MUTED);
  wrap("Alt Gr masque ou réaffiche le panneau. Maj + Alt Gr le replace devant vous. Le panneau reste lisible des deux côtés.",1344,620,485,24,MUTED);
 }
 private void sky(){
  toggle(1724,148,s().sky,()->s().sky=!s().sky);
  card(350,282,1524,635);String[] modes={"Colour","Nova","Galaxy","Waves","Aurora","Black Hole","Solar Eclipse","Nebula","Meteor Shower","Polar Veil"};
  for(int i=0;i<10;i++){int n=i;choice(390+i%5*290,310+i/5*65,274,52,modes[i],()->s().skyMode=n,s().skyMode==i,Edition.sky(Edition.PLAN,i));}
  rowSlider("Intensité",390,445,665,s().skyIntensity,pct(s().skyIntensity),v->s().skyIntensity=v);
  rowSlider("Vitesse",1150,445,665,s().skySpeed,pct(s().skySpeed),v->s().skySpeed=v);
  rowSlider("Étoiles",390,605,665,s().stars,pct(s().stars),v->s().stars=v);
  label("Teinte",1150,605,25,TEXT,false);hue(1150,652,665,s().skyHue,v->s().skyHue=v);
  button(390,793,665,66,trialLabel(),()->PreviewTrial.start(3),true);
  button(1150,793,665,66,s().skyClouds?"Nuages Minecraft : oui":"Nuages Minecraft : non",()->s().skyClouds=!s().skyClouds,false);
 }
 private void ambience(){
  card(350,282,725,635);card(1104,282,770,635);
  option(390,350,"Pétales de sakura",s().sakura,()->s().sakura=!s().sakura);
  option(390,440,"Points de lumière",s().motes,()->s().motes=!s().motes);
  option(390,530,"Braises discrètes",s().embers,()->s().embers=!s().embers);
  option(390,620,"Neige légère",s().snow,()->s().snow=!s().snow);
  option(390,710,"Pollen doré",s().pollen,()->s().pollen=!s().pollen);
  option(1144,710,"Comètes violettes",s().comets,()->s().comets=!s().comets);
  rowSlider("Densité globale",1144,355,685,s().ambientDensity,pct(s().ambientDensity),v->s().ambientDensity=v);
  rowSlider("Vent",1144,528,685,s().wind,pct(s().wind),v->s().wind=v);
  wrap("Des arcs lumineux dans le décor, sans entités supplémentaires.",1144,800,680,22,MUTED);
  button(390,803,645,66,trialLabel(),()->PreviewTrial.start(4),true);
 }
 private void lyrics(){
  toggle(1724,148,s().lyrics,()->{s().lyrics=!s().lyrics;LyricsService.settingsChanged();});
  card(350,282,925,635);card(1304,282,570,635);
  if(Edition.musicCustomization(Edition.PLAN)){rowSlider("Taille",390,334,390,s().lyricSize,pct(s().lyricSize),v->s().lyricSize=v);
  rowSlider("Distance",840,334,390,s().lyricDistance,String.format("%.1f m",5.5+s().lyricDistance*8),v->s().lyricDistance=v);
  rowSlider("Hauteur",390,492,390,s().lyricHeight,s().lyricHeight<.4?"Plus bas":s().lyricHeight>.6?"Plus haut":"Devant vous",v->s().lyricHeight=v);
  rowSlider("Écartement",840,492,390,s().lyricSpacing,pct(s().lyricSpacing),v->s().lyricSpacing=v);
  rowSlider("Opacité",390,650,390,s().lyricOpacity,pct(s().lyricOpacity),v->s().lyricOpacity=v);
  }else{wrap("Lyrics in Sky · automatic placement. Appearance customization is included in Premium.",390,410,800,27,MUTED);}
  rowSlider("Synchro globale",840,650,390,s().lyricOffset,String.format("%+.1f s",(s().lyricOffset-.5)*30),v->s().lyricOffset=v);
  if(Edition.musicCustomization(Edition.PLAN))rowSlider("Groupes simultanés",390,765,840,s().lyricOverlap,(2+(int)Math.round(s().lyricOverlap*2))+" groupes",v->s().lyricOverlap=v);
  if(Edition.musicCustomization(Edition.PLAN))button(390,842,840,53,"Démo du placement · 6 s",()->PreviewTrial.start(5),true);
  label("Spotify sur le PC",1344,341,28,TEXT,true);
  wrap(LyricsService.status(),1344,397,480,24,s().lyrics?TEXT:MUTED);
  label(LyricsService.playbackLabel(),1344,511,20,PURPLE,false);
  String line=LyricsService.currentLine();wrap(line.length()>100?line.substring(0,97)+"…":line,1344,558,480,23,TEXT);
  button(1344,650,235,49,"Phrase précédente",()->LyricsService.calibrateLine(-1),false);
  button(1595,650,239,49,"Phrase suivante",()->LyricsService.calibrateLine(1),false);
  button(1344,716,490,53,"Caler cette phrase maintenant",()->LyricsService.calibrateLine(0),true);
  button(1344,782,490,43,"Réinitialiser la correction",LyricsService::resetCalibration,false);
  button(1344,842,490,43,"Relancer la détection",LyricsService::retry,false);

 }
 private void knives(){
  toggle(1724,148,s().knives,()->s().knives=!s().knives);
  card(350,282,925,635);card(1304,282,570,635);
  label("Collection · 22 couteaux",390,330,27,TEXT,true);
  for(int i=0;i<KnifeCatalog.COUNT;i++){int n=i;choice(390+(i%4)*220,355+(i/4)*69,205,53,KnifeCatalog.LABELS[i],()->s().knifeModel=n,s().knifeModel==i,Edition.knife(Edition.PLAN,i));}
  rowSlider("Taille de la vue",390,825,390,s().knifeScale,pct(s().knifeScale),v->s().knifeScale=v);
  rowSlider("Vitesse des gestes",840,825,390,s().knifeSpeed,String.format("%.2f ×",.5+s().knifeSpeed),v->s().knifeSpeed=v);
  label(KnifeCatalog.LABELS[s().knifeModel],1344,341,27,TEXT,true);
  int model=s().knifeModel;if(PREVIEWS[model]==null)try(var in=UiCanvas.class.getResourceAsStream("/assets/cakeclient/knives/preview-"+model+".png")){if(in!=null)PREVIEWS[model]=javax.imageio.ImageIO.read(in);}catch(Exception ignored){}
  if(PREVIEWS[model]!=null){g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC);g.drawImage(PREVIEWS[model],1324,358,530,283,null);}
  label(s().knifeModel==1?"Maintenir G : rotations":"G : inspection",1344,668,25,TEXT,true);label("Clic gauche : coupes alternées",1344,710,21,TEXT,false);label("Clic droit : estoc",1344,745,21,MUTED,false);
  button(1344,768,490,48,"Knife Materials →",()->tab=24,true);
  button(1344,835,238,56,"Sons CS : "+(s().knifeOriginalSounds?"oui":"non"),()->s().knifeOriginalSounds=!s().knifeOriginalSounds,s().knifeOriginalSounds);
  button(1594,835,240,56,"Inspection",()->PreviewTrial.start(6),true);
 }

 private void effects(boolean jump){
  boolean active=jump?s().jumpEnabled:s().trailEnabled;toggle(1724,148,active,()->{if(jump)s().jumpEnabled=!s().jumpEnabled;else s().trailEnabled=!s().trailEnabled;});
  card(350,282,925,635);card(1304,282,570,635);
  String[] names=jump?CosmeticFx.RINGS:CosmeticFx.TRAILS;
  for(int i=0;i<names.length;i++){int n=i;button(390+(i%3)*286,330+(i/3)*102,268,74,names[i],()->{if(jump)s().jumpStyle=n;else s().trailStyle=n;},(jump?s().jumpStyle:s().trailStyle)==i);}
  label("Couleur",1344,340,27,TEXT,true);hue(1344,378,490,jump?s().jumpHue:s().trailHue,v->{if(jump)s().jumpHue=v;else s().trailHue=v;});
  rowSlider("Taille",1344,490,490,jump?s().jumpSize:s().trailSize,pct(jump?s().jumpSize:s().trailSize),v->{if(jump)s().jumpSize=v;else s().trailSize=v;});
  rowSlider("Durée",1344,640,490,jump?s().jumpLife:s().trailLife,String.format("%.1f s",jump?.4+s().jumpLife*2:.25+s().trailLife*1.75),v->{if(jump)s().jumpLife=v;else s().trailLife=v;});
  if(!jump)rowSlider("Densité",1344,790,490,s().trailDensity,pct(s().trailDensity),v->s().trailDensity=v);
  else wrap("Déclenché au décollage. Six cercles maximum, avec disparition progressive.",1344,800,470,23,MUTED);
 }
 private void follow(){
  toggle(1724,148,s().followEnabled,()->{s().followEnabled=!s().followEnabled;PlayerFollow.clear();});card(350,282,1524,635);
  label("Silent Aura",390,350,32,TEXT,true);
  button(390,400,620,65,"V: activate / stop",PlayerFollow::select,true);
  button(1050,400,760,65,"Épée requise : "+(s().followSword?"oui":"non"),()->s().followSword=!s().followSword,s().followSword);
  rowSlider("Attack range",390,560,880,s().followRange,String.format("%.2f m",2+s().followRange),v->s().followRange=v);
  wrap("Aims and hits for you. Your view stays yours. Press V to activate or stop. Selects the nearest visible player in range and attacks at full cooldown.",390,710,1370,27,TEXT);
 }
 private void hitboxes(){
  toggle(1724,148,s().hitboxEnabled,()->s().hitboxEnabled=!s().hitboxEnabled);card(350,282,1524,635);
  label("Contours visibles",390,350,32,TEXT,true);hue(390,410,1350,s().hitboxHue,v->s().hitboxHue=v);
  rowSlider("Marge visuelle",390,540,620,s().hitboxPadding,String.format("%.2f bloc",s().hitboxPadding*.3),v->s().hitboxPadding=v);
  rowSlider("Distance",1100,540,620,s().hitboxRange,(int)(4+s().hitboxRange*28)+" blocs",v->s().hitboxRange=v);
  button(390,720,620,65,s().hitboxPlayers?"Joueurs uniquement":"Entités vivantes",()->s().hitboxPlayers=!s().hitboxPlayers,s().hitboxPlayers);
  wrap("La marge règle le contour affiché. Les collisions et la portée des coups gardent leurs dimensions Minecraft.",390,840,1400,24,MUTED);
 }
 private void hotbar(){
  toggle(1724,148,s().hotbarEnabled,()->s().hotbarEnabled=!s().hotbarEnabled);card(350,282,1524,635);
  for(int i=0;i<4;i++){int n=i;button(390+i*355,322,330,66,HotbarHud.STYLES[i],()->s().hotbarStyle=n,s().hotbarStyle==i);}
  label("Couleur de la sélection",390,466,27,TEXT,true);hue(390,505,1350,s().hotbarHue,v->s().hotbarHue=v);
  rowSlider("Opacité du fond",390,610,650,s().hotbarOpacity,pct(s().hotbarOpacity),v->s().hotbarOpacity=v);
  rowSlider("Taille de la hotbar",1130,610,650,s().hotbarScale,String.format("%.0f %%",80+s().hotbarScale*50),v->s().hotbarScale=v);
  rowSlider("Hauteur",390,770,1390,s().hotbarLift,pct(s().hotbarLift),v->s().hotbarLift=v);
  label("Fonds, cases et sélection remplacés · objets, quantités et main secondaire conservés.",390,892,24,MUTED,false);
 }
 private static final BufferedImage[] SKIN_PREVIEWS=new BufferedImage[AvatarSkins.IDS.length];
 private int skinPage;
 private BufferedImage avatarPreview(int model){if(SKIN_PREVIEWS[model]==null)try(var in=UiCanvas.class.getResourceAsStream("/assets/cakeclient/avatars/preview-"+model+".png")){if(in!=null)SKIN_PREVIEWS[model]=javax.imageio.ImageIO.read(in);}catch(Exception ignored){}return SKIN_PREVIEWS[model];}
 private void profileCard(){card(30,785,260,175);var mc=Minecraft.getInstance();String name=mc.player==null?"Cake Player":mc.player.getName().getString();round(48,804,64,64,14,new Color(87,61,130));if(s().skinEnabled){var preview=avatarPreview(s().avatarSkin-1);if(preview!=null)g.drawImage(preview,48,804,64,64,null);}else{label(name.substring(0,1).toUpperCase(),69,848,31,TEXT,true);}String shown=name.length()>13?name.substring(0,12)+"…":name;label(shown,126,834,20,TEXT,true);label(Edition.name().toUpperCase(java.util.Locale.ROOT),126,862,16,PURPLE,true);label("Minecraft 1.21.11",48,905,18,MUTED,false);label("Avatar & cosmetics →",48,938,17,PURPLE,false);widgets.add(new Widget(30,785,260,175,()->tab=15,null));}
 private int avatarPage=0;
 private void skins(){
 toggle(1724,148,s().skinEnabled,()->s().skinEnabled=!s().skinEnabled);card(350,282,925,635);card(1304,282,570,635);
 int pages=(SkinCatalog.IDS.length+5)/6;avatarPage=Math.clamp(avatarPage,0,pages-1);
 for(int k=0;k<6&&avatarPage*6+k<SkinCatalog.IDS.length;k++){int n=SkinCatalog.IDS[avatarPage*6+k];double x=390+k%3*285,y=312+k/3*225;button(x,y,267,210,"",()->s().avatarSkin=n,s().avatarSkin==n);var preview=avatarPreview(n-1);if(preview!=null)g.drawImage(preview,(int)x+45,(int)y+4,176,176,null);label(AvatarSkins.NAMES[n-1],x+15,y+197,19,s().avatarSkin==n?PURPLE:TEXT,true);}
 button(390,770,220,46,"← Previous",()->avatarPage=(avatarPage+pages-1)%pages,false);label((avatarPage+1)+" / "+pages,760,803,23,MUTED,false);button(1000,770,220,46,"Next →",()->avatarPage=(avatarPage+1)%pages,false);
 rowSlider("Taille visuelle",390,860,820,s().skinScale,String.format("%.2f ×",.85+s().skinScale*.3),v->s().skinScale=v);
 var preview=avatarPreview(s().avatarSkin-1);if(preview!=null)g.drawImage(preview,1324,295,530,530,null);label(AvatarSkins.NAMES[s().avatarSkin-1],1344,867,25,TEXT,true);label("Imported 3D models · author credits included",1344,901,18,MUTED,false);
 }
 private void musicViz(){
  toggle(1724,148,s().visualizer,()->s().visualizer=!s().visualizer);card(350,282,1524,635);
  label(SpotifyVisualizer.status(),390,344,26,TEXT,true);
  if(!Edition.musicCustomization(Edition.PLAN)){wrap("Spotify playback card, cover art and progress. Appearance customization is included in Premium.",390,460,1300,27,MUTED);return;}
  button(390,395,660,70,"Déplacer la carte à la souris",()->{var mc=Minecraft.getInstance();mc.setScreen(new SpotifyHudEditor(mc.screen));},true);
  button(1100,395,700,70,"Réinitialiser la position",()->{s().musicX=.025;s().musicY=.08;},false);
  button(390,495,660,62,"Pochette : "+(s().musicArtwork?"affichée":"masquée"),()->s().musicArtwork=!s().musicArtwork,s().musicArtwork);
  button(1100,495,700,62,"Mini VU-mètre : "+(s().musicBars?"oui":"non"),()->s().musicBars=!s().musicBars,s().musicBars);
  rowSlider("Taille de la carte",390,635,650,s().musicScale,pct(s().musicScale),v->s().musicScale=v);rowSlider("Opacité",1130,635,650,s().musicOpacity,pct(s().musicOpacity),v->s().musicOpacity=v);
  label("Accent",390,771,26,TEXT,true);hue(580,760,1200,s().visualizerHue,v->s().visualizerHue=v);
  wrap("Pochette Spotify, titre, artiste, progression et durée totale. Détection locale via les contrôles multimédias Windows ; la carte reste visible en pause.",390,866,1390,24,MUTED);
 }
 private void particles(boolean extra){
  toggle(1724,148,extra?s().extraParticles:s().hitParticles,()->{if(extra)s().extraParticles=!s().extraParticles;else s().hitParticles=!s().hitParticles;});card(350,282,1524,635);
  String[] styles=extra?new String[]{"Poussière lumineuse","Étoiles","Pétales","Bulles"}:new String[]{"Étoiles","Cœurs","Prismes","Orbes"};for(int i=0;i<4;i++){int n=i;button(390+(i%2)*710,330+(i/2)*90,675,65,styles[i],()->{if(extra)s().extraParticleStyle=n;else s().hitParticleStyle=n;},(extra?s().extraParticleStyle:s().hitParticleStyle)==i);}
  hue(390,590,1350,extra?s().extraParticleHue:s().hitParticleHue,v->{if(extra)s().extraParticleHue=v;else s().hitParticleHue=v;});rowSlider("Taille",390,700,1100,extra?s().extraParticleSize:s().hitParticleSize,"",v->{if(extra)s().extraParticleSize=v;else s().hitParticleSize=v;});
  if(!extra)rowSlider("Quantité",390,825,1100,s().hitParticleCount,pct(s().hitParticleCount),v->s().hitParticleCount=v);else wrap("Les particules restent dans le décor. Rendu groupé avec les autres effets et nombre limité pour préserver la fluidité.",390,850,1400,24,MUTED);
 }
 private int packItem,packAngle;
 private void packCustomizer(){
  card(350,282,925,635);card(1304,282,570,635);String[] labels={"Cristal","Mace","Totem","Obsidienne","Anchor"};
  for(int i=0;i<5;i++){int n=i;int y=320+i*90;button(380,y,200,64,labels[i],()->{packItem=n;packAngle=0;},packItem==i);button(600,y,70,64,"‹",()->{packItem=n;PvpCustomizer.cycle(n,-1);},false);String text=PvpCustomizer.label(s().pvpTextures[i]);if(text.length()>28)text=text.substring(0,27)+"…";button(690,y,455,64,text,()->{packItem=n;packAngle=0;PvpCustomizer.cycle(n,1);},s().pvpTextures[i]>0);button(1160,y,70,64,"›",()->{packItem=n;packAngle=0;PvpCustomizer.cycle(n,1);},false);}
  button(390,825,420,56,"Appliquer la sélection",PvpCustomizer::apply,true);wrap(PvpCustomizer.status(),840,850,390,21,MUTED);
  var preview=PvpCustomizer.preview(packItem,s().pvpTextures[packItem],packAngle);if(preview!=null)g.drawImage(preview,1330,300,520,520,null);else wrap("Le pack actuel reste actif pour cet objet.",1360,510,440,29,TEXT);
  button(1340,825,95,48,"↶",()->packAngle=Math.floorMod(packAngle-1,4),false);button(1735,825,95,48,"↷",()->packAngle=(packAngle+1)%4,false);
  String name=PvpCustomizer.label(s().pvpTextures[packItem]);wrap(name,1455,845,250,20,TEXT);label("Aperçu 1080p · textures et modèles réels",1344,901,20,MUTED,false);
 }
 private void fps(){
  var o=Minecraft.getInstance().options;card(350,282,925,635);card(1304,282,570,635);
  String[] modes={"Équilibré","Performance","Maximum"};for(int i=0;i<3;i++){int n=i+1;button(390+i*286,320,268,58,modes[i],()->CakeOptimize.preset(n),s().fpsPreset==n);}
  rowSlider("Distance de rendu",390,440,390,(CakeOptimize.requestedDistance()-2)/22.,o.renderDistance().get()+" chunks",v->CakeOptimize.view(2+(int)Math.round(v*22)));
  rowSlider("Simulation",840,440,390,(o.simulationDistance().get()-5)/7.,o.simulationDistance().get()+" chunks",v->CakeOptimize.edit(a->a.simulationDistance().set(5+(int)Math.round(v*7))));
  rowSlider("Distance des entités",390,610,390,(o.entityDistanceScaling().get()-.5)/.5,Math.round(o.entityDistanceScaling().get()*100)+" %",v->CakeOptimize.edit(a->a.entityDistanceScaling().set(.5+v*.5)));
  rowSlider("Limite d’images",840,610,390,(o.framerateLimit().get()-30)/230.,o.framerateLimit().get()>=260?"Illimitée":o.framerateLimit().get()+" i/s",v->CakeOptimize.edit(a->a.framerateLimit().set(30+(int)Math.round(v*23)*10)));
  label("Particules Minecraft",390,782,25,TEXT,false);String[] particle={"Toutes","Réduites","Minimales"};
  for(int i=0;i<3;i++){int n=i;button(390+i*282,817,262,57,particle[i],()->CakeOptimize.edit(a->a.particles().set(net.minecraft.server.level.ParticleStatus.values()[n])),o.particles().get().ordinal()==i);}
  button(1344,320,490,51,"VSync : "+(o.enableVsync().get()?"oui":"non"),()->CakeOptimize.edit(a->a.enableVsync().set(!a.enableVsync().get())),o.enableVsync().get());
  button(1344,390,490,51,"Ombres des entités : "+(o.entityShadows().get()?"oui":"non"),()->CakeOptimize.edit(a->a.entityShadows().set(!a.entityShadows().get())),o.entityShadows().get());
  button(1344,460,490,51,"Nuages : "+(o.cloudStatus().get()==net.minecraft.client.CloudStatus.OFF?"non":"oui"),()->CakeOptimize.edit(a->a.cloudStatus().set(a.cloudStatus().get()==net.minecraft.client.CloudStatus.OFF?net.minecraft.client.CloudStatus.FAST:net.minecraft.client.CloudStatus.OFF)),o.cloudStatus().get()!=net.minecraft.client.CloudStatus.OFF);
  button(1344,530,490,51,"Feuilles rapides : "+(o.cutoutLeaves().get()?"oui":"non"),()->CakeOptimize.edit(a->a.cutoutLeaves().set(!a.cutoutLeaves().get())),o.cutoutLeaves().get());
  button(1344,600,490,51,"Occlusion native : "+(o.ambientOcclusion().get()?"oui":"non"),()->CakeOptimize.edit(a->a.ambientOcclusion().set(!a.ambientOcclusion().get())),o.ambientOcclusion().get());
  button(1344,670,490,51,"Alléger les effets Cake : "+(s().fpsReduceEffects?"oui":"non"),()->{s().fpsReduceEffects=!s().fpsReduceEffects;if(s().fpsReduceEffects)CakeOptimize.reduceEffects();},s().fpsReduceEffects);
  button(1344,740,490,51,"Textures Potato 8×8 : "+(s().potatoEnabled?"oui":"non"),()->CakeOptimize.potato(!s().potatoEnabled),s().potatoEnabled);
  button(1344,835,490,56,"Rétablir mes réglages",CakeOptimize::restore,false);
 }
 private void fog(){
  toggle(1724,148,s().fogEnabled,()->{s().fogEnabled=!s().fogEnabled;if(s().fogEnabled&&s().fogStrength==0)s().fogStrength=.65;CakeOptimize.applyFogLimit();});
  card(350,282,925,635);card(1304,282,570,635);
  rowSlider("Distance",390,345,390,s().fogDistance,(int)FogMath.distance(s().fogDistance)+" blocs",v->{s().fogDistance=v;CakeOptimize.applyFogLimit();});
  rowSlider("Début de la brume",840,345,390,s().fogStart,Math.round(s().fogStart*100)+" %",v->s().fogStart=v);
  rowSlider("Densité",390,525,390,s().fogStrength,pct(s().fogStrength),v->s().fogStrength=v);
  rowSlider("Opacité",840,525,390,s().fogOpacity,pct(s().fogOpacity),v->s().fogOpacity=v);
  String[] presets={"Brume","Naturel","Violet"};for(int i=0;i<3;i++){int n=i;button(390+i*286,690,268,58,presets[i],()->{s().fogEnabled=true;s().fogStrength=.7;s().fogDistance=n==0?.09:.3;s().fogStart=n==0?.08:.35;s().fogTint=n==1?.12:.8;s().fogSaturation=n==2?.45:.18;s().fogBrightness=.76;s().fogHue=n==2?.76:.56;s().fogOpacity=1;CakeOptimize.applyFogLimit();},false);}
  float visible=Math.min(FogMath.distance(s().fogDistance),Minecraft.getInstance().options.renderDistance().get()*16f);
  label("Visibilité effective : "+(int)visible+" blocs",390,811,24,TEXT,true);
  wrap("Activez la limite de rendu pour dessiner moins de chunks au-delà du brouillard. La résolution reste inchangée.",390,855,840,21,MUTED);
  label("Couleur",1344,340,25,TEXT,false);hue(1344,374,490,s().fogHue,v->s().fogHue=v);
  rowSlider("Saturation",1344,460,490,s().fogSaturation,pct(s().fogSaturation),v->s().fogSaturation=v);
  rowSlider("Luminosité",1344,565,490,s().fogBrightness,pct(s().fogBrightness),v->s().fogBrightness=v);
  rowSlider("Mélange de la teinte",1344,670,490,s().fogTint,pct(s().fogTint),v->s().fogTint=v);
  button(1344,770,490,49,"Brume sur le ciel : "+(s().fogSky?"oui":"non"),()->s().fogSky=!s().fogSky,s().fogSky);
  button(1344,835,490,56,"Limiter le rendu : "+(s().fogLimitTerrain?"oui":"non"),()->{s().fogLimitTerrain=!s().fogLimitTerrain;CakeOptimize.applyFogLimit();},s().fogLimitTerrain);
 }
 private void optics(){
  toggle(1724,148,s().atmosphere,()->s().atmosphere=!s().atmosphere);
  card(350,282,925,635);card(1304,282,570,635);
  String[] names={"Naturel","Cinéma","Doré","Clair de lune"};for(int i=0;i<4;i++){int n=i;button(390+(i%2)*430,322+(i/2)*78,410,58,names[i],()->s().atmosphereStyle=n,s().atmosphereStyle==i);}
  rowSlider("Bloom lumineux",390,530,390,s().bloom,pct(s().bloom),v->s().bloom=v);
  rowSlider("Ombres de contact",840,530,390,s().occlusion,pct(s().occlusion),v->s().occlusion=v);
  rowSlider("Exposition",390,700,390,s().exposure,pct(s().exposure),v->s().exposure=v);
  rowSlider("Vignette douce",840,700,390,s().vignette,pct(s().vignette),v->s().vignette=v);
  label("Rendu à la résolution du jeu",1344,341,26,TEXT,true);
  wrap("Bloom des zones lumineuses, occlusion locale à partir de la profondeur et étalonnage des couleurs. Interface et paroles restent nettes.",1344,410,480,24,MUTED);
  wrap("1080p si le jeu est en 1920 × 1080. Aucun sous-échantillonnage. Le coût dépend de votre carte graphique.",1344,644,480,22,MUTED);
  button(1344,803,490,66,trialLabel(),()->PreviewTrial.start(7),true);
 }
 private void trialCard(int module,String title,String description){
  card(1304,282,570,635);label("Aperçu en jeu",1344,341,28,TEXT,true);
  wrap(title,1344,409,480,26,TEXT);wrap(description,1344,508,480,24,MUTED);
  wrap("À la fin, vos réglages précédents sont rétablis et le menu revient.",1344,642,480,23,MUTED);
  button(1344,803,490,66,trialLabel(),()->PreviewTrial.start(module),true);
 }
 private boolean enabled(int t){var a=s();return switch(t){case 0->a.motionBlur;case 1->a.fireflies;case 2->a.spatial;case 3->a.sky;case 4->a.sakura||a.motes||a.embers;case 5->a.lyrics;case 6,24->a.knives;case 7->a.atmosphere;case 8->a.fpsPreset>0;case 9->a.fogEnabled;case 10->a.trailEnabled;case 11->a.jumpEnabled;case 12->a.followEnabled;case 13->a.hitboxEnabled;case 14->a.hotbarEnabled;case 15->a.skinEnabled;case 16->a.visualizer;case 17->a.hitParticles;case 18->a.extraParticles;case 19->java.util.Arrays.stream(a.pvpTextures).anyMatch(n->n>0);case 20->a.backEnabled;case 21->a.realistic;case 22->a.crosshairEnabled;case 23->a.hitColor||a.deathEffects;default->false;};}
 private void preferences(){card(350,282,1524,635);label("General settings",390,340,30,TEXT,true);button(390,400,660,65,"Menu : "+MenuKey.name()+" · modifier",()->MenuKey.waiting=true,MenuKey.waiting);button(1100,400,690,65,"Language · "+s().language.toUpperCase(),()->s().language=s().language.equals("en")?"fr":s().language.equals("fr")?"ru":"en",false);option(390,590,"Partage cosmétiques : "+(s().shareCosmetics?"oui":"non"),s().shareCosmetics,()->s().shareCosmetics=!s().shareCosmetics);option(390,710,"Sons activés",s().sounds,()->s().sounds=!s().sounds);}
 private static final BufferedImage[] BACK_PREVIEWS=new BufferedImage[13];
 private BufferedImage backPreview(int model){if(BACK_PREVIEWS[model]==null)try(var in=UiCanvas.class.getResourceAsStream("/assets/cakeclient/back/preview-"+model+".png")){if(in!=null)BACK_PREVIEWS[model]=javax.imageio.ImageIO.read(in);}catch(Exception ignored){}return BACK_PREVIEWS[model];}
 private void backs(){
 toggle(1724,148,s().backEnabled,()->s().backEnabled=!s().backEnabled);card(350,282,925,635);card(1304,282,570,635);
 label("Choose your wings",390,337,29,TEXT,true);
 for(int i=0;i<BackCosmetics.NAMES.length;i++){final int n=i;button(390+i%3*285,370+i/3*68,267,55,BackCosmetics.NAMES[i],()->s().backStyle=n,s().backStyle==i);}
 rowSlider("Scale",390,785,390,s().backScale,pct(s().backScale),v->s().backScale=v);rowSlider("Animation speed",840,785,390,s().backSpeed,pct(s().backSpeed),v->s().backSpeed=v);
 label(BackCosmetics.NAMES[s().backStyle],1344,337,28,TEXT,true);
 var backImage=backPreview(s().backStyle);if(backImage!=null)g.drawImage(backImage,1324,350,530,298,null);else wrap("Choose a style, adjust its fit, then preview it on your character.",1344,425,470,24,MUTED);
 rowSlider("Shoulder height",1344,680,490,s().backLift,pct(s().backLift),v->s().backLift=v);
 label(s().backStyle<=5?"Smooth wingbeat":"Luminous contour",1344,791,22,TEXT,false);toggle(1724,758,s().backHalo,()->s().backHalo=!s().backHalo);
 button(1344,835,490,50,"Preview on my character · F5",()->PreviewTrial.start(20),true);label("Imported model collection · credits included",390,895,18,MUTED,false);
 }
 private void realism(){
 toggle(1724,148,s().realistic,()->s().realistic=!s().realistic);card(350,282,1524,635);
 button(390,310,690,52,"Realis · atmospheric",()->s().shaderPack=0,s().shaderPack==0);button(1110,310,690,52,"MakeUp · cinematic",()->s().shaderPack=1,s().shaderPack==1);label("Visual mood",390,414,24,TEXT,true);
 for(int i=0;i<3;i++){final int n=i;button(390+i*475,440,450,52,new String[]{"Natural light","Cinema","Dreamscape"}[i],()->s().realismLook=n,s().realismLook==i);}
 label("Render quality",390,553,25,TEXT,true);
 for(int i=0;i<3;i++){final int n=i;button(390+i*475,580,450,50,new String[]{"Balanced","High","Ultra"}[i],()->s().realismQuality=n,s().realismQuality==i);}
 if(s().shaderPack==0){ rowSlider("Exposure",390,690,420,s().realismExposure,pct(s().realismExposure),v->s().realismExposure=v);rowSlider("Bloom",870,690,420,s().realismBloom,pct(s().realismBloom),v->s().realismBloom=v);rowSlider("Rain & puddles",1350,690,420,s().realismRain,pct(s().realismRain),v->s().realismRain=v);}else wrap("MakeUp uses the author’s quality profiles. Advanced shader controls are available in Iris settings.",390,713,1370,24,MUTED);
 wrap("Photographic 256x materials, surface relief, animated water and wet reflections. Select a mood, then apply. Rain follows the world's weather.",390,800,900,22,MUTED);
 button(1344,775,450,64,"Apply look",()->{s().realistic=true;RealisticMode.refresh();},true);wrap(RealisticMode.status,390,895,1380,18,PURPLE);
 }
 private void crosshairs(){
 card(350,282,1524,635);option(390,340,"Custom crosshair",s().crosshairEnabled,()->s().crosshairEnabled=!s().crosshairEnabled);
 for(int i=0;i<8;i++){final int n=i;button(390+i%4*350,400+i/4*65,330,52,CrosshairHud.NAMES[i],()->s().crosshairStyle=n,s().crosshairStyle==i);}
 rowSlider("Size",390,600,640,s().crosshairSize,pct(s().crosshairSize),v->s().crosshairSize=v);rowSlider("Gap",1110,600,640,s().crosshairGap,pct(s().crosshairGap),v->s().crosshairGap=v);rowSlider("Thickness",390,750,640,s().crosshairThickness,pct(s().crosshairThickness),v->s().crosshairThickness=v);hue(1110,800,640,s().crosshairHue,v->s().crosshairHue=v);
 }
 private void combat(){
 card(350,282,1524,635);option(390,340,"Hit color",s().hitColor,()->s().hitColor=!s().hitColor);hue(1110,315,640,s().hitColorHue,v->s().hitColorHue=v);rowSlider("Tint strength",390,425,640,s().hitColorStrength,pct(s().hitColorStrength),v->s().hitColorStrength=v);
 option(390,620,"Death effects",s().deathEffects,()->s().deathEffects=!s().deathEffects);
 for(int i=0;i<5;i++){final int n=i;button(390+i%3*475,680+i/3*78,450,62,CombatEffects.NAMES[i],()->s().deathStyle=n,s().deathStyle==i);}
 }
 private void materials(){
 card(350,282,1524,635);
 for(int i=0;i<KnifeFinishes.NAMES.length;i++){final int n=i;button(390+i%4*350,320+i/4*72,330,58,KnifeFinishes.NAMES[i],()->s().knifeFinish=n,s().knifeFinish==i);}
 option(390,680,"Blade trails",s().knifeTrails,()->s().knifeTrails=!s().knifeTrails);hue(1110,660,640,s().knifeTrailHue,v->s().knifeTrailHue=v);rowSlider("Trail lifetime",390,790,640,s().knifeTrailLife,pct(s().knifeTrailLife),v->s().knifeTrailLife=v);
 wrap("Factory uses the original CS asset. The other materials are original Cake designs, available on every knife.",1110,780,640,22,MUTED);
 }
 private void option(double x,double y,String label,boolean on,Runnable action){label(label,x,y,27,TEXT,false);toggle(x+540,y-32,on,action);}
 private void rowSlider(String label,double x,double y,double w,double value,String display,DoubleConsumer consumer){label(label,x,y,25,TEXT,false);slider(x,y+50,w,value,display,consumer);}
 private void slider(double x,double y,double w,double value,String display,DoubleConsumer consumer){
  right(display,x+w,y-27,22,PURPLE);round(x,y,w,8,4,new Color(55,52,65));round(x,y,Math.max(8,w*Math.clamp(value,0,1)),8,4,PURPLE);
  g.setColor(TEXT);g.fill(new Ellipse2D.Double(x+w*Math.clamp(value,0,1)-10,y-6,20,20));
  widgets.add(new Widget(x,y-20,w,48,null,consumer));
 }
 private void hue(double x,double y,double w,double value,DoubleConsumer consumer){
  for(int i=0;i<(int)w;i++){g.setColor(Color.getHSBColor((float)(i/w),.5f,1));g.fillRect((int)x+i,(int)y,1,18);}
  g.setStroke(new BasicStroke(3));g.setColor(TEXT);g.draw(new RoundRectangle2D.Double(x+w*value-8,y-5,16,28,6,6));
  widgets.add(new Widget(x,y-14,w,48,null,consumer));
 }
 private void toggle(double x,double y,boolean on,Runnable action){round(x,y,110,51,25,on?PURPLE:new Color(66,63,74));g.setColor(on?TEXT:new Color(170,167,181));g.fill(new Ellipse2D.Double(x+(on?66:8),y+8,35,35));widgets.add(new Widget(x,y,110,51,action,null));}
 private void choice(double x,double y,double w,double h,String text,Runnable action,boolean primary,boolean allowed){text=I18n.tr(text);if(allowed){button(x,y,w,h,text,action,primary);return;}round(x,y,w,h,18,new Color(55,49,65,100));g.setFont(font((float)(h<55?21:24),false));var fm=g.getFontMetrics();g.setColor(new Color(127,119,140));float tx=(float)(x+(w-fm.stringWidth(text))/2),ty=(float)(y+(h-fm.getHeight())/2+fm.getAscent());g.drawString(text,tx,ty);g.setStroke(new BasicStroke(2));g.draw(new Line2D.Double(tx,ty-h*.14,tx+fm.stringWidth(text),ty-h*.14));}
 private void button(double x,double y,double w,double h,String text,Runnable action,boolean primary){text=I18n.tr(text);
  boolean hover=!spatial&&pointerX>=x&&pointerX<=x+w&&pointerY>=y&&pointerY<=y+h;
  Color color=primary?new Color(128,85,210,55):new Color(40,41,59,240);if(hover)color=new Color(92,73,134,210);
  round(x,y,w,h,12,color);if(primary){g.setStroke(new BasicStroke(1));g.setColor(new Color(178,139,255,125));g.draw(new RoundRectangle2D.Double(x,y,w,h,24,24));}float fs=(float)(h<55?21:24);g.setFont(font(fs,primary));while(g.getFontMetrics().stringWidth(text)>w-24&&fs>12){fs-=1;g.setFont(font(fs,primary));}FontMetrics fm=g.getFontMetrics();g.setColor(primary?new Color(225,204,255):TEXT);g.drawString(text,(float)(x+(w-fm.stringWidth(text))/2),(float)(y+(h-fm.getHeight())/2+fm.getAscent()));widgets.add(new Widget(x,y,w,h,action,null));
 }
 private void navItem(int c,double x,double y,double w,double h){
  boolean selected=category(tab)==c;round(x,y,w,h,13,selected?new Color(120,86,190,70):new Color(31,35,51,140));
  g.setColor(selected?PURPLE:MUTED);g.setStroke(new BasicStroke(1.8f));double cx=x+27,cy=y+h/2;
  if(c==0){g.draw(new Ellipse2D.Double(cx-7,cy-7,14,14));for(int k=0;k<8;k++){double a=k*Math.PI/4;g.draw(new Line2D.Double(cx+Math.cos(a)*10,cy+Math.sin(a)*10,cx+Math.cos(a)*13,cy+Math.sin(a)*13));}}
  else if(c==1){g.draw(new Arc2D.Double(cx-12,cy-7,16,14,0,180,Arc2D.OPEN));g.draw(new Arc2D.Double(cx-3,cy-4,14,10,0,180,Arc2D.OPEN));g.draw(new Line2D.Double(cx-12,cy+3,cx+11,cy+3));}
  else if(c==2){Path2D p=new Path2D.Double();p.moveTo(cx,cy-11);p.lineTo(cx+11,cy);p.lineTo(cx,cy+11);p.lineTo(cx-11,cy);p.closePath();g.draw(p);}
  else if(c==3){g.draw(new Ellipse2D.Double(cx-9,cy-9,18,18));g.draw(new Line2D.Double(cx-13,cy,cx+13,cy));g.draw(new Line2D.Double(cx,cy-13,cx,cy+13));}
  else if(c==4){g.draw(new Line2D.Double(cx-5,cy+6,cx-5,cy-9));g.draw(new Line2D.Double(cx-5,cy-9,cx+7,cy-12));g.draw(new Line2D.Double(cx+7,cy-12,cx+7,cy+3));g.fill(new Ellipse2D.Double(cx-11,cy+3,7,6));g.fill(new Ellipse2D.Double(cx+1,cy,7,6));}
  else{g.draw(new RoundRectangle2D.Double(cx-11,cy-9,22,18,5,5));g.draw(new Line2D.Double(cx-6,cy-3,cx+6,cy-3));g.draw(new Line2D.Double(cx-6,cy+3,cx+3,cy+3));}
  label(CATEGORY_NAMES[c],x+52,y+h/2+7,20,selected?TEXT:MUTED,selected);
  if(selected)round(x+52,y+h-5,40,2,1,PURPLE);
  widgets.add(new Widget(x,y,w,h,()->{tab=CATEGORIES[c][0];drag=-1;dirty=true;},null));
 }
 private void card(double x,double y,double w,double h){round(x+2,y+5,w,h,18,new Color(0,0,0,35));round(x,y,w,h,18,CARD);g.setPaint(new GradientPaint((float)x,(float)y,new Color(121,96,172,13),(float)(x+w),(float)(y+h),new Color(15,18,30,0)));g.fill(new RoundRectangle2D.Double(x,y,w,h,36,36));g.setStroke(new BasicStroke(1));g.setColor(LINE);g.draw(new RoundRectangle2D.Double(x,y,w,h,28,28));}
 private void line(double x,double y,double x2,double y2){g.setColor(LINE);g.setStroke(new BasicStroke(1.2f));g.draw(new Line2D.Double(x,y,x2,y2));}
 private void round(double x,double y,double w,double h,double r,Color color){g.setColor(color);g.fill(new RoundRectangle2D.Double(x,y,w,h,r*2,r*2));}
 private void label(String text,double x,double y,float size,Color color,boolean bold){text=I18n.tr(text);if(x==51&&size==23){while(g.getFontMetrics(font(size,bold)).stringWidth(text)>230&&size>13)size-=1;}g.setFont(font(size,bold));g.setColor(color);g.drawString(text,(float)x,(float)y);}
 private void right(String text,double x,double y,float size,Color color){text=I18n.tr(text);g.setFont(font(size,false));label(text,x-g.getFontMetrics().stringWidth(text),y,size,color,false);}
 private void wrap(String text,double x,double y,double w,float size,Color color){text=I18n.tr(text);
  g.setFont(font(size,false));StringBuilder line=new StringBuilder();
  for(String word:text.split(" ")){String next=line.isEmpty()?word:line+" "+word;if(g.getFontMetrics().stringWidth(next)>w&&!line.isEmpty()){label(line.toString(),x,y,size,color,false);line.setLength(0);y+=size*1.6;}if(!line.isEmpty())line.append(' ');line.append(word);}
  if(!line.isEmpty())label(line.toString(),x,y,size,color,false);
 }
 private static String trialLabel(){return Minecraft.getInstance().level==null?"Ouvrez un monde pour essayer":"Essayer en jeu · 6 s";}
 private static String pct(double v){return Math.round(v*100)+" %";}
 public void close(){if(texture!=null){texture.close();texture=null;}}
}
