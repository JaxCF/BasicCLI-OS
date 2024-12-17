/**
 * Project				BasicCLI
 * File					Scheduler.java
 * Authors				Jaxen Fullerton, CSSE Dept. at UWB
 * Description			The actual scheduler class to be used by the OS.
 * 						Just copy the code from one of the 3 scheduler
 * 						sources
 */
import java.util.*; // Scheduler_mfq.java

public class Scheduler extends Thread
{   @SuppressWarnings({"unchecked","rawtypes"})
    private Vector<TCB>[] queue = new Vector[3];
    private int timeSlice;
    private static final int DEFAULT_TIME_SLICE = 1000;

    private boolean[] tids;
    private static final int DEFAULT_MAX_THREADS = 10000;

    private int nextId = 0;
    private void initTid( int maxThreads ) {
		tids = new boolean[maxThreads];
		for ( int i = 0; i < maxThreads; i++ )
			tids[i] = false;
    }

    private int getNewTid( ) {
		for ( int i = 0; i < tids.length; i++ ) {
			int tentative = ( nextId + i ) % tids.length;

			if ( tids[tentative] == false ) {
				tids[tentative] = true;
				nextId = ( tentative + 1 ) % tids.length;
				return tentative;
			}
		}

		return -1;
    }

    private boolean returnTid( int tid ) {
		if ( tid >= 0 && tid < tids.length && tids[tid] == true ) {
			tids[tid] = false;
			return true;
		}

		return false;
    }

    public TCB getMyTcb( ) {
		Thread myThread = Thread.currentThread( );

		synchronized( queue ) {
			for ( int level = 0; level < 3; level++ ) {
				for ( int i = 0; i < queue[level].size( ); i++ ) {
					TCB tcb=queue[level].elementAt( i );
					Thread thread = tcb.getThread( );
					if ( thread == myThread )
					return tcb;
				}
			}
		}

		return null;
    }

    public int getMaxThreads( ) {
		return tids.length;
    }

    public Scheduler( ) {
		timeSlice = DEFAULT_TIME_SLICE;
		initTid( DEFAULT_MAX_THREADS );

		for ( int i = 0; i < 3; i++ ) queue[i] = new Vector<TCB>( );
    }

    public Scheduler( int quantum ) {
		timeSlice = quantum;
		initTid( DEFAULT_MAX_THREADS );

		for ( int i = 0; i < 3; i++ ) queue[i] = new Vector<TCB>( );
    }

    public Scheduler( int quantum, int maxThreads ) {
		timeSlice = quantum;
		initTid( maxThreads );

		for ( int i = 0; i < 3; i++ ) queue[i] = new Vector<TCB>( );
    }

    private void schedulerSleep( ) {
		try {
			Thread.sleep( timeSlice / 2 );
		} catch ( InterruptedException e ) {}
    }

    public TCB addThread( Thread t ) {
		TCB parentTcb = getMyTcb( );
		int pid = ( parentTcb != null ) ? parentTcb.getTid( ) : -1;

		int tid = getNewTid( );
		if ( tid == -1) return null;

		TCB tcb = new TCB( t, tid, pid );
		queue[0].add( tcb );

		return tcb;
    }

    public boolean deleteThread( ) {
		TCB tcb = getMyTcb( ); 

		if ( tcb!= null ) {
			this.interrupt( );
			return tcb.setTerminated( );
		}
		else return false;
    }

    public void sleepThread( int milliseconds ) {
		try {
			sleep( milliseconds );
		} catch ( InterruptedException e ) {}
    }
    
    public void run( ) {
	Thread current = null;
	TCB currentTCB = null;
	TCB prevTCB = null;
	int slice[] = new int[3];
	
	for ( int i = 0; i < 3; i++ )
	    slice[i] = 0;

	while ( true ) {
	    try {
		int level = 0;
		for ( ; level < 3; level++ )
		{
			if ( slice[level] == 0 )
			{
				if ( queue[level].size( ) == 0 )
					continue;
				currentTCB = queue[level].firstElement( );
				break;
		    }
		    else
			{
				currentTCB = prevTCB;
				break;
		    }
		}

		if ( level == 3 )
		    continue;

		if ( currentTCB.getTerminated( ) == true )
		{
			queue[level].remove( currentTCB );
			returnTid( currentTCB.getTid( ) );

		    continue;
		}
		current = currentTCB.getThread( );

		if ( ( current != null ) )
		{
		    if ( current.isAlive( ) )
				current.resume();
		    else
				current.start( ); 
		}

		schedulerSleep( );

		synchronized( queue )
		{
		    if ( current != null && current.isAlive( ) )
			{
				current.suspend();
			
				slice[level]++;

				if ( level == 0 && slice[level] >= 1 )
					slice[level] = 0;
				if ( level == 1 && slice[level] >= 2 )
					slice[level] = 0;
				if ( level > 1 && slice[level] >= 4 )
					slice[level] = 0;

				if ( slice[level] == 0 )
				{
					queue[level].remove( currentTCB );

					if ( level < 2 )
					{
						queue[level + 1].add( currentTCB );
						slice[level + 1] = 0;
					}
					else 
					{
						queue[level].add( currentTCB );
					}
					slice[level] = 0;
				}
			}
			else
			{
				queue[level].remove( currentTCB );
				slice[level] = 0;
				continue;
			}


		}
		prevTCB = currentTCB;

	    } catch ( NullPointerException e3 ) { };
	}
    }
}
