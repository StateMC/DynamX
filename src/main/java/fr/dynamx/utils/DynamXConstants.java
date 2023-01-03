package fr.dynamx.utils;

import net.minecraftforge.fml.common.versioning.ArtifactVersion;
import net.minecraftforge.fml.common.versioning.DefaultArtifactVersion;

public class DynamXConstants
{
    public static final String NAME = "DynamX";
    public static final String ID = "dynamxmod";
    public static final String VERSION = "3.3.2";
    public static final String VERSION_TYPE = "Beta";
    public static final String RES_DIR_NAME = "DynamX";

    public static final String ACS_GUIS_BASE_URL = "https://maven.dynamx.fr/artifactory/ACsGuisRepo/fr/aym/acsguis/ACsGuis/%1$s/ACsGuis-%1$s.jar";
    public static final String DEFAULT_ACSGUIS_VERSION = "1.2.4-3";
    public static final String ACSLIBS_REQUIRED_VERSION = "[1.1.0,)"; //TODO UPGRADE TO 1.2.0

    public static final String LIBBULLET_VERSION = "16.3.0";
    /** .dc file version, only change when an update of libbullet breaks the .dc files, to regenerate them */
    public static final String DC_FILE_VERSION = "12.5.0";
    /**
     * Version of the {@link fr.dynamx.common.contentpack.ContentPackLoader}
     */
    public static final ArtifactVersion PACK_LOADER_VERSION = new DefaultArtifactVersion("1.0.2");

    public static final String DYNAMX_CERT = "certs/lets-encrypt-r3.der", DYNAMX_AUX_CERT = null;
    public static final String MPS_URL = "https://dynamx.fr/mps/", MPS_AUX_URL = "https://mps2.dynamx.fr/", MPS_KEY = "", MPS_STARTER = null;// "fr.dynamx.impl.ProtectionStarter";

    public static final String STATS_URL = "https://dynamx.fr/statsbot/statsbotrcv.php", STATS_PRODUCT = "DNX_"+VERSION+"_BETA", STATS_TOKEN = "";
}