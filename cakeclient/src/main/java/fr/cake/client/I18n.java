package fr.cake.client;
import java.util.*;import java.io.*;import java.nio.charset.StandardCharsets;
public final class I18n{
 private static final Map<String,String[]> entries=new LinkedHashMap<>();static{try(var in=I18n.class.getResourceAsStream("/assets/cakeclient/lang/ui.tsv")){if(in!=null)for(String line:new String(in.readAllBytes(),StandardCharsets.UTF_8).split("\n")){String[] a=line.split("\\t",-1);if(a.length==3){entries.put(a[0],a);entries.put(a[1],a);entries.put(a[2],a);}}}catch(Exception e){CakeClient.LOG.warn("Translations unavailable",e);}}
 public static String tr(String text){if(text==null)return "";var s=CakeClient.settings;int i=s==null||s.language.equals("en")?0:s.language.equals("fr")?1:2;var a=entries.get(text);if(a!=null)return a[i];for(var e:entries.entrySet()){String key=e.getKey();if(key.length()>4&&(key.endsWith(": ")||key.endsWith(" · "))&&text.startsWith(key))return e.getValue()[i]+tr(text.substring(key.length()));}if(text.endsWith(" · modifier"))return tr(text.substring(0,text.length()-11))+new String[]{" · edit"," · modifier"," · изменить"}[i];return text;}
}
