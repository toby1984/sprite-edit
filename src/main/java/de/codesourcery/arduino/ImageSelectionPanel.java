package de.codesourcery.arduino;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import org.apache.commons.lang3.Validate;
import de.codesourcery.arduino.events.CurrentImageChangedEvent;

public class ImageSelectionPanel extends JPanel
{
    private static final int IMG_SPACING = 5;

    private static final int IMG_WIDTH = 32;
    private static final int IMG_HEIGHT = 32;

    private Project project;
    private Image selectedImage;

    private int x0,y0;
    private int imgWidth, imgHeight;

    public ImageSelectionPanel(Project project)
    {
        Validate.notNull( project, "project must not be null" );
        this.project = project;
        this.selectedImage = project.getFirstImage();
        setFocusable( true );

        final MouseAdapter a = new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e)
            {
                int imgIdx = e.getX() - x0;
                if ( imgIdx >= x0 ) {
                    if ( imgIdx <= ( x0 + imgWidth ) ) {
                        imgIdx = 0;
                    }
                    else
                    {
                        imgIdx = imgIdx / (imgWidth + IMG_SPACING);
                    }
                }
                final List<Image> images = getProject().getImages();
                if ( imgIdx >=0 && imgIdx < images.size() ) {
                    setSelectedImage( images.get( imgIdx ) );
                    repaint();
                }
            }
        };
        addMouseListener( a );
    }

    public Image getSelectedImage()
    {
        return selectedImage;
    }

    public void setSelectedImage(Image selectedImage)
    {
        Validate.notNull( selectedImage, "selectedImage must not be null" );
        this.selectedImage = selectedImage;
        final int idx = getProject().getImages().indexOf( selectedImage );
        final Rectangle imageRect = new Rectangle();
        getPreviewBounds( idx, imageRect );
        final JViewport vp = (JViewport) getParent();
        final Rectangle viewRect = vp.getViewRect();

        if ( imageRect.x < viewRect.x )
        {
            final Point pos = vp.getViewPosition();
            pos.x = Math.max( 0, imageRect.x - 1 );
            vp.setViewPosition( pos );
        } else
        {
            vp.scrollRectToVisible( imageRect );
        }
        update();
        EventBus.send( new CurrentImageChangedEvent( this, getProject(), selectedImage) );
    }

    public void setProject(Project project)
    {
        Validate.notNull( project, "project must not be null" );
        this.project = project;
        setSelectedImage( project.getFirstImage() );
        update();
    }

    @Override
    public Dimension getPreferredSize()
    {
        final List<Image> images = project.getImages();
        final int w = (int) (1.2f*(images.size() * IMG_WIDTH + images.size() * IMG_SPACING));
        final int h = (int) (IMG_HEIGHT * 1.2f);
        final Dimension s = new Dimension( w, h );
        System.out.println("New panel size:"+s);
        return s;
    }

    private void update() {
        getParent().revalidate();
        repaint();
    }

    public void imageChanged(Image selectedImage)
    {
        update();
    }

    public void newImage() {
        System.out.println( "Adding image." );
        final Image img = new Image();
        project.add( img );
        setSelectedImage( img );
        update();
    }

    private void getPreviewBounds(int imageIndex, Rectangle r) {
        final int startX = x0 + imageIndex* IMG_SPACING + imageIndex* imgWidth;
        r.setBounds( startX-1, y0-1, imgWidth, imgHeight );
    }

    @Override
    protected void paintComponent(Graphics g)
    {
        super.paintComponent( g );

        g.setColor( Color.BLACK );
        g.fillRect( 0, 0, getWidth(), getHeight() );

        int w = (int) (getWidth() * 0.99f);
        int h = (int) (getHeight() * 0.95f);
        x0 = (int) (getWidth()*0.01f);
        y0 = (int) (getHeight()*0.05f);

        final List<Image> images = project.getImages();
        imgWidth = (w - (images.size() * IMG_SPACING)) / images.size();
        imgHeight = (int) (h*0.95f);

        final Rectangle tmp = new Rectangle();

        for ( int i = 0; i < images.size(); i++ )
        {
            final Image image = images.get( i );
            getPreviewBounds( i, tmp );

            g.setColor( image == selectedImage ? Color.RED : Color.WHITE );
            g.drawRect( tmp.x, tmp.y, tmp.width, tmp.height );

            final java.awt.Image toDraw = image.render();
            g.drawImage( toDraw, tmp.x+1, tmp.y + 1, tmp.width-1, tmp.height-1, null  );
        }
    }

    public void duplicateImage() {
        final Image copy = getSelectedImage().createCopy();
        final List<Image> images = project.getImages();
        final int idx = images.indexOf( getSelectedImage() );
        project.add( idx+1, copy );
        setSelectedImage( copy );
        update();
    }

    public void deleteImage(Image image) {
        final List<Image> images = project.getImages();
        final int idx = images.indexOf( image );
        project.delete( image );
        if ( selectedImage == image )
        {
            setSelectedImage( idx < images.size() ? images.get( idx ) : images.getLast() );
        }
        update();
    }

    public boolean selectPreviousImage()
    {
        final List<Image> images = project.getImages();
        final int newIdx = images.indexOf( selectedImage )-1;
        if ( newIdx >= 0 ) {
            setSelectedImage( images.get( newIdx ) );
            repaint();
            return true;
        }
        return false;
    }

    public boolean selectNextImage()
    {
        final List<Image> images = project.getImages();
        final int newIdx = images.indexOf( selectedImage ) + 1;
        if ( newIdx < images.size() ) {
            setSelectedImage( images.get( newIdx ) );
            repaint();
            return true;
        }
        return false;
    }

    public Project getProject()
    {
        return project;
    }
}
