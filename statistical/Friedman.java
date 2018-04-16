package jmetal.experiments.util;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.*;

import jmetal.experiments.Experiment;

public class Friedman {

  public static void main(String[] args) {

    Vector algoritmos;
    Vector datasets;
    Vector datos;
    String cadena = "";
    StringTokenizer lineas, tokens;
    String linea, token;
    int i, j, k, m;
    int posicion;
    double mean[][];
    Pareja orden[][];
    Pareja rank[][];
    boolean encontrado;
    int ig;
    double sum;
    boolean visto[];
    Vector porVisitar;
    double Rj[];
    double friedman;
    double sumatoria=0;
    double termino1, termino2, termino3;
    double iman;
    double Qprima010[] = {0.0,1.645,1.960,2.128,2.242,2.327,2.394,2.450,2.498,2.540,2.576,2.609,2.639,2.666,2.690,2.713,2.735,2.755,2.733,2.791,2.807,2.823,2.838,2.852,2.866};
    double Qprima005[] = {0.0,1.960,2.242,2.394,2.498,2.576,2.639,2.690,2.735,2.773,2.807,2.838,2.866,2.891,2.914,2.936,2.955,2.974,2.992,3.008,3.024,3.038,3.052,3.066,3.078};
    double Qprima001[] = {0.0,2.576,2.807,2.936,3.024,3.091,3.144,3.189,3.227,3.261,3.291,3.317,3.342,3.364,3.384,3.403,3.421,3.437,3.453,3.467,3.481,3.494,3.506,3.518,3.529};
    double q010, q005, q001, CD010, CD005, CD001;
    boolean vistos[];
    int pos, tmp;
    double min;
    double maxVal;
    double rankingRef;
    double Pi[];
    double ALPHAiHolm[];
    double ALPHAiHolland[];
    double ALPHAiRom[];
    double ALPHAiShaffer[];
    String ordenAlgoritmos[];
    double ordenRankings[];
    int order[];
    double adjustedP[][];
    double Ci[];
    double SE;
    boolean parar, otro;
    Vector indices = new Vector();
    Vector exhaustiveI = new Vector();
    boolean[][] cuadro;
    double minPi, tmpPi, maxAPi,tmpAPi;
    Relation[] parejitas;
    int lineaN = 0;
    int columnaN = 0;
	Vector T;
	int Tarray[];

    if (args.length != 1) {
      System.err.println("Error. Hace falta un parámetro: Fichero de resultados en formato CSV.");
      System.exit(1);
    }

    algoritmos = new Vector();
    datasets = new Vector();
    datos = new Vector();


    /*Lectura del fichero de resultados*/
    cadena = Fichero.leeFichero(args[0]);
    lineas = new StringTokenizer (cadena,"\n\r");
    while (lineas.hasMoreTokens()) {
      linea = lineas.nextToken();
      tokens = new StringTokenizer(linea,",\t");
      columnaN = 0;
      while (tokens.hasMoreTokens()) {
        if (lineaN == 0) {
          if (columnaN == 0) {
            token = tokens.nextToken();
          } else {
            token = tokens.nextToken();
            algoritmos.add(new String(token));
            datos.add(new Vector());
          }
        } else {
          if (columnaN == 0) {
            token = tokens.nextToken();
            datasets.add(new String(token));
          } else {
            token = tokens.nextToken();
            ((Vector)datos.elementAt(columnaN-1)).add(new Double(token));
          }
        }
        columnaN++;
      }
      lineaN++;
    }

    mean = new double[datasets.size()][algoritmos.size()];

    /*Cálculo del valor medio de performance para cada algoritmo por dataset*/
    for (i=0; i<datasets.size(); i++) {
      for (j=0; j<algoritmos.size(); j++) {
        mean[i][j] = ((Double)((Vector)datos.elementAt(j)).elementAt(i)).doubleValue();
      }
    }

	    /*Usamos la estructura pareja para ordenar y calcular rankings*/
	    orden = new Pareja[datasets.size()][algoritmos.size()];
	    for (i=0; i<datasets.size(); i++) {
	    	for (j=0; j<algoritmos.size(); j++){
	    		orden[i][j] = new Pareja (j,mean[i][j]);
	    	}
	    	Arrays.sort(orden[i]);
	    }

	    /*Construimos la tabla de rankings por dataset y algoritmo*/
	    rank = new Pareja[datasets.size()][algoritmos.size()];
	    posicion = 0;
	    for (i=0; i<datasets.size(); i++) {
	    	for (j=0; j<algoritmos.size(); j++){
	    		encontrado = false;
	    		for (k=0; k<algoritmos.size() && !encontrado; k++) {
	    			if (orden[i][k].indice == j) {
	    				encontrado = true;
	    				posicion = k+1;
	    			}
	    		}
	    		rank[i][j] = new Pareja(posicion,orden[i][posicion-1].valor);
	    	}
	    }

	    /*Hay que igualar rankings en el caso de tener el mismo performance*/
	    for (i=0; i<datasets.size(); i++) {
	    	visto = new boolean[algoritmos.size()];
	    	porVisitar= new Vector();

	    	Arrays.fill(visto,false);
	    	for (j=0; j<algoritmos.size(); j++) {
		    	porVisitar.removeAllElements();
	    		sum = rank[i][j].indice;
	    		visto[j] = true;
	    		ig = 1;
	    		for (k=j+1;k<algoritmos.size();k++) {
	    			if (rank[i][j].valor == rank[i][k].valor && !visto[k]) {
	    				sum += rank[i][k].indice;
	    				ig++;
	    				porVisitar.add(new Integer(k));
	    				visto[k] = true;
	    			}
	    		}
	    		sum /= (double)ig;
	    		rank[i][j].indice = sum;
	    		for (k=0; k<porVisitar.size(); k++) {
	    			rank[i][((Integer)porVisitar.elementAt(k)).intValue()].indice = sum;
	    		}
	    	}
	    }

	    /*Calculamos el valor medio de ranking por algoritmo*/
	    Rj = new double[algoritmos.size()];
	    for (i=0; i<algoritmos.size(); i++){
	    	Rj[i] = 0;
	    	for (j=0; j<datasets.size(); j++) {
	    		Rj[i] += rank[j][i].indice / ((double)datasets.size());
	    	}
	    }

	    System.out.println("\\documentclass[a4paper,10pt]{article}\n" +
	    		"\\usepackage{graphicx}\n" +
	    		"\\usepackage{lscape}\n" +
	    		"\\title{Results}\n" +
	    		"\\author{}\n" +
	    		"\\date{\\today}\n" +
	    		"\\begin{document}\n" +
	    		"\\begin{landscape}\n" +
	    		"\\maketitle\n" +
	    		"\\section{Tables of Friedman, Bonferroni-Dunn, Holm, Hochberg, Hommel, Holland, Rom Tests}");

            /*Mostramos el valor medio de ranking de cada algoritmo*/
            System.out.println("\\begin{table}[!htp]\n" +
            "\\centering\n" +
            "\\caption{Average Rankings of the algorithms\n}" +
            "\\begin{tabular}{c|c}\n" +
            "Algorithm&Ranking\\\\\n\\hline");
            for (i=0; i<algoritmos.size();i++) {
              System.out.println((String)algoritmos.elementAt(i)+"&"+Rj[i]+"\\\\");
            }
            System.out.println("\\end{tabular}\n\\end{table}");

	    /*Cálculo del valor estadístico de Friedman*/
	    termino1 = (12*(double)datasets.size())/((double)algoritmos.size()*((double)algoritmos.size()+1));
	    termino2 = (double)algoritmos.size()*((double)algoritmos.size()+1)*((double)algoritmos.size()+1)/(4.0);
	    for (i=0; i<algoritmos.size();i++) {
	    	sumatoria += Rj[i]*Rj[i];
	    }
	    friedman = (sumatoria - termino2) * termino1;
	    System.out.println("\n\nFriedman statistic considering reduction performance (distributed according to chi-square with "+(algoritmos.size()-1)+" degrees of freedom: "+friedman+".\n\n");

	    /*Cálculo del valor estadístico de Iman and Davenport*/
	    iman = ((datasets.size()-1)*friedman)/(datasets.size()*(algoritmos.size()-1) - friedman);
	    System.out.println("Iman and Davenport statistic considering reduction performance (distributed according to F-distribution with "+(algoritmos.size()-1)+" and "+ (algoritmos.size()-1)*(datasets.size()-1) +" degrees of freedom: "+iman+".\n\n");

	    termino3 = Math.sqrt((double)algoritmos.size()*((double)algoritmos.size()+1)/(6.0*(double)datasets.size()));

	    q010 = Qprima010[algoritmos.size()-1];
	    q005 = Qprima005[algoritmos.size()-1];
	    q001 = Qprima001[algoritmos.size()-1];
	    
	    CD010 = q010*termino3;
	    CD005 = q005*termino3;
	    CD001 = q001*termino3;

	    System.out.println("Critical Difference (CD) value for Bonferroni-Dunn test considering $p=0.10$: "+CD010+".\n\n");
	    System.out.println("Critical Difference (CD) value for Bonferroni-Dunn test considering $p=0.05$: "+CD005+".\n\n");
	    System.out.println("Critical Difference (CD) value for Bonferroni-Dunn test considering $p=0.01$: "+CD001+".\n\n");
	    
	    /************ COMPARACIÓN CON UN MÉTODO CONTROL **************/	    

	    /*Calculamos el valor p_i no ajustado de cada comparación alpha=0.05*/
	    Pi = new double[algoritmos.size()-1];
	    ALPHAiHolm = new double[algoritmos.size()-1];
	    ALPHAiHolland = new double[algoritmos.size()-1];
	    ALPHAiRom = new double[algoritmos.size()-1];
	    ordenAlgoritmos = new String[algoritmos.size()-1];

	    calcularROM(0.05,ALPHAiRom);
	    SE = termino3;
	    vistos = new boolean[algoritmos.size()];
	    rankingRef = 0.0;
	    Arrays.fill(vistos,false);
	    for (i=0; i<algoritmos.size();i++) {
	    	for (j=0;vistos[j]==true;j++);
	    	pos = j;
	    	maxVal = Rj[j];
	    	for (j=j+1;j<algoritmos.size();j++) {
	    		if (i > 0) {
	    			if (vistos[j] == false && Rj[j] > maxVal) {
	    				pos = j;
	    				maxVal = Rj[j];
	    			}
	    		} else {
	    			if (vistos[j] == false && Rj[j] < maxVal) {
	    				pos = j;
	    				maxVal = Rj[j];
	    			}
	    		}
	    	}
	    	vistos[pos] = true;
	    	if (i==0) {
	    		rankingRef = maxVal;
                System.out.println("\\begin{table}[!htp]\n\\centering\\scriptsize\n\\caption{Holm / Hochberg / Holland / Rom Table for $\\alpha=0.05$}\n" +
               		"\\begin{tabular}{ccccccc}\n" +
               		"$i$&algorithm&$z=(R_0 - R_i)/SE$&$p$&Holm/Hochberg/Hommel&Holland&Rom\\\\\n\\hline");
	    	} else {
	    		ALPHAiHolm[i-1] = 0.05/((double)algoritmos.size()-(double)i);
	    		ALPHAiHolland[i-1] = 1.0 - Math.pow((1.0 - 0.05),(1.0/((double)algoritmos.size()-(double)i)));
	    		ordenAlgoritmos[i-1] = new String ((String)algoritmos.elementAt(pos));
	    		
                System.out.println((algoritmos.size()-i) + "&" + algoritmos.elementAt(pos) + "&" +
               		Math.abs((rankingRef-maxVal)/SE) + "&" +
               		2*CDF_Normal.normp((-1)*Math.abs((rankingRef-maxVal)/SE)) + 
               		"&" + ALPHAiHolm[i-1] +
               		"&" + ALPHAiHolland[i-1] + 
               		"&" + ALPHAiRom[i-1] + "\\\\");
           		Pi[i-1] = 2*CDF_Normal.normp((-1)*Math.abs((rankingRef-maxVal)/SE));
	    	}
	    }
	    System.out.println("\\hline\n" + "\\end{tabular}\n" + "\\end{table}");
        
	    
	    /*Cálculo de las hipótesis rechazadas por cada test*/
	    
        System.out.println("Bonferroni-Dunn's procedure rejects those hypotheses that have a p-value $\\le"+0.05/(double)(algoritmos.size()-1)+"$.\n\n");
	    
	    parar = false;
	    for (i=0; i<algoritmos.size()-1 && !parar; i++) {
	    	if (Pi[i] > ALPHAiHolm[i]) {	    		
	    		System.out.println("Holm's procedure rejects those hypotheses that have a p-value $\\le"+ALPHAiHolm[i]+"$.\n\n");
	    		parar = true;
	    	}
	    }

	    parar = false;
	    for (i=algoritmos.size()-2; i>=0 && !parar; i--) {
	    	if (Pi[i] <= ALPHAiHolm[i]) {	    		
	    		System.out.println("Hochberg's procedure rejects those hypotheses that have a p-value $\\le"+ALPHAiHolm[i]+"$.\n\n");
	    		parar = true;
	    	}
	    }

	    otro = true;
	    for (j=algoritmos.size()-1; j>0 && otro; j--) {
	    	otro = false;
	    	for (k=1; k<=j && !otro; k++) {
	    		if (Pi[algoritmos.size()-1-j+k-1] <= 0.05*(double)k/(double)j) {
	    			otro = true;
	    		}
	    	}
	    }
	    if (otro == true) {
	    	System.out.println("Hommel's procedure rejects all hypotheses.\n\n");
	    } else {
	    	j++;
	    	System.out.println("Hommel's procedure rejects those hypotheses that have a p-value $\\le"+0.05/(double)j+"$.\n\n");
	    }
	    
	    parar = false;
	    for (i=0; i<algoritmos.size()-1 && !parar; i++) {
	    	if (Pi[i] > ALPHAiHolland[i]) {	    		
	    		System.out.println("Holland's procedure rejects those hypotheses that have a p-value $\\le"+ALPHAiHolland[i]+"$.\n\n");
	    		parar = true;
	    	}
	    }

	    parar = false;
	    for (i=algoritmos.size()-2; i>=0 && !parar; i--) {
	    	if (Pi[i] <= ALPHAiRom[i]) {	    		
	    		System.out.println("Rom's procedure rejects those hypotheses that have a p-value $\\le"+ALPHAiRom[i]+"$.\n\n");
	    		parar = true;
	    	}
	    }	    
	    

	    /*Calculamos el valor p_i no ajustado de cada comparación alpha=0.10*/
	    
	    
	    Pi = new double[algoritmos.size()-1];
	    ALPHAiHolm = new double[algoritmos.size()-1];
	    ALPHAiHolland = new double[algoritmos.size()-1];
	    ALPHAiRom = new double[algoritmos.size()-1];

	    calcularROM(0.1,ALPHAiRom);
	    SE = termino3;
	    vistos = new boolean[algoritmos.size()];
	    rankingRef = 0.0;
	    Arrays.fill(vistos,false);
	    for (i=0; i<algoritmos.size();i++) {
	    	for (j=0;vistos[j]==true;j++);
	    	pos = j;
	    	maxVal = Rj[j];
	    	for (j=j+1;j<algoritmos.size();j++) {
	    		if (i > 0) {
	    			if (vistos[j] == false && Rj[j] > maxVal) {
	    				pos = j;
	    				maxVal = Rj[j];
	    			}
	    		} else {
	    			if (vistos[j] == false && Rj[j] < maxVal) {
	    				pos = j;
	    				maxVal = Rj[j];
	    			}
	    		}
	    	}
	    	vistos[pos] = true;
	    	if (i==0) {
	    		rankingRef = maxVal;
                System.out.println("\\begin{table}[!htp]\n\\centering\\scriptsize\n\\caption{Holm / Hochberg / Holland / Rom Table for $\\alpha=0.10$}\n" +
               		"\\begin{tabular}{ccccccc}\n" +
               		"$i$&algorithm&$z=(R_0 - R_i)/SE$&$p$&Holm/Hochberg/Hommel&Holland&Rom\\\\\n\\hline");
	    	} else {
	    		ALPHAiHolm[i-1] = 0.1/((double)algoritmos.size()-(double)i);
	    		ALPHAiHolland[i-1] = 1.0 - Math.pow((1.0 - 0.1),(1.0/((double)algoritmos.size()-(double)i)));
	    		
                System.out.println((algoritmos.size()-i) + "&" + algoritmos.elementAt(pos) + "&" +
               		Math.abs((rankingRef-maxVal)/SE) + "&" +
               		2*CDF_Normal.normp((-1)*Math.abs((rankingRef-maxVal)/SE)) + 
               		"&" + ALPHAiHolm[i-1] +
               		"&" + ALPHAiHolland[i-1] + 
               		"&" + ALPHAiRom[i-1] + "\\\\");
           		Pi[i-1] = 2*CDF_Normal.normp((-1)*Math.abs((rankingRef-maxVal)/SE));
	    	}
	    }
	    System.out.println("\\hline\n" + "\\end{tabular}\n" + "\\end{table}");
        
	    
	    /*Cálculo de las hipótesis rechazadas por cada test*/
	    
        System.out.println("Bonferroni-Dunn's procedure rejects those hypotheses that have a p-value $\\le"+0.1/(double)(algoritmos.size()-1)+"$.\n\n");
	    
	    parar = false;
	    for (i=0; i<algoritmos.size()-1 && !parar; i++) {
	    	if (Pi[i] > ALPHAiHolm[i]) {	    		
	    		System.out.println("Holm's procedure rejects those hypotheses that have a p-value $\\le"+ALPHAiHolm[i]+"$.\n\n");
	    		parar = true;
	    	}
	    }

	    parar = false;
	    for (i=algoritmos.size()-2; i>=0 && !parar; i--) {
	    	if (Pi[i] <= ALPHAiHolm[i]) {	    		
	    		System.out.println("Hochberg's procedure rejects those hypotheses that have a p-value $\\le"+ALPHAiHolm[i]+"$.\n\n");
	    		parar = true;
	    	}
	    }

	    otro = true;
	    for (j=algoritmos.size()-1; j>0 && otro; j--) {
	    	otro = false;
	    	for (k=1; k<=j && !otro; k++) {
	    		if (Pi[algoritmos.size()-1-j+k-1] <= 0.10*(double)k/(double)j) {
	    			otro = true;
	    		}
	    	}
	    }
	    if (otro == true) {
	    	System.out.println("Hommel's procedure rejects all hypotheses.\n\n");
	    } else {
	    	j++;
	    	System.out.println("Hommel's procedure rejects those hypotheses that have a p-value $\\le"+0.05/(double)j+"$.\n\n");
	    }
	    
	    parar = false;
	    for (i=0; i<algoritmos.size()-1 && !parar; i++) {
	    	if (Pi[i] > ALPHAiHolland[i]) {	    		
	    		System.out.println("Holland's procedure rejects those hypotheses that have a p-value $\\le"+ALPHAiHolland[i]+"$.\n\n");
	    		parar = true;
	    	}
	    }

	    parar = false;
	    for (i=algoritmos.size()-2; i>=0 && !parar; i--) {
	    	if (Pi[i] <= ALPHAiRom[i]) {	    		
	    		System.out.println("Rom's procedure rejects those hypotheses that have a p-value $\\le"+ALPHAiRom[i]+"$.\n\n");
	    		parar = true;
	    	}
	    }	      

	    /************ VALORES P AJUSTADOS CON MÉTODO CONTROL **************/	    

	    adjustedP = new double[algoritmos.size()-1][5];
	    for (i=0; i<adjustedP.length; i++) {
	    	adjustedP[i][0] = Pi[i] * (double)(algoritmos.size()-1);
	    	adjustedP[i][1] = Pi[i] * (((double)(algoritmos.size()-1))-i);
	    	adjustedP[i][2] = Pi[i] * (((double)(algoritmos.size()-1))-i);
	    	adjustedP[i][4] = 1.0 - Math.pow((1.0 - Pi[i]),((double)(algoritmos.size()-1))-i);
	    }
	    
	    for (i=1; i<adjustedP.length; i++) {
	    	if (adjustedP[i][1] < adjustedP[i-1][1])
	    		adjustedP[i][1] = adjustedP[i-1][1];
	    	if (adjustedP[i][4] < adjustedP[i-1][4])
	    		adjustedP[i][4] = adjustedP[i-1][4];
	    }
	    for (i=adjustedP.length-2; i>=0; i--) {
	    	if (adjustedP[i][2] > adjustedP[i+1][2])
	    		adjustedP[i][2] = adjustedP[i+1][2];
	    }
	    
	    /*Algoritmo que calcula los valores p ajustados para Hommel*/
	    Ci= new double[adjustedP.length+1];
	    for (i=0; i<adjustedP.length; i++) {
	    	adjustedP[i][3] = Pi[i];
	    }
	    for (m=adjustedP.length; m>1; m--) {	    	
	    	for (i=adjustedP.length; i> (adjustedP.length-m); i--) {
	    			Ci[i] = ((double)m*Pi[i-1])/((double)(m+i-adjustedP.length));
	    	}
	    	min = Double.POSITIVE_INFINITY;
	    	for (i=adjustedP.length; i> (adjustedP.length-m); i--) {
	    		if (Ci[i] < min)
	    			min = Ci[i];
	    	}
	    	for (i=adjustedP.length; i> (adjustedP.length-m); i--) {
	    		if (adjustedP[i-1][3] < min)
	    			adjustedP[i-1][3] = min;
	    	}
	    	for (i=1; i<=(adjustedP.length-m); i++) {
	    		Ci[i] = Math.min(min, (double)m * Pi[i-1]);
	    	}
	    	for (i=1; i<=(adjustedP.length-m); i++) {
	    		if (adjustedP[i-1][3] < Ci[i])
	    			adjustedP[i-1][3] = Ci[i];
	    	}
	    }
	    
        System.out.println("\\begin{table}[!htp]\n\\centering\\scriptsize\n\\caption{Adjusted $p$-values}\n" +
           		"\\begin{tabular}{cccccccc}\n" +
           		"i&algorithm&unadjusted $p$&$p_{Bonf}$&$p_{Holm}$&$p_{Hoch}$&$p_{Homm}$&$p_{Holl}$\\\\\n\\hline");
	    for (i=0; i<Pi.length; i++) {	    	
            System.out.println((i+1) + "&" + ordenAlgoritmos[i] + "&" + Pi[i] +
            		"&" + adjustedP[i][0] +
               		"&" + adjustedP[i][1] +
               		"&" + adjustedP[i][2] +
               		"&" + adjustedP[i][3] +
               		"&" + adjustedP[i][4] + "\\\\");
	    }
	    System.out.println("\\hline\n" + "\\end{tabular}\n" + "\\end{table}\n");
	    
	    
	    
	    /************ COMPARACIÓN DE TODOS CONTRA TODOS **************/	    

	    /*Calculamos el valor p_i no ajustado de cada comparación alpha=0.05*/
	    Pi = new double[(int)combinatoria(2,algoritmos.size())];
	    ALPHAiHolm = new double[(int)combinatoria(2,algoritmos.size())];
	    ALPHAiShaffer = new double[(int)combinatoria(2,algoritmos.size())];
	    ordenAlgoritmos = new String[(int)combinatoria(2,algoritmos.size())];
	    ordenRankings = new double[(int)combinatoria(2,algoritmos.size())];
	    order = new int[(int)combinatoria(2,algoritmos.size())];
	    parejitas = new Relation[(int)combinatoria(2,algoritmos.size())];
	    T = new Vector();
	    T = trueHShaffer(algoritmos.size());
	    Tarray = new int[T.size()];
	    for (i=0; i<T.size(); i++) {
	    	Tarray[i] = ((Integer)T.elementAt(i)).intValue();
	    }
	    Arrays.sort(Tarray);

	    SE = termino3;
	    vistos = new boolean[(int)combinatoria(2,algoritmos.size())];
	    for (i=0, k=0; i<algoritmos.size();i++) {
	    	for (j=i+1; j<algoritmos.size();j++,k++) {
	    		ordenRankings[k] = Math.abs(Rj[i] -Rj[j]);
	    		ordenAlgoritmos[k] = (String)algoritmos.elementAt(i) + " vs. " + (String)algoritmos.elementAt(j);
	    		parejitas[k] = new Relation(i,j);
	    	}
	    }
	    
	    Arrays.fill(vistos,false);
	    for (i=0; i<ordenRankings.length; i++) {
	    	for (j=0;vistos[j]==true;j++);
	    	pos = j;
	    	maxVal = ordenRankings[j];
	    	for (j=j+1;j<ordenRankings.length;j++) {
	    		if (vistos[j] == false && ordenRankings[j] > maxVal) {
	    			pos = j;
	    			maxVal = ordenRankings[j];
	    		}
	    	}
	    	vistos[pos] = true;
	    	order[i] = pos;
	    }
	    
	    /*Cálculo de los test con relaciones lógicas (Shaffer y Bergmann)*/
	    pos = 0;
	    tmp = Tarray.length-1;
	    for (i=0; i<order.length; i++) {
	    	Pi[i] = 2*CDF_Normal.normp((-1)*Math.abs((ordenRankings[order[i]])/SE));
	    	ALPHAiHolm[i] = 0.05/((double)order.length-(double)i);
	    	ALPHAiShaffer[i] = 0.05/((double)order.length-(double)Math.max(pos,i));
	    	if (i == pos && Pi[i] <= ALPHAiShaffer[i]) {
	    		tmp--;
	    		pos = (int)combinatoria(2,algoritmos.size()) - Tarray[tmp];
	    	}
	    }
	    
        System.out.println("\\begin{table}[!htp]\n\\centering\\scriptsize\n\\caption{Holm / Shaffer Table for $\\alpha=0.05$}\n" +
           		"\\begin{tabular}{cccccc}\n" +
           		"$i$&algorithms&$z=(R_0 - R_i)/SE$&$p$&Holm&Shaffer\\\\\n\\hline");
        
	    for (i=0; i<order.length; i++) {
            System.out.println((order.length-i) + "&" + ordenAlgoritmos[order[i]] + "&" +
               		Math.abs((ordenRankings[order[i]])/SE) + "&" +
               		Pi[i] + 
               		"&" + ALPHAiHolm[i] +
               		"&" + ALPHAiShaffer[i] + "\\\\");
	    }
	    System.out.println("\\hline\n" + "\\end{tabular}\n" + "\\end{table}");
        
	    
	    /*Cálculo de las hipótesis rechazadas por cada test*/
	    
        System.out.println("Nemenyi's procedure rejects those hypotheses that have a p-value $\\le"+0.05/(double)(order.length)+"$.\n\n");
	    
	    parar = false;
	    for (i=0; i<order.length && !parar; i++) {
	    	if (Pi[i] > ALPHAiHolm[i]) {	    		
	    		System.out.println("Holm's procedure rejects those hypotheses that have a p-value $\\le"+ALPHAiHolm[i]+"$.\n\n");
	    		parar = true;
	    	}
	    }

	    parar = false;
	    for (i=0; i<order.length && !parar; i++) {
	    	if (Pi[i] <= ALPHAiShaffer[i]) {	    		
	    		System.out.println("Shaffer's procedure rejects those hypotheses that have a p-value $\\le"+ALPHAiShaffer[i]+"$.\n\n");
	    		parar = true;
	    	}
	    }
	    
	    if (algoritmos.size() < 9) {
		    for (i=0; i<algoritmos.size(); i++) {
		    	indices.add(new Integer(i));
		    }	    	
	        exhaustiveI = obtainExhaustive(indices);
	        cuadro = new boolean[algoritmos.size()][algoritmos.size()];
	        for (i=0; i<algoritmos.size(); i++) {
	        	Arrays.fill(cuadro[i], false);
	        }
	        for (i=0; i<exhaustiveI.size(); i++) {	
        		minPi = 2*CDF_Normal.normp((-1)*Math.abs(Rj[((Relation)((Vector)exhaustiveI.elementAt(i)).elementAt(0)).i] - Rj[((Relation)((Vector)exhaustiveI.elementAt(i)).elementAt(0)).j])/SE);
	        	for (j=1; j<((Vector)exhaustiveI.elementAt(i)).size(); j++) {
	        		tmpPi = 2*CDF_Normal.normp((-1)*Math.abs(Rj[((Relation)((Vector)exhaustiveI.elementAt(i)).elementAt(j)).i] - Rj[((Relation)((Vector)exhaustiveI.elementAt(i)).elementAt(j)).j])/SE);
	        		if (tmpPi < minPi) {
	        			minPi = tmpPi;
	        		}
	        	}
	        	if (minPi > (0.05/((double)((Vector)exhaustiveI.elementAt(i)).size()))) {	        		
		        	for (j=0; j<((Vector)exhaustiveI.elementAt(i)).size(); j++) {
		        		cuadro[((Relation)((Vector)exhaustiveI.elementAt(i)).elementAt(j)).i][((Relation)((Vector)exhaustiveI.elementAt(i)).elementAt(j)).j] = true;
		        	}	        		
	        	}
	        }
			System.out.println("Bergmann's procedure rejects these hypotheses:\n\n");
			System.out.println("\\begin{itemize}\n\n");
			for (i=0; i<cuadro.length;i++) {
				for (j=i+1; j<cuadro.length;j++) {					
					if (cuadro[i][j] == false) {
						System.out.println("\\item "+algoritmos.elementAt(i)+" vs. "+algoritmos.elementAt(j));
					}
				}
			}
			System.out.println("\\end{itemize}\n\n");
	    }

	    
	    /*Calculamos el valor p_i no ajustado de cada comparación alpha=0.10*/
	    Pi = new double[(int)combinatoria(2,algoritmos.size())];
	    ALPHAiHolm = new double[(int)combinatoria(2,algoritmos.size())];
	    ALPHAiShaffer = new double[(int)combinatoria(2,algoritmos.size())];
	    ordenAlgoritmos = new String[(int)combinatoria(2,algoritmos.size())];
	    ordenRankings = new double[(int)combinatoria(2,algoritmos.size())];
	    order = new int[(int)combinatoria(2,algoritmos.size())];

	    SE = termino3;
	    vistos = new boolean[(int)combinatoria(2,algoritmos.size())];
	    for (i=0, k=0; i<algoritmos.size();i++) {
	    	for (j=i+1; j<algoritmos.size();j++,k++) {
	    		ordenRankings[k] = Math.abs(Rj[i] -Rj[j]);
	    		ordenAlgoritmos[k] = (String)algoritmos.elementAt(i) + " vs. " + (String)algoritmos.elementAt(j);
	    	}
	    }
	    
	    Arrays.fill(vistos,false);
	    for (i=0; i<ordenRankings.length; i++) {
	    	for (j=0;vistos[j]==true;j++);
	    	pos = j;
	    	maxVal = ordenRankings[j];
	    	for (j=j+1;j<ordenRankings.length;j++) {
	    		if (vistos[j] == false && ordenRankings[j] > maxVal) {
	    			pos = j;
	    			maxVal = ordenRankings[j];
	    		}
	    	}
	    	vistos[pos] = true;
	    	order[i] = pos;
	    }
	    
	    /*Cálculo de los test con relaciones lógicas (Shaffer y Bergmann)*/
	    pos = 0;	    
	    tmp = Tarray.length-1;
	    for (i=0; i<order.length; i++) {
	    	Pi[i] = 2*CDF_Normal.normp((-1)*Math.abs((ordenRankings[order[i]])/SE));
	    	ALPHAiHolm[i] = 0.1/((double)order.length-(double)i);
	    	ALPHAiShaffer[i] = 0.1/((double)order.length-(double)Math.max(pos,i));
	    	if (i == pos && Pi[i] <= ALPHAiShaffer[i]) {
	    		tmp--;
	    		pos = (int)combinatoria(2,algoritmos.size()) - Tarray[tmp];
	    	}
	    }
	    
        System.out.println("\\begin{table}[!htp]\n\\centering\\scriptsize\n\\caption{Holm / Shaffer Table for $\\alpha=0.10$}\n" +
           		"\\begin{tabular}{cccccc}\n" +
           		"$i$&algorithms&$z=(R_0 - R_i)/SE$&$p$&Holm&Shaffer\\\\\n\\hline");
        
	    for (i=0; i<order.length; i++) {
            System.out.println((order.length-i) + "&" + ordenAlgoritmos[order[i]] + "&" +
               		Math.abs((ordenRankings[order[i]])/SE) + "&" +
               		Pi[i] + 
               		"&" + ALPHAiHolm[i] +
               		"&" + ALPHAiShaffer[i] + "\\\\");
	    }
	    System.out.println("\\hline\n" + "\\end{tabular}\n" + "\\end{table}");
        
	    
	    /*Cálculo de las hipótesis rechazadas por cada test*/
	    
        System.out.println("Nemenyi's procedure rejects those hypotheses that have a p-value $\\le"+0.05/(double)(order.length)+"$.\n\n");
	    
	    parar = false;
	    for (i=0; i<order.length && !parar; i++) {
	    	if (Pi[i] > ALPHAiHolm[i]) {	    		
	    		System.out.println("Holm's procedure rejects those hypotheses that have a p-value $\\le"+ALPHAiHolm[i]+"$.\n\n");
	    		parar = true;
	    	}
	    }

	    parar = false;
	    for (i=0; i<order.length && !parar; i++) {
	    	if (Pi[i] <= ALPHAiShaffer[i]) {	    		
	    		System.out.println("Shaffer's procedure rejects those hypotheses that have a p-value $\\le"+ALPHAiShaffer[i]+"$.\n\n");
	    		parar = true;
	    	}
	    }
	    
	    if (algoritmos.size() < 9) {
	    	indices.removeAllElements();
		    for (i=0; i<algoritmos.size(); i++) {
		    	indices.add(new Integer(i));
		    }	    	
	        exhaustiveI = obtainExhaustive(indices);
	        cuadro = new boolean[algoritmos.size()][algoritmos.size()];
	        for (i=0; i<algoritmos.size(); i++) {
	        	Arrays.fill(cuadro[i], false);
	        }
	        for (i=0; i<exhaustiveI.size(); i++) {	
        		minPi = 2*CDF_Normal.normp((-1)*Math.abs(Rj[((Relation)((Vector)exhaustiveI.elementAt(i)).elementAt(0)).i] - Rj[((Relation)((Vector)exhaustiveI.elementAt(i)).elementAt(0)).j])/SE);
	        	for (j=1; j<((Vector)exhaustiveI.elementAt(i)).size(); j++) {
	        		tmpPi = 2*CDF_Normal.normp((-1)*Math.abs(Rj[((Relation)((Vector)exhaustiveI.elementAt(i)).elementAt(j)).i] - Rj[((Relation)((Vector)exhaustiveI.elementAt(i)).elementAt(j)).j])/SE);
	        		if (tmpPi < minPi) {
	        			minPi = tmpPi;
	        		}
	        	}
	        	if (minPi > 0.1/((double)((Vector)exhaustiveI.elementAt(i)).size())) {	        		
		        	for (j=0; j<((Vector)exhaustiveI.elementAt(i)).size(); j++) {
		        		cuadro[((Relation)((Vector)exhaustiveI.elementAt(i)).elementAt(j)).i][((Relation)((Vector)exhaustiveI.elementAt(i)).elementAt(j)).j] = true;
		        	}	        		
	        	}
	        }
			System.out.println("Bergmann's procedure rejects these hypotheses:\n\n");
			System.out.println("\\begin{itemize}\n\n");
			for (i=0; i<cuadro.length;i++) {
				for (j=i+1; j<cuadro.length;j++) {					
					if (cuadro[i][j] == false) {
						System.out.println("\\item "+algoritmos.elementAt(i)+" vs. "+algoritmos.elementAt(j));
					}
				}
			}
			System.out.println("\\end{itemize}\n\n");
	    }
	    
	    /************ VALORES P AJUSTADOS DE TODOS CONTRA TODOS **************/	    

	    adjustedP = new double[Pi.length][4];
	    pos = 0;
	    tmp = Tarray.length-1;
	    for (i=0; i<adjustedP.length; i++) {
	    	adjustedP[i][0] = Pi[i] * (double)(adjustedP.length);
	    	adjustedP[i][1] = Pi[i] * (double)(adjustedP.length-i);
	    	adjustedP[i][2] = Pi[i] * ((double)adjustedP.length-(double)Math.max(pos,i));
	    	if (i == pos) {
	    		tmp--;
	    		pos = (int)combinatoria(2,algoritmos.size()) - Tarray[tmp];
	    	}
	    	if (algoritmos.size() < 9) {
	    		maxAPi = Double.MIN_VALUE;
	    		minPi = Double.MAX_VALUE;
		        for (j=0; j<exhaustiveI.size(); j++) {
		        	if (exhaustiveI.elementAt(j).toString().contains(parejitas[order[i]].toString())) {
		        		minPi = 2*CDF_Normal.normp((-1)*Math.abs(Rj[((Relation)((Vector)exhaustiveI.elementAt(j)).elementAt(0)).i] - Rj[((Relation)((Vector)exhaustiveI.elementAt(j)).elementAt(0)).j])/SE);
			        	for (k=1; k<((Vector)exhaustiveI.elementAt(j)).size(); k++) {
			        		tmpPi = 2*CDF_Normal.normp((-1)*Math.abs(Rj[((Relation)((Vector)exhaustiveI.elementAt(j)).elementAt(k)).i] - Rj[((Relation)((Vector)exhaustiveI.elementAt(j)).elementAt(k)).j])/SE);
			        		if (tmpPi < minPi) {
			        			minPi = tmpPi;
			        		}
			        	}		        		
			        	tmpAPi = minPi * (double)(((Vector)exhaustiveI.elementAt(j)).size());
			        	if (tmpAPi > maxAPi) {
			        		maxAPi = tmpAPi;
			        	}			     
		        	}
		        }	    		
		    	adjustedP[i][3] = maxAPi;
	    	}
	    }
	    
	    for (i=1; i<adjustedP.length; i++) {
	    	if (adjustedP[i][1] < adjustedP[i-1][1])
	    		adjustedP[i][1] = adjustedP[i-1][1];
	    	if (adjustedP[i][2] < adjustedP[i-1][2])
	    		adjustedP[i][2] = adjustedP[i-1][2];
	    	if (adjustedP[i][3] < adjustedP[i-1][3])
	    		adjustedP[i][3] = adjustedP[i-1][3];
	    }
	    
        System.out.println("\\begin{table}[!htp]\n\\centering\\scriptsize\n\\caption{Adjusted $p$-values}\n" +
           		"\\begin{tabular}{cccccccc}\n" +
           		"i&hypothesis&unadjusted $p$&$p_{Neme}$&$p_{Holm}$&$p_{Shaf}$&$p_{Berg}$\\\\\n\\hline");
	    for (i=0; i<Pi.length; i++) {	    	
            System.out.println((i+1) + "&" + algoritmos.elementAt(parejitas[order[i]].i) + " vs ." + algoritmos.elementAt(parejitas[order[i]].j) + "&" + Pi[i] +
            		"&" + adjustedP[i][0] +
               		"&" + adjustedP[i][1] +
               		"&" + adjustedP[i][2] +
               		"&" + adjustedP[i][3] + "\\\\");
	    }
	    System.out.println("\\hline\n" + "\\end{tabular}\n" + "\\end{table}\n");
	    
	    System.out.println("\\end{landscape}\\end{document}");
	}
	
	public static void calcularROM(double alpha, double vector[]) {
		
		int i, j;
		int m;
		double suma1, suma2;
		
		m = vector.length;
		
		vector[m-1] = alpha;
		vector[m-2] = alpha/2.0;
		
		for (i=3; i<=m; i++) {
			suma1 = suma2 = 0;
			for (j=1;j<(i-1);j++) {
				suma1 += Math.pow(alpha, (double)j);
			}
			for (j=1;j<(i-2);j++){
				suma2 += combinatoria(j,i)*Math.pow(vector[m-j-1], (double)(i-j));
			}
			vector[m-i] = (suma1-suma2)/(double)i;
		}		
		
	}

	public static double combinatoria (int m, int n) {

		double result = 1;
		int i;
		
		if (n >= m) {
			for (i=1; i<=m; i++)
				result *= (double)(n-m+i)/(double)i;
		} else {
			result = 0;
		}
		return result;
	}
	
	public static Vector obtainExhaustive (Vector indices) {
		
		Vector result = new Vector();
		int i,j,k;
		String binario;
		boolean[] number = new boolean[indices.size()];
		Vector ind1, ind2;
		Vector set = new Vector();
		Vector res1, res2;
		Vector temp;
		Vector temp2;
		Vector temp3;

		ind1 = new Vector();
		ind2 = new Vector();
		temp = new Vector();
		temp2 = new Vector();
		temp3 = new Vector();
		
		for (i=0; i<indices.size();i++) {
			for (j=i+1; j<indices.size();j++) {
				set.addElement(new Relation(((Integer)indices.elementAt(i)).intValue(),((Integer)indices.elementAt(j)).intValue()));
			}
		}
		if (set.size()>0)
			result.addElement(set);
		
		for (i=1; i<(int)(Math.pow(2, indices.size()-1)); i++) {
			Arrays.fill(number, false);
			ind1.removeAllElements();
			ind2.removeAllElements();
			temp.removeAllElements();
			temp2.removeAllElements();
			temp3.removeAllElements();
			binario = Integer.toString(i, 2);
			for (k=0; k<number.length-binario.length();k++) {
				number[k] = false;
			}
			for (j=0; j<binario.length();j++,k++) {
				if (binario.charAt(j) == '1')
					number[k] = true;
			}
			for (j=0; j<number.length; j++) {
				if (number[j] == true) {
					ind1.addElement(new Integer(((Integer)indices.elementAt(j)).intValue()));					
				} else {					
					ind2.addElement(new Integer(((Integer)indices.elementAt(j)).intValue()));					
				}
			}
			res1 = obtainExhaustive (ind1);
			res2 = obtainExhaustive (ind2);
			for (j=0; j<res1.size();j++) {
				result.addElement(new Vector((Vector)res1.elementAt(j)));
			}
			for (j=0; j<res2.size();j++) {
				result.addElement(new Vector((Vector)res2.elementAt(j)));
			}
			for (j=0; j<res1.size();j++) {
				temp = (Vector)((Vector)res1.elementAt(j)).clone();
				for (k=0; k<res2.size();k++) {
					temp2 = (Vector)temp.clone();
					temp3 = (Vector)((Vector)res2.elementAt(k)).clone();
					if (((Relation)temp2.elementAt(0)).i < ((Relation)temp3.elementAt(0)).i) {
						temp2.addAll((Vector)temp3);					
						result.addElement(new Vector(temp2));						
					} else {
						temp3.addAll((Vector)temp2);					
						result.addElement(new Vector(temp3));					
						
					}
				}
			} 
		}
		for (i=0;i<result.size();i++) {
			if (((Vector)result.elementAt(i)).toString().equalsIgnoreCase("[]")) {
				result.removeElementAt(i);
				i--;
			}
		}
		for (i=0;i<result.size();i++) {
			for (j=i+1; j<result.size(); j++) {	
				if (((Vector)result.elementAt(i)).toString().equalsIgnoreCase(((Vector)result.elementAt(j)).toString())) {
					result.removeElementAt(j);
					j--;
				}
			}
		}
		return result;		
	}

	public static Vector trueHShaffer (int k) {
		
		Vector number;
		int j;
		Vector tmp, tmp2;
		int p;
		
		number = new Vector();
		tmp = new Vector();
		if (k <= 1) {			
			number.addElement(new Integer(0));	
		} else {
			for (j=1; j<=k; j++) {
				tmp = trueHShaffer (k-j);
				tmp2 = new Vector();
				for (p=0; p<tmp.size(); p++) {
					tmp2.addElement(((Integer)(tmp.elementAt(p))).intValue() + (int)combinatoria(2,j));
				}
				number = unionVectores (number,tmp2);
			}
		}
		
		return number;
	}
	
	public static Vector unionVectores (Vector a, Vector b) {

		int i;
		
		for (i=0; i<b.size(); i++) {
			if (a.contains(new Integer((Integer)(b.elementAt(i)))) == false) {
				a.addElement(b.elementAt(i));
			}			
		}
		
		return a;		
	}

}

