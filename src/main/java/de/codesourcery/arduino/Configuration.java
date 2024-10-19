package de.codesourcery.arduino;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;
import org.apache.commons.lang3.Validate;

public class Configuration
{
    private List<File> recentFiles = new ArrayList<>();

    private Configuration() {
    }

    private Configuration(File recentFile)
    {
        this.recentFiles.add( recentFile );
    }

    public List<File> getRecentFiles()
    {
        return recentFiles;
    }

    public void addRecentFile(File recentFile)
    {
        Validate.notNull( recentFile, "recentFile must not be null" );
        this.recentFiles.remove( recentFile );
        this.recentFiles.addFirst( recentFile );
        if ( this.recentFiles.size() > 6 ) {
            this.recentFiles.removeLast();
        }
    }

    private static File file() {
        final File homeDir = new File( System.getProperty( "user.home" ) );
        return new File( homeDir, ".spriteEdit" );
    }

    public void save() throws IOException {

        final Properties props = new Properties();
        if ( ! recentFiles.isEmpty() )
        {
            final String s = recentFiles.stream().map( File::getAbsolutePath ).collect( Collectors.joining( "," ) );
            props.setProperty( "recentFiles", s );
        }
        try ( FileOutputStream out = new FileOutputStream( file() ) ) {
            props.store( out, "Automatically generated, do not edit." );
        }
    }

    public static Configuration load() throws IOException
    {
        final Configuration result = new Configuration();

        if ( file().exists() ) {
            final Properties props = new Properties();
            try ( FileInputStream in = new FileInputStream( file() ) ) {
                props.load( in );
            }
            String recentFiles = props.getProperty( "recentFiles");
            if ( recentFiles != null ) {
                final List<String> files = Arrays.stream( recentFiles.split( "," ) )
                    .collect( Collectors.toCollection( ArrayList::new ) );
                Collections.reverse(files);
                for ( final String file : files )
                {
                    final File f = new File( file );
                    if ( f.exists() && f.isFile() && f.canRead() )
                    {
                        result.addRecentFile( f );
                    }
                }
            }
        }
        return result;
    }
}
