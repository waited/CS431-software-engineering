package utils;

public class utils {
	
	public static Integer[] timeConverter(String time){
		Integer timeInInt[] = new Integer[2];
		
		try{
		int Hour = Integer.valueOf(time.substring(0, time.indexOf(":")));
		int Min = Integer.valueOf(time.substring(time.indexOf(":")+1));
		timeInInt[0] = Hour;
		timeInInt[1] = Min;
		}  catch(StringIndexOutOfBoundsException SIOOBE) {
			System.err.println("Wrong user input detected in utils. Error 01, Please follow the format of example");
			return null;
		} catch(NumberFormatException NFE) {
			System.err.println("Wrong user input detedted in utils. Error 02, Please enter Integer Number with the format.");
			return null;
		}
		return timeInInt;
	}
}
