package fr.dynamx.utils;

import fr.aym.mps.core.BasicMpsConfig;
import fr.aym.mps.utils.ProtectionException;

import static fr.dynamx.utils.DynamXConstants.*;

public class DynamXMpsConfig extends BasicMpsConfig {
    public DynamXMpsConfig() {
        super(VERSION, MPS_KEY, null, MPS_URL, new String[]{MPS_AUX_URL},
                new String[0], MPS_STARTER, 0, 0, 0, new byte[]{});
    }

    @Override
    public void checkIntegrity(int l0x00, int h0x01, int l0x10, byte[] a0X21) throws ProtectionException {
        //...
    }
}
