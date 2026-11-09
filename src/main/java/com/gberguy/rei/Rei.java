package com.gberguy.rei;

import com.gberguy.rei.command.CommandRei;
import com.gberguy.rei.network.ReiNetwork;
import com.gberguy.rei.proxy.CommonProxy;
import java.util.Map;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppedEvent;
import net.minecraftforge.fml.common.network.NetworkCheckHandler;
import net.minecraftforge.fml.relauncher.Side;

@Mod(
        modid = Rei.MOD_ID,
        name = Rei.NAME,
        version = Rei.VERSION,
        acceptedMinecraftVersions = "[1.12.2]",
        dependencies = "required-after:forge@[14.23.5.2859,);required-after:baubles@[1.5.2,)"
)
public class Rei {
    public static final String MOD_ID = "rei";
    public static final String NAME = "Rei";
    public static final String VERSION = "0.1.1";

    @SidedProxy(clientSide = "com.gberguy.rei.client.ClientProxy", serverSide = "com.gberguy.rei.proxy.CommonProxy")
    public static CommonProxy proxy;

    @NetworkCheckHandler
    public boolean acceptsRemote(Map<String, String> versions, Side remoteSide) {
        return VERSION.equals(versions.get(MOD_ID));
    }

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        ReiNetwork.register();
        proxy.initialize();
    }

    @Mod.EventHandler
    public void serverStopped(FMLServerStoppedEvent event) {
        com.gberguy.rei.player.VisualSync.clear();
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new CommandRei());
    }
}
