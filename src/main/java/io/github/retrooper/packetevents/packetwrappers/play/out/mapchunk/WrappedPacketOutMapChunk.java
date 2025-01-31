/*
 * This file is part of packetevents - https://github.com/retrooper/packetevents
 * Copyright (C) 2021 retrooper and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.github.retrooper.packetevents.packetwrappers.play.out.mapchunk;

import io.github.retrooper.packetevents.packettype.PacketTypeClasses;
import io.github.retrooper.packetevents.packetwrappers.NMSPacket;
import io.github.retrooper.packetevents.packetwrappers.WrappedPacket;
import io.github.retrooper.packetevents.utils.reflection.SubclassUtil;
import io.github.retrooper.packetevents.utils.server.ServerVersion;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.BitSet;
import java.util.Optional;

//TODO finish wrapper
public class WrappedPacketOutMapChunk extends WrappedPacket {
    private static boolean v_1_8_x, v_1_17;
    private static Class<?> chunkMapClass;
    private Constructor<?> chunkMapConstructor;
    private Object nmsChunkMap;

    public WrappedPacketOutMapChunk(NMSPacket packet) {
        super(packet);
    }

    @Override
    protected void load() {
        v_1_8_x = version.isNewerThan(ServerVersion.v_1_7_10) && version.isOlderThan(ServerVersion.v_1_9);
        v_1_17 = version.isNewerThanOrEquals(ServerVersion.v_1_17);
        if (v_1_8_x) {
            chunkMapClass = SubclassUtil.getSubClass(PacketTypeClasses.Play.Server.MAP_CHUNK, 0);
            try {
                chunkMapConstructor = chunkMapClass.getConstructor();
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }
        }
    }

    public int getChunkX() {
        return readInt(v_1_17 ? 1 : 0);
    }

    public void setChunkX(int chunkX) {
        writeInt(v_1_17 ? 1: 0, chunkX);
    }

    public int getChunkZ() {
        return readInt(v_1_17 ? 2: 1);
    }

    public void setChunkZ(int chunkZ) {
        writeInt(v_1_17 ? 2: 1, chunkZ);
    }

    public BitSet getBitSet() {
        if (v_1_17) {
            return readObject(0, BitSet.class);
        }
        return BitSet.valueOf(new long[]{getPrimaryBitMask()});
    }

    @Deprecated
    public void setPrimaryBitMap(int primaryBitMask) {
        setPrimaryBitMask(primaryBitMask);
    }

    @Deprecated
    public Integer getPrimaryBitMask() {
        if (v_1_17) { // Possible lossy conversion
            long[] bitset = readObject(0, BitSet.class).toLongArray();
            return bitset.length == 0 ? 0 : (int) bitset[0];
        }
        if (v_1_8_x) {
            if (nmsChunkMap == null) {
                nmsChunkMap = readObject(0, chunkMapClass);
            }
            WrappedPacket nmsChunkMapWrapper = new WrappedPacket(new NMSPacket(nmsChunkMap));
            return nmsChunkMapWrapper.readInt(0);
        } else {
            return readInt(2);
        }
    }

    /**
     *
     * @param bitSet Bitset that can hold multiple integer values to support the expanded world height on
     *               1.17 and newer servers. This is due to 1.17 needing to support up to 127 chunk sections
     *               (2032 blocks) and an integer only being able to hold 32 bits to represent 32 chunk sections
     */
    public void setPrimaryBitMask(BitSet bitSet) {
        if (v_1_17) {
            writeObject(0, bitSet);
        }
        setPrimaryBitMask((int) bitSet.toLongArray()[0]);
    }

    /**
     *
     * @param primaryBitMask Integer that determines which chunk sections the server is sending
     *
     * @deprecated Possible lossy conversion on 1.17 servers that could result in the client only reading 32
     * out of the total possible 127 chunk sections. Safe to use on 1.16 and below servers
     */
    @Deprecated
    public void setPrimaryBitMask(int primaryBitMask) {
        if (v_1_17) {
            writeObject(0, BitSet.valueOf(new long[] {primaryBitMask}));
            return;
        }
        if (v_1_8_x) {
            if (nmsChunkMap == null) {
                try {
                    nmsChunkMap = chunkMapConstructor.newInstance();
                } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                    e.printStackTrace();
                }
            }
            WrappedPacket nmsChunkMapWrapper = new WrappedPacket(new NMSPacket(nmsChunkMap));
            nmsChunkMapWrapper.writeInt(0, primaryBitMask);
            write(chunkMapClass, 0, nmsChunkMap);

        } else {
            writeInt(2, primaryBitMask);
        }
    }

    /**
     *
     * @return Whether the packet overwrites the entire chunk column or just the sections being sent
     */
    public Optional<Boolean> isGroundUpContinuous() {
        if (v_1_17) {
            return Optional.empty();
        }
        return Optional.of(readBoolean(0));
    }

    /**
     *
     * @param groundUpContinuous Whether the packet overwrites the entire chunk column or just the sections being sent
     */
    public void setGroundUpContinuous(boolean groundUpContinuous) {
        if (v_1_17) {
            return;
        }
        writeBoolean(0, groundUpContinuous);
    }

    public byte[] getCompressedData() {
        if (v_1_8_x) {
            if (nmsChunkMap == null) {
                nmsChunkMap = readObject(0, chunkMapClass);
            }
            WrappedPacket nmsChunkMapWrapper = new WrappedPacket(new NMSPacket(nmsChunkMap));
            return nmsChunkMapWrapper.readByteArray(0);
        } else {
            return readByteArray(0);
        }
    }

    public void setCompressedData(byte[] data) {
        if (v_1_8_x) {
            if (nmsChunkMap == null) {
                try {
                    nmsChunkMap = chunkMapConstructor.newInstance();
                } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                    e.printStackTrace();
                }
            }
            WrappedPacket nmsChunkMapWrapper = new WrappedPacket(new NMSPacket(nmsChunkMap));
            nmsChunkMapWrapper.writeByteArray(0, data);
            write(chunkMapClass, 0, nmsChunkMap);
        } else {
            writeByteArray(0, data);
        }
    }
}
