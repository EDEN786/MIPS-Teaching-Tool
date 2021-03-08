## Feedback

You may suggest new features/give feedback by leaving a comment on an issue.

------

> | Users should always be careful before running software downloaded from GitHub. |
> | :----------------------------------------------------------: |
> | This software is covered by the [MIT License](https://github.com/EDEN786/MIPS-Teaching-Tool/blob/Development/LICENSE) exempting the author from liability. |
> | Please read and understand the License before continuing to run this software on your personal machine. If you are uncomfortable running this software on your personal machine but would still like to provide feedback, contact a member of the research team, to arrange something. |


### Arbitrary limits @see( [**Known_Inaccuracies**](#Known-Inaccuracies) )

****

> These may change in future releases after doing more testing.

- `MAX_FILE_LENGTH = 512` (Lines)

- `MAX_INS_COUNT = 256` (Instructions)  *upto 257 instructions including auto Exit.

- `MAX_DATA_SEGMENTS = 256`	(arbitrary limit for testing purposes)

MEMORY DATA Segments are DoubleWord addressable (multiple of 8) to facilitate and simplify future double-precision float support.

	This makes the last supported addressable block of memory 0x100107F8

WORDS are all presumed to be signed (32bit) integers. This is to match Java's built in Integer data type.

Support for more data types (single-precision float, doubleWords, half-words, bytes) could be implemented in the future. But complete support of the MIPS ISA is not main focus of the application.

- [***Supported Instructions*** ](#Supported-Instructions)
- [***Operands Format***](#Operands-Format)
- [***Operands***](#Operands)
- [***Registers***](#Registers)
- [***Labels***](#Labels)

# Standard use:

	All input is made lowercase when parsed. This means ($ZERO, $Zero, $zero) are all treated the same.

## Supported Instructions:

> Format: **[OpCode]**\<Whitespace\>**[Operands]**

- R_type
  - **ADD**	`Operands_R`
  - **SUB**	`Operands_R`
- I_type
  - **ADDI**	`Operands_I-1`
  - **LW** 	`Operands_I-L` ; (pseudo)
  - **SW**	`Operands_I-L` ; (pseudo)

> Planned - **LA**	`Operands_I-L`	;  (pseudo) loads address to register

- J_type
  - **J** 	`P_Direct_Address`, `Label_Address`
  - **JAL** 	`P_Direct_Address`,`Label_Address`
  	- Register $ra is overwritten
- Other
  - **HALT**	; No Operands
  - **EXIT**	; No Operands - same as HALT

**Pseudo instructions are not accurately executed**

	- At the moment the assembler does not replace Pseudo instructions with REAL instructions, Keep this in mind when writing code.

## Operands Format:

<,\s*> - mean comma ',' followed by 0 or more whitespace '\s*'.

 [@See **Supported Address Segments**](#Supported-Address-Segments)

| Operands Type:        | Operand 1                  | <,\s*>    | Operand 2             | <,\s*>    | Operand 3               |
| --------------------- | -------------------------- | --------- | --------------------- | --------- | ----------------------- |
| **Operands_R:**       | **[Destination_Register]** |           | **[Source_Register]** |           | **[Third_Register]**    |
| **Operands_I-1**:    | **[Third_Register]**       |           | **[Source_Register]** |           | _[Immediate]_           |
| **Operands_I-2:**     | **[Third_Register]**       |           | _[Offset]_            | _**N/A**_ | (**[Source_Register]**) |
| **Operands_I-3:**     | **[Third_Register]**       |           | _[Immediate]_         | _**N/A**_ | _**N/A**_               |
| **Operands_I-L:**     | **[Third_Register]**       |           | [Label]               | _**N/A**_ | _**N/A**_               |
| **P_Direct_Address:** | _[Address]_                | _**N/A**_ | _**N/A**_             | _**N/A**_ | _**N/A**_               |
| **Label_Address**    | [Label]                    | _**N/A**_ | _**N/A**_             | _**N/A**_ | _**N/A**_               |

## Operands:

> [Destination_Register]/[Source_Register]/[Third_Register]

 - _**[Third_Register]**_ $rt can be the Destination or Source register for I_type instructions.

 - _**[Immediate]**_ Must be a valid signed 16bit integer.

 - _**[Offset]**_ is an _[Immediate]_ value. ∴ Must also be a valid signed 16bit integer.

	- Offsets used in branches are multiplied by 4, to force them to align with instruction addresses.

	- This is not done for load/store, as in other implementations memory is byte addressable.

		- It is upto the user to ensure offsets used with Load/Store are doubleWord (8bytes) addressable.

		- Meaning the Offset+$RS_Val is a multiple of 8.

 - _**[Address]**_ Must be a valid unsigned 28bit integer.

- _Labels_ are Strings, which reference an address. - they are converted into [Immediate] values at assembly. [@see Labels](#Labels)
   - for `LA` the address is not checked.
       - At execution, the address loaded into a register, MUST be valid for the instruction.

## Registers:

| Implemented   | Number |    Name    | Purpose                                        | Preserved*[2] |
| ------------- | :----: | :--------: | ---------------------------------------------- | ------------- |
| **Yes**\*[0]  |   0    |    ZERO    | Always equal to zero;                          | _**N/A**_     |
| NO \*[1]      |   1    |     AT     | Assembler temporary; used by the assembler     | NO            |
| **Yes** \*[2] |  2-3   |   V0-V1    | Return value from a function call              | NO            |
| **Yes** \*[2] |  4-7   |   A0-A3    | First four parameters for a function call      | NO            |
| **Yes**       |  8-15  |   T0-T7    | Temporary variables;                           | NO            |
| **Yes**       | 16-23  |   S0-S7    | Function variables;                            | **YES**       |
| **Yes**       | 24-25  |   T8-T9    | Two more temporary variables                   | NO            |
| NO            | 26-27  |   K0-K1    | Kernel use registers;                          | NO            |
| NO            |   28   |     GP     | Global pointer                                 | **YES**       |
| NO \*[2]      |   29   |     SP     | Stack pointer                                  | **YES**       |
| NO \*[3]      |   30   | ~~FP/~~ S8 | ~~Stack frame pointer or~~ subroutine variable | **YES**       |
| **Yes** \*[2] |   31   |     RA     | Return address of the last subroutine call     | **YES**       |

	 - [0] Using this register as a destination register, is effectively a "nop". - Warnings are issued in parsing phase.
	 - [1] Used by assembler to recode pseudo instructions into actual ones. Avoid using yourself.
	    - pseudo instructions not currently broken down - hence not used by assembler in current build.
	 - [2] Function Calls not tested/ Implemented, Therefor StackPointer disabled until this is tested.
	 - [3] FramePointer not implemented, reference this register as $s8 if used named referencing.
		- "FP" name for referencing not supported atm.

Registers can be referenced by name (e.g. $s2, $t0, $zero) or R_Number (e.g. $r18, $r8).

	Altough it is prefered register references start with a '$', This is not strictly required.

## Labels:

Labels must start with a letter (a-z/A-Z) (The application is case-insensitive), or Underscore '_'

 - periods,hyphens and underscores '.','-','_' may be used in a label name for readability.

 - no other symbols can be used, and spaces are not allowed. 

 - Label references are parsed for errors after instructions are parsed.

In the place of a label operand for an instruction a Hexadecimal address may be written instead, or the address as a decimal number.

 - Hexadecimal values must start with "0x".

> Where a label is used as an operand it must not contain the colon ':' !

### Supported Address Segments:

Labels are converted into addresses at assembly after all the code has been parsed.

Jump and Branch instructions can reference Labels directed to instruction address space.

Load and Store instructions can reference Labels directed to data address space.

`Code segments 0x00400000 to 0x00500000 in steps of 4, 2^18 valid segments`

`Data segments 0x10010000 to 0x100107F8 in steps of 8, 2^8 valid segments`

Only 256 (2^8) instructions supported, (which is a maximum address of 0x004003E8).

Jumping to this gap, where the address is supported, but can't possibly contain an instruction. 
 The Application acts as if no Exit instruction was ran.

And An Exit ('halt') instruction will automatically be ran next. With a warning.

###### MIPS Register addressing:

	Allows a full 32bit address to be loaded into a register.
	Then jump instructions / Load&Store use the address stored in the register.
	
	This introduced a runtime address hazard not detectable by the parser.
	The value of the register is determined at runtime.
	In the Execution Phase the address read from the instruction can be checked to be valid.

###### MIPS Base addressing: - Base+Offset

I_Type

- Offset(base) - Valid Data Address - Offset Addressing.

Label

- 	Branch - Converted to Offset.
- 	Load/Store - Pseudo Instruction -Direct Address (will be adjusted in future build)

###### MIPS Direct addressing:

J_Type

- Address - Valid Code Address - Pseudo Direct Addressing.
- Label directed to a code address. e.g. "main",

## Branch Delay Slots:

The current build executes a very basic model and Jump instructions update the PC in the same cycle (instead of being delayed). This means jumps are taken instantly.

>  **! This behaviour is expected to change in the next build!**



# End of User Manual



## Error/Warning messages:

### Valid File Checks:

	Check - File Exists
	Check - File is accessible (not being used by another resource)
	Check - File Length
	
	If any of these fails, the application will terminate.
		(future versions may allow selecting a new file).

## Parsing & Assembly:

> **Whitespace is trimmed, and case is converted to lowercase.**
> Parser checks file contains no syntactical errors.

	Comments:	Segment of the line after a pound'#' or semi-colon';' symbol.
	Labels:		Segment of the line before a colon':' symbol. {can start with a underscore_ or letter}
	
	Directives: Segment of the line before space seperator, Starts with a period '.'
	Values: 	Segment of the line after space seperator. Can contain a 'colon', or decimal point. and be comma seperated list.
	
	OpCode:		Segment of the line before space seperator, That Does Not! Start with a period '.'
	OpCode:		Segment of the line after space seperator.
		Registers:	usually start with a $, but don't have to.
		Immediates:	continuous stream of digits.	- atm, only Integer is supported.
			Hex Immediates: differentiated by "0x" sign at the beginning.
			
		Labels:	Same as above labels, but without the colon ":" at the end.
	
	
	From this the Parser builds a model:
	Directive					(.data, .text, .code)
		\
		Comments				(comments can begin with a pound sigh '#', or semicolon ';')
		   \					(comments are not used in any way by the application)
			\					
			Labels				(Labels must end with a colon ':')
				\
				Sub_Directive	(.word //future support for .double planned)
				|	\
				|	Values		(single int, int:range, int_array)
				|	
				OpCode			(see list of supported instructions)
					\
					Opperands	(depends on instruction type^)
						\
						Extra
				- Any extra characters past the last opperand, but not part of the comments
					- will break the formatting, and it will think the operands are invalid
	
	No Instructions are read:
		And Error is Reported at Assembly.
	
	Over 256 Instructions are read:
		A Warning will be issued And no further instructions will be parsed.
		
	If no EXIT/'halt' instruction is read:
		A Warning will be issued. And one will automatically be appended to the end.

Parser will not stop after an error is thrown. It will attempt to parse the remainder of the lines checking for additional errors.

> **However - It will fail assembly and not allow execution.**



### Assembly

During Assembly the real addresses labels point to is calculated.

Labels point to the next valid data/instruction.

> This can cause a scenario where a tag was intended to point to data on the same line.
>
> ​	But due to an error with the data formatting it is not recognised.
>
> ​	Then the label will incorrectly be attached to the next instruction.

Therefore users should fix Parsing Errors, before Assembly Errors.



## MIPS Addresses Segments:

```
*  Code range (.text) 0x00400000 to 0x004FFFFF (4194304 to 5242879)
*  
*  Global Data (.data) 0x10010000 to 0x1003FFFF (268500992 to 268697599)
*  Heap data 0x10040000 to 0x1FFFFFFF (268697600 to 536870911)
*  Stack data 0x70000000 to 0x7FFFFFFF (1879048192 to 2147483647)
* 
*  In powers:
*      0x00400000:(2^22)       >= Code     <0x00500000:(2^22 +2^20)
*      0x10010000:(2^28 +2^16) >= Global   <0x10040000:(2^28 +2^18)
*      0x10040000:(2^28 +2^18) >= Heap     <0x20000000:(2^29)	- Not Supported
*      0x70000000:(2^31-2^28)  >= Stack    <0x80000000:(2^31)	- Not Supported
```

	.data segment usually has a size of 49152 word address (2^15+2^14). Addresses 0x10010000 to 0x1003FFFC.
		As double words this becomes 24576 doubleword address (2^14+2^13). Addresses 0x10010000 to 0x1003FFF8.

_Hex notation 0xXXXXXXXX (8 digits = 32bit Address)_

# Known Inaccuracies

The application currently does not support the CoProcessors.

Meaning it does not support Traps (Exceptions) or Functional Units.

# InProgress

Branches are being added first.

Then, Functional Unit support is planned.

Then, Scoreboard DLX architecture.