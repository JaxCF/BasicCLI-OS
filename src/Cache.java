/**
 * Project				BasicCLI
 * File					Cache.java
 * Authors				Jaxen Fullerton, CSSE Dept. at UWB
 * Description			Implements a caching scheme to be utilized
 * 						by the OS, with an implementation of the 
 * 						second-chance algorithm for page replacement.
 */
import java.util.*;

public class Cache {
    private int blockSize;            // 512 bytes
    private Vector<byte[]> pages;
    private int victim;

    private class Entry {
		public static final int INVALID = -1;
		public boolean reference;
		public boolean dirty;
		public int frame;
		public Entry( ) {
			reference = false;
			dirty = false;
			frame = INVALID;
		}
    }

    private Entry[] pageTable = null;

    private int findFreePage( )
	{
		if ( pageTable == null ) return -1;

		for ( int i = 0; i < pageTable.length; i++ )
		{
			if ( pageTable[i].frame == Entry.INVALID )
			{
				return i;
			}
		}

		return -1;
    }

    private int nextVictim( )
	{
		if ( pageTable == null ) return -1;
		int i = 0;

		while ( true )
		{
			if ( pageTable[i] == null || pageTable[i].reference == false )
			{
				victim = i;
				return i;
			}
			else pageTable[i].reference = false;

			i = ( i + 1 ) % pageTable.length;
		}
    }

    private void writeBack( int victimEntry )
	{
		if ( pageTable[victimEntry] != null && pageTable[victimEntry].frame >= 0 &&
			 pageTable[victimEntry].dirty )
		{
			SysLib.rawwrite( pageTable[victimEntry].frame, pages.get( victimEntry ) );

			pageTable[victimEntry].dirty = false;
		}
    }

    public Cache( int blockSize, int cacheBlocks ) {
		this.blockSize = blockSize;
		pages = new Vector<byte[]>( );
		for ( int i = 0; i < cacheBlocks; i++ ) {
			byte[] p = new byte[blockSize];
			pages.addElement( p );
		}
		victim = cacheBlocks - 1;
		pageTable = new Entry[ cacheBlocks ];
		for ( int i = 0; i < cacheBlocks; i++ )
			pageTable[i] = new Entry( );
	}

	public synchronized boolean read( int blockId, byte buffer[] )
	{
		if ( blockId < 0 )
		{
			SysLib.cerr( "threadOS: a wrong blockId for cread\n" );
			return false;
		}

		for ( int i = 0; i < pageTable.length; i++ )
		{
			if ( pageTable[i] != null && pageTable[i].frame == blockId )
			{
				System.arraycopy( pages.get( i ), 0, buffer, 0, blockSize );
				pageTable[i].reference = true;

				return true;
			}
		}
		int victimEntry = -1;
		
		victimEntry = findFreePage();
		if ( victimEntry < 0 ) victimEntry = nextVictim();
		if ( victimEntry < 0 ) return false;

		writeBack( victimEntry );

		SysLib.rawread( blockId, buffer );

		// System.arraycopy( buffer, 0, pages.get( victimEntry ), 0, blockSize );
		byte[] dest = pages.get( victimEntry );
		for ( int i = 0; i < blockSize; i++ )
		{
			dest[i] = buffer[i];
		}
		
		pageTable[victimEntry].frame = blockId;
		pageTable[victimEntry].reference = true;
		pageTable[victimEntry].dirty = false;

		return true;
	}

    public synchronized boolean write( int blockId, byte buffer[] ) {
		if ( blockId < 0 ) {
			SysLib.cerr( "threadOS: a wrong blockId for cwrite\n" );
			return false;
		}

		for ( int i = 0; i < pageTable.length; i++ )
		{
			if ( pageTable[i] != null && pageTable[i].frame == blockId )
			{
				System.arraycopy( pages.get( i ), 0, buffer, 0, blockSize );
				pageTable[i].reference = true;
				pageTable[i].dirty = true;

				return true;
			}
		}
		int victimEntry = -1;

		victimEntry = findFreePage();
		if ( victimEntry < 0 ) victimEntry = nextVictim();
		if ( victimEntry < 0 ) return false;

		writeBack(victimEntry);

		// System.arraycopy( buffer, 0, pages.get( victimEntry ), 0, blockSize );
		byte[] dest = pages.get( victimEntry );
		for ( int i = 0; i < blockSize; i++ )
		{
			dest[i] = buffer[i];
		}
		
		pageTable[victimEntry].frame = blockId;
		pageTable[victimEntry].reference = true;
		pageTable[victimEntry].dirty = true;

		return true;
	}

	public synchronized void sync( ) {
		for ( int i = 0; i < pageTable.length; i++ )
			writeBack( i );
		SysLib.sync( );
	}

	public synchronized void flush( ) {
		for ( int i = 0; i < pageTable.length; i++ ) {
			writeBack( i );
			pageTable[i].reference = false;
			pageTable[i].frame = Entry.INVALID;
		}
		SysLib.sync( );
    }
}
