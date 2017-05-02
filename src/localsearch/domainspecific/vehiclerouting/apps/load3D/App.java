package localsearch.domainspecific.vehiclerouting.apps.load3D;

import java.io.*;
import java.util.Iterator;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.util.*;

import localsearch.domainspecific.vehiclerouting.vrp.ConstraintSystemVR;
import localsearch.domainspecific.vehiclerouting.vrp.IFunctionVR;
import localsearch.domainspecific.vehiclerouting.vrp.VRManager;
import localsearch.domainspecific.vehiclerouting.vrp.VarRoutesVR;
import localsearch.domainspecific.vehiclerouting.vrp.entities.ArcWeightsManager;
import localsearch.domainspecific.vehiclerouting.vrp.entities.Point;
import localsearch.domainspecific.vehiclerouting.vrp.functions.TotalCostVR;
import localsearch.domainspecific.vehiclerouting.vrp.utils.googlemaps.GoogleMapsQuery;
import localsearch.domainspecific.vehiclerouting.vrp.utils.googlemaps.LatLng;
class Item{
	int w;
	int l;
	int h;
	String name;
	int quantity;
	
	public Item(int w, int l ,int h, int qtt, String name){
		this.w = w;
		this.l = l;
		this.h = h;
		this.quantity = qtt;
		this.name = name;
	}
}
class Request{
	public String addr;
	public LatLng latlng;
	public String orderID;
	public ArrayList<Item> items;
	
	public Request(String addr, String orderID, ArrayList<Item> items){
		this.addr = addr;
		this.orderID = orderID;
		this.items = items;
	}
}
public class App {

	ArrayList<Request> requests = null;
	GoogleMapsQuery G = new GoogleMapsQuery();
	double[][] distance;
	HashMap<String, Integer> mCode2Index = null; 
	
	// modelling
	VRManager mgr;
	VarRoutesVR XR;
	ConstraintSystemVR CS;
	ArcWeightsManager awm;
	IFunctionVR obj;
	int nbVehicles;
	ArrayList<Point> clientPoints;
	ArrayList<Point> startPoints;
	ArrayList<Point> endPoints;
	ArrayList<Point> allPoints;
	Point depot;
	double lat_depot = 21.028811;
	double lng_depot = 105.778229;
	
	public void mapping(int nbVehicles){
		this.nbVehicles = nbVehicles;
		clientPoints = new ArrayList<Point>();
		startPoints = new ArrayList<Point>();
		endPoints = new ArrayList<Point>();
		allPoints = new ArrayList<Point>();
		for(int i = 0; i < requests.size(); i++){
			Point p = new Point(i);
			clientPoints.add(p);
			allPoints.add(p);
		}
		
		int iddepot = requests.size();
		for(int k = 1; k <= nbVehicles; k++){
			Point s = new Point(iddepot);
			Point e = new Point(iddepot);
			startPoints.add(s);
			endPoints.add(e);
			allPoints.add(s);
			allPoints.add(e);
		}
		
		awm = new ArcWeightsManager(allPoints);
		for(int i = 0; i < requests.size(); i++){
			Point pi = clientPoints.get(i);
			Request req = requests.get(i);
			for(int j = 0; j < requests.size(); j++){
				Point pj = clientPoints.get(j);
				awm.setWeight(pi, pj, distance[i][j]);
			}
			for(Point s: startPoints){
				double d = G.getApproximateDistanceMeter(req.latlng.lat,req.latlng.lng, lat_depot,lng_depot);
				awm.setWeight(pi, s, d);
				awm.setWeight(s, pi, d);
			}
			for(Point e: endPoints){
				double d = G.getApproximateDistanceMeter(req.latlng.lat,req.latlng.lng, lat_depot,lng_depot);
				awm.setWeight(pi, e, d);
				awm.setWeight(e, pi, d);
			}
		}
		
	}
	public void solve(int nbVehicles){
		mapping(nbVehicles);
		stateModel();
		search();
	}
	
	public void search(){
		
	}
	public void stateModel(){
		mgr = new VRManager();
		XR = new VarRoutesVR(mgr);
		CS = new ConstraintSystemVR(mgr);
		for(int k = 1; k <= nbVehicles; k++){
			Point s = startPoints.get(k-1);
			Point e = endPoints.get(k-1);
			XR.addRoute(s, e);
		}
		for(Point p: clientPoints)
			XR.addClientPoint(p);
		
		obj = new TotalCostVR(XR, awm);
		
		mgr.close();
	}
	public void readData(String fn) {
		try {
			XSSFWorkbook wb = new XSSFWorkbook(
					new FileInputStream(new File(fn)));
			XSSFSheet sheet = wb.getSheetAt(0);
			Iterator rows = sheet.rowIterator();
			int idx
			= 0;
			
			requests = new ArrayList<Request>();
			mCode2Index = new HashMap<String, Integer>();
			
			while (rows.hasNext()) {
				XSSFRow row = (XSSFRow) rows.next();
				idx++;
				
				if(idx == 1)continue;
				
				Iterator cells = row.cellIterator();
				XSSFCell cell = (XSSFCell) cells.next();
				cell = (XSSFCell) cells.next();
				cell = (XSSFCell) cells.next();
				String addr = cell.getStringCellValue();
				
				cell = (XSSFCell)cells.next();
				
				cell = (XSSFCell)cells.next();
				String itemName = cell.getStringCellValue();
				
				cell = (XSSFCell)cells.next();
				String orderID = (int)cell.getNumericCellValue() + "";
				
				cell = (XSSFCell)cells.next();
				int qtt = (int)cell.getNumericCellValue();
				
				cell = (XSSFCell)cells.next();
				
				cell = (XSSFCell)cells.next();
				String sz = cell.getStringCellValue();
				
				
				cell = (XSSFCell)cells.next();
				int l = (int)cell.getNumericCellValue();
				cell = (XSSFCell)cells.next();
				int w = (int)cell.getNumericCellValue();
				cell = (XSSFCell)cells.next();
				int h = (int)cell.getNumericCellValue();
				
				System.out.println(idx + " :\t"  + addr + "\t" + itemName + "\t" + orderID + "\t" + qtt + "\t" + sz +
						", (" + w + "," + l + "," + h + ")");
			
				Request req = null;
				if(mCode2Index.get(orderID) == null){
					req = new Request(addr,orderID,new ArrayList<Item>());
					requests.add(req);
					mCode2Index.put(orderID, requests.size()-1);
					//LatLng ll = G.getCoordinate(req.addr);
					//req.latlng = ll;
				
				}
				
				Item I = new Item(w,l,h,qtt,itemName);
				req.items.add(I);
				
				if(idx==28)break;
				
			}
			
			
			// read coordinates
			sheet = wb.getSheetAt(3);
			rows = sheet.rowIterator();
			idx = 0;
			while(idx < 19){
				XSSFRow row = (XSSFRow) rows.next();
				Iterator cells = row.cellIterator();
				
				XSSFCell cell = (XSSFCell) cells.next();
				cell = (XSSFCell)cells.next();
				String addr = cell.getStringCellValue();
				System.out.println("addr = " + addr);
				
				cell = (XSSFCell)cells.next();
				String orderID = (int)cell.getNumericCellValue() + "";
				System.out.println("OrderID = " + orderID);
				cell = (XSSFCell)cells.next();
				String latlng = (String)cell.getStringCellValue();
				Request req = requests.get(mCode2Index.get(orderID));
				String[] s = latlng.split(",");
				double lat = Double.valueOf(s[0]);
				double lng = Double.valueOf(s[1]);
				req.latlng = new LatLng(lat,lng);
				
				idx++;
			}
			
			distance = new double[requests.size()][requests.size()];
			// read distances
			sheet = wb.getSheetAt(4);
			rows = sheet.rowIterator();
			idx = 0;
			for(int i = 1; i <= 171; i++){
				XSSFRow row = (XSSFRow) rows.next();
				Iterator cells = row.cellIterator();
				
				XSSFCell cell = (XSSFCell) cells.next();
				String orderID1 = (int)cell.getNumericCellValue() + "";
				
				cell = (XSSFCell)cells.next();
				String orderID2 = (int)cell.getNumericCellValue() + "";
				
				cell = (XSSFCell)cells.next();
				double d = cell.getNumericCellValue();
				
				System.out.println(orderID1 + "\t" + orderID2 + "\t" + d);
				int idx1 = mCode2Index.get(orderID1);
				int idx2 = mCode2Index.get(orderID2);
				
				distance[idx1][idx2] = d;
				distance[idx2][idx1] = d;
			}
			
			
			wb.close();
			
			for(Request req: requests){
				System.out.println("Req " + req.orderID + ", addr = " + req.addr + ", items = ");
				for(Item I: req.items) System.out.println(I.name + " : " + I.w + "," + I.l + "," + I.h + ", qtt = " + I.quantity );
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	
	public void queryDistance(String fn){
		GoogleMapsQuery G = new GoogleMapsQuery();
		try{
			PrintWriter out = new PrintWriter(fn);
			for(int i = 0; i < requests.size()-1; i++){
				Request ri = requests.get(i);
				for(int j = i+1; j < requests.size(); j++){
					Request rj = requests.get(j);
					double d = G.getApproximateDistanceMeter(ri.latlng.lat,ri.latlng.lng, rj.latlng.lat,rj.latlng.lng);
					
					//if(ri.latlng != null && rj.latlng != null)
					d = G.getDistance(ri.latlng.lat,ri.latlng.lng, rj.latlng.lat,rj.latlng.lng)*1000;
					if(d < 0)
						d = G.getApproximateDistanceMeter(ri.latlng.lat,ri.latlng.lng, rj.latlng.lat,rj.latlng.lng);
					out.println(ri.orderID + "\t" + rj.orderID + "\t" + d);
					System.out.println(i + "-" + j + "\t" + ri.orderID + "\t" + rj.orderID + "\t" + d);
				}
			}
			out.close();
		}catch(Exception ex){
			ex.printStackTrace();
		}
	}
	
	public void queryLatLng(String fn){
		try{
			PrintWriter out = new PrintWriter(fn);
			for(int i = 0; i < requests.size(); i++){
				Request r = requests.get(i);
				r.latlng = G.getCoordinate(r.addr);
				String latlng = "-";
				if(r.latlng  != null) latlng = r.latlng.lat + "," + r.latlng.lng;
				
				out.println(i + "\t" + r.addr + "\t" + r.orderID + "\t" + latlng);
				System.out.println(i + "\t" + r.addr + "\t" + r.orderID + "\t" + latlng);
			}
			out.close();
		}catch(Exception ex){
			ex.printStackTrace();
		}
	}
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		App A = new App();
		A.readData("FDC-T03-2017.xlsx");
		//A.queryLatLng("latlng.txt");
		//A.queryDistance("distances.txt");
	}

}
