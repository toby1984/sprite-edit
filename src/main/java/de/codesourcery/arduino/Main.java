package de.codesourcery.arduino;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import org.apache.commons.lang3.Validate;

public class Main extends JFrame
{
    private final MainWindowPanel mainPanel;
    private final Configuration configuration;

    private JMenu recentFiles;

    private static JMenuItem menuItem(String name, ActionListener l) {
        final JMenuItem item = new JMenuItem( name );
        item.addActionListener( l );
        return item;
    }

    private void error(String message) {
        error(message, null);
    }

    private void error(String message, Throwable t) {

        JOptionPane.showConfirmDialog( null, t == null ? message : message + " ("+t.getMessage()+")", "Error",
            JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE );
    }

    public Main(Configuration configuration)
    {
        Validate.notNull( configuration, "configuration must not be null" );

        this.configuration = configuration;
        {
            final Project project = new Project( "example", null );
            mainPanel = new MainWindowPanel( project);
            mainPanel.setPreferredSize( new Dimension( 800, 600 ) );
        }

        setupMenu( configuration );

        getContentPane().setLayout( new GridBagLayout() );
        GridBagConstraints cnstrs = new GridBagConstraints();
        cnstrs.fill = GridBagConstraints.BOTH;
        cnstrs.weightx = 1;
        cnstrs.weighty = 1;
        cnstrs.gridx = 0;
        cnstrs.gridy = 0;
        cnstrs.gridwidth = 1;
        cnstrs.gridheight = 1;
        getContentPane().add( mainPanel, cnstrs);
        setDefaultCloseOperation( JFrame.DO_NOTHING_ON_CLOSE );
        setLocationRelativeTo( null );
        pack();
        setVisible( true );

        addWindowListener( new WindowAdapter()
        {
            @Override
            public void windowClosing(WindowEvent e)
            {
                quit();
            }
        });
    }

    private void refreshRecentFilesMenu() {
        recentFiles.removeAll();
        configuration.getRecentFiles().forEach( file -> recentFiles.add( menuItem( file.getName(), ev -> loadProject( file ) ) ) );
    }

    private void setupMenu(Configuration configuration)
    {
        JMenuBar bar = new JMenuBar();

        final JMenu menu = new JMenu( "File" );
        bar.add( menu );

        // load
        menu.add( menuItem("Load...", ev -> loadProject() ) );

        recentFiles = new JMenu("Recent files");
        configuration.getRecentFiles().forEach( file -> {
            menu.add( recentFiles );
            refreshRecentFilesMenu();
        } );
        menu.add( menuItem("Copy to clipboard", ev -> {
            copyToClipboard( mainPanel.getProject().toDataString() );
        } ));
        menu.add( menuItem("Save as...", ev -> saveAs() ));
        menu.add( menuItem("Save", ev -> save() ) );

        final JMenu animationSpeed = new JMenu("Animation Speed");
        bar.add( animationSpeed );

        animationSpeed.add( menuItem("60 FPS", ev -> getProject().setAnimationSpeedMillis( 16 ) ));
        animationSpeed.add( menuItem("30 FPS", ev -> getProject().setAnimationSpeedMillis( 32 ) ));
        animationSpeed.add( menuItem("15 FPS", ev -> getProject().setAnimationSpeedMillis( 48 ) ));

        menu.add( menuItem("Quit", ev -> {
            quit();
        }));
        setJMenuBar( bar );
    }

    private void quit() {
        if ( mainPanel.getProject().isDirty() ) {
            final int result =
                JOptionPane.showConfirmDialog( null, "Project is dirty - save?",
                    "Project dirty", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE );
            if ( result == JOptionPane.YES_OPTION ) {
                if ( mainPanel.getProject().getFile() == null ) {
                    saveAs();
                } else {
                    save();
                }
                System.out.println("Project saved.");
            }
        }
        try
        {
            configuration.save();
        } catch(IOException e) {
            e.printStackTrace();
        }
        System.exit(0);
    }

    private void loadProject()
    {
        final JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode( JFileChooser.FILES_ONLY);
        if ( ! configuration.getRecentFiles().isEmpty() ) {
            chooser.setSelectedFile( configuration.getRecentFiles().getFirst() );
        }
        int result = chooser.showOpenDialog( null );
        if ( result == JFileChooser.APPROVE_OPTION )
        {
            loadProject( chooser.getSelectedFile() );
        }
    }

    private void loadProject(File file) {
        try
        {
            final Project p = Project.load( file );
            mainPanel.setProject( p );
            setTitle( p.getName()+" - "+p.getFile().getAbsolutePath() );
            configuration.addRecentFile( file );
            refreshRecentFilesMenu();
        }
        catch( IOException e )
        {
            error( "Failed to load " + file, e );
        }
    }

    private void save()
    {
        if ( mainPanel.getProject().getFile() != null ) {
            save( mainPanel.getProject().getFile() );
        }
    }

    private void saveAs()
    {
        final JFileChooser chooser = new JFileChooser();
        chooser.setApproveButtonText( "Save" );
        chooser.setFileSelectionMode( JFileChooser.FILES_ONLY);
        if ( mainPanel.getProject().getFile() != null ) {
            chooser.setSelectedFile( mainPanel.getProject().getFile() );
        }
        int result = chooser.showOpenDialog( null );
        if ( result == JFileChooser.APPROVE_OPTION )
        {
            final File file = chooser.getSelectedFile();
            save( file );
        }
    }

    private void save(File file)
    {
        final Project p = mainPanel.getProject();
        try
        {
            p.setFile( file );
            p.save();
            configuration.addRecentFile( p.getFile() );
            setTitle( p.getName()+" - "+p.getFile().getAbsolutePath() );
        }
        catch( IOException e )
        {
            error("Failed to save "+ p.getFile(), e);
        }
    }

    private void copyToClipboard(String content) {
        final Clipboard clip = Toolkit.getDefaultToolkit().getSystemClipboard();
        final StringSelection strse1 = new StringSelection(content);
        clip.setContents(strse1, strse1);
    }

    public static void main(String[] args) throws InterruptedException, InvocationTargetException
    {
        SwingUtilities.invokeAndWait( () -> {
            try
            {
                new Main(Configuration.load());
            }
            catch( IOException e )
            {
                throw new RuntimeException( e );
            }
        } );
    }

    private Project getProject() {
        return mainPanel.getProject();
    }
}