package de.codesourcery.arduino;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Optional;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.Timer;
import org.apache.commons.lang3.Validate;

final class MainWindowPanel extends JPanel
{
    public enum Mode {
        SET,CLEAR
    }

    private final ImageSelectionPanel imageSelectionPanel;

    private float dx, dy, x0, y0;

    private boolean animate;

    private Timer animationTimer;

    private final JPanel renderPanel = new JPanel()
    {
        {
            setFocusable( true );
            requestFocus();
            final MouseAdapter listener = new MouseAdapter()
            {
                private Mode mode;

                private Optional<Mode> getMode(MouseEvent e)
                {
                    if ( e.getButton() == MouseEvent.BUTTON1 )
                    {
                        return Optional.of( Mode.SET );
                    }
                    if ( e.getButton() == MouseEvent.BUTTON3 )
                    {
                        return Optional.of( Mode.CLEAR );
                    }
                    return Optional.empty();
                }

                @Override
                public void mouseClicked(MouseEvent e)
                {
                    if ( ! isAnimationRunning() )
                    {
                        getMode( e ).ifPresent( m -> {
                            update( e.getPoint(), m );
                        } );
                    } else {
                        stopAnimation();
                    }
                }

                @Override
                public void mousePressed(MouseEvent e)
                {
                    if ( ! isAnimationRunning() )
                    {
                        if ( mode == null )
                        {
                            getMode( e ).ifPresent( m -> mode = m );
                        }
                    } else {
                        stopAnimation();
                    }
                }

                @Override
                public void mouseReleased(MouseEvent e)
                {
                    if ( ! isAnimationRunning() )
                    {
                        if ( mode != null )
                        {
                            getMode( e ).ifPresent( m -> {
                                if ( mode == m )
                                {
                                    mode = null;
                                }
                            } );
                        }
                    } else {
                        stopAnimation();
                    }
                }

                private void update(Point mousePosition, Mode mode)
                {
                    if ( mode != null )
                    {
                        viewToModel( mousePosition ).ifPresent( p -> {
                            if ( imageSelectionPanel.getSelectedImage().set( p, mode == Mode.SET ) ) {
                                imageSelectionPanel.imageChanged( imageSelectionPanel.getSelectedImage() );
                            }
                            System.out.println( "========================" );
                            System.out.println( imageSelectionPanel.getProject().toDataString() );
                            repaint();
                        } );
                    }
                }

                private Optional<Point> viewToModel(Point p)
                {
                    int x = (int) ((p.x - x0) / dx);
                    int y = (int) ((p.y - y0) / dy);
                    Optional<Point> result;
                    if ( x >= 0 && y >= 0 && x < 8 && y < 8 )
                    {
                        result = Optional.of( new Point( x, y ) );
                    }
                    else
                    {
                        result = Optional.empty();
                    }
                    System.out.println( "=> " + result );
                    return result;
                }

                @Override
                public void mouseDragged(MouseEvent e)
                {
                    update( e.getPoint(), mode );
                }
            };
            addMouseListener( listener );
            addMouseMotionListener( listener );
        }

        @Override
        protected void paintComponent(Graphics g)
        {
            super.paintComponent( g );
            g.setColor( Color.BLACK );
            g.fillRect( 0, 0, getWidth(), getHeight() );

            System.out.println( "PANEL: " + getWidth() + "x" + getHeight() );
            dx = getWidth() * 0.9f / 8;
            dy = getHeight() * 0.9f / 8;

            x0 = getWidth() * 0.02f;
            y0 = getHeight() * 0.02f;

            float px, py;
            for ( int y = 0; y < 8; y++ )
            {
                py = y0 + y * dy;
                for ( int x = 0; x < 8; x++ )
                {
                    px = x0 + x * dx;
                    g.setColor( imageSelectionPanel.getSelectedImage().isSet( x, y ) ? Color.WHITE : Color.BLACK );
                    g.fillRect( (int) px, (int) py, (int) dx, (int) dy );
                }
            }
            // draw grid
            g.setColor( Color.WHITE );
            for ( int y = 0; y <= 8; y++ )
            {
                py = y0 + y * dy;
                g.drawLine( (int) x0, (int) py, (int) (x0 + 8 * dx), (int) py );
            }
            for ( int x = 0; x <= 8; x++ )
            {
                px = x0 + x * dx;
                g.drawLine( (int) px, (int) y0, (int) px, (int) (y0 + 8 * dy) );
            }
            g.setColor( Color.RED );
        }
    };

    private final KeyAdapter keyAdapter = new KeyAdapter()
    {
        private boolean isControl(KeyEvent e) {
            return ( e.getModifiersEx() & InputEvent.CTRL_DOWN_MASK ) != 0;
        }

        @Override
        public void keyReleased(KeyEvent e)
        {
            if ( e.getKeyCode() == KeyEvent.VK_DELETE ) {
                imageSelectionPanel.deleteImage(  imageSelectionPanel.getSelectedImage() );
                renderPanel.repaint();
            } else if ( e.getKeyCode() == KeyEvent.VK_LEFT ) {
                if ( imageSelectionPanel.selectPreviousImage() )
                {
                    renderPanel.repaint();
                }
            } else if ( e.getKeyCode() == KeyEvent.VK_RIGHT ) {
                if ( imageSelectionPanel.selectNextImage() )
                {
                    renderPanel.repaint();
                }
            }
        }

        @Override
        public void keyTyped(KeyEvent e)
        {
            if ( isAnimationRunning() ) {
                stopAnimation();
                return;
            }
            if ( e.getKeyChar() == 'p') {
                startAnimation();
            } else if ( e.getKeyChar() == 'd' ) {
                imageSelectionPanel.duplicateImage();
                renderPanel.repaint();
            } else if ( e.getKeyChar() == 'n' ) {
                imageSelectionPanel.newImage();
                renderPanel.repaint();
            } else if ( e.getKeyChar() == 'c' ) {
                if ( imageSelectionPanel.getSelectedImage().clear() ) {
                    imageSelectionPanel.repaint();
                    renderPanel.repaint();
                }
            } else if ( e.getKeyChar() == 'f' ) {
                if ( imageSelectionPanel.getSelectedImage().fill() ) {
                    imageSelectionPanel.repaint();
                    renderPanel.repaint();
                }
            }
        }
    };

    public MainWindowPanel(Project p)
    {
        Validate.notNull( p, "project must not be null" );
        imageSelectionPanel = new ImageSelectionPanel( p );
        setFocusable( true );
        imageSelectionPanel.addKeyListener( keyAdapter );

        addKeyListener( keyAdapter );

        setLayout( new GridBagLayout() );

        // render panel
        GridBagConstraints cnstr = new GridBagConstraints();
        cnstr.fill = GridBagConstraints.BOTH;
        cnstr.weightx = 1;
        cnstr.weighty = 1;
        cnstr.gridx = 0;
        cnstr.gridy = 0;
        cnstr.gridwidth=GridBagConstraints.REMAINDER;
        cnstr.gridheight = 1;
        add( renderPanel, cnstr );

        // images panel
        cnstr = new GridBagConstraints();
        cnstr.fill = GridBagConstraints.BOTH;
        cnstr.weightx = 0;
        cnstr.weighty = 0.1;
        cnstr.gridx = 0;
        cnstr.gridy = 1;
        cnstr.gridwidth=GridBagConstraints.REMAINDER;
        cnstr.gridheight=GridBagConstraints.REMAINDER;
        add( new JScrollPane( imageSelectionPanel , JScrollPane.VERTICAL_SCROLLBAR_NEVER, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS ), cnstr );
    }

    public void setProject(Project project) {
        imageSelectionPanel.setProject( project );
        renderPanel.repaint();
    }
    public Project getProject() {
        return imageSelectionPanel.getProject();
    }

    private void startAnimation() {
        if ( animate ) {
            stopAnimation();
        }
        animate = true;
        final int millis = imageSelectionPanel.getProject().getAnimationSpeedMillis();
        animationTimer = new Timer(millis, ev -> {
            final Image img = getProject().getNextImage( imageSelectionPanel.getSelectedImage() );
            imageSelectionPanel.setSelectedImage( img );
            imageSelectionPanel.repaint();
            renderPanel.repaint();
        });
        animationTimer.start();
    }

    private boolean isAnimationRunning() {
        return animate;
    }

    private void stopAnimation() {
        if ( animate )
        {
            animationTimer.stop();
        }
    }
}