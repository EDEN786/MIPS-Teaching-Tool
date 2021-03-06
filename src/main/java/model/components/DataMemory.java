package model.components;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import util.Convert;
import util.Util;
import util.ansi_codes.Color;
import util.logs.ErrorLog;
import util.logs.ExecutionLog;
import util.validation.AddressValidation;

import java.util.HashMap;

/**
 Wrapper for float[] data.
 <p>
 Index's are double-word aligned (multiple of 8).
 <p>
 Describes actions being performed in the DataLog
 <p>
 use {@link <b><i>AddressValidation#isValidDataAddr(int, ErrorLog)</i></b>}
 
 @see AddressValidation#dataAddr2index(int, ErrorLog)
 @see #BASE_DATA_ADDRESS
 @see #OVER_SUPPORTED_DATA_ADDRESS */
public class DataMemory {
	public static final int DATA_ALIGN=8;
	public static final int MAX_DATA_ITEMS=256;
	public static final int BASE_DATA_ADDRESS=0x10010000;
	public static final int OVER_SUPPORTED_DATA_ADDRESS=BASE_DATA_ADDRESS + (MAX_DATA_ITEMS)*DATA_ALIGN;
	public static final int OVER_DATA_ADDRESS=0x10040000;
	private final String NAME= "\t"+ Color.fmtSubTitle( Color.MAGENTA, "DataMemory");
	private final HashMap<Integer, Double> data;
	private final ExecutionLog executionLog;
	
	public DataMemory(@NotNull HashMap<Integer, Double> data, @NotNull ExecutionLog executionLog) throws IllegalArgumentException{
		if ( data.size( )>MAX_DATA_ITEMS )
			throw new IllegalArgumentException( "Data Memory cannot have move than " + MAX_DATA_ITEMS + " indexes" );
		
		data.forEach((k,v) ->{
			if( !Util.notNullAndInRange( k, 0, MAX_DATA_ITEMS - 1) )
				throw new IllegalArgumentException( "Data Contains a Key["+k+"] Out Of Range[0->"+(MAX_DATA_ITEMS-1)+"]!" );
		} );// Check all the Keys are InRange
		
		this.data=data;
		this.executionLog=executionLog;
	}
	
	/**
	 Given a valid Data Address, Returns the Data object for that address.
	 <p>
	 if data has not been set or null input, returns 0 as default value
	 
	 @throws IndexOutOfBoundsException for non-supported address.
	 @see AddressValidation#isSupportedDataAddr(int, ErrorLog)
	 @see #BASE_DATA_ADDRESS
	 @see #OVER_SUPPORTED_DATA_ADDRESS
	 */
	public int readData(@Nullable Integer address) throws IndexOutOfBoundsException, IllegalArgumentException {
		int val=0;
		if ( address==null ) {
			noAction( );
		} else if ( inRange( address ) ) {
			int index=toIndex( address );
			if ( data.containsKey( index ) )
				val=this.data.get( index ).intValue( );
			
			this.executionLog.append( NAME + ":\t" + "Reading Value[" + colorize( ""+val,false) + "]\tFrom Memory Address["
									  + fmtMem( address, false ) + "]!" );
		}
		return val;
	}
	
	//Explicit instruction to do nothing,
	public void noAction() {
		this.executionLog.append( NAME + ":\t" + "No Action!" );
	}
	
	private boolean inRange(int address) throws IndexOutOfBoundsException{
		if ( address>=BASE_DATA_ADDRESS && address<=(OVER_SUPPORTED_DATA_ADDRESS - DATA_ALIGN) )
			return true;
		else
			throw new IndexOutOfBoundsException( "Data Address ["+Convert.int2Hex(address)+", "+address+"] Must Be >="
												 + Convert.int2Hex( BASE_DATA_ADDRESS ) + " and <="
												 + Convert.int2Hex( OVER_SUPPORTED_DATA_ADDRESS - DATA_ALIGN ) + "!" );
	}
	
	private int toIndex(int address) throws IllegalArgumentException{
		if ( address%DATA_ALIGN!=0 && inRange( address ) )
			throw new IllegalArgumentException( "Data Address ["+Convert.int2Hex(address)
												+", "+address+"] Must Be DoubleWord Aligned!" );
		return Convert.dataAddr2Index( address )/2;
	}
	
	private String fmtMem(int address, boolean write) {
		return colorize( Convert.int2Hex( address ), write );
	}
	/** Depending on the status colorize the output */
	private String colorize (String text, boolean write){
		final String READ_COL=Color.READ;
		final String WRITE_COL=Color.WRITE;
		
		if ( write )
			return Color.fmt( WRITE_COL, text);
		else
			return Color.fmt( READ_COL, text );
	}
	
	/**
	 Given a valid Data Address, Sets the Data object for that address.
	 <p> <b>Use {@link AddressValidation#isSupportedDataAddr(int, ErrorLog)} to check input</b>
	 <p>
	 If either input is <i>null</i>, No action is performed.
	 
	 @throws IndexOutOfBoundsException for non-supported address.
	 @see AddressValidation#isSupportedDataAddr(int, ErrorLog)
	 @see #BASE_DATA_ADDRESS
	 @see #OVER_SUPPORTED_DATA_ADDRESS
	 */
	public boolean writeData(@Nullable Integer address, @Nullable Integer data) throws IndexOutOfBoundsException, IllegalArgumentException {
		if ( address==null || data==null ) {
			noAction( );
			return false;
		} else if ( inRange( address ) ) {
			this.data.put( toIndex( address ), data.doubleValue( ) );
			this.executionLog.append( NAME + ":\t" + "Writing Value[" +colorize( ""+data,true)  + "]\tTo Memory Address[" + fmtMem( address, true ) + "]!" );
		}
		return true;
	}
}
