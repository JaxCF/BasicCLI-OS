/**
 * Project				BasicCLI
 * File					QueueNode.java
 * Authors				Jaxen Fullerton, CSSE Dept. at UWB
 * Description			Provides a QueueNode class to be used with
 *                      the scheduler
 */
import java.util.*;

public class QueueNode
{
    private Vector<Integer> tidQueue;

    public QueueNode( )
    {
        tidQueue = new Vector<>();
    }

    public synchronized int sleep( )
    {
        if ( tidQueue.isEmpty() )
        {
            try
            { 
                wait();
            }
            catch ( InterruptedException e ) {}
        }
        int temp = tidQueue.lastElement();
        tidQueue.removeElementAt( tidQueue.size() - 1 );

        return temp;

    }

    public synchronized void wakeup( int tid )
    {
        tidQueue.add( tid );

        notify();
    }
}
