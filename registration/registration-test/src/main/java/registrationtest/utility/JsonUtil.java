package registrationtest.utility;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import registrationtest.runapplication.StartApplication;

import java.io.IOException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
public class JsonUtil {
	private static final Logger logger = LogManager.getLogger(JsonUtil.class); 
	
	private  static ObjectMapper mapper;

	static
	{
		mapper=new ObjectMapper();
	}

	public static String convertJavaToJson(Object object)
	{

		String jsonResult="";
		try {
			jsonResult=mapper.writeValueAsString(object);
		} catch (JsonParseException e) {
			e.printStackTrace();
		}
		catch (JsonMappingException e) {
			e.printStackTrace();
		}catch (IOException e) {
			e.printStackTrace();
		}
		return jsonResult;
	}

	public static <T> T convertJsonintoJava(String jsonString,Class<T> cls)
	{
		T payload =null;
		try {
			payload=mapper.readValue(jsonString,cls);
		} catch (JsonParseException e) {
			e.printStackTrace();
		}
		catch (JsonMappingException e) {
			e.printStackTrace();
		}catch (IOException e) {
			e.printStackTrace();
		}
		return  payload;
	}

/**
 * For Simple Type
 * @param json
 * @param jsonObjName
 * @param idfield
 * @param lang
 * @return
 * @throws Exception
 */
//	public static String JsonObjParsing1(JSONObject json,String jsonObjName,String idfield,String lang) throws Exception {
//		String value =null;   
//
//		JSONObject identity = json.getJSONObject(jsonObjName);
//
//
//		JSONArray identityitems = identity.getJSONArray(idfield);
//		for (Object o : identityitems) {
//			JSONObject idItem = (JSONObject) o;
//
//			String language = idItem.getString("language");
//			if(language.equals(lang))
//				value = idItem.getString("value");
//
//		}
//
//		return value; 
//	}


	      
	  	public static String JsonObjSimpleParsing(String jsonIdentity,String idfield) throws Exception {
			String value =null;   
			JSONObject json = new JSONObject(jsonIdentity); 
			
			JSONObject identity = json.getJSONObject(PropertiesUtil.getKeyValue("jsonObjName"));


			JSONArray identityitems = identity.getJSONArray(idfield);
			
			 for (int i = 0, size = identityitems.length(); i < size; i++)
			    {
				 JSONObject idItem = identityitems.getJSONObject(i);
			      

				String language = idItem.getString("language");
				if(language.equals(PropertiesUtil.getKeyValue("lang")))
					value = idItem.getString("value");

			}

			return value; 
		}

/**
 * Direct String
 * @param json
 * @param jsonObjName
 * @param idfield
 * @return
 * @throws Exception
 */
	public static String JsonObjParsing(String jsonIdentity,String idfield) throws Exception {
		String value =null; 
		JSONObject json = new JSONObject(jsonIdentity); 
		JSONObject identity = json.getJSONObject(PropertiesUtil.getKeyValue("jsonObjName"));

		value = identity.getString(idfield);


		return value; 
	}


	public static double JsonObjIntParsing(String jsonIdentity,String idfield) throws Exception {
		double value =0; 
		JSONObject json = new JSONObject(jsonIdentity); 
		JSONObject identity = json.getJSONObject(PropertiesUtil.getKeyValue("jsonObjName"));

		value = identity.getDouble(idfield);


		return value; 
	}





}
