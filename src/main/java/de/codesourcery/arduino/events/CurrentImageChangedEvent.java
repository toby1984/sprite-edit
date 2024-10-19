package de.codesourcery.arduino.events;

import de.codesourcery.arduino.Image;
import de.codesourcery.arduino.Project;

public final class CurrentImageChangedEvent extends AbstractEvent
{
    public final Project project;
    public final Image newImage;

    public CurrentImageChangedEvent(Object sender, Project project, Image newImage)
    {
        super(sender);
        this.project = project;
        this.newImage = newImage;
    }
}
