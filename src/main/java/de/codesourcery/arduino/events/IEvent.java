package de.codesourcery.arduino.events;

public sealed interface IEvent permits AbstractEvent
{
    Object getSender();
}
