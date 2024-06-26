package fr.dynamx.common.physics.terrain.cache;

import fr.dynamx.common.DynamXMain;
import fr.dynamx.utils.DynamXConfig;
import fr.dynamx.utils.VerticalChunkPos;

import java.io.*;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Terrain data cache
 */
public class TerrainFile extends VirtualTerrainFile
{
    public static boolean ULTIMATEDEBUG;

    private static final int VERSION = 6;
    private static final int SLOPES_VERSION = 4;

    private final File container;
    private final int version;

    //TODO BETTER MT
    private volatile boolean ioLoading;
    private final Lock ioLock = new ReentrantLock();

    public TerrainFile(File container) {
        this(container, false);
    }
    public TerrainFile(File container, boolean isSlopes) {
        if(DynamXConfig.enableDebugTerrainManager)
            DynamXMain.log.info("Chunk debug pos are "+DynamXConfig.chunkDebugPoses);
        this.container = container;
        this.version = isSlopes ? SLOPES_VERSION : VERSION;
    }

    @Override
    public void lock(VerticalChunkPos pos) {
        if(ioLoading) {
            if(ULTIMATEDEBUG) {
                System.out.println("Got locked on "+this+" "+container.getName()+" at "+pos);
            }
            ioLock.lock(); //simply wait until io load end
            ioLock.unlock(); //yes this is ugly
            if(ULTIMATEDEBUG) {
                System.out.println("Finished get locked on "+this+" "+container.getName()+" at "+pos);
            }
        }
        super.lock(pos);
    }

    public int getVersion() {
        return version;
    }

    public void load() throws IOException, ClassNotFoundException {
        if(ULTIMATEDEBUG) {
            System.out.println("Locking ! "+this+" "+container.getName());
        }
        ioLock.lock();
        ioLoading = true;
        try {
            if(container.exists()) {
                ObjectInputStream in = new ObjectInputStream(new GZIPInputStream(new FileInputStream(container)));
                short version = in.readShort();
                if (version >= this.getVersion()) {
                    int size = in.readInt();
                    for (int i = 0; i < size; i++) {
                        VerticalChunkPos pos = new VerticalChunkPos(in.readInt(), in.readInt(), in.readInt());
                        putData(pos, (byte[]) in.readObject());
                    }
                    in.close();
                }
                else if(version >= 1 && this.version != SLOPES_VERSION) { //Take care if SLOPES_VERSION == VERSION
                    in.close();
                    DynamXMain.log.warn("Outdated chunks collisions file : version "+version+", everything will be erased !");
                    DynamXMain.log.info("Deleted with success : "+container.delete());
                }
                else {
                    in.close();
                    throw new UnsupportedOperationException("Dnx chunk version " + version);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            ioLoading = false;
            ioLock.unlock();
            if(ULTIMATEDEBUG) {
                System.out.println("Unlocking ! "+this+" "+container.getName());
            }
        }
    }

    public void save() throws IOException {
        if(ULTIMATEDEBUG) {
            System.out.println("SAV Locking ! "+this+" "+container.getName());
        }
        ioLock.lock();
        ioLoading = true;
        try {
            container.createNewFile();
            ObjectOutputStream out = new ObjectOutputStream(new GZIPOutputStream(new FileOutputStream(container)));
            out.writeShort(version);
            out.writeInt(dataCache.size());
            for(Map.Entry<VerticalChunkPos, byte[]> entry : dataCache.entrySet()) {
                out.writeInt(entry.getKey().x);
                out.writeInt(entry.getKey().y);
                out.writeInt(entry.getKey().z);
                out.writeObject(entry.getValue());
            }
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            ioLoading = false;
            ioLock.unlock();
            if(ULTIMATEDEBUG) {
                System.out.println("SAV Unlocking ! "+this+" "+container.getName());
            }
        }
    }
}
