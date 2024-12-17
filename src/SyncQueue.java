/**
 * Project				BasicCLI
 * File					SyncQueue.java
 * Authors				Jaxen Fullerton, CSSE Dept. at UWB
 * Description			Implements synchronization and threading mechanics
 */
public class SyncQueue
{
    
	private QueueNode queue[] = null;
    private final int COND_MAX = 10;
    private final int NO_TID = -1;

    private void initQueue( int condMax )
    {
		queue = new QueueNode[ condMax ];
		for ( int i = 0; i < condMax; i++ )
			queue[i] = new QueueNode( );
	}

    public SyncQueue( )
    {
        initQueue( COND_MAX );
    }

    public SyncQueue( int condMax )
    {
        initQueue( condMax );
    }

    int enqueueAndSleep( int condition )
    {
        if ( condition < 0 || condition > COND_MAX ) return -1;

        return queue[condition].sleep();
    }

    void dequeueAndWakeup( int condition, int tid )
    {
        if ( condition < 0 || condition > COND_MAX ) return;

        queue[condition].wakeup( tid );
    }

    void dequeueAndWakeup( int condition )
    {
        if ( condition < 0 ) return;

        queue[condition].wakeup( 0 );
    }
}
