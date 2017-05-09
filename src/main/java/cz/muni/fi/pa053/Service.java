package cz.muni.fi.pa053;
 
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

	private static final String WEATHER_API_KEY = "1a916200fcdebd243b41054ad54dc6c7";
	private static final Logger LOG = LoggerFactory.getLogger(Service.class);


	@GET
	@Path("/service")
	@Produces(MediaType.APPLICATION_XML)
	public String getMsg(@QueryParam("query") String query) throws Exception {
		LOG.info("The input param is: '" + query + "'");
		Double result = null;
		if(query.matches("[A-Z]{3}")){ //airport code with three letters
			JSONObject airport = new JSONObject(getHTML("http://www.airport-data.com/api/ap_info.json?iata=" + query));
			LOG.info("Airport information is:\n" + airport.toString(3));
			if(airport.get("icao") != JSONObject.NULL){ // if airport exist
				result = ((Number)getTemperatureOfAirport(airport)).doubleValue();
			}else{
				result = getValueOfStock(query);
			}
		}else if(query.matches("[A-Z]{1,4}")){ //STOCK
			result = getValueOfStock(query);
		}else{ //math expression otherwise
			Expression e = new ExpressionBuilder(query)
					.build();
			if(e.validate().isValid()){ //if expression is valid
				Double evaluated = e.evaluate();
				LOG.info("The result of " + query + " is: " + evaluated);
				result = evaluated;
			}
		}
		if(result!=null){
			LOG.info("Result for query " + query + " is: " + result);
			return "<result>" + result + "</result>";
		}else{
			LOG.error("This query is not supported: " + query);
			return "<result></result>";
		}
	}

	private String getHTML(String urlToRead) throws Exception {
		LOG.info("Get url is: " + urlToRead);
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
		JSONObject weather = new JSONObject(getHTML("http://api.openweathermap.org/data/2.5/weather?lat=" + latitude
						+ "&lon=" + longitude + "&appid=" + WEATHER_API_KEY
						+ "&units=metric"));
		LOG.info("Weather information is:\n" + weather.toString(3));
		return ((JSONObject) weather.get("main")).get("temp");
	}

	private Double getValueOfStock(String stockSymbol) throws IOException {
		Double result = null;
		Stock stock = YahooFinance.get(stockSymbol);
		LOG.info("Info about stock" + stock.toString());
		BigDecimal tempResult =  stock.getQuote().getPrice();
		if(tempResult != null){
			result = tempResult.doubleValue();
		}
		return result;
	}
}