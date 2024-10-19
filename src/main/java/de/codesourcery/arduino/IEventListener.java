package de.codesourcery.arduino;

import de.codesourcery.arduino.events.IEvent;

@FunctionalInterface
public interface IEventListener
{
    void handle(IEvent event);
}
