package util.validation;

import _test.Tags;
import _test.Tags.Pkg;
import _test.TestLogs;
import _test.TestLogs.FMT_MSG;
import _test.providers.*;
import _test.providers.InstrProvider.I;
import _test.providers.InstrProvider.J;
import _test.providers.InstrProvider.NO_OPS;
import _test.providers.InstrProvider.RD_RS_RT;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.junit.jupiter.params.provider.ValueSource;

import model.DataType;
import model.instr.Branch;
import model.instr.Instruction;
import model.instr.Instruction.Type;
import model.instr.J_Type;
import model.instr.Nop;

import util.Convert;
import util.logs.ErrorLog;

import java.util.Arrays;
import java.util.HashMap;

import static _test.TestLogs.FMT_MSG._opsForOpcodeNotValid;
import static org.junit.jupiter.api.Assertions.*;

/**
 Testing a New Instruction
 <ol>
 <li> - add it's configuration to {@link InstrProvider} </li>
 <li> - Invalid_SetOperands  </li>
 <li> - Add to appropriate Instruction_Type under {@link Split_Valid_Instruction} </li>
 <li> - See the Layout Instructions for {@link Split_Valid_Instruction} </li>
 </ol>
 */
@Tag ( Pkg.UTIL )
@Tag ( Pkg.VALID )
@Tag ( Tags.INSTR )
@DisplayName ( Pkg.UTIL + " : " + Tags.INSTR + " : " + Pkg.VALID + " Test" )
public class InstructionValidationTest {
	
	private static final TestLogs testLogs=new TestLogs( );
	private static ErrorLog expectedErrs;
	private static InstructionValidation ValidateInstr;
	
	@BeforeAll
	static void beforeAll ( ) {
		expectedErrs=testLogs.expectedErrors;
		ValidateInstr=new InstructionValidation( testLogs.actualErrors, testLogs.actualWarnings );
	}
	
	@AfterEach
	void clear ( ) { testLogs.after( ); }
	
	@Nested	// Possibly Move to ValidateTest,
	@DisplayName ( "isValidOpcode : Validate Instruction" )
	class Validate_Opcode {
		
		@ParameterizedTest ( name="[{index}] Valid Opcode[{0}]" )
		@ArgumentsSource ( InstrProvider.class )
		void isValidOpcode_Valid (String opcode) {
			assertTrue( ValidateInstr.isValidOpCode( 1, opcode ) );
		}
		
		@ParameterizedTest ( name="[{index}] Not Valid - Opcode[{0}]" )
		@ArgumentsSource ( InstrProvider.Invalid.class )
		@ArgumentsSource ( BlankProvider.class )
		void isValidOpcode_Not_Valid (String opcode) {
			assertFalse( ValidateInstr.isValidOpCode( 230, opcode ) );
			expectedErrs.appendEx( 230, FMT_MSG.Opcode_NotSupported( opcode ) );
		}
		
	}
	
	/**
	 <ul> <li> Type </li>
	 <ul> <li> Register Formation/NumberOf </li>
	 <ul><li> Valid -> Operands Split Successfully, to Create an Instruction. [ins then needs to be assembled]
	 <ul> Assembles -> Checking The Result or trying to Assemble ({@link Instruction#assemble(ErrorLog, HashMap, int)})
	 <ul><li> FailAssemble.</li>
	 <li> ThrowsAssemble.</li></ul>
	 </ul></li>
	 <li> Invalid_Operands -> Invalid Formatting (with correct #of operands)
	 <ul>Should Also Test If It Reads From $Zero, Produces a Warning </ul>
	 </li>
	 <li>Additional Methods Closely Related/ This can also be put into {@link testSubModules} </li>
	 </ul>
	 </ul>
	 </ul>
	 */
	@Nested
	@DisplayName ( "splitValidInstruction : Validate Instruction" )
	class Split_Valid_Instruction {
		
		private final String FA=" -> Fail!Assemble";
		private final String A=" -> Assembles!";
		private final HashMap<String, Integer> LABELS_MAP=InstrProvider.labelsMap;
		private final Expect expect=new Expect( );
		
		// Wrapper / Helper Class for Expected Errors/Warnings .. since there was so many
		private class Expect {
			private final ErrorLog errLog=testLogs.actualErrors;
			
			/** {@link InstructionValidation#splitValidOperands(int, String, String)} */
			private void invalidOperandsForOpcode (int LineNo, String opcode, String operands) {
				assertNull( ValidateInstr.splitValidOperands( LineNo, opcode, operands ) );
				expectedErrs.append( LineNo, _opsForOpcodeNotValid( opcode, operands ) );
			}
			
			/** Use Default LineNo of "-1", use {@link InstructionValidation#setLineNo(int)} before this! */
			private void invalidOperandsForOpcode (String opcode, String operands) { invalidOperandsForOpcode( -1, opcode, operands ); }
			
			/** Sets the LineNo to -1 automatically, then expects NO_OPS is an invalid operand for the opcode. */
			private void NoOps_NotValid_AndSetLineNo (String opcode) {
				ValidateInstr.setLineNo( -1 );
				expectedErrs.append( -1, FMT_MSG._NO_OPS );
				invalidOperandsForOpcode( opcode, NO_OPS.OPS );
			}
			
			/**
			 For a given list of Operands, It runs the remainder (excluding NO_OPS) of the preset Operand configurations
			 <p> After, It runs the runAfter list of operands, but does not make any assertions.
			 <p> Runs with the lineNo '-450'
			 */
			private void runAgainstOperandsList_Excluding_NoOps (String opcode, String... runAfter) {
				// This Needs a Rework
				InstrProvider.OperandsList_ExcludingNoOps( )
							 .filter( ops -> !Arrays.asList( runAfter ).contains( ops ) )
							 .forEach( ops -> invalidOperandsForOpcode( opcode, ops ) );
				Arrays.stream( runAfter ).forEach( ops -> ValidateInstr.splitValidOperands( -450, opcode, ops ) );
			}
			
			/**
			 (J/I_MEM) Tests Instruction Assemble correctly.
			 Setting the ops.Imm==Addr.
			 <p> Then tests it throws on 2nd attempt to Assemble.
			 */
			private void assertAssemblesSuccessfully (Instruction ins, Integer postAssembleAddr) {
				assertNotNull(ins);
				assertAll(
						( ) -> assertTrue( ins.assemble( errLog, LABELS_MAP, 0x00400004) ),
						( ) -> assertEquals( postAssembleAddr, ins.getImmediate() )
				);
			}
			
			private void assertNotNull_FailAssemble (Instruction ins, int PC){
				assertNotNull(ins);
				assertFalse( ins.assemble( errLog, LABELS_MAP, PC) );
			}
			
			/**
			 Tests Instruction Fails to Assemble, and Correct Err Msg Provided.
			 <p>Determines The error, based on if the Ops is a Jump Type
			 <p>Or If the label is not in the {@link #LABELS_MAP}
			 */
			private void assertFailAssemble_LabelPtr (Instruction ins, String label) {
				assertNotNull_FailAssemble(ins, 0x00400004);
				if ( LABELS_MAP.containsKey( label ) ) {
					int addr=LABELS_MAP.get( label );
					if ( ins instanceof J_Type || ins instanceof Branch ) {
						AddressValidation.isSupportedInstrAddr( addr, expectedErrs );
						expectedErrs.appendEx( FMT_MSG.label.points2Invalid_Address( label, "Instruction" ) );
					} else {
						AddressValidation.isSupportedDataAddr( addr, expectedErrs );
						expectedErrs.appendEx( FMT_MSG.label.points2Invalid_Address( label, "Data" ) );
					}
				} else {
					expectedErrs.appendEx( FMT_MSG.label.labelNotFound( label ) );
				}
			}
			
			/** {@link IllegalArgumentException} */
			private void assertIMMEDIATE_Equals_AndAssembles (Instruction ins, String opcode,
															  Integer imm, Integer rs, Integer rt) {
				assertNotNull_InsEquals( ins, opcode, Type.IMMEDIATE, imm, null, rs, rt);
				assertAssemblesSuccessfully(ins, imm);
			}
			/** {@link IllegalStateException} */
			private void assertREGISTER_Equals_AndAssembles (Instruction ins, String opcode, Integer rd, Integer rs, Integer rt) {
				assertNotNull_InsEquals( ins, opcode, Type.REGISTER, null, rd, rs, rt );
				assertAssemblesSuccessfully(ins, null);
			}
			
			private void assertNotNull_InsEquals (Instruction ins, String opcode, Type type, Integer rs, Integer rt, String label) {
				assertNotNull( ins );
				assertEquals( "Instruction{ opcode= '"+opcode+"', type= " + type + ", RD= null, RS= " + rs + ", RT= " + rt +
							  ", IMM= null, label= '" + label + "' }", ins.toString() );
			}
			
			private void assertNotNull_InsEquals (Instruction ins, String opcode, Type type, Integer imm, Integer rd, Integer rs, Integer rt) {
				assertNotNull( ins );
				assertEquals( "Instruction{ opcode= '"+opcode+"', type= " + type + ", RD= " + rd + ", RS= " + rs + ", RT= " + rt +
							 ", IMM= " + imm+" }", ins.toString() );
			}
			private void assertNotNull_InsEquals_Jump (Instruction ins, String opcode, String label) {
				assertNotNull_InsEquals( ins, opcode, Type.JUMP, null, null, label);
			}
			private void assertNotNull_InsEquals_Mem (Instruction ins, String opcode, Integer rt, String label) {
				assertNotNull_InsEquals( ins, opcode, Type.IMMEDIATE, 0, rt, label);
			}
				/** asserts the list of registers are not Recognised */
			private void notRecognised (int lineNo, String... notRecognised) {
				Arrays.stream( notRecognised ).forEach( op -> expectedErrs.append( lineNo, FMT_MSG.reg._NotRecognised( op ) ) );
			}
			
		}
		
		private int parseImm (String imm) {
			return (imm.length( )>2 && imm.charAt( 1 )=='x') ? Long.decode( imm ).intValue( ) : Integer.parseInt( imm );
		}
		
		/** Exclude where the No.of Ops matches the instr, then manually specify any errors*/
		@Nested
		class Invalid_SetOperands {
			// Need to manually print the errors that might be reported
			// For Operands the same length as the Type accepts
			
			@ParameterizedTest ( name="Invalid {index} - opcode\"{0}\"" )
			@ArgumentsSource ( NO_OPS.class )
			@Tag ( "Invalid_Operands_Assemble" )
			void invalid (String opcode) {
				//excluding NO_OPS.OPS
				expect.runAgainstOperandsList_Excluding_NoOps( opcode, "" );
			}
			
			@ParameterizedTest ( name="Invalid Operands[{index}] For R_Type[\"{0}\"]" )
			@ArgumentsSource ( RD_RS_RT.class )
			@Tag ( "Invalid_Operands_Assemble" )
			void invalidOperands_R_Type (String opcode) {
				expect.NoOps_NotValid_AndSetLineNo( opcode );
				
				expect.runAgainstOperandsList_Excluding_NoOps( opcode, RD_RS_RT.OPS,
															   I.RT_RS_IMM.OPS,
															   I.RS_RT_OFFSET.OPS_IMM,
															   I.RS_RT_OFFSET.OPS_LABEL);
				
				// I_type RT, RS, IMM
				expect.notRecognised( -450, "-40" );
				expectedErrs.append( -450, _opsForOpcodeNotValid( opcode, I.RT_RS_IMM.OPS ) );
				
				// I_type Branch
				expect.notRecognised( -450, "5" );
				expectedErrs.append( -450, _opsForOpcodeNotValid( opcode, I.RS_RT_OFFSET.OPS_IMM) );
				// I_type Branch
				expect.notRecognised( -450, "instr" );
				expectedErrs.append( -450, _opsForOpcodeNotValid( opcode, I.RS_RT_OFFSET.OPS_LABEL) );
			}
			
			@ParameterizedTest ( name="Invalid Operands[{index}] For I_Type RT_RS_IMM[\"{0}\"]" )
			@ArgumentsSource ( I.RT_RS_IMM.class )
			@Tag ( "Invalid_Operands_Assemble" )
			void invalidOperands_I_Type_RT_RS_IMM (String opcode) {
				expect.NoOps_NotValid_AndSetLineNo( opcode );
				
				expect.runAgainstOperandsList_Excluding_NoOps( opcode, I.RT_RS_IMM.OPS,
															   RD_RS_RT.OPS,
															   I.RS_RT_OFFSET.OPS_IMM,
															   I.RS_RT_OFFSET.OPS_LABEL );
				
				// R_type RD, RS, RT
				expectedErrs.appendEx( -450, FMT_MSG.imm.notValInt( "$at" ) );
				expectedErrs.append( -450, _opsForOpcodeNotValid( opcode, RD_RS_RT.OPS ) );
				
				// I_type Branch
				expectedErrs.appendEx( -450, FMT_MSG.imm.notValInt( "instr" ));
				expectedErrs.append( -450, _opsForOpcodeNotValid( opcode, I.RS_RT_OFFSET.OPS_LABEL) );
			}
			
			@ParameterizedTest ( name="Invalid Operands[{index}] For I_Type Branch[\"{0}\"]" )
			@ArgumentsSource ( I.RS_RT_OFFSET.class )
			@Tag ( "Invalid_Operands_Assemble" )
			void invalidOperands_I_Type_BRANCH (String opcode) {
				expect.NoOps_NotValid_AndSetLineNo( opcode );
				
				expect.runAgainstOperandsList_Excluding_NoOps( opcode, I.RS_RT_OFFSET.OPS_IMM, I.RS_RT_OFFSET.OPS_LABEL,
															   I.RT_RS_IMM.OPS,
															   RD_RS_RT.OPS );
				
				// R_type RD, RS, RT
				expectedErrs.appendEx( -450, FMT_MSG.label.notSupp( "$at" ) );
				expectedErrs.append( -450, _opsForOpcodeNotValid( opcode, RD_RS_RT.OPS ) );
			}
			
			@Tag ( "Invalid_Operands_Assemble" )
			@ParameterizedTest ( name="Invalid Operands[{index}] For I_Type RT_MEM[\"{0}\"]" )
			@ArgumentsSource ( I.RT_MEM.class )
			void invalidOperands_I_Type_RT_MEM (String opcode) {
				expect.NoOps_NotValid_AndSetLineNo( opcode );
				
				expect.runAgainstOperandsList_Excluding_NoOps( opcode, I.RT_MEM.OPS_IMM_RS, I.RT_MEM.OPS_LABEL);
			}
			
			@Tag ( "Invalid_Operands_Assemble" )
			@ParameterizedTest ( name="Invalid Operands[{index}] For J_Type[\"{0}\"]" )
			@ArgumentsSource ( J.class )
			void invalidOperands_Jump (String opcode) {
				expect.NoOps_NotValid_AndSetLineNo( opcode );
				
				expect.runAgainstOperandsList_Excluding_NoOps( opcode,
															   J.OPS_IMM, J.OPS_LABEL );
			}
			@Test    // Null -> Does Nothing
			@DisplayName ( "Null Opcode" )
			void nullOpcode ( ) {
				assertNull( ValidateInstr.splitValidOperands( 230, null, null ) );
			}
			@ParameterizedTest ( name="[{index}] Invalid Opcode[\"{0}\"] -> Returns Null" )
			@ArgumentsSource ( InstrProvider.Invalid.Limit_Two.class )
			@ArgumentsSource ( BlankProvider.class )
			void invalidOpcode (String opcode) {
				assertNull( ValidateInstr.splitValidOperands( 500, opcode,null) );
				expectedErrs.appendEx( 500, FMT_MSG.Opcode_NotSupported( opcode ) );
				
			}
			
			@Nested
			class Operand_Spacing {
				// ANY Spacing At The Beginning/ End Of the Line
				// Spacing of larger than 1  internally
				
				// In the Logger Output, Tabs and NewLn are removed ???
				
				@Test
				void R_Type__Leading_Trailing_And_Internal_Spaces ( ) {
					expect.notRecognised( 42, " r1", "  r2", "r3 " );
					expect.invalidOperandsForOpcode( 42, "add", " r1,   r2 , r3 " );
					
				}
				
				@Test
				void SingleTabs_Valid ( ) {
					Instruction ins=ValidateInstr.splitValidOperands( 12, "sub", "s8,\tr3,\t$8" );
					expect.assertREGISTER_Equals_AndAssembles( ins,"sub",30, 3, 8 );
				}
				@Test
				void DoubleTabs_OrLeading_Trailing_Tabs_Invalid ( ) {
					expect.notRecognised( 12, "\t$24", "\t$16" );
					expect.invalidOperandsForOpcode( 12, "sub", "\t$24,\tr20\t,\t\t$16" );
				}
				
				@Test
				void NewLine_Invalid ( ) {
					expect.notRecognised( 67, "R0" );
					expect.invalidOperandsForOpcode( 67, "sub", "t8\t,r20,\nR0" );
				}
				
			}
			
		}
		
		@Nested
		class No_Operands {
			
			@ParameterizedTest ( name="Valid {index} - opcode\"{0}\"" + A )
			@ArgumentsSource ( NO_OPS.class )
			void assemble_ValidOperands (String opcode) {
				Instruction ins=ValidateInstr.splitValidOperands( 12, opcode, " " );
				expect.assertNotNull_InsEquals( ins, opcode, Type.NOP, null, null,null, null );
				expect.assertAssemblesSuccessfully(ins, null);
			}
			
			@Test
			void comments_Not_Removed ( ) {
				assertThrows( IllegalStateException.class, ()-> ValidateInstr.splitValidOperands( 20, "j", " #"));
				
			}
			
			@Test
			void Invalid_OpCode (){
				assertThrows( IllegalArgumentException.class, ()-> new Nop(" "));
			}
			
		}
		
		@Nested
		class Register_Type {
			
			@ParameterizedTest ( name="Valid {index} - opcode\"{0}\"" + A )
			@ArgumentsSource ( RD_RS_RT.class )
			void assemble_ValidOperands (String opcode) {
				Instruction ins=ValidateInstr.splitValidOperands( 12, opcode, RD_RS_RT.OPS );
				expect.assertREGISTER_Equals_AndAssembles( ins, opcode, 1, 1, 1 );
			}
			@ParameterizedTest ( name="Valid {index} - opcode\"{0}\" -  Different Register Names" + A )
			@ArgumentsSource ( RD_RS_RT.class )
			void testOperands_Add_Sub ( ) {
				Instruction ins=ValidateInstr.splitValidOperands( 12, "add", "$8,r31, $s2" );
				expect.assertREGISTER_Equals_AndAssembles( ins, "add", 8, 31, 18 );
			}
			
			@ParameterizedTest ( name="Valid {index} - opcode [sub], operands \"{0}\" -  Mixed Spacing" + A )
			@ValueSource ( strings={ "$24 , $s4, $s0", "t8,r20,\t$16" } )
			void testOperands_R_Type_Spacing (String ops) {
				Instruction ins=ValidateInstr.splitValidOperands( 12, "sub", ops );
				expect.assertREGISTER_Equals_AndAssembles( ins, "sub", 24, 20, 16 );
			}
			
			@Nested
			class Invalid_Operands {
				
				@ParameterizedTest ( name="Multiple Invalid {index} - opcode\"{0}\" And ZeroWriteWarning" )
				@ArgumentsSource ( RD_RS_RT.class )
				void multipleInvalid_ZWW ( ) {
					expect.notRecognised( 76, "$panda", "31" );
					expect.invalidOperandsForOpcode( 76, "add", "zero, $panda, 31" );
					testLogs.zeroWarning( 76, "zero" );
				}
			}
			
		}
		
		@Nested
		class Immediate_Type {
			
			@Nested
			@DisplayName ( "RT, RS, IMM" )
			class RT_RS_IMM {
				
				@ParameterizedTest ( name="Valid {index} - opcode\"{0}\"" + A )
				@ArgumentsSource ( I.RT_RS_IMM.class )
				void assemble_ValidOperands (String opcode) {
					Instruction ins=ValidateInstr.splitValidOperands( 12, opcode, I.RT_RS_IMM.OPS );
					expect.assertIMMEDIATE_Equals_AndAssembles( ins, opcode, -40, 1, 1 );
				}
				
				@ParameterizedTest ( name="{index} - Immediate\"{2}\" Valid 16Bit :: Hex" )
				@ArgumentsSource ( ImmediateProvider._16Bit.class )
				void Valid_16Bit_Hex (String addr, Integer address, String hex, Integer imm) {
					Instruction ins = ValidateInstr.splitValidOperands( 30, "addi", "$2, $2, " + hex);
					expect.assertIMMEDIATE_Equals_AndAssembles( ins, "addi", imm, 2, 2);
				}
				
				@ParameterizedTest ( name="{index} - Immediate\"{3}\" Valid 16Bit :: Imm" )
				@ArgumentsSource ( ImmediateProvider._16Bit.class )
				void Valid_16Bit_Imm (String addr, Integer address, String hex, Integer imm) {
					Instruction ins = ValidateInstr.splitValidOperands( 30, "addi", "$2, $2, " + imm);
					expect.assertIMMEDIATE_Equals_AndAssembles( ins, "addi", imm, 2, 2);
				}
				
				@Nested
				class Invalid_Operands {
					
					@Test
					@DisplayName ( "Invalid Operands I_RT_RS_IMM (ADDI), given I_RT_Imm" )
					void invalidOperands_I_RT_RS ( ) { expect.invalidOperandsForOpcode( 52, "addi", "$1, 0x20" ); }
					
					@ParameterizedTest ( name="Multiple Invalid {index} - opcode\"{0}\" And ZeroWriteWarning" )
					@ArgumentsSource ( I.RT_MEM.class )
					void multipleInvalid_ZWW ( ) {
						Instruction ins=ValidateInstr.splitValidOperands( 30, "addi", "$0, $panda, 32769" );
						// Errors with all Operands
						assertNull( ins );
						testLogs.appendErrors(
								30,
								FMT_MSG.reg._NotRecognised( "$panda" ),
								FMT_MSG.imm.notSigned16Bit( 32769 ) + "!",
								_opsForOpcodeNotValid( "addi", "$0, $panda, 32769" )
						);
						testLogs.zeroWarning( 30, "$0" );
					}
					
					@ParameterizedTest ( name="{index} - Immediate\"{2}\" Not Valid 16Bit :: Hex" )
					@ArgumentsSource ( ImmediateProvider._16Bit.Invalid.class )
					void Invalid_16Bit_Hex (String addr, Integer address, String hex, Integer imm) {
						expectedErrs.appendEx(30,FMT_MSG.imm.notSigned16Bit( imm ));
						expect.invalidOperandsForOpcode( 30, "addi", "$2, $2, " + hex);
					}
					
					@ParameterizedTest ( name="{index} - Immediate\"{3}\" Not Valid 16Bit :: Imm" )
					@ArgumentsSource ( ImmediateProvider._16Bit.Invalid.class )
					void Invalid_16Bit_Imm (String addr, Integer address, String hex, Integer imm) {
						expectedErrs.appendEx(30,FMT_MSG.imm.notSigned16Bit( imm ));
						expect.invalidOperandsForOpcode( 30, "addi", "$2, $2, " + imm);
					}
				}
				
			}
			
			@Nested
			class Memory {
				
				/*
				 TODO - Inconsistency with Base+Offset
				 	When having just an Imm - it is checked if it is a valid address.
				 	When Imm with Empty Brackets (Register $0),   It does not !. -> This should be caught during Execution
				 */
				
				@Nested
				class Valid {
					
					@ParameterizedTest ( name="Valid {index} - opcode\"{0}\", Label" + A )
					@ArgumentsSource ( I.RT_MEM.class )
					void assemble_ValidOperands_Label (String opcode) {
						Instruction ins=ValidateInstr.splitValidOperands( 12, opcode, "$9, data" );
						expect.assertNotNull_InsEquals_Mem( ins, opcode, 9, "data" );
						expect.assertAssemblesSuccessfully( ins, 0x10010000 );
						//MAX
						Instruction ins1=ValidateInstr.splitValidOperands( 12, opcode, "r2, data_top" );
						expect.assertNotNull_InsEquals_Mem( ins1, opcode, 2, "data_top" );
						expect.assertAssemblesSuccessfully( ins1, 0x100107F8 );
					}
					
					@Nested
					@DisplayName( "Base+Offset/Imm(RS)" )
					class Base_Offset {
						
						@ParameterizedTest ( name="Valid {index} - opcode\"{0}\", Imm(RS)" + A )
						@ArgumentsSource ( I.RT_MEM.class )
						void assemble_ValidOperands (String opcode) {
							Instruction ins=ValidateInstr.splitValidOperands( 12, opcode, I.RT_MEM.OPS_IMM_RS );
							expect.assertIMMEDIATE_Equals_AndAssembles( ins, opcode,-8, 1, 1 );
						}
						
						@ParameterizedTest ( name="Valid {index} - opcode\"{0}\" Base+Offset :: Int" + A )
						@ArgumentsSource ( I.RT_MEM.class )
						void assemble_ValidOperands_BaseOffset_INT (String opcode) {
							Instruction ins=ValidateInstr.splitValidOperands( 0, opcode, "$5, 20 ($1)" );
							expect.assertIMMEDIATE_Equals_AndAssembles( ins, opcode,20, 1, 5 );
						}
						
						@ParameterizedTest ( name="Valid {index} - opcode\"{0}\" Base+Offset :: NoImm" + A )
						@ArgumentsSource ( I.RT_MEM.class )
						void assemble_ValidOperands_BaseOffset_NoImm (String opcode) {
							Instruction ins=ValidateInstr.splitValidOperands( 0, opcode, "$6, ($1)" );
							expect.assertIMMEDIATE_Equals_AndAssembles( ins, opcode, 0, 1, 6 );
						}
						
						@ParameterizedTest ( name="Valid {index} - opcode\"{0}\" Base+Offset :: HEX" + A )
						@ArgumentsSource ( I.RT_MEM.class )
						@Tag ( Tags.MULTIPLE )
						@DisplayName ( "Test Operands, Base+Offset" )
						void assemble_ValidOperands_BaseOffset_Hex (String opcode) {
							Instruction ins=ValidateInstr.splitValidOperands( 0, opcode, "$5, 0x290($8)" );
							expect.assertIMMEDIATE_Equals_AndAssembles( ins, opcode, 656, 8, 5 );
						}
						
						@Nested
						class no_RS {
							
							@ParameterizedTest ( name="Valid {index} - opcode\"{0}\" Base+Offset :: NoImm, noRS" + A )
							@ArgumentsSource ( I.RT_MEM.class )
							void assemble_ValidOperands_BaseOffset_NoImm_noRS (String opcode) {
								// TODO check the IMM is a valid address, when RS=0
								Instruction ins1=ValidateInstr.splitValidOperands( 0, opcode, "$8,  ()" );
								expect.assertIMMEDIATE_Equals_AndAssembles( ins1, opcode, 0, 0, 8 );
							}
							
							@ParameterizedTest ( name="Valid {index} - opcode\"{0}\" Base+Offset :: Int, noRS" + A )
							@ArgumentsSource ( I.RT_MEM.class )
							void assemble_ValidOperands_BaseOffset_INT_noRS (String opcode) {
								// TODO check the IMM is a valid address, when RS=0
								Instruction ins1=ValidateInstr.splitValidOperands( 0, opcode, "$7, -800 ()" );
								expect.assertIMMEDIATE_Equals_AndAssembles( ins1, opcode, -800, 0, 7 );
							}
							
							@ParameterizedTest ( name="Valid {index} - opcode\"{0}\" Base+Offset :: HEX, noRS" + A )
							@ArgumentsSource ( I.RT_MEM.class )
							void assemble_ValidOperands_BaseOffset_Hex_noRS (String opcode) {
								// TODO check the IMM is a valid address, when RS=0
								Instruction ins1=ValidateInstr.splitValidOperands( 0, opcode, "$9, 0xFFFF8000 ()" );
								expect.assertIMMEDIATE_Equals_AndAssembles( ins1, opcode, -32768, 0, 9);
							}
						}
						
					}
					
				}
				
				@Nested
				class Invalid_Operands {
					
					@ParameterizedTest ( name="Valid - opcode\"{0}\", NonData Label" + FA )
					@ArgumentsSource ( I.RT_MEM.class )
					void NonData_Label (String opcode) {
						for ( String label : InstrProvider.KeysExcluding( "data","data_top" ) ) {
							Instruction ins=ValidateInstr.splitValidOperands( 12, opcode, "$2," + label );
							expect.assertNotNull_InsEquals_Mem( ins, opcode, 2, label );
							expect.assertFailAssemble_LabelPtr( ins, label );
						}
					}
					
					@ParameterizedTest ( name="Invalid {index} - opcode\"{0}\" LabelNotFound And ZeroWriteWarning" + FA )
					@ArgumentsSource ( I.RT_MEM.class )
					void LabelNotFound_ZWW (String opcode) {
						Instruction ins=ValidateInstr.splitValidOperands( 30, opcode, "$0, panda" );
						
						expect.assertNotNull_InsEquals_Mem( ins, opcode, 0, "panda" );
						expect.assertFailAssemble_LabelPtr( ins, "panda" );
						if ( InstructionValidation.I_MEM_WRITE.contains( opcode ) )
							testLogs.zeroWarning( 30, "$0" );
					}
					
					
					@ParameterizedTest ( name="Invalid {index} - opcode\"{0}\" Invalid Integer" )
					@ArgumentsSource ( I.RT_MEM.class )
					void invalid_Integer (String opcode) {
						ValidateInstr.splitValidOperands( 23, opcode, "$1, 1.5" );
						// Errors with all Operands
						expectedErrs.appendEx( 23, FMT_MSG.imm.notValInt( "1.5" ) );
						expectedErrs.append( 23, _opsForOpcodeNotValid( opcode, "$1, 1.5" ) );
						
						expectedErrs.appendEx( 23, FMT_MSG.imm.notValInt( "1.5" ) );
						expect.invalidOperandsForOpcode( 23, opcode, "$1, 1.5" );
					}
					
					@ParameterizedTest ( name="Invalid {index} - opcode\"{0}\" No ImmRS Found" )
					@ArgumentsSource ( I.RT_MEM.class )
					void NoImmRS_NotFound (String opcode) {
						expectedErrs.appendEx( 78, "\t\tNo Imm(RS) found" );
						expect.invalidOperandsForOpcode( 78, opcode, "$1," );
					}
					
				}
				
				@Nested
				@DisplayName ( "Imm(RS) method" )
				class immRS_method {
					@BeforeEach
					void setUp ( ) {
						ValidateInstr.setLineNo( -1 );	// TODO change back to parameters being passed into every method
						ValidateInstr.setOpcode( "lw" );// valid opcode for ImmRS
					}
					
					@Nested
					@DisplayName ( "Valid Imm(RS)" )
					class validImmRS {
						
						private void assertImmRS (Instruction ins, int imm, int rs) {
							assertNotNull( ins );
							System.out.println( ins );
							assertAll(
									( ) -> assertTrue( ins.toString().contains( "IMM= "+imm ) ),
									( ) -> assertTrue( ins.toString().contains( "RS= "+rs ) )
							);
						}
						
						@ParameterizedTest ( name="Valid no RS, no IMM {index}, Just Brackets[{0}]] -> Default" )
						@ValueSource ( strings={ "( )", "()" } )
						void onlyBrackets (String immRS) {
							assertImmRS( ValidateInstr.rt_ImmRs( 9, immRS ), 0, 0 );
						}
						
						@ParameterizedTest ( name="Valid {index} - \"{0}\" -> noIMM -> Default" )
						@ValueSource ( strings={ "($2)", "( $2 )" } )
						void noIMM (String immRS) {
							assertImmRS( ValidateInstr.rt_ImmRs( 8, immRS ), 0, 2 );// RS = $2 ==>2
						}
						
						@ParameterizedTest ( name="Valid {index} - \"{0}\" -> noRS -> Default" )
						@ValueSource ( strings={ "8()", "8 ()", "8 ( )", "0x8 ()" } )
						void noRS (String immRS) {
							assertImmRS( ValidateInstr.rt_ImmRs( 7, immRS ), 8, 0 );// Imm = 8
						}
						
					}
					
					@Nested
					@DisplayName ( "Invalid Imm(RS)" )
					class invalidImmRS {
						
						@ParameterizedTest ( name="invalidImmRS {index} - \"{0}\" -> MissingOpeningBracket" )
						@ValueSource ( strings={ "1.5 )", "8$2)", "8 $2)", "8)", ")", "$2)", "0x2 $4)" } )
						void missingOpeningBracket (String immRS) {
							assertNull( ValidateInstr.rt_ImmRs( 5, immRS ) );
							expectedErrs.appendEx( -1, FMT_MSG.imm.RS_MissingOpeningBracket );
						}
						@ParameterizedTest ( name="invalidImmRS {index} - \"{0}\" -> MissingClosingBracket" )
						@ValueSource ( strings={ "1.5 (", "8($2", "8 ($2", "8(", "(", "($2", "0x2 ($4" } )
						void missingClosingBracket (String immRS) {
							assertNull( ValidateInstr.rt_ImmRs( 4, immRS ) );
							expectedErrs.appendEx( -1, FMT_MSG.imm.RS_MissingClosingBracket );
						}
						
					}
					
					@Nested
					@DisplayName ( "Label_MEM-Address" )
					class I_Label_MEM {
						
						@ParameterizedTest ( name="Imm {index} - \"{0}\" -> Not Valid Data Address" )
						@ValueSource ( strings={ "8", "0x8" } )
						void noRS (String immRS) {
							Instruction ins=ValidateInstr.rt_ImmRs( 7, immRS );    // Imm
							assertNull( ins );
							AddressValidation.isSupportedDataAddr( 4*parseImm( immRS ), expectedErrs );
						}
						
						@Nested
						@DisplayName ( "Invalid Imm" )
						class invalid_Imm {
							
							@ParameterizedTest ( name="{index}, invalid Imm[{0}]]" )
							@ValueSource ( strings={ "1.5", "0x 2" } )
							void invalidImm (String imm) {
								assertNull( ValidateInstr.rt_ImmRs( 4, imm ) );
								expectedErrs.appendEx( -1, FMT_MSG.imm.notValInt( imm ) );
								
								assertNull( ValidateInstr.rt_ImmRs( 4, imm + " ()" ) );// + Brackets
								expectedErrs.appendEx( -1, FMT_MSG.imm.notValInt( imm ) );
								
							}
							
							@ParameterizedTest ( name="{index}, invalid Imm[{0}]] Not 16Bit" )
							@ValueSource ( strings={ "67108863", "-32769", "32768", "0xFFFF7FFF", "0x00008000" } )// TODO ARG SOURCE
							void Not_16bit (String imm) {
								assertNull( ValidateInstr.rt_ImmRs( 4, imm ) );
								expectedErrs.appendEx( -1, FMT_MSG.imm.notSigned16Bit( parseImm( imm ) ) );
								
								assertNull( ValidateInstr.rt_ImmRs( 4, imm + " ()" ) );// + Brackets
								expectedErrs.appendEx( -1, FMT_MSG.imm.notSigned16Bit( parseImm( imm ) ) );
							}
							
						}
						
					}
					
				}
				
			}
			
			@Nested
			class Branches {
				
				@Test
				void Branch_InfiniteLoop_CaughtAtAssembly () {
					Instruction ins = ValidateInstr.splitValidOperands( 20,"beq","$0, $0, -1" );
					expect.assertNotNull_FailAssemble(ins, 0x00400004);
					expectedErrs.appendEx( "Branch PC is the same as its targetPC:[0x00400004], Imm:[-1], This will cause an infinite loop" );
				}
				
				@Nested
				class IMM { // Assume PC = 0x00400000
					
					@ParameterizedTest ( name="Valid {index} - opcode\"{0}\"" + A )
					@ArgumentsSource ( I.RS_RT_OFFSET.class )
					void assemble_ValidOperands (String opcode) {
						Instruction ins=ValidateInstr.splitValidOperands( 12, opcode, I.RS_RT_OFFSET.OPS_IMM );
						expect.assertIMMEDIATE_Equals_AndAssembles( ins, opcode, 5, 1, 1 );
					}
					
					@ParameterizedTest ( name="{index} - Immediate\"{2}\" Valid 16Bit :: Hex" + A )
					@ArgumentsSource ( ImmediateProvider._16Bit.class )
					void Valid_16Bit_Hex (String addr, Integer address, String hex, Integer imm) {
						Instruction ins = ValidateInstr.splitValidOperands( 30, "bne", "$2, $3, " + hex);
						
						expect.assertNotNull_InsEquals( ins, "bne", Type.IMMEDIATE, imm, null, 2, 3);
						assertTrue( ins.assemble( testLogs.actualErrors, LABELS_MAP, 0x00450000) );
						assertEquals( imm, ins.getImmediate() );
					}
					
					@ParameterizedTest ( name="{index} - Immediate\"{3}\" Valid 16Bit :: Imm" + A)
					@ArgumentsSource ( ImmediateProvider._16Bit.class )
					void Valid_16Bit_Imm (String addr, Integer address, String hex, Integer imm) {
						Instruction ins = ValidateInstr.splitValidOperands( 30, "beq", "$2, $3, " + imm);
						
						
						expect.assertNotNull_InsEquals( ins, "beq", Type.IMMEDIATE, imm, null, 2, 3);
						assertTrue( ins.assemble( testLogs.actualErrors, LABELS_MAP, 0x00450000) );
						assertEquals( imm, ins.getImmediate() );
					}
					
					@Nested
					class Throws_Assemble {	// TODO print LineNo with assembly errors
						private void expectOutOfRangeImm(int NPC, int imm, int target){
							String targetPC = Convert.int2Hex(target);
							expectedErrs.appendEx( "NPC["+Convert.int2Hex(NPC)+"], Offset["+imm+"], Target["+targetPC+"]" );
						}
						
						@ParameterizedTest ( name="Valid {index} - opcode\"{0}\", Imm :: UnderRange " + A )
						@ArgumentsSource ( I.RS_RT_OFFSET.class )
						void assemble_ValidOperands_Imm_UnderRange  (String opcode) {
							Instruction ins=ValidateInstr.splitValidOperands( 0, opcode, "$2, $3, -50" );
							expect.assertNotNull_InsEquals( ins, opcode, Type.IMMEDIATE, -50,null,2, 3);
							
							expect.assertNotNull_FailAssemble(ins,0x00400000);
							expectedErrs.appendEx( FMT_MSG.xAddressNot( "Instruction", "0x003FFF3C", "Valid" ));
							expectOutOfRangeImm(0x00400004, -50, 0x003FFF3C);
						}
						
						@ParameterizedTest ( name="Valid {index} - opcode\"{0}\", Imm :: UnderRange :: Hex" + A )
						@ArgumentsSource ( I.RS_RT_OFFSET.class )
						void assemble_ValidOperands_Imm_Hex_UnderRange (String opcode) {
							Instruction ins=ValidateInstr.splitValidOperands( 0, opcode, "$2, $2, 0xFFFFFFCE" );
							expect.assertNotNull_InsEquals( ins, opcode, Type.IMMEDIATE, -50,null,2, 2);
							
							expect.assertNotNull_FailAssemble(ins,0x00400000);
							expectedErrs.appendEx( FMT_MSG.xAddressNot( "Instruction", "0x003FFF3C", "Valid" ));
							expectOutOfRangeImm(0x00400004, -50, 0x003FFF3C);
						}
						
						@ParameterizedTest ( name="Valid {index} - opcode\"{0}\", Imm :: OverRange " + A )
						@ArgumentsSource ( I.RS_RT_OFFSET.class )
						void assemble_ValidOperands_Imm_OverRange  (String opcode) {
							Instruction ins=ValidateInstr.splitValidOperands( 0, opcode, "$2, $2, 5" );
							expect.assertNotNull_InsEquals( ins, opcode, Type.IMMEDIATE, 5,null,2, 2);
							
							expect.assertNotNull_FailAssemble(ins,0x00500000);
							expectedErrs.appendEx( FMT_MSG.xAddressNot( "Instruction","0x00500018", "Supported" ));
							expectOutOfRangeImm(0x00500004, 5, 0x00500018);
						}
						
						@ParameterizedTest ( name="Valid {index} - opcode\"{0}\", Imm :: OverRange :: Hex" + A )
						@ArgumentsSource ( I.RS_RT_OFFSET.class )
						void assemble_ValidOperands_Imm_Hex_OverRange  (String opcode) {
							Instruction ins=ValidateInstr.splitValidOperands( 0, opcode, "$2, $2, 0x5" );
							expect.assertNotNull_InsEquals( ins, opcode, Type.IMMEDIATE, 5,null,2, 2);
							
							expect.assertNotNull_FailAssemble(ins,0x00500000);
							expectedErrs.appendEx( FMT_MSG.xAddressNot( "Instruction","0x00500018", "Supported" ));
							expectOutOfRangeImm(0x00500004, 5, 0x00500018);
						}
						
					}
					
					@Nested
					class Invalid_Operands {
						
						@ParameterizedTest ( name="Invalid {index} - opcode\"{0}\" Invalid Integer" )
						@ArgumentsSource ( I.RS_RT_OFFSET.class )
						void invalid_Integer (String opcode) {
							ValidateInstr.splitValidOperands( 23, opcode, "$1,$1, 1.5" );
							// Errors with all Operands
							expectedErrs.appendEx( 23, FMT_MSG.imm.notValInt( "1.5" ) );
							expectedErrs.append( 23, _opsForOpcodeNotValid( opcode, "$1,$1, 1.5" ) );
							
							expectedErrs.appendEx( 23, FMT_MSG.imm.notValInt( "1.5" ) );
							expect.invalidOperandsForOpcode( 23, opcode, "$1,$1, 1.5" );
						}
						
						@ParameterizedTest ( name="{index} - Immediate\"{2}\" Not Valid 16Bit :: Hex" )
						@ArgumentsSource ( ImmediateProvider._16Bit.Invalid.class )
						void Invalid_16Bit_Hex (String addr, Integer address, String hex, Integer imm) {
							expectedErrs.appendEx(30,FMT_MSG.imm.notSigned16Bit( imm ));
							expect.invalidOperandsForOpcode( 30, "bgt", "$2, $2, " + hex);
						}
						
						@ParameterizedTest ( name="{index} - Immediate\"{3}\" Not Valid 16Bit :: Imm" )
						@ArgumentsSource ( ImmediateProvider._16Bit.Invalid.class )
						void Invalid_16Bit_Imm (String addr, Integer address, String hex, Integer imm) {
							expectedErrs.appendEx(30,FMT_MSG.imm.notSigned16Bit( imm ));
							expect.invalidOperandsForOpcode( 30, "blt", "$2, $2, " + imm);
						}
					}
					
				}
				
				@Nested
				class Label {
					
					@Nested
					class Valid {
						
						@ParameterizedTest ( name="Valid {index} - opcode\"{0}\", Label" + A )
						@ArgumentsSource ( I.RS_RT_OFFSET.class )
						void assemble_ValidOperands_Label (String opcode) {
							Instruction ins=ValidateInstr.splitValidOperands( 12, opcode, "$9, $20, instr" );
							expect.assertNotNull_InsEquals( ins, opcode, Type.IMMEDIATE, 9,20, "instr" );
							expect.assertAssemblesSuccessfully( ins, -2 );
							//MAX
							Instruction ins1=ValidateInstr.splitValidOperands( 12, opcode, "r2, r4, instr_15_top" );
							expect.assertNotNull_InsEquals( ins1, opcode, Type.IMMEDIATE, 2,4, "instr_15_top" );
							expect.assertAssemblesSuccessfully( ins1, 32767 );
							// Assuming PC is 0x00400000
						}
						
						@Nested
						class Throws_Assemble {
							
							@ParameterizedTest ( name="Valid - opcode\"{0}\", NonInstr Label" + FA )
							@ArgumentsSource ( I.RS_RT_OFFSET.class )
							void NonData_Label (String opcode) {
								for ( String label : InstrProvider.KeysExcluding( "instr","instr_top","instr_15_top" ) ) {
									Instruction ins=ValidateInstr.splitValidOperands( 12, opcode, "$2,$2," + label );
									
									expect.assertNotNull_InsEquals( ins, opcode,Type.IMMEDIATE, 2,2, label );
									expect.assertFailAssemble_LabelPtr( ins, label );
								}
							}
							
							@ParameterizedTest ( name="Valid - opcode\"{0}\", Instr Label Out Of Range" + FA )
							@ArgumentsSource ( I.RS_RT_OFFSET.class )
							void Instr_Label_OutOfRange (String opcode) {
								Instruction ins=ValidateInstr.splitValidOperands( 12, opcode, "$2,$2," + "instr_top" );
								
								expect.assertNotNull_InsEquals( ins, opcode,Type.IMMEDIATE, 2,2, "instr_top" ); // Leads to non16bit Imm
								assertNotNull(ins);
								assertFalse( ins.assemble( testLogs.actualErrors, LABELS_MAP, 0x00400000) );
								expectedErrs.appendEx( "Offset Imm["+262143+"], Is not a Valid Signed 16Bit Number" );
								
							}
							
							@ParameterizedTest ( name="Invalid {index} - opcode\"{0}\" LabelNotFound" +FA )
							@ArgumentsSource ( I.RS_RT_OFFSET.class )
							void LabelNotFound (String opcode) {
								Instruction ins=ValidateInstr.splitValidOperands( 30, opcode, "$0,$0, panda" );
								
								expect.assertNotNull_InsEquals( ins, opcode,Type.IMMEDIATE, 0,0, "panda" );
								expect.assertFailAssemble_LabelPtr( ins, "panda" );
							}
						}
						
					}
				}
				
				// TODO test sub-method directly. Should be fine as it is
			}
			
		}
		
		@Nested
		@DisplayName ( "J_Type" )
		class Jump {
			
			@Nested
			@Tag( Tags.MULTIPLE )
			class Valid {
				
				@ParameterizedTest ( name="Valid {index} - opcode\"{0}\", Jump_Type :: Label" + A )
				@ArgumentsSource ( J.class )
				void assemble_ValidOperands_Label (String opcode) {
					Instruction ins=ValidateInstr.splitValidOperands( 60, opcode, "instr" );
					expect.assertNotNull_InsEquals_Jump( ins, opcode, "instr");
					expect.assertAssemblesSuccessfully( ins, 0x00400000/4);
					//BranchMax
					Instruction ins1=ValidateInstr.splitValidOperands( 60, opcode, "instr_15_top" );
					expect.assertAssemblesSuccessfully( ins1, 0x00420004/4);
					//MAX
					Instruction ins2=ValidateInstr.splitValidOperands( 60, opcode, "instr_top" );
					expect.assertAssemblesSuccessfully( ins2, 0x00500000/4);
				}
				
				@ParameterizedTest ( name="Valid {index} - opcode\"{0}\", Jump_Type :: Imm" + A )
				@ArgumentsSource ( J.class )
				void assemble_ValidOperands_Imm (String opcode) {
					Instruction ins=ValidateInstr.splitValidOperands( 0, opcode, "1048576" );
					expect.assertNotNull_InsEquals( ins, opcode, Type.JUMP, 1048576,null,null, null);
					//MAX
					Instruction ins1=ValidateInstr.splitValidOperands( 0, opcode, "1310720" );
					expect.assertNotNull_InsEquals( ins1, opcode, Type.JUMP, 1310720,null,null, null);
				}
				
				@ParameterizedTest ( name="Valid {index} - opcode\"{0}\", Jump_Type :: Hex" + A )
				@ArgumentsSource ( J.class )
				void assemble_ValidOperands_Hex (String opcode) {
					Instruction ins=ValidateInstr.splitValidOperands( 0, opcode, "0x00100000" );
					expect.assertNotNull_InsEquals( ins, opcode, Type.JUMP, 1048576, null,null, null);
					//MAX
					Instruction ins1=ValidateInstr.splitValidOperands( 0, opcode, "0x00140000" );
					expect.assertNotNull_InsEquals( ins1, opcode, Type.JUMP, 1310720, null,null, null);
				}
				
				@Test
				void Jump_InfiniteLoop_CaughtAtAssembly () {
					Instruction ins = ValidateInstr.splitValidOperands( 20,"j","0x00100001" );
					expect.assertNotNull_FailAssemble(ins, 0x00400004);
					expectedErrs.appendEx( "Jump PC is the same as its targetPC:[0x00400004], This will cause an infinite loop" );
				}
			}
			
			@Nested
			class Invalid_Operands {
				
				@ParameterizedTest ( name="IO {index}Jump _Immediate[{0}] - Out of Range" )
				@ArgumentsSource (  ImmediateProvider.ConvertInvalid.OutOfRange.class )
				void testInvalid_OperandsJump_ImmOutOfRange (String hex, int imm) {
					Instruction ins=ValidateInstr.splitValidOperands( 0, "j", "" + imm );
					
					assertNull( ins );
					expectedErrs.appendEx( 0, FMT_MSG.imm.notUnsigned26Bit(imm) );
					expectedErrs.append( 0, _opsForOpcodeNotValid( "j", "" + imm ) );
				}
				
				// Immediate Values <(0x00100000) & >(0x00140000) Convert To Valid Addresses, but not Valid for Jump
				@ParameterizedTest ( name="[{index}] Invalid 26BitImm[{2}, {3}] for for Jump Instruction" )
				@ArgumentsSource ( ImmediateProvider.Instr_Imm.Invalid.class )
				@ArgumentsSource ( ImmediateProvider.u_26Bit.class )
				@Tag( Tags.MULTIPLE )
				void testInvalid_OperandsJump_ValImm (String hexAddr, long addr, String hexImm, long imm) {
					assertEquals(addr, imm*4); // Test Variables Invalid if False
					
					int a=(int) imm*4;
					String err="Instruction Address: \"" +hexAddr + "\" Not "+ ((a>=0x10000000 || a<0x00400000) ? "Valid" : "Supported");
					expectedErrs.appendEx( err );
					expect.invalidOperandsForOpcode( 97, "j", "" + imm);
					
					//Hex
					expectedErrs.appendEx( err );
					expect.invalidOperandsForOpcode( 120, "j", "" + imm);
				}
				
				@Test
				@DisplayName ( "Test Invalid Operands, Jump _TooManyOperands" )
				void testInvalid_OperandsJump_TooManyOperands ( ) {
					expect.invalidOperandsForOpcode( "j", "0x100009, 50" );
				}
				
				@ParameterizedTest ( name="Valid {index} - opcode\"{0}\", NonInstr Label" + FA )
				@ArgumentsSource ( J.class )
				void NonData_Label (String opcode) {
					for ( String label : InstrProvider.KeysExcluding( "instr","instr_top","instr_15_top" ) ) {
						Instruction ins=ValidateInstr.splitValidOperands( 12, opcode, label );
						expect.assertNotNull_InsEquals_Jump( ins, opcode, label );
						expect.assertFailAssemble_LabelPtr( ins, label );
					}
				}
			}
			
			@Nested
			class Jump_LabelOrImm_method {	// TODO refactor Helper Methods to use dependency injection
				@BeforeEach
				void setUp ( ) {
					ValidateInstr.setLineNo( -1 );	// TODO change back to parameters being passed into every method
					ValidateInstr.setOpcode( "j" );// valid opcode for Jump
				}
				@ParameterizedTest ( name="[{index}] Immediate[{0}], Valid for Jump" )
				@ArgumentsSource(ImmediateProvider.Instr_Imm.class )
				void Jump_LabelAddress (String hexAddr, long addr, String hexImm, long imm) {
					assertNotNull( ValidateInstr.Jump_LabelOrInt( "" + imm ) );
					assertNotNull( ValidateInstr.Jump_LabelOrInt( hexImm ));
				}
				
				@ParameterizedTest ( name="[{index}] Label[{0}], Valid Label/Address for Jump" )
				@ArgumentsSource(SetupProvider.ValidLabels.class )
				void Valid_LabelAddress (String label) { assertNotNull( ValidateInstr.Jump_LabelOrInt(label) ); }
				
				@Nested
				class Invalid {
					
					@ParameterizedTest ( name="[{index}] Label[{0}], Invalid Labels" )
					@ArgumentsSource( SetupProvider.InvalidLabels.class )
					void Invalid_LabelAddress (String label) {
						assertNull( ValidateInstr.Jump_LabelOrInt( label ) );
						expectedErrs.appendEx( -1, FMT_MSG.label.notSupp( label ) );
					}
					
					@ParameterizedTest ( name="[{index}] Label[{0}], Invalid Imm :: int" )
					@ArgumentsSource( ImmediateProvider.u_26Bit.Invalid.class )
					void Invalid_Imm_Int(String hexAddr, int Addr, String hexImm, int Imm) {
						assertNull( ValidateInstr.Jump_LabelOrInt( ""+Imm ) );
						expectedErrs.appendEx( -1, FMT_MSG.imm.notUnsigned26Bit( Imm ) );
					}
					@ParameterizedTest ( name="[{index}] Label[{0}],  Invalid Imm :: Hex" )
					@ArgumentsSource( ImmediateProvider.u_26Bit.Invalid.class )
					void Invalid_Imm_Hex(String hexAddr, int Addr, String hexImm, int Imm) {
						assertNull( ValidateInstr.Jump_LabelOrInt( hexImm) );
						expectedErrs.appendEx( -1, FMT_MSG.imm.notUnsigned26Bit( Imm ) );
					}
				}
				
			}
		}
		
	}
	
	@Nested
	@DisplayName ( "Test Sub Modules" )
	class testSubModules {
		@Test
		@DisplayName ( "Convert Invalid Register" )
		void validateConvertRegister ( ) {
			ValidateInstr.setLineNo( -1 );
			assertNull( ValidateInstr.convertRegister( "$f0", DataType.FLOATING_POINT ) );
			expectedErrs.appendEx( -1, FMT_MSG.reg.wrongData( "$f0" ) );
			
			assertNull( ValidateInstr.convertRegister( "$-40", DataType.NORMAL ) );
			expectedErrs.appendEx( -1, FMT_MSG.reg.notInRange( "$-40" ) );
			
			assertNull( ValidateInstr.convertRegister( "$50", DataType.NORMAL ) );
			expectedErrs.appendEx( -1, FMT_MSG.reg.notInRange( "$50" ) );
		}
		
		@ParameterizedTest ( name="NO_zeroWarning[{index}] on Read, Reg[{0}]" )
		@ArgumentsSource ( RegisterProvider.ZERO.class )
		void zeroWarning_Read (String regName) {
			//isValidLoadRegister
			assertNotNull( ValidateInstr.convertRegister( regName, DataType.NORMAL ) );
		}
		
		@ParameterizedTest ( name="zeroWarning[{index}] on Write, Reg[{0}]" )
		@ArgumentsSource ( RegisterProvider.ZERO.class )
		void zeroWarning_Write (String regName) {
			ValidateInstr.setLineNo( -1 );
			assertNotNull( ValidateInstr.convertWriteRegister( regName, DataType.NORMAL ) );
			testLogs.zeroWarning( -1, regName );
		}
		
	}
	
}
