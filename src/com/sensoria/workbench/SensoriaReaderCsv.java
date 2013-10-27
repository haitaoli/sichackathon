package com.sensoria.workbench;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Scanner;

public class SensoriaReaderCsv {
    private Scanner s = null;
    private FileReader br = null;

    public SensoriaReaderCsv(String filePath) {
        try {
            s = new Scanner(new FileInputStream(filePath));
            // Skip the header lines
            for (int i = 0; i < 5; i++) {
                s.nextLine();
            }
        }
        catch (FileNotFoundException ex) {
            br = null;
        }
    }

    public SensoriaDataEntry nextEntry() throws  IOException {
        SensoriaDataEntry newEntry = new SensoriaDataEntry();

        String nextLine = s.nextLine();
        if (nextLine == null) {
            return null;
        }

        String[] values = nextLine.split(",");
        if (values.length > 5) {
            newEntry.s0 = Integer.parseInt(values[3]);
            newEntry.s1 = Integer.parseInt(values[4]);
            newEntry.s2 = Integer.parseInt(values[5]);
        }

        if (values.length > 10) {
            newEntry.lat = Float.parseFloat(values[9]);
            newEntry.lon = Float.parseFloat(values[10]);
        }

        if (values.length > 8) {
            newEntry.ax = Float.parseFloat(values[6]);
            newEntry.ay = Float.parseFloat(values[7]);
            newEntry.az = Float.parseFloat(values[8]);
        }

        return newEntry;
    }

    public boolean hasNext() {
        return s.hasNextLine();
    }
}
