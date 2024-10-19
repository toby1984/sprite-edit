package de.codesourcery.arduino.events;

import org.apache.commons.lang3.Validate;

public abstract sealed class AbstractEvent implements IEvent permits CurrentImageChangedEvent
{
    final Object sender;

    public AbstractEvent(Object sender)
    {
        Validate.notNull( sender, "sender must not be null" );
        this.sender = sender;
    }

    @Override
    public final Object getSender()
    {
        return sender;
    }
}
