package com.sensoria.webapi;

import com.sensoria.workbench.BitStreamWriter;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class DataStreamHeader {
    public short Prefix;
    public short HeaderSize;
    public short DataType;
    public short SourceType;
    public int SamplingPeriod;
    public int SamplesCount;
    public short RowSize;

    public DataStreamHeader() {
        Prefix = 0x5346;
        HeaderSize = 18;
    }

    public static DataStreamHeader readFromStream(DataInputStream in) throws IOException {
        DataStreamHeader newHeader = new DataStreamHeader();
        newHeader.Prefix = in.readShort();
        newHeader.HeaderSize = in.readShort();
        newHeader.DataType = in.readShort();
        newHeader.SourceType = in.readShort();
        newHeader.SamplingPeriod = in.readInt();
        newHeader.SamplesCount = in.readInt();
        newHeader.RowSize = in.readShort();

        return newHeader;
    }

    public void writeHeader(DataOutputStream out) throws IOException {
        out.writeShort(Prefix);
        out.writeShort(HeaderSize);
        out.writeShort(DataType);
        out.writeShort(SourceType);
        out.writeInt(SamplingPeriod);
        out.writeInt(SamplesCount);
        out.writeShort(RowSize);
    }

    public void writeHeader(BitStreamWriter out) throws IOException {
        out.write(Prefix, 16);
        out.write(HeaderSize, 16);
        out.write(DataType, 16);
        out.write(SourceType, 16);
        out.write(SamplingPeriod, 32);
        out.write(SamplesCount, 32);
        out.write(RowSize, 16);
    }

}
