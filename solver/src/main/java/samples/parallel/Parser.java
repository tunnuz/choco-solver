package samples.parallel; /**
 *  Copyright (c) 1999-2011, Ecole des Mines de Nantes
 *  All rights reserved.
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are met:
 *
 *      * Redistributions of source code must retain the above copyright
 *        notice, this list of conditions and the following disclaimer.
 *      * Redistributions in binary form must reproduce the above copyright
 *        notice, this list of conditions and the following disclaimer in the
 *        documentation and/or other materials provided with the distribution.
 *      * Neither the name of the Ecole des Mines de Nantes nor the
 *        names of its contributors may be used to endorse or promote products
 *        derived from this software without specific prior written permission.
 *
 *  THIS SOFTWARE IS PROVIDED BY THE REGENTS AND CONTRIBUTORS ``AS IS'' AND ANY
 *  EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL THE REGENTS AND CONTRIBUTORS BE LIABLE FOR ANY
 *  DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 *  (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *  LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 *  ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import java.io.*;

/**
 * Parse and solve an symmetric Traveling Salesman Problem instance of the TSPLIB
 */
public class Parser {

	//***********************************************************************************
	// PARSING
	//***********************************************************************************

	public static int[][] parseInstance(String url) {
		File file = new File(url);
		try {
			BufferedReader buf = new BufferedReader(new FileReader(file));
			String line = buf.readLine();
			String name = line.split(":")[1].replaceAll(" ", "");
			System.out.println("parsing instance " + name + "...");
			while(!line.contains("DIMENSION")){
				line = buf.readLine();
			}
			int n = Integer.parseInt(line.split(":")[1].replaceAll(" ", ""));
			System.out.println("n : "+n);
			int[][] dist = new int[n][n];
			//
			while(!line.contains("EDGE_WEIGHT_TYPE")){
				line = buf.readLine();
			}
			String type = line.split(": ")[1];
			if(type.contains("EXPLICIT")){
				while(!line.contains("EDGE_WEIGHT_FORMAT")){
					line = buf.readLine();
				}
				String format = line.split(": ")[1];
				while(!line.contains("EDGE_WEIGHT_SECTION")){
					line = buf.readLine();
				}
				if(format.contains("UPPER_ROW")){
					halfMatrix(dist,buf);
				}else if(format.contains("FULL_MATRIX")){
					fullMatrix(dist,buf);
				}else if(format.contains("LOWER_DIAG_ROW")){
					lowerDiagMatrix(dist,buf);
				}else if(format.contains("UPPER_DIAG_ROW")){
					upperDiagMatrix(dist,buf);
				}else{
					return null;
				}
			}else if(type.contains("CEIL_2D")||type.contains("EUC_2D")||type.contains("ATT")||type.contains("GEO")){
				while(!line.contains("NODE_COORD_SECTION")){
					line = buf.readLine();
				}
				coordinates(dist,buf,type);
			}else{
				throw new UnsupportedOperationException();
			}
			return dist;
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}
		return null;
	}
	private static void coordinates(int[][] dist, BufferedReader buf, String type) throws IOException {
		int n = dist.length;
		String line;
		double[] x = new double[n];
		double[] y = new double[n];
		line = buf.readLine();
		String[] lineNumbers;
		for(int i=0;i<n;i++){
			line = line.replaceAll(" * ", " ");
			lineNumbers = line.split(" ");
			if(lineNumbers.length!=4 && lineNumbers.length!=3){
				System.out.println("wrong line "+line);
				throw new UnsupportedOperationException("wrong format");
			}
			x[i] = Double.parseDouble(lineNumbers[lineNumbers.length-2]);
			y[i] = Double.parseDouble(lineNumbers[lineNumbers.length-1]);
			line = buf.readLine();
		}
		if(!line.contains("EOF")){
//			throw new UnsupportedOperationException();
		}
		for(int i=0;i<n;i++){
			for(int j=i+1;j<n;j++){
				dist[i][j] = getDist(x[i],x[j],y[i],y[j],type);
				dist[j][i] = dist[i][j];
			}
		}
	}
	private static int getDist(double x1, double x2, double y1, double y2, String type) {
		double xd = x2-x1;
		double yd = y2-y1;
		if(type.contains("CEIL_2D")){
			double rt = Math.sqrt((xd*xd+yd*yd));
			return (int)Math.ceil(rt);
		}
		if(type.contains("EUC_2D")){
			double rt = Math.sqrt((xd*xd+yd*yd));
			return (int)Math.round(rt);
		}
		if(type.contains("ATT")){
			double rt = Math.sqrt((xd*xd+yd*yd)/10);
			int it = (int)Math.round(rt);
			if(it<rt){
				it++;
			}
			return it;
		}
		if(type.contains("GEO")){
			double PI = 3.141592;
			double min;
			int deg;
			// i
			deg = (int) x1;
			min = x1 - deg;
			double lati = PI*(deg+(5.0*min)/3.0)/180.0;
			deg = (int) y1;
			min = y1 - deg;
			double longi = PI*(deg+(5.0*min)/3.0)/180.0;
			// j
			deg = (int) x2;
			min = x2 - deg;
			double latj = PI*(deg+(5.0*min)/3.0)/180.0;
			deg = (int) y2;
			min = y2 - deg;
			double longj = PI*(deg+(5.0*min)/3.0)/180.0;

			double RRR = 6378.388;
			double q1 = Math.cos(longi-longj);
			double q2 = Math.cos(lati-latj);
			double q3 = Math.cos(lati+latj);

			double dij = RRR*Math.acos(((1+q1)*q2-(1-q1)*q3)/2)+1;
			return (int) dij;
		}
		throw new UnsupportedOperationException("wrong format");
	}
	private static void halfMatrix(int[][] dist, BufferedReader buf) throws IOException {
		int n = dist.length;
		String line;
		line = buf.readLine();
		String[] lineNumbers;
		for(int i=0;i<n-1;i++){
			line = line.replaceAll(" * ", " ");
			lineNumbers = line.split(" ");
			int off = 0;
			if(lineNumbers[0].equals("")){
				off++;
			}
			for(int k=0;k<lineNumbers.length-off;k++){
				dist[i][i+k+1] = Integer.parseInt(lineNumbers[k+off]);
				dist[i+k+1][i] = dist[i][i+k+1];
			}
			line = buf.readLine();
		}
	}
	private static void fullMatrix(int[][] dist, BufferedReader buf) throws IOException {
		int n = dist.length;
		String line;
		line = buf.readLine();
		String[] lineNumbers;
		for(int i=0;i<n;i++){
			line = line.replaceAll(" * ", " ");
			lineNumbers = line.split(" ");
			int off = 0;
			if(lineNumbers[0].equals("")){
				off++;
			}
			for(int k=0;k<n;k++){
				dist[i][k] = Integer.parseInt(lineNumbers[k+off]);
			}
			line = buf.readLine();
		}
	}
	private static void lowerDiagMatrix(int[][] dist, BufferedReader buf) throws IOException {
		int n = dist.length;
		String line;
		line = buf.readLine();
		String[] lineNumbers;
		int l=0,c=0;
		while(true){
			line = line.replaceAll(" * ", " ");
			lineNumbers = line.split(" ");
			int off = 0;
			if(lineNumbers[0].equals("")){
				off++;
			}
			for(int i=off;i<lineNumbers.length;i++){
				dist[l][c] = Integer.parseInt(lineNumbers[i]);
				dist[c][l] = dist[l][c];
				c++;
				if(c>l){
					c=0;
					l++;
				}
				if(l==dist.length){
					return;
				}
			}
			line = buf.readLine();
		}
	}
	private static void upperDiagMatrix(int[][] dist, BufferedReader buf) throws IOException {
		int n = dist.length;
		String line;
		line = buf.readLine();
		String[] lineNumbers;
		int l=0,c=0;
		while(true){
			line = line.replaceAll(" * ", " ");
			lineNumbers = line.split(" ");
			int off = 0;
			if(lineNumbers[0].equals("")){
				off++;
			}
			for(int i=off;i<lineNumbers.length;i++){
				dist[l][c] = Integer.parseInt(lineNumbers[i]);
				dist[c][l] = dist[l][c];
				c++;
				if(c>=n){
					l++;
					c=l;
				}
				if(l==dist.length){
					return;
				}
			}
			line = buf.readLine();
		}
	}
	
	//***********************************************************************************
	// OPTIMUM VALUE
	//***********************************************************************************

	public static int getOpt(String s) {
		File file = new File("/Users/jfages07/github/In4Ga/ALL_tsp/bestSols.csv");
		try {
			BufferedReader buf = new BufferedReader(new FileReader(file));
			String line = buf.readLine();
			while(!line.contains(s)){
				line = buf.readLine();
			}
			return Integer.parseInt(line.split(";")[1]);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}
		return 0;
	}

	//***********************************************************************************
	// RECORDING RESULTS
	//***********************************************************************************

	public static void writeTextInto(String text, String file) {
		try {
			FileWriter out = new FileWriter(file, true);
			out.write(text);
			out.flush();
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void clearFile(String file) {
		try {
			FileWriter out = new FileWriter(file, false);
			out.write("");
			out.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}