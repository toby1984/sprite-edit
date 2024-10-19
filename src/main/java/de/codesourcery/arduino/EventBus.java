package de.codesourcery.arduino;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.Validate;
import de.codesourcery.arduino.events.IEvent;

public class EventBus
{

    private static final List<IEventListener> listeners = new ArrayList<>();

    public static void register(IEventListener listener) {
        Validate.notNull( listener, "listener must not be null" );
        listeners.add(listener);
    }

    public static void send(IEvent event)
    {
        Validate.notNull( event, "event must not be null" );
        listeners.forEach( l->l.handle( event ) );
    }
}
