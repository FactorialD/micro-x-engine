package com.microx.engine.ui;
import javax.microedition.lcdui.*; import com.microx.engine.world.Player;
public final class Hud {
 private final Font font=Font.getFont(Font.FACE_SYSTEM,Font.STYLE_PLAIN,Font.SIZE_SMALL);
 public void paint(Graphics g,Player p,int fps,int entities,int rooms,boolean debug){int w=g.getClipWidth(),h=g.getClipHeight();int vx=(w-240)/2,vy=(h-320)/2;g.setFont(font);g.setColor(0xffffff);g.drawString("HP "+p.health+"  AR "+p.armor,vx+4,vy+4,Graphics.TOP|Graphics.LEFT);g.drawString("ST "+p.stamina+"  AMMO "+p.ammo,vx+4,vy+18,Graphics.TOP|Graphics.LEFT);int cx=w/2,cy=h/2;g.drawLine(cx-5,cy,cx+5,cy);g.drawLine(cx,cy-5,cx,cy+5);if(debug)g.drawString("FPS "+fps+" E "+entities+" R "+rooms,vx+4,vy+302,Graphics.TOP|Graphics.LEFT);}
}
