/*
 * $Id$
 * Copyright (C) 2001-2002 The Apache Software Foundation. All rights reserved.
 * For details on use and redistribution please refer to the
 * LICENSE file included with these sources.
 */

package org.apache.fop.svg;

import java.awt.GraphicsConfiguration;
import java.awt.Rectangle;
import java.awt.GraphicsDevice;
import java.awt.Transparency;
import java.awt.image.ColorModel;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

/**
 * Our implementation of the class that returns information about
 * roughly what we can handle and want to see (alpha for example).
 */
class PDFGraphicsConfiguration extends GraphicsConfiguration {
    // We use this to get a good colormodel..
    private static final BufferedImage BI_WITH_ALPHA =
        new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
    // We use this to get a good colormodel..
    private static final BufferedImage BI_WITHOUT_ALPHA =
        new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);

    /**
     * Construct a buffered image with an alpha channel, unless
     * transparencty is OPAQUE (no alpha at all).
     *
     * @param width the width of the image
     * @param height the height of the image
     * @param transparency the alpha value of the image
     * @return the new buffered image
     */
    public BufferedImage createCompatibleImage(int width, int height,
            int transparency) {
        if (transparency == Transparency.OPAQUE) {
            return new BufferedImage(width, height,
                                     BufferedImage.TYPE_INT_RGB);
        } else {
            return new BufferedImage(width, height,
                                     BufferedImage.TYPE_INT_ARGB);
        }
    }

    /**
     * Construct a buffered image with an alpha channel.
     *
     * @param width the width of the image
     * @param height the height of the image
     * @return the new buffered image
     */
    public BufferedImage createCompatibleImage(int width, int height) {
        return new BufferedImage(width, height,
                                 BufferedImage.TYPE_INT_ARGB);
    }

    /**
     * FIXX ME: This should return the page bounds in Pts,
     * I couldn't figure out how to get this for the current
     * page from the PDFDocument (this still works for now,
     * but it should be fixed...).
     *
     * @return the bounds of the PDF document page
     */
    public Rectangle getBounds() {
        System.out.println("getting getBounds");
        return null;
    }

    /**
     * Return a good default color model for this 'device'.
     * @return the colour model for the configuration
     */
    public ColorModel getColorModel() {
        return BI_WITH_ALPHA.getColorModel();
    }

    /**
     * Return a good color model given <tt>transparency</tt>
     *
     * @param transparency the alpha value for the colour model
     * @return the colour model for the configuration
     */
    public ColorModel getColorModel(int transparency) {
        if (transparency == Transparency.OPAQUE) {
            return BI_WITHOUT_ALPHA.getColorModel();
        } else {
            return BI_WITH_ALPHA.getColorModel();
        }
    }

    /**
     * The default transform (1:1).
     *
     * @return the default transform for the configuration
     */
    public AffineTransform getDefaultTransform() {
        System.out.println("getting getDefaultTransform");
        return new AffineTransform();
    }

    /**
     * The normalizing transform (1:1) (since we currently
     * render images at 72dpi, which we might want to change
     * in the future).
     *
     * @return the normalizing transform for the configuration
     */
    public AffineTransform getNormalizingTransform() {
        System.out.println("getting getNormalizingTransform");
        return new AffineTransform(2, 0, 0, 2, 0, 0);
    }

    /**
     * Return our dummy instance of GraphicsDevice
     *
     * @return the PDF graphics device
     */
    public GraphicsDevice getDevice() {
        return new PDFGraphicsDevice(this);
    }

    /*
     // for jdk1.4
     public java.awt.image.VolatileImage createCompatibleVolatileImage(int width, int height) {
     return null;
     }
     */
}

