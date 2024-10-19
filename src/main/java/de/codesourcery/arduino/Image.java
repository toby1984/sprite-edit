package de.codesourcery.arduino;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class Image
{
    private static final AtomicLong ID = new AtomicLong();

    private final long imageId = ID.getAndIncrement();

    public byte[] data = new byte[8];

    private boolean isDirty;

    public Image() {
    }

    public int getWidth() {
        return 8;
    }

    public int getHeight() {
        return 8;
    }


    public Image(byte[] data) {
        this.data = data;
    }

    public boolean isSet(int x, int y) {
        final int bitMask =1<<y;
        return (data[x] & bitMask) != 0;
    }

    public boolean fill() {

        boolean changed = false;
        for ( final byte b : data )
        {
            if (b != 0xff ) {
                changed = true;
                break;
            }
        }
        Arrays.fill( data, (byte) 0xff );
        isDirty |= changed;
        return changed;
    }

    public boolean clear() {

        boolean changed = false;
        for ( final byte b : data )
        {
            if (b != 0 ) {
                changed = true;
                break;
            }
        }
        Arrays.fill( data, (byte) 0 );
        isDirty |= changed;
        return changed;
    }

    public boolean set(Point point, boolean onOff) {
        return set(point.x,point.y, onOff);
    }

    public boolean set(int x, int y,boolean onOff) {
        final int bitMask = 1<<y;
        byte oldValue = data[x];
        if ( onOff ) {
            data[x] |= bitMask;
        } else {
            data[x] &= ~bitMask;
        }
        final boolean hasChanged = oldValue != data[x];
        isDirty |= hasChanged;
        return hasChanged;
    }

    public java.awt.Image render() {
        final BufferedImage img = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
        final Graphics2D gfx = img.createGraphics();
        for ( int y = 0 ; y < getHeight() ; y++ )
        {
            for ( int x = 0 ; x < getWidth() ; x++ )
            {
                gfx.setColor( isSet(x,y) ? Color.WHITE : Color.BLACK );
                gfx.fillRect( x, y, 1, 1 );
            }
        }
        gfx.dispose();
        return img;
    }

    public String toDataString() {

        StringBuilder b = new StringBuilder();
        for ( int x = 0 ; x < getWidth() ; x++ )
        {
            final byte value = data[x];
            b.append("0x").append(Integer.toHexString(value & 0xff));
            if ( (x+1) <  getWidth()  ) {
                b.append( ", ");
            }
        }
        return b.toString();
    }

    public static Image fromDataString(String s) {

        String[] parts = s.split(",");
        final List<Byte> data = new ArrayList<>();
        for ( String part : parts )
        {
            part = part.trim();
            if ( part.startsWith("0x") || part.startsWith( "0X" ) ) {
                part = part.substring( 2 );
            }
            data.add( (byte) Integer.parseInt( part, 16 ) );
        }
        final byte[] byteData = new byte[data.size()];
        for ( int i = 0 ; i < data.size() ; i++ ) {
            byteData[i] = data.get(i);
        }
        return new Image( byteData );
    }

    public void setDirty(boolean dirty)
    {
        isDirty = dirty;
    }

    public boolean isDirty()
    {
        return isDirty;
    }

    public Image createCopy()
    {
        final byte[] data = new byte[this.data.length];
        System.arraycopy( this.data, 0, data , 0 , data.length );
        return new Image( data );
    }

    @Override
    public String toString()
    {
        return "Image #" + imageId;
    }
}