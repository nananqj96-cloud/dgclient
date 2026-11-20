package fr.cake.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import org.lwjgl.system.MemoryUtil;
import java.awt.image.BufferedImage;

/** Antialiased artwork at native resolution, uploaded only when its content changes. */
public final class SmoothTexture extends DynamicTexture {
 public final Identifier id;
 private final int[] previous,row;
 private int[] decodedPixels;
 private final int width,height;
 private boolean initialized;
 private long uploadedBytes,uploads;
 public long uploadedBytes(){return uploadedBytes;}
 public long uploads(){return uploads;}
 public SmoothTexture(String name,int width,int height){
  super("Cake / "+name,width,height,true);
  id=Identifier.fromNamespaceAndPath("cakeclient","dynamic/"+name);
  sampler=RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);
  this.width=width;this.height=height;previous=new int[width*height];row=new int[width];Minecraft.getInstance().getTextureManager().register(id,this);
 }
 public void update(BufferedImage image){
  int[] source=ImagePixels.packedArgb(image,width,height);
  if(source==null){
   if(decodedPixels==null)decodedPixels=new int[width*height];
   ImagePixels.copyArgb(image,width,height,decodedPixels);
   source=decodedPixels;
  }
  DirtyRegion dirty=initialized?DirtyRegion.between(source,previous,width,height):new DirtyRegion(0,0,width,height);
  if(dirty==null)return;
  var pixels=MemoryUtil.memIntBuffer(getPixels().getPointer(),width*height);
  for(int y=dirty.y();y<dirty.y()+dirty.height();y++){
   int offset=y*width+dirty.x();
   for(int x=0;x<dirty.width();x++){int p=source[offset+x];previous[offset+x]=p;row[x]=(p&0xff00ff00)|((p>>>16)&255)|((p&255)<<16);}
   pixels.position(offset);pixels.put(row,0,dirty.width());
  }
  RenderSystem.getDevice().createCommandEncoder().writeToTexture(getTexture(),getPixels(),0,0,dirty.x(),dirty.y(),dirty.width(),dirty.height(),dirty.x(),dirty.y());
  initialized=true;uploads++;uploadedBytes+=(long)dirty.width()*dirty.height()*4;
 }
}
