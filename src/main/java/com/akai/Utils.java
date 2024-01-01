package com.akai;

public final class Utils {
    static int getPadColum(int padId, int[][] pads) {
        for (int i = 0; i < pads.length; i++) {
            for (int j = 0; j < pads[i].length; j++) {
                if (pads[i][j] == padId)
                    return i;
            }
        }
        return -1;
    }

    static int getPadRow(int padId, int[][] pads) {
        for (int i = 0; i < pads.length; i++) {
            for (int j = 0; j < pads[i].length; j++) {
                if (pads[i][j] == padId)
                    return j;
            }
        }
        return -1;
    }
}
