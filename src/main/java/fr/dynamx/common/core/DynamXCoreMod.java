package fr.dynamx.common.core;

import fr.aym.acslib.services.impl.stats.core.StatsBotCorePlugin;
import fr.aym.loadingscreen.client.SplashScreenTransformer;
import fr.dynamx.common.core.asm.EntityLivingBasePatcher;
import fr.dynamx.common.core.asm.EntityPatcher;
import fr.dynamx.utils.DynamXConstants;
import fr.dynamx.utils.LibraryInstaller;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.Mixins;

import java.io.File;
import java.util.Map;

@IFMLLoadingPlugin.MCVersion("1.12.2")
public class DynamXCoreMod implements IFMLLoadingPlugin
{
	public static final Logger LOG = LogManager.getLogger("DynamXCoreMod");

	public DynamXCoreMod() {
		MixinBootstrap.init();
		Mixins.addConfiguration("mixins.dynamxmod.json");
	}

	@Override
	public String[] getASMTransformerClass() {
		return new String[] {SplashScreenTransformer.class.getName(), EntityPatcher.class.getName(), EntityLivingBasePatcher.class.getName(), StatsBotCorePlugin.class.getName()};
	}
	@Override
	public String getModContainerClass() {return null;}
	@Override
	public String getSetupClass() {return null;}
	@Override
	public void injectData(Map<String, Object> data) 
	{
		EntityPatcher.runtimeDeobfuscationEnabled = (Boolean) data.get("runtimeDeobfuscationEnabled");
		if(EntityPatcher.runtimeDeobfuscationEnabled) { //Production
			LOG.info("Checking ACsGuis installation...");
			if (!LibraryInstaller.loadACsGuis(LOG, new File("mods"), DynamXConstants.DEFAULT_ACSGUIS_VERSION)) {
				LOG.fatal("ACsGuis library cannot be found or installed !");
			}
		} else {
			LOG.info("DevEnv detected, skipping ACsGuis installation check");
		}
	}
	@Override
	public String getAccessTransformerClass() {return null;}
}
