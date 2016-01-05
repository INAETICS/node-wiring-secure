/**
 * Licensed under Apache License v2. See LICENSE for more information.
 */
package org.inaetics.remote.admin.itest.api;

/**
 * Simple POJO with default constructor to test custom type roundtrips.
 */
public class EchoData {

    private int x;
    private String y;

    public EchoData() {
    }

    public EchoData(int x, String y) {
        this.x = x;
        this.y = y;
    }

    public int getX() {
        return x;
    }

    public String getY() {
        return y;
    }

    public void setX(int x) {
        this.x = x;
    }

    public void setY(String y) {
        this.y = y;
    }
}
