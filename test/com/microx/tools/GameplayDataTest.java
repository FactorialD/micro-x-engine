package com.microx.tools;
import java.nio.charset.StandardCharsets;import java.nio.file.*;
public final class GameplayDataTest {
 public static void main(String[] args)throws Exception{Path d=Files.createTempDirectory("microx-data");Path q=d.resolve("quests.data");Files.write(q,("1|a|A|requires=2\n2|b|B|requires=1\n").getBytes(StandardCharsets.UTF_8));boolean failed=false;try{AssetConverter.writeGameplayData(d,d.resolve("out.dat"));}catch(java.io.IOException expected){failed=true;}if(!failed)throw new AssertionError("bad quest references accepted");System.out.println("GameplayDataTest OK");}
}
