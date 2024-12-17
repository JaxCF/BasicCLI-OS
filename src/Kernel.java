/**
 * Project				BasicCLI
 * File					Kernel.java
 * Authors				Jaxen Fullerton, CSSE Dept. at UWB
 * Description			
 */
import java.util.*;
import java.lang.reflect.*;
import java.io.*;

public class Kernel
{
    // Interrupt requests
    public final static int INTERRUPT_SOFTWARE = 1;  // System calls
    public final static int INTERRUPT_DISK     = 2;  // Disk interrupts
    public final static int INTERRUPT_IO       = 3;  // Other I/O interrupts

    // System calls
    public final static int BOOT    =  0; // SysLib.boot( )
    public final static int EXEC    =  1; // SysLib.exec(String args[])
    public final static int WAIT    =  2; // SysLib.join( )
    public final static int EXIT    =  3; // SysLib.exit( )
    public final static int SLEEP   =  4; // SysLib.sleep(int milliseconds)
    public final static int RAWREAD =  5; // SysLib.rawread(int blk, byte b[])
    public final static int RAWWRITE=  6; // SysLib.rawwrite(int blk, byte b[])
    public final static int SYNC    =  7; // SysLib.sync( )
    public final static int READ    =  8; // SysLib.cin( )
    public final static int WRITE   =  9; // SysLib.cout( ) and SysLib.cerr( )

    public final static int CREAD   = 10; // SysLib.cread(int blk, byte b[])
    public final static int CWRITE  = 11; // SysLib.cwrite(int blk, byte b[])
    public final static int CSYNC   = 12; // SysLib.csync( )
    public final static int CFLUSH  = 13; // SysLib.cflush( )

    public final static int OPEN    = 14; // SysLib.open( String fileName )
    public final static int CLOSE   = 15; // SysLib.close( int fd )
    public final static int SIZE    = 16; // SysLib.size( int fd )
    public final static int SEEK    = 17; // SysLib.seek( int fd, int offest, 
                                          //              int whence )
    public final static int FORMAT  = 18; // SysLib.format( int files )
    public final static int DELETE  = 19; // SysLib.delete( String fileName )

    public final static int STDIN  = 0;
    public final static int STDOUT = 1;
    public final static int STDERR = 2;

    // Return values
    public final static int OK = 0;
    public final static int ERROR = -1;

    // System thread references
    private static Scheduler scheduler;
    private static Disk disk;
    private static Cache cache;

    // Synchronized Queues
    private static SyncQueue waitQueue;  // for threads to wait for their child
    private static SyncQueue ioQueue;    // I/O queue

    private final static int COND_DISK_REQ = 1; // wait condition 
    private final static int COND_DISK_FIN = 2; // wait condition

    // Standard input
    private static BufferedReader input
	= new BufferedReader( new InputStreamReader( System.in ) );

    // The heart of Kernel
    public static int interrupt( int irq, int cmd, int param, Object args )
	{
		TCB myTcb;

		switch( irq )
		{
		case INTERRUPT_SOFTWARE: // System calls
			switch( cmd )
			{ 
			case BOOT:

				// SCHEDULER
				scheduler = new Scheduler( ); 
				scheduler.start( );
				
				// DISK
				disk = new Disk( 1000 );
				disk.start( );

				// CACHE
				cache = new Cache( disk.blockSize, 10 );

				// QUEUES
				ioQueue = new SyncQueue( );
				waitQueue = new SyncQueue( scheduler.getMaxThreads( ) );
				return OK;

			case EXEC:
				return sysExec( ( String[] )args );

			case WAIT:
				myTcb = scheduler.getMyTcb();
				int ctid = waitQueue.enqueueAndSleep( myTcb.getTid() );
				if ( ctid >= 0 )
					return ctid;
				else
					return ERROR;

			case EXIT:
				myTcb = scheduler.getMyTcb();
				int ptid = myTcb.getPid();
				int tid = myTcb.getTid();

				waitQueue.dequeueAndWakeup( ptid, tid );

				if ( scheduler.deleteThread() )
					return OK;
				else
					return ERROR;

			case SLEEP:
				scheduler.sleepThread( param );
				return OK;

			case RAWREAD:
				while ( disk.read( param, ( byte[] )args ) == false )
					; // busy wait
				while ( disk.testAndResetReady( ) == false )
					; // busy wait
				return OK;

			case RAWWRITE:
				while ( disk.write( param, ( byte[] )args ) == false )
					; // busy wait
				while ( disk.testAndResetReady( ) == false )
					; // busy wait
				return OK;

			case SYNC:
				while ( disk.sync( ) == false )
					; // busy wait
				while ( disk.testAndResetReady( ) == false )
					; // busy wait
				return OK;

			case READ:
				switch ( param ) {

					case STDIN:
						try {
							String s = input.readLine();
							if ( s == null ) {
								return ERROR;
							}
							StringBuffer buf = ( StringBuffer )args;

							buf.append( s ); 

							return s.length( );
						} catch ( IOException e ) {
							System.out.println( e );
							return ERROR;
						}

					case STDOUT:

					case STDERR:
						System.out.println( "threaOS: caused read errors" );
						return ERROR;
				}
				// return FileSystem.read( param, byte args[] );
				return ERROR;

			case WRITE:
				switch ( param ) {
					case STDIN:
						System.out.println( "threaOS: cannot write to System.in" );
						return ERROR;

					case STDOUT:
						System.out.print( (String)args );
						break;

					case STDERR:
						System.err.print( (String)args );
						break;
				}
				return OK;

			case CREAD: 
				return cache.read( param, ( byte[] )args ) ? OK : ERROR;

			case CWRITE:
				return cache.write( param, ( byte[] )args ) ? OK : ERROR;

			case CSYNC: 
				cache.sync( );
				return OK;

			case CFLUSH:
				cache.flush( );
				return OK;

			case OPEN:
				return OK;
			case CLOSE: 
				return OK;
			case SIZE:
				return OK;
			case SEEK:
				return OK;
			case FORMAT:
				return OK;
			case DELETE:
				return OK;
			}
		return ERROR;

		case INTERRUPT_DISK: // Disk interrupts
			// wake up the thread waiting for a service completion
			//ioQueue.dequeueAndWakeup( COND_DISK_FIN );

			// wake up the thread waiting for a request acceptance
			//ioQueue.dequeueAndWakeup( COND_DISK_REQ );

			return OK;

		case INTERRUPT_IO:   // other I/O interrupts (not implemented)
			return OK;
		}
		return OK;
    }

    // Spawning a new thread
    private static int sysExec( String args[] ) {
		String thrName = args[0]; // args[0] has a thread name
		Object thrObj = null;

		try {
			Class thrClass = Class.forName( thrName ); 
			if ( args.length == 1 )
			thrObj = thrClass.newInstance( );
			else {            
			String thrArgs[] = new String[ args.length - 1 ];
			for ( int i = 1; i < args.length; i++ )
				thrArgs[i - 1] = args[i];
			Object[] constructorArgs = new Object[] { thrArgs };

			Constructor thrConst 
				= thrClass.getConstructor( new Class[] {String[].class} );

			thrObj = thrConst.newInstance( constructorArgs );
			}
			Thread t = new Thread( (Runnable)thrObj );

			TCB newTcb = scheduler.addThread( t );
			return ( newTcb != null ) ? newTcb.getTid( ) : ERROR;
		}
		catch ( ClassNotFoundException e ) {
			System.out.println( e );
			return ERROR;
		}
		catch ( NoSuchMethodException e ) {
			System.out.println( e );
			return ERROR;
		}
		catch ( InstantiationException e ) {
			System.out.println( e );
			return ERROR;
		}
		catch ( IllegalAccessException e ) {
			System.out.println( e );
			return ERROR;
		}
		catch ( InvocationTargetException e ) {
			System.out.println( e );
			return ERROR;
		}
    }
}
