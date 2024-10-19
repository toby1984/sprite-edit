package de.codesourcery.arduino;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

public class Project
{
    private File file;
    private List<Image> images = new ArrayList<>();
    private String name;
    private boolean isDirty;
    private int animationSpeedMillis = 16;

    public Project(String name, File file) {
        this( name, file, new ArrayList<>( List.of( new Image() ) ) );
    }

    public Project(String name, File file, List<Image> images)
    {
        this.file = file;
        this.name = name;
        this.images = images;
    }

    public boolean isDirty() {
        return isDirty || images.stream().anyMatch( Image::isDirty );
    }

    public void add(Image image) {
        add( images.size(), image );
    }

    public void add(int idx, Image image) {
        images.add(idx, image);
        isDirty = true;
    }

    public Image getFirstImage() {
        return images.getFirst();
    }

    public void delete(Image image) {
        images.remove(image);
        if ( images.isEmpty() ) {
            images.add( new Image() );
        }
        isDirty = true;
    }

    public void setName(String name)
    {
        Validate.notBlank( name, "name must not be null or blank");
        this.name = name;
    }

    public void save() throws IOException
    {
        final Properties props = new Properties();
        props.setProperty( "name", name );
        for ( int i = 0; i < images.size(); i++ )
        {
            final Image image = images.get( i );
            props.setProperty( "image." + i, image.toDataString() );
        }
        props.setProperty( "animationSpeed", Integer.toString( animationSpeedMillis ) );

        try ( FileWriter writer = new FileWriter( file ) )
        {
            props.store( writer, "Automatically generated, do not alter." );
        }
        images.forEach( img -> img.setDirty( false ) );
        isDirty = false;
    }

    public static Project load(File file) throws IOException
    {
        Validate.notNull( file, "file must not be null" );

        final Properties props = new Properties();
        try( FileInputStream reader = new FileInputStream( file ) )
        {
            props.load( reader );
        }
        final String name = props.getProperty( "name" );
        if ( StringUtils.isBlank(name) ) {
            throw new IOException( "Not a valid file" );
        }
        final List<Image> images = new ArrayList<>();
        final Project result = new Project( name, file, images );

        String speed = props.getProperty( "animationSpeed" );
        if ( StringUtils.isNotBlank( speed ) ) {
            result.setAnimationSpeedMillis( Integer.parseInt( speed ) );
        }
        int imgIndex = 0;
        while ( true ) {
            final String key = "image." + imgIndex;
            if ( ! props.containsKey( key ) )
            {
                break;
            }
            images.add( Image.fromDataString( props.getProperty( key ) ) );
            imgIndex++;
        }
        if ( result.getImages().isEmpty() ) {
            throw new IllegalStateException( "Project without images?" );
        }
        return result;
    }

    public String getName()
    {
        return name;
    }

    public File getFile()
    {
        return file;
    }

    public void setFile(File file)
    {
        Validate.notNull( file, "file must not be null" );
        this.file = file;
    }

    public String toDataString() {
        final String data = images.stream().map( x -> "{" + x.toDataString() + "}" ).collect( Collectors.joining( ",\n" ) );
        final int height = getFirstImage().getHeight();
        return """
    const uint8_t data[%d][%d] = {
        %s
    };
    """.formatted(images.size(), height, data);
    }

    public List<Image> getImages()
    {
        return images;
    }

    public Image getNextImage(Image current) {
        int nextIdx = images.indexOf(current)+1;
        if ( nextIdx < images.size() ) {
            return images.get( nextIdx );
        }
        return images.getFirst();
    }

    public Optional<Image> getPreviousImage(Image current) {
        int nextIdx = images.indexOf(current)-1;
        if ( nextIdx >= 0 ) {
            return Optional.ofNullable( images.get( nextIdx ) );
        }
        return Optional.empty();
    }

    public int getAnimationSpeedMillis()
    {
        return animationSpeedMillis;
    }

    public void setAnimationSpeedMillis(int animationSpeedMillis)
    {
        Validate.isTrue( animationSpeedMillis > 0 );
        this.animationSpeedMillis = animationSpeedMillis;
    }
}