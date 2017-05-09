package cz.muni.fi.pa053;
 
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import yahoofinance.Stock;
import yahoofinance.YahooFinance;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;

@Path("/")
public class Service {

	private static String WEATHER_API_KEY = "1a916200fcdebd243b41054ad54dc6c7";

	@GET
	@Path("/service")
	@Produces(MediaType.APPLICATION_JSON)
	public String getMsg(@QueryParam("query") String msg) throws Exception {
		JSONObject result = new JSONObject();
		if(msg.matches("[A-Z]{3}")){ //airport code with three letters
			JSONObject airport = (JSONObject) new JSONParser()
					.parse(getHTML("http://www.airport-data.com/api/ap_info.json?iata=" + msg));
			System.out.println("Airport information is:\n" + parseJsonToConsole(airport));
			if(airport.get("icao")!=null){ // if airport exist
				result.put("result",getTemperatureOfAirport(airport));
			}else{
				result.put("result",getValueOfStock(msg));
			}
		}else if(msg.matches("[A-Z]{1,4}")){ //STOCK
			result.put("result",getValueOfStock(msg));
		}else if(msg.matches("([-+]?[0-9]*\\.?[0-9]+[\\/\\+\\-\\*])+([-+]?[0-9]*\\.?[0-9]+)")){ //math
			result.put("result",executeExpression(msg));
		}
		return result.toJSONString();
	}

	private String getHTML(String urlToRead) throws Exception {
		System.out.println("Get url is: " + urlToRead);
		StringBuilder result = new StringBuilder();
		URL url = new URL(urlToRead);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setRequestMethod("GET");
		BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
		String line;
		while ((line = rd.readLine()) != null) {
			result.append(line);
		}
		rd.close();
		return result.toString();
	}

	private Object getTemperatureOfAirport(JSONObject airportInfo) throws Exception {
		String longitude = (String) airportInfo.get("longitude");
		String latitude = (String) airportInfo.get("latitude");
		JSONObject weather = (JSONObject) new JSONParser()
				.parse(getHTML("http://api.openweathermap.org/data/2.5/weather?lat=" + latitude
						+ "&lon=" + longitude + "&appid=" + WEATHER_API_KEY
						+ "&units=metric"));
		System.out.println("Weather information is:\n" + parseJsonToConsole(weather));
		return ((JSONObject) weather.get("main")).get("temp");
	}

	private double getValueOfStock(String stockSymbol) throws IOException {
		Stock stock = YahooFinance.get(stockSymbol);
		System.out.println("Info about stock" + stock.toString());
		BigDecimal price = stock.getQuote().getPrice();
		return price.doubleValue();
	}

	private double executeExpression(String expression){
		Expression e = new ExpressionBuilder(expression)
				.build();
		Double evaluated = e.evaluate();
		System.out.println("The result of " + expression + " is: " + evaluated);
		return evaluated;
	}

	private String parseJsonToConsole(JSONObject json){
		return json.toJSONString();
	}
}