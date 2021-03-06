package util.validation;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import util.Convert;
import util.logs.ErrorLog;
import util.logs.WarningsLog;

import java.util.List;

/**
 Provides validation for data types. And wrappers of {@link Convert}'s methods with validation performed.
 <p>
 <b>Expects all String parameters to be lowercase ! {@link String#toLowerCase()}</b>
 <p>
 Adds appropriate messages to {@link ErrorLog} {@link WarningsLog}.
 <p>
 After using any method, you should check errLog {@link ErrorLog#hasEntries()},
 and exit the application after printing the errLog's contents.
 
 @see Convert
 @see ErrorLog
 @see WarningsLog */
public class Validate{
	//TODO make this auto generate based on {@link DataType} - perhaps a HashMap?
	private static final List<String> SUPPORTED_DATATYPE_CSV = List.of( ".word" );
	private static final List<String>  SUPPORTED_DIRECTIVES_CSV = List.of(".data", ".text", ".code");
	//TODO refactor to Enum? or, Loop Up Table ?
	
	private final ErrorLog errLog;
	
	public Validate(ErrorLog errLog){
		this.errLog = errLog;
	}
	public static boolean isDataType(@NotNull String dataType){
		return (SUPPORTED_DATATYPE_CSV.contains(dataType));
	}
	
	/**
	 If not valid, adds to the {@link #errLog}.
	 <p>	see README for list of supported directives. and DataTypes.
	 
	 @see ErrorLog
	 */
	public static boolean isValidDirective(int lineNo, @NotNull String directive, @NotNull ErrorLog errorLog){
		boolean rtn = (SUPPORTED_DIRECTIVES_CSV.contains(directive) || isDataType(directive));
		
		if (!rtn)
			errorLog.append("LineNo: "+lineNo+"\tDirective: \""+directive+"\" Not Supported!");
		
		return rtn;
	}
	
	
	/**
	 If not valid, adds to the {@link #errLog}. If Null, does nothing.
	 <p>	see README for valid label definition.
	 
	 @see ErrorLog
	 */
	@Nullable
	public static String isValidLabel(int lineNo, @Nullable String label, @NotNull ErrorLog errorLog){
		if (label!=null) {
			if (label.matches("[_a-z][_.\\-a-z\\d]*"))
				return label;
			
			errorLog.append("LineNo: "+lineNo+"\tLabel: \""+label+"\" Not Supported!");
		}
		return null;
	}
	
	/**Wrapper for {@link #isValidDirective(int, String, ErrorLog)}*/
	public boolean isValidDirective(int lineNo, @NotNull String directive){
		return isValidDirective( lineNo, directive, this.errLog );
	}
	
	/**Wrapper for {@link #isValidLabel(int, String, ErrorLog)}*/
	public String isValidLabel(int lineNo, @NotNull String label){
		return isValidLabel( lineNo, label, this.errLog );
	}
}
