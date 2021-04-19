package model;

import org.jetbrains.annotations.NotNull;

import model.components.Component;
import model.components.DataMemory;
import model.components.RegisterBank;

import util.logs.ExecutionLog;
import util.validation.OperandsValidation;

import java.util.List;

public class I_Type extends Instruction {
	
	/**{@link util.validation.OperandsValidation#I_TYPE}*/
	protected I_Type (@NotNull List<String> codes, @NotNull String ins, int RS, int RT, int IMM) throws IllegalArgumentException{
		super( Type.IMMEDIATE, codes, ins, RS, RT, 0, IMM, null );
	}
	
	/**{@link util.validation.OperandsValidation#I_TYPE}, Label needs to be assembled into IMM value*/
	protected I_Type (@NotNull List<String> codes, @NotNull String ins, int RS, int RT, @NotNull String label) throws IllegalArgumentException{
		super( Type.IMMEDIATE, codes, ins, RS, RT, 0, null, label );
		if ( RS!=0 )
			throw new IllegalArgumentException("RS:["+RS+"], must be 0, when IMM is null");
	}
	
	/**{@link util.validation.OperandsValidation#I_TYPE_RT_RS_IMM}*/
	public I_Type (@NotNull String ins, int RS, int RT, int IMM) throws IllegalArgumentException{
		this( OperandsValidation.I_TYPE_RT_RS_IMM, ins, RS, RT, IMM );
		if (!OperandsValidation.notNullAndInRange(IMM, -32768, 32767))//Signed 16Bit
			throw new IllegalArgumentException("Immediate["+IMM+"] Not In Range!");
	}
	
	@Override
	protected void action(@NotNull DataMemory dataMem, @NotNull RegisterBank regBank,
						  @NotNull ExecutionLog executionLog)
			throws IndexOutOfBoundsException, IllegalArgumentException{
		if ( opcode.equals( "addi" ) ){
			int rsVal=regBank.read( RS );
			
			executionLog.append( "[IMMEDIATE: " + IMM + "]" );
			executionLog.append( "\tCalculating Result:" );
			int rtVal=Component.ALU( rsVal, IMM, "add", executionLog);
			dataMem.noAction( );
			regBank.write( RT, rtVal );
		} else
			throw new IllegalStateException( "Instruction [" + opcode + "] Not Implemented");
	}
	
}
