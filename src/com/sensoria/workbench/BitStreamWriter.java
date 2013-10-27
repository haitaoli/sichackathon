package com.sensoria.workbench;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class BitStreamWriter {
    private ByteArrayOutputStream output = null;
    private int currentValue = 0;
    private int currentBitsOffset = 0;
    private int  maxBits = 32;

    public BitStreamWriter(ByteArrayOutputStream osw) {
        output = osw;
    }

    private void writeInt() throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(4);
        buffer.putInt(currentValue);
        output.write(buffer.array());
    }

    public void write(int value, int bits) throws IOException{
        while (bits > 0) {
            int freeSpace = maxBits - currentBitsOffset;
            if (freeSpace >= bits) {
                currentValue <<= bits;
                currentValue |= value;
                currentBitsOffset += bits;
                bits = 0;
            } else {
                int valueHigh = value >> freeSpace;
                int maskLow = 0xffffffff << freeSpace;

                currentValue <<= freeSpace;
                currentValue |= valueHigh;
                writeInt();
                currentValue = 0;
                currentBitsOffset = 0;
                bits -= freeSpace;
                value = value & maskLow;
            }
        }

        if (currentBitsOffset == maxBits) {
            writeInt();
            currentBitsOffset = 0;
            currentValue = 0;
        }
    }

    public void close() throws IOException {
        if (currentBitsOffset > 0) {
            writeInt();
        }

        output.close();
    }

    public byte[] toByteArray() {
        return output.toByteArray();
    }
}
