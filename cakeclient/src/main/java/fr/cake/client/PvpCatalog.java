package fr.cake.client;
import com.google.gson.Gson;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
public final class PvpCatalog {
 public record Choice(int id,String name,int[] items,String author,String license,String source){public boolean supports(int item){for(int i:items)if(i==item)return true;return false;}}
 public static final List<Choice> CHOICES=load();
 private static List<Choice> load(){try(var in=PvpCatalog.class.getResourceAsStream("/assets/cakeclient/pvp/catalog.json")){return List.of(new Gson().fromJson(new InputStreamReader(in,StandardCharsets.UTF_8),Choice[].class));}catch(Exception e){throw new ExceptionInInitializerError(e);}}
 public static Choice get(int id){return CHOICES.get(Math.clamp(id,0,CHOICES.size()-1));}
 public static int valid(int item,int id){return id>=0&&id<CHOICES.size()&&get(id).supports(item)?id:0;}
 public static int next(int item,int id,int direction){int result=id;do{result=Math.floorMod(result+direction,CHOICES.size());}while(!get(result).supports(item));return result;}
}
