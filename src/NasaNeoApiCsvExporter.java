import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.json.JSONObject;
import org.json.JSONArray;


public class NasaNeoApiCsvExporter {
	public static void main(String[] args) {
		String apiUrl = "https://api.nasa.gov/neo/rest/v1/feed?" +
				"start_date=2024-11-25&" +
				"end_date=2024-12-02&" +
				"api_key=DEMO_KEY";
		try {
			HttpClient client = HttpClient.newBuilder()
					.version(HttpClient.Version.HTTP_2)
					.connectTimeout(Duration.ofSeconds(10))
					.build();
			
			HttpRequest request = HttpRequest.newBuilder()
					.uri(URI.create(apiUrl))
					.GET()
					.build();
			
			HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
			
			if (response.statusCode() == 200) {
				parseAndExportToCsv(response.body());
			} else {
				System.out.println("Error: HTTP " + response.statusCode());
				System.out.println("Response Body:" + response.body());
			}
			
		} catch (IOException e) {
			System.err.println("IO Error: " + e.getMessage());
			e.printStackTrace();
		} catch (InterruptedException e) {
			System.err.println("Request Interrupted: " + e.getMessage());
			e.printStackTrace();
		}
	}
		
		private static void parseAndExportToCsv(String jsonResponse) {
			try {
				JSONObject jsonObject = new JSONObject(jsonResponse);
				
				try (FileWriter csvWriter = new FileWriter("neo_data.csv")) {
					csvWriter.append("Date,Neo ID,Name,Is Potentially Hazardous,Absolute Magnitude,Min Diameter (km),Max Diameter (km),Relative Velocity (km/s),Miss Distance (km)\n");
					
					for (String date : jsonObject.getJSONObject("near_earth_objects").keySet()) {
						JSONArray dailyNeos = jsonObject.getJSONObject("near_earth_objects").getJSONArray(date);
						
						for (int i = 0; i < dailyNeos.length(); i++) {
							JSONObject neo = dailyNeos.getJSONObject(i);
							
							String neoId = neo.getString("neo_reference_id");
							String name = neo.getString("name");
							boolean isPotentiallyHazardous = neo.getBoolean("is_potentially_hazardous_asteroid");
							
	                        JSONObject estimatedDiameter = neo.getJSONObject("estimated_diameter");
	                        double minDiameter = estimatedDiameter.getJSONObject("kilometers").getDouble("estimated_diameter_min");
	                        double maxDiameter = estimatedDiameter.getJSONObject("kilometers").getDouble("estimated_diameter_max");
	                        
	                        JSONObject closeApproach = neo.getJSONArray("close_approach_data").getJSONObject(0);
	                        double relativeVelocity = closeApproach.getJSONObject("relative_velocity").getDouble("kilometers_per_second");
	                        double missDistance = closeApproach.getJSONObject("miss_distance").getDouble("kilometers");

	                        String csvLine = String.format("%s,%s,%s,%b,%.2f,%.2f,%.2f,%.2f,%.2f\n", 
	                            date, 
	                            neoId, 
	                            name.replace(",", ""),
	                            isPotentiallyHazardous,
	                            neo.getDouble("absolute_magnitude_h"),
	                            minDiameter,
	                            maxDiameter,
	                            relativeVelocity,
	                            missDistance
	                        );
	                        
	                        csvWriter.append(csvLine);
						}
					}
					
					System.out.println("NEO data exported to csv");
				}
			} catch (IOException e) {
				System.err.println("Error writing to CSV: " + e.getMessage());
				e.printStackTrace();
			}
	}
	
}
