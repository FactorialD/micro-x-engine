package com.microx.engine.assets;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.microedition.media.Manager;
import javax.microedition.media.Player;

/** Loads compact binary render data and keeps shared/location ownership separate. */
public final class AssetManager {
    private static final int MESH_MAGIC=0x4d584d32,TEXTURE_MAGIC=0x4d585432;
    private static final TextureData WHITE=new TextureData(1,1,new int[]{0xffffff});
    private MeshSection[] sharedSections,locationSections;private TextureData[] sharedTextures,locationTextures;private Player music;

    public boolean loadShared(String root){releaseShared();sharedSections=readMesh(root+"/geometry.mesh");sharedTextures=readTextures(root+"/textures.tex");return sharedSections!=null||sharedTextures!=null;}
    public boolean loadLocation(String name,int volume){
        unloadLocation();String root="/levels/"+name;locationSections=readMesh(root+"/geometry.mesh");locationTextures=readTextures(root+"/textures.tex");
        if(volume>0){InputStream stream=getClass().getResourceAsStream(root+"/music.mid");if(stream!=null)try{music=Manager.createPlayer(stream,"audio/midi");music.realize();}catch(Exception ignored){music=null;}}
        return true;
    }
    public int locationSectionCount(){return locationSections==null?0:locationSections.length;}
    public MeshSection locationSection(int index){return locationSections[index];}
    public int maximumLocationVertices(){int max=0,i;if(locationSections!=null)for(i=0;i<locationSections.length;i++)if(locationSections[i].vertexCount()>max)max=locationSections[i].vertexCount();return max;}
    public TextureData texture(int id){
        if(id>=0&&locationTextures!=null&&id<locationTextures.length)return locationTextures[id];
        int shared=-id-1;if(shared>=0&&sharedTextures!=null&&shared<sharedTextures.length)return sharedTextures[shared];return WHITE;
    }
    public void unloadLocation(){if(music!=null){music.close();music=null;}locationSections=null;locationTextures=null;}
    public void releaseShared(){sharedSections=null;sharedTextures=null;}
    public void release(){unloadLocation();releaseShared();}

    private MeshSection[] readMesh(String path){InputStream raw=getClass().getResourceAsStream(path);if(raw==null)return null;DataInputStream in=new DataInputStream(raw);try{
        if(in.readInt()!=MESH_MAGIC)return null;int sectionCount=in.readUnsignedShort();MeshSection[] result=new MeshSection[sectionCount];int s;
        for(s=0;s<sectionCount;s++){int room=in.readUnsignedShort(),texture=in.readShort(),vertices=in.readUnsignedShort(),triangles=in.readUnsignedShort();int[] xyz=new int[vertices*3],uv=new int[vertices*2];short[] index=new short[triangles*3];int i;for(i=0;i<xyz.length;i++)xyz[i]=in.readInt();for(i=0;i<uv.length;i++)uv[i]=in.readInt();for(i=0;i<index.length;i++){index[i]=in.readShort();if((index[i]&0xffff)>=vertices)return null;}result[s]=new MeshSection(room,texture,xyz,uv,index);}return result;
    }catch(IOException e){return null;}finally{try{in.close();}catch(IOException ignored){}}}
    private TextureData[] readTextures(String path){InputStream raw=getClass().getResourceAsStream(path);if(raw==null)return null;DataInputStream in=new DataInputStream(raw);try{
        if(in.readInt()!=TEXTURE_MAGIC)return null;int count=in.readUnsignedShort();TextureData[] result=new TextureData[count];int t;for(t=0;t<count;t++){int w=in.readUnsignedShort(),h=in.readUnsignedShort();if(w<=0||h<=0||w>256||h>256)return null;int[] rgb=new int[w*h];int i;for(i=0;i<rgb.length;i++)rgb[i]=in.readInt()&0xffffff;result[t]=new TextureData(w,h,rgb);}return result;
    }catch(IOException e){return null;}finally{try{in.close();}catch(IOException ignored){}}}
}
