package cabRouting;

import jsprit.analysis.toolbox.GraphStreamViewer;
import jsprit.analysis.toolbox.GraphStreamViewer.Label;
import jsprit.core.algorithm.VehicleRoutingAlgorithm;
import jsprit.core.algorithm.box.GreedySchrimpfFactory;
import jsprit.core.algorithm.box.Jsprit;
import jsprit.core.algorithm.box.SchrimpfFactory;
import jsprit.core.problem.Location;
import jsprit.core.problem.VehicleRoutingProblem;
import jsprit.core.problem.VehicleRoutingProblem.FleetSize;
import jsprit.core.problem.cost.VehicleRoutingTransportCosts;
import jsprit.core.problem.io.VrpXMLWriter;
import jsprit.core.problem.job.Job;
import jsprit.core.problem.job.Service;
import jsprit.core.problem.job.Shipment;
import jsprit.core.problem.solution.VehicleRoutingProblemSolution;
import jsprit.core.problem.solution.route.activity.TimeWindow;
import jsprit.core.problem.solution.route.VehicleRoute;
import jsprit.core.problem.solution.route.activity.TourActivity;
import jsprit.core.problem.solution.route.activity.TourActivity.JobActivity;
import jsprit.core.problem.vehicle.VehicleImpl;
import jsprit.core.problem.vehicle.VehicleImpl.Builder;
import jsprit.core.problem.vehicle.VehicleType;
import jsprit.core.problem.vehicle.VehicleTypeImpl;
import jsprit.core.util.Solutions;
import jsprit.core.util.VehicleRoutingTransportCostsMatrix;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Random;

class Pickup {
	String name;
	int serialNo;
	Zone zone;
	String id;
	String address;
}

class Drop {
	String name;
	int serialNo;
	Zone zone;
	String id;
	String address;
}

class Zone {
	int id;
	String name;
	double locationX;
	double locationY;
}

public class RunRouting {

	static int WEIGHT_INDEX = 0;

	static double COST_MULTIPLIER_X = 110;

	static double COST_MULTIPLIER_Y = 97;


	static double COST_6_SEATER_PER_KM;

	static double AVG_VELOCITY_CAR_PER_KM;

	static int CAPACITY;
	
	static ArrayList<VehicleImpl> vehicles8 = new ArrayList<VehicleImpl>();

	static String OFFICE_ADDRESS;

	
	static int NUM_VEHICLES;

	static int ALGORITHM_ACCURACY = 2;
	
	static HashMap<Integer, Zone> allzones;

	static double OFFICE_X = 28.507243 * COST_MULTIPLIER_X;

	static double OFFICE_Y = 77.0674481 * COST_MULTIPLIER_Y;

	
	
	static ArrayList<VehicleImpl> create8Seater() {
		
		VehicleTypeImpl.Builder vehicleTypeBuilder1 = VehicleTypeImpl.Builder.newInstance("vehicleType8").addCapacityDimension(WEIGHT_INDEX, CAPACITY);
		vehicleTypeBuilder1.setCostPerDistance(COST_6_SEATER_PER_KM);
		vehicleTypeBuilder1.setMaxVelocity(AVG_VELOCITY_CAR_PER_KM);
		VehicleType vehicleType1 = vehicleTypeBuilder1.build();
		
//		for (int k = 0; k <zones.size(); k++){
//		for (int k = 0; k <NUM_VEHICLES; k++){
			Builder vehicleBuilder1 = VehicleImpl.Builder.newInstance("vehicle8");
			vehicleBuilder1.setStartLocation(Location.newInstance(829));
			vehicleBuilder1.setType(vehicleType1);
			vehicleBuilder1.setReturnToDepot(false);
			VehicleImpl vehicle1 = vehicleBuilder1.build();
			vehicles8.add(vehicle1);
//		}
		return vehicles8;
	}

	
	
	static void createPickups(VehicleRoutingProblem.Builder vrpBuilder, String content2, String content1) throws IOException {
		
		JSONObject jsonResponse = new JSONObject(content1);
		JSONObject jsonResponse2 = new JSONObject(content2);
		
		CAPACITY = jsonResponse.getInt("capacity");
		COST_6_SEATER_PER_KM = jsonResponse.getDouble("cost_per_km");
		NUM_VEHICLES = jsonResponse.getInt("numvehicles");
		AVG_VELOCITY_CAR_PER_KM = jsonResponse.getDouble("avg_speed");
		
//		OFFICE_X = jsonResponse.getDouble("depot_lat") * COST_MULTIPLIER_X;
//		OFFICE_Y = jsonResponse.getDouble("depot_lng") * COST_MULTIPLIER_Y;
		
		OFFICE_ADDRESS = jsonResponse.getString("depot_address");
		
		
		VehicleRoutingTransportCostsMatrix.Builder costMatrixBuilder = VehicleRoutingTransportCostsMatrix.Builder.newInstance(true);
		JSONArray cost_array = jsonResponse2.getJSONArray("cost_matrix");
		
		for(int k =0; k<cost_array.length(); k++){	
			JSONObject cost_object = cost_array.getJSONObject(k);
			costMatrixBuilder.addTransportDistance(cost_object.getString("start"), cost_object.getString("end"), cost_object.getDouble("distance"));
			costMatrixBuilder.addTransportTime(cost_object.getString("start"), cost_object.getString("end"), cost_object.getInt("duration")/60);
		}
		
		VehicleRoutingTransportCosts costMatrix = costMatrixBuilder.build();
		
		vrpBuilder.setRoutingCost(costMatrix);
		
		JSONArray pickups_array = jsonResponse.getJSONArray("shipments_list");

		

		for (int i=0; i<pickups_array.length(); i++) {
			JSONObject pickup_object = pickups_array.getJSONObject(i);
			Pickup pickup = new Pickup();
			Drop drop = new Drop();
			pickup.serialNo = i;
			drop.serialNo = i;
			pickup.id = pickup_object.getString("order_id") +" - pickup";
			drop.id = pickup_object.getString("order_id") +" - drop";
			pickup.name = pickup_object.getString("pickup_name");
			drop.name = pickup_object.getString("drop_name");
			
			pickup.address = pickup_object.getString("pickup_address");
			drop.address = pickup_object.getString("drop_address");
			
			pickup.zone = new Zone();
			pickup.zone.id = pickup_object.getInt("pickup_address");
			pickup.zone = allzones.get(pickup.zone.id);
			
			drop.zone = new Zone();
			drop.zone.id = pickup_object.getInt("drop_address");
			drop.zone = allzones.get(drop.zone.id);
//			pickups.add(pickup);
			
//			pickup_names.add(pickup.name);

			
		    Shipment.Builder shipmentb = Shipment.Builder.newInstance(pickup_object.getString("order_id") + " - "+pickup.name+" - "+drop.name+", "+i);
			shipmentb.addSizeDimension(WEIGHT_INDEX, pickup_object.getInt("volume"));
			shipmentb.setPickupLocation(Location.newInstance(pickup.address));
			shipmentb.setDeliveryLocation(Location.newInstance(drop.address));
			shipmentb.setPickupServiceTime((double)pickup_object.getInt("pickup_duration"));
			shipmentb.setDeliveryServiceTime((double)pickup_object.getInt("drop_duration"));
			
			
/*			int pstime = (pickup_object.getInt("pickup_start_time")/100) - 8;
			int petime = (pickup_object.getInt("pickup_end_time")/100) - 8;
			int dstime = (pickup_object.getInt("drop_start_time")/100) - 8;
			int detime = (pickup_object.getInt("drop_end_time")/100) - 8;
			
			System.out.println(pstime+" - "+petime+" - "+dstime+" - "+detime);
			
			if(pstime <= 0 || petime <= 0 || dstime <= 0 || detime <= 0){
				continue;
			}*/
			
			
/*			TimeWindow PTimeWindow = new TimeWindow(60.0*pstime,60.0*petime);
			TimeWindow DTimeWindow = new TimeWindow(60.0*dstime,60.0*detime);*/
			
			TimeWindow PTimeWindow = new TimeWindow(pickup_object.getInt("pickup_start_time"),pickup_object.getInt("pickup_end_time"));
			TimeWindow DTimeWindow = new TimeWindow(pickup_object.getInt("drop_start_time"),pickup_object.getInt("drop_end_time"));
			
			shipmentb.setPickupTimeWindow(PTimeWindow);
			shipmentb.setDeliveryTimeWindow(DTimeWindow);
			
			Shipment shipment = shipmentb.build();
			vrpBuilder.addJob(shipment);

		}		
		
	}

	
	static void readZones(String content) throws IOException {
		
		JSONObject jsonResponse = new JSONObject(content);
		
		JSONArray zones_array = jsonResponse.getJSONArray("zone_list");
		
		allzones = new HashMap<Integer, Zone>(zones_array.length());
		for (int i=0; i<zones_array.length(); i++) {
			JSONObject entry = zones_array.getJSONObject(i);
			if (entry.get("lat") == null || entry.get("lng") == null)
				continue;
			Zone zone = new Zone();
			zone.id = entry.getInt("id");
			zone.name = entry.getString("name").toLowerCase();
			zone.locationX = entry.getDouble("lat") * COST_MULTIPLIER_X;
			zone.locationY = entry.getDouble("lng") * COST_MULTIPLIER_Y;
			allzones.put(entry.getInt("id"), zone);
//			create8Seater(zone.id);
		}
		
		
//		JSONObject office = jsonResponse.getJSONObject("office");
		
//		OFFICE_X = office.getDouble("lat") * COST_MULTIPLIER_X;
//		OFFICE_Y = office.getDouble("lng") * COST_MULTIPLIER_Y;
	}
	
	
	
	public static void main(String[] args) throws IOException {
		
		
		VehicleRoutingProblem.Builder vrpBuilder = VehicleRoutingProblem.Builder.newInstance();
		
		String content = readFile("input/Zones1.txt", StandardCharsets.UTF_8);
		readZones(content);
		
		String content2 = readFile("input/CostMatrix1.txt", StandardCharsets.UTF_8);
//		String content2 = readFile(args[1], StandardCharsets.UTF_8);
		String content1 = readFile("input/Passengers.txt", StandardCharsets.UTF_8);
//		String content1 = args[0];
		createPickups(vrpBuilder, content2, content1);
		
		create8Seater();
		
		System.out.println("Size: "+vehicles8.size());
		for (int u=0; u<vehicles8.size(); u++){
			vrpBuilder.addVehicle(vehicles8.get(u));
		}
		
		vrpBuilder.setFleetSize(FleetSize.INFINITE);
		
		
		

		VehicleRoutingProblem problem = vrpBuilder.build();

		
		VehicleRoutingAlgorithm algorithm;
		if (ALGORITHM_ACCURACY == 0)
			algorithm = new SchrimpfFactory().createAlgorithm(problem);
		else if (ALGORITHM_ACCURACY == 1)
			algorithm = new GreedySchrimpfFactory().createAlgorithm(problem);
		else
			algorithm = Jsprit.createAlgorithm(problem);
		
		algorithm.setMaxIterations(4096);

		
		Collection<VehicleRoutingProblemSolution> solutions = algorithm.searchSolutions();

		VehicleRoutingProblemSolution bestSolution = Solutions.bestOf(solutions);

		printSolution(problem, bestSolution);

		
	}


	
	static void printSolution(VehicleRoutingProblem problem, VehicleRoutingProblemSolution solution) {
		String leftAlgin = "%-3s | %-8s | %-5s | %-50s | %-50s | %-5s | %-5s | %-5s%n";
		// "routeNo | vehicleType | passengerNo | passengerName| passengerLocation | arrTime | endTime | costs%n"
		int routeNu = 1;
		
		JSONObject jobject = new JSONObject();
		
		JSONArray jarray = new JSONArray();
		
		for (VehicleRoute route : solution.getRoutes()) {
			
			JSONObject jobject0 = new JSONObject();
			jobject0.put("route_number", routeNu);
			JSONArray jarray0 = new JSONArray();
			
			double costs = 0;
			TourActivity prevAct = route.getStart();
			int pos = 1;
			for (TourActivity act : route.getActivities()) {
				String jobId = ((JobActivity) act).getJob().getId();
				
				JSONObject jobject1 = new JSONObject();
				jobject1.put("job_id", jobId);
//				jobject1.put("time_arr", START_TIME.plusMinutes(Math.round(act.getArrTime() / 5) * 5));
//				jobject1.put("time_dep", START_TIME.plusMinutes(Math.round(act.getEndTime() / 5) * 5));
				jobject1.put("pos", pos);
				
				jarray0.put(jobject1);
				
				double c = problem.getTransportCosts().getTransportCost(prevAct.getLocation(), act.getLocation(), prevAct.getEndTime(), route.getDriver(),
						route.getVehicle());
				c += problem.getActivityCosts().getActivityCost(act, act.getArrTime(), route.getDriver(), route.getVehicle());
				costs += c;
				
//				out.format(leftAlgin, routeNu, route.getVehicle().getId(), jobId[0], jobId[1], jobId[2], arrivalTime, departureTime, Math.round(costs));
				
				prevAct = act;
				pos++;
			}
			
			jobject0.put("array", jarray0);
			jarray.put(jobject0);
			
			double c = problem.getTransportCosts().getTransportCost(prevAct.getLocation(), route.getEnd().getLocation(), prevAct.getEndTime(),
					route.getDriver(), route.getVehicle());
			c += problem.getActivityCosts().getActivityCost(route.getEnd(), route.getEnd().getArrTime(), route.getDriver(), route.getVehicle());
			costs += c;
			routeNu++;
		}
		
		jobject.put("num_routes", solution.getRoutes().size());
		jobject.put("num_unassigned", solution.getUnassignedJobs().size());
		jobject.put("routes", jarray);
		
		Collection<Job> unassigned_jobs = solution.getUnassignedJobs();
		
		if (solution.getUnassignedJobs().size() != 0){
			JSONArray jarray_u = new JSONArray();
			for (Job job : unassigned_jobs){
				JSONObject jobject_u = new JSONObject();
				jobject_u.put("job_id", job.getId());
				jarray_u.put(jobject_u);
			}
			jobject.put("unassigned_array", jarray_u);
		}
		
		System.out.println(jobject.toString());
		
//		out.flush();
		
		
	}

	
	

	
	
	static String readFile(String path, Charset encoding) 
			  throws IOException 
			{
			  byte[] encoded = Files.readAllBytes(Paths.get(path));
			  return new String(encoded, encoding);
			}

}
