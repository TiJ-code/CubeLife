package de.tij.sandboxgame;

import java.nio.IntBuffer;
import java.util.Random;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL15;
import org.lwjgl.util.vector.Vector3f;
import static org.lwjgl.opengl.GL11.*;

public class World extends RenderObject {

    private int chunkBoundingBoxID;
    private IntBuffer occlusionQueries;
    Thread updateThread;
    Chunk[][][] chunks;
    Random rand = new Random();

    PerlinNoiseGenerator pGen = new PerlinNoiseGenerator();
    public static final Vector3f worldDimensions = new Vector3f(1024, 256, 1024);

    public World() {
        chunks = new Chunk[(int) Configurator.viewingDistanceInChunks.x][(int) Configurator.viewingDistanceInChunks.y][(int) Configurator.viewingDistanceInChunks.z];

        occlusionQueries = BufferUtils.createIntBuffer(8192);
        GL15.glGenQueries(occlusionQueries);

        updateThread = new Thread(new Runnable() {

            @Override
            public void run() {
                updateWorld();
            }
        });

        updateThread.start();

        chunkBoundingBoxID = glGenLists(1);

        glNewList(chunkBoundingBoxID, GL_COMPILE);
        glColor3f(255.0f, 0.0f, 0.0f);
        glBegin(GL_LINE_LOOP);
        glVertex3f(0.0f, 0.0f, 0.0f);
        glVertex3f(Chunk.chunkDimensions.x, 0.0f, 0.0f);
        glVertex3f(Chunk.chunkDimensions.x, Chunk.chunkDimensions.y, 0.0f);
        glVertex3f(0.0f, Chunk.chunkDimensions.y, 0.0f);
        glEnd();

        glBegin(GL_LINE_LOOP);
        glVertex3f(0.0f, 0.0f, 0.0f);
        glVertex3f(0.0f, 0.0f, Chunk.chunkDimensions.z);
        glVertex3f(0.0f, Chunk.chunkDimensions.y, Chunk.chunkDimensions.z);
        glVertex3f(0.0f, Chunk.chunkDimensions.y, 0.0f);
        glVertex3f(0.0f, 0.0f, 0.0f);
        glEnd();

        glBegin(GL_LINE_LOOP);
        glVertex3f(0.0f, 0.0f, Chunk.chunkDimensions.z);
        glVertex3f(Chunk.chunkDimensions.x, 0.0f, Chunk.chunkDimensions.z);
        glVertex3f(Chunk.chunkDimensions.x, Chunk.chunkDimensions.y, Chunk.chunkDimensions.z);
        glVertex3f(0.0f, Chunk.chunkDimensions.y, Chunk.chunkDimensions.z);
        glVertex3f(0.0f, 0.0f, Chunk.chunkDimensions.z);
        glEnd();

        glBegin(GL_LINE_LOOP);
        glVertex3f(Chunk.chunkDimensions.x, 0.0f, 0.0f);
        glVertex3f(Chunk.chunkDimensions.x, 0.0f, Chunk.chunkDimensions.z);
        glVertex3f(Chunk.chunkDimensions.x, Chunk.chunkDimensions.y, Chunk.chunkDimensions.z);
        glVertex3f(Chunk.chunkDimensions.x, Chunk.chunkDimensions.y, 0.0f);
        glVertex3f(Chunk.chunkDimensions.x, 0.0f, 0.0f);
        glEnd();
        glEndList();

        glEndList();
    }

    @Override
    public void render() {

        glPushMatrix();

        glDisable(GL_COLOR_MATERIAL);
        //glDepthMask(false);
        //glColorMask(false, false, false, false);

        /*for (int x = 0; x < (int) Configurator.viewingDistanceInChunks.x; x++) {
            for (int y = 0; y < (int) Configurator.viewingDistanceInChunks.y; y++) {
                for (int z = 0; z < (int) Configurator.viewingDistanceInChunks.z; z++) {

                    glPushMatrix();
                    glTranslatef(x * 16.0f, y * 128.0f, z * 16.0f);

                    GL15.glBeginQuery(GL15.GL_SAMPLES_PASSED, occlusionQueries.get(64 * x + 2 * y + 64 * z));
                    glCallList(chunkBoundingBoxID);
                    GL15.glEndQuery(GL15.GL_SAMPLES_PASSED);

                    glPopMatrix();
                }

            }
        }*/

        glEnable(GL_COLOR_MATERIAL);
        glColorMask(true, true, true, true);
        glDepthMask(true);

        int chunkUpdates = 0;

        for (int x = 0; x < (int) Configurator.viewingDistanceInChunks.x; x++) {
            for (int y = 0; y < (int) Configurator.viewingDistanceInChunks.y; y++) {
                for (int z = 0; z < (int) Configurator.viewingDistanceInChunks.z; z++) {
                    if (chunks[x][y][z] != null) {

                        if (!updateThread.isAlive()) {

                            if (chunkUpdates < 5) {
                                if (chunks[x][y][z].updateDisplayList()) {
                                    chunkUpdates++;
                                }
                            }

                            chunks[x][y][z].render();
                        }
                    }
                }
            }
        }
        glPopMatrix();
    }


    public final void updateWorld() {
        long timeStart = System.currentTimeMillis();

        for (int x = 0; x < worldDimensions.x; x++) {
            for (int z = 0; z < worldDimensions.z; z++) {
                setBlock(new Vector3f(x, 0, z), 0x3);
            }
        }

        for (int x = 0; x < worldDimensions.x; x++) {
            for (int z = 0; z < worldDimensions.z; z++) {
                float height = calcTerrainElevation(x, z) + (calcTerrainRoughness(x, z) * calcTerrainDetail(x, z)) * 64 + 64;

                float y = height;

                while (y > 0) {
                    if (getCaveDensityAt(x, y, z) > 0) {
                        if (height == y) {
                            setBlock(new Vector3f(x, y, z), 0x1);
                        } else {
                            setBlock(new Vector3f(x, y, z), 0x2);
                        }
                    }

                    if (y < 50) {
                        if (getBlock(new Vector3f(x, y, z)) == 0) {
                            setBlock(new Vector3f(x, y, z), 0x4);
                        }
                    }
                    y--;
                }
            }
        }

        /*for (int x = 0; x < worldDimensions.x; x++) {
            for (int z = 0; z < worldDimensions.z; z++) {
                for (int y = 0; y < 128; y++) {
                    if (getCaveDensityAt(x, y, z) < 0.5) {
                        setBlock(new Vector3f(x, y, z), 0x3);
                    }
                }
            }
        }

                for (int x = 0; x < worldDimensions.x; x++) {
            for (int z = 0; z < worldDimensions.z; z++) {
                for (int y = 128; y >= 128 && y < 140; y++) {
                    if (getCaveDensityAt(x, y, z) < 0.8) {
                        setBlock(new Vector3f(x, y, z), 0x2);
                    }
                }
            }
        }*/
        System.out.println("World updated (" + (System.currentTimeMillis() - timeStart) / 1000d + "s).");
    }

    public final void setBlock(Vector3f pos, int type) {
        Vector3f chunkPos = calcChunkPos(pos);
        Vector3f blockCoord = calcBlockPos(pos, chunkPos);

        try {
            Chunk c = chunks[(int) chunkPos.x][(int) chunkPos.y][(int) chunkPos.z];

            if (c == null) {
                //System.out.println("Generating chunk at X: " + chunkPos.x + ", Y: " + chunkPos.y + ", Z: " + chunkPos.z);
                c = new Chunk(this, new Vector3f(chunkPos.x, chunkPos.y, chunkPos.z));
                chunks[(int) chunkPos.x][(int) chunkPos.y][(int) chunkPos.z] = c;
            }
            c.setBlock(blockCoord, type);
        } catch (Exception e) {
            return;
        }
    }

    public final int getBlock(Vector3f pos) {
        Vector3f chunkPos = calcChunkPos(pos);
        Vector3f blockCoord = calcBlockPos(pos, chunkPos);

        try {
            Chunk c = chunks[(int) chunkPos.x][(int) chunkPos.y][(int) chunkPos.z];
            return c.getBlock((int) blockCoord.x, (int) blockCoord.y, (int) blockCoord.z);
        } catch (Exception e) {
            return 0;
        }
    }

    public boolean isHitting(Vector3f pos) {
        Vector3f chunkPos = calcChunkPos(pos);
        Vector3f blockCoord = calcBlockPos(pos, chunkPos);

        try {
            Chunk c = chunks[(int) chunkPos.x][(int) chunkPos.y][(int) chunkPos.z];
            return (c.getBlock((int) blockCoord.x, (int) blockCoord.y, (int) blockCoord.z) > 0);
        } catch (Exception e) {
            return false;
        }
    }

    private Vector3f calcChunkPos(Vector3f pos) {
        return new Vector3f((int) (pos.x / Chunk.chunkDimensions.x), (int) (pos.y / Chunk.chunkDimensions.y), (int) (pos.z / Chunk.chunkDimensions.z));
    }

    private Vector3f calcBlockPos(Vector3f pos, Vector3f chunkPos) {
        return new Vector3f(pos.x - (chunkPos.x * Chunk.chunkDimensions.x), pos.y - (chunkPos.y * Chunk.chunkDimensions.y), pos.z - (chunkPos.z * Chunk.chunkDimensions.z));
    }

    float calcTerrainElevation(float x, float y) {
        float result = 0.0f;
        result += pGen.noise(0.2f * x, 0.2f * y, 0.2f);
        return result;
    }

    float calcTerrainRoughness(float x, float y) {
        float result = 0.0f;
        result += pGen.noise(0.01f * x, 0.01f * y, 0.01f);
        return result;
    }

    float calcTerrainDetail(float x, float y) {
        float result = 0.0f;
        result += pGen.noise(0.1f * x, 0.1f * y, 1.318293f);
        return result;
    }

    float getCaveDensityAt(float x, float y, float z) {
        float result = 0.0f;
        result += pGen.noise(0.005f * x, 0.005f * y, 0.005f * z);
        return result;
    }
}
