package com.microx.engine.ui;
import javax.microedition.lcdui.*;
/** Menu painter; all displayed strings are allocated once at class loading. */
public final class UIView {
 private static final String[] TITLES={"MICRO X","GAME","PAUSED","PDA","INVENTORY","MAP","QUESTS","DIALOGUE","TRADE","LOOT","SETTINGS","LOAD ERROR"};
 private static final String[] MAIN={"NEW GAME","LOAD","SETTINGS","EXIT"},PAUSE={"RESUME","SAVE","LOAD","SETTINGS","MAIN MENU"},PDA={"INVENTORY","MAP","QUESTS","BACK"},SETTINGS={"VOLUME","RESOLUTION","DEBUG","CONTROLS"};
 private static final String[] EMPTY={"NO ENTRIES"};private final Font font=Font.getFont(Font.FACE_SYSTEM,Font.STYLE_BOLD,Font.SIZE_SMALL);
 public void paint(Graphics g,UIStateMachine ui,UISettings settings){int w=g.getClipWidth(),h=g.getClipHeight();g.setColor(0x101820);g.fillRect(0,0,w,h);g.setFont(font);g.setColor(0xe0d080);g.drawString(TITLES[ui.state()],w/2,22,Graphics.TOP|Graphics.HCENTER);String[] items=items(ui.state());for(int i=0;i<items.length;i++){g.setColor(i==ui.selection()?0xffffff:0x809090);g.drawString(items[i],w/2,62+i*24,Graphics.TOP|Graphics.HCENTER);}if(ui.state()==UIStateMachine.SETTINGS){value(g,settings.volume,190,62);value(g,settings.resolution,190,86);value(g,settings.debug?1:0,190,110);value(g,settings.controls,190,134);}}
 private String[] items(int state){if(state==UIStateMachine.MAIN_MENU)return MAIN;if(state==UIStateMachine.PAUSE)return PAUSE;if(state==UIStateMachine.PDA)return PDA;if(state==UIStateMachine.SETTINGS)return SETTINGS;return EMPTY;}
 private void value(Graphics g,int n,int x,int y){g.drawChar((char)('0'+n),x,y,Graphics.TOP|Graphics.LEFT);}
}
