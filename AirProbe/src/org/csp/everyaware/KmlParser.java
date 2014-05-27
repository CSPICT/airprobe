/**
 * AirProbe
 * Air quality application for Android, developed as part of 
 * EveryAware project (<http://www.everyaware.eu>).
 *
 * Copyright (C) 2014 CSP Innovazione nelle ICT. All rights reserved.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * For any inquiry please write to <devel@csp.it>
 * 
 * CONTRIBUTORS
 * 
 * This program was made with the contribution of:
 *   Fabio Saracino <fabio.saracino@csp.it>
 *   Patrick Facco <patrick.facco@csp.it>
 * 
 * 
 * SOURCE CODE
 * 
 *  The source code of this program is available at
 *  <https://github.com/CSPICT/airprobe>
 */

package org.csp.everyaware;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.csp.everyaware.db.MapCluster;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import android.util.Log;

public class KmlParser 
{
	//given a xml string, returns a xml document (DOM)
    public Document getDomElement(String xml)
    {
        Document doc = null;
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        
        try 
        { 
            DocumentBuilder db = dbf.newDocumentBuilder();
 
            InputSource is = new InputSource();
            is.setCharacterStream(new StringReader(xml));
            doc = db.parse(is); 
        } 
        catch (ParserConfigurationException e) 
        {
        	Log.e("Error: ", e.getMessage());
            return null;
        } 
        catch (SAXException e) 
        {
        	Log.e("Error: ", e.getMessage());
            return null;
        } 
        catch (IOException e) 
        {
        	Log.e("Error: ", e.getMessage());
            return null;
        }
        
        // return DOM
        return doc;
    }
    
    //legge una InputStream un byte alla volta e ottiene i tag di interesse, costruendo la struttura dati appropriata
    public List<MapCluster> parseKml(InputStream is)
    {
		byte[] buffer = new byte[1];
		byte[] actualTag = new byte[2048];
		byte[] tagContent = new byte[2048];
		
		byte[] startTag = {'<'};
		byte[] endTag = {'>'};
		//byte[] backSlash = {'/'};

		byte[] boundingBoxStart = {'<','D', 'a', 't', 'a', ' ', 'n', 'a', 'm', 'e', '=', '\"', 'b', 'o', 'u', 'n', 'd', 'i', 'n', 'g', 'B', 'o', 'x', '\"', '>'};
		byte[] boundingBoxEnd = {'<', '/', 'D', 'a', 't', 'a', '>'};
		
		byte[] bcSensorStart = {'<','D', 'a', 't', 'a', ' ', 'n', 'a', 'm', 'e', '=', '\"', 'b', 'c', 'S', 'e', 'n', 's', 'o', 'r', '\"', '>'};
		
		boolean openTag = false; //vale true quandro incontro '<'
		boolean boundingBoxFound = false; //vale true quando incontro il tag '<Data name="boundingBox">'
		boolean bcSensorFound = false; //vale true quando incontro il tag '<Data name="bcSensor">'
		
		int actualTagIndex = 0; //indice di avanzamento su actualTag
		int tagContentIndex = 0;  //indice di avanzamento su tagContent
		
		int counterBbStart = 0; //tiene traccia del numero di tag '<Data name="boundingBox">' aperti
		int counterBbEnd = 0; //tiene traccia del numero di tag '</Data>' che chiudono '<Data name="boundingBox">'
		
		int counterBcSStart = 0; //tiene traccia del numero di tag '<Data name="bcSensor">' aperti
		int counterBcSEnd = 0; //tiene traccia del numero di tag '</Data>' che chiudono '<Data name="bcSensor">'
		
		double bcLevel = 0;
		double minLat = 0;
		double minLon = 0;
		double maxLat = 0;
		double maxLon = 0;		
		List<MapCluster>mapClusters = new ArrayList<MapCluster>();
		
		try
		{
			while(is.read(buffer) != -1)
			{				
				//se il byte attuale corrisponde al carattere '<', allora sono all'inizio di un nuovo tag
				if(buffer[0] == startTag[0])
				{
					actualTag[actualTagIndex] = buffer[0]; //copio '<' nell'array actualTag che conterrà tutto il tag '<...>'
					openTag = true;
				}
				//altrimenti se il tag risulta già aperto (in un ciclo precedente ho incontrato '<')
				else if(openTag)
				{
					actualTagIndex++; //incremento l'indice su actualTag, in modo che il nuovo carattere venga copiato in posizione successiva alla precedente								
					actualTag[actualTagIndex] = buffer[0]; //copio alla posizione index attuale il carattere attuale					
					
					/************** TAG RILEVATO INTERAMENTE **************/
						
					//se il carattere attuale è uguale a '>', l'array actualTag contiene un tag intero '<...>'  e verifico se corrisponde al
					//tag '<Data name="boundingBox">' (--> tag rilevato interamente, ma devo capire quale tag è)
					if(buffer[0] == endTag[0]) 							
					{
						openTag = false; //ho rilevato '>' quindi il tag è chiuso
							
						/************ RILEVAMENTO TAG '<Data name="boundingBox"> ***************/
						
						//se il secondo carattere di actualTag è una 'D' e il terzo è una 'a', allora ho trovato un tag del tipo '<Data name=...>' 
						//e devo verificare che sia un tag '<Data name="boundingBox">'
						if((actualTag[1] == boundingBoxStart[1])&&(actualTag[2] == boundingBoxStart[2]))
						{
							boolean foundBbStart = true; 
							//se actualTag contiene '<Data name="boundingBox">', il booleano foundbBStart rimane a true
							for(int i = 0; i < boundingBoxStart.length; i++)
								if(actualTag[i] != boundingBoxStart[i])
									foundBbStart = false;
								
							//se actualTag contiene '<Data name="boundingBox">', allora sono in presenza di informazioni utili e imposto la variabile booleana
							//boundingBoxFound a true, che rimarrà così fino a quando non verrà rilevato il corrispondete tag di chiusura
							if(foundBbStart)
							{
								//Log.d("Map", "copyStreamBytePerByte()--> open tag: " +new String(actualTag, 0, actualTagIndex+1));
								boundingBoxFound = true;
								counterBbStart++; //incremento il numero di tag '<Data name="boundingBox">' aperti
							}		
						}
						//se il tag '<Data name="boundingBox">' risulta aperto, il terzo carattere di actualTag è uguale a 'D', verifico che actualTag contenga il tag
						//di chiusura '</Data>'
						else if((boundingBoxFound)&&(actualTag[2] == boundingBoxEnd[2]))
						{
							boolean foundBbEnd = true;
							for(int i = 0; i < boundingBoxEnd.length; i++)
								if(actualTag[i] != boundingBoxEnd[i])
									foundBbEnd = false;
								
							//se actualTag contiene il tag di chiusura '</Data>', allora ho raccolto informazioni utili racchiuse nella coppia di tags 
							//'<Data name="boundingBox">...</Data>'
							if(foundBbEnd)
							{
								//Log.d("Map", "copyStreamBytePerByte()--> close tag: " +new String(actualTag, 0, actualTagIndex+1));
								boundingBoxFound = false;
								counterBbEnd++;
								
								//elaboro il contenuto del tag fin qui rilevato: sono le coordinate del quadrante
								String coordsStr = new String(tagContent, 0, tagContentIndex);
								//Log.d("Map", "copyStreamBytePerByte()--> tag content: " +coordsStr);
								coordsStr = coordsStr.substring(1, coordsStr.length()-1); //scarto le parentesi quadre che racchiudo le coordinate
								
								try
								{
									minLon = Double.valueOf(coordsStr.split(", ")[0]);
									minLat = Double.valueOf(coordsStr.split(", ")[1]);
									maxLon = Double.valueOf(coordsStr.split(", ")[2]);
									maxLat = Double.valueOf(coordsStr.split(", ")[3]);
								}
								catch(Exception e)
								{
									e.printStackTrace();
								}
								
								tagContentIndex = 0; //resetto indice sul buffer tagContent
							}		
						}
						
						/************ RILEVAMENTO TAG '<Data name="bcSensor"> ***************/
						
						//caso di elaborazione del tag '<Data name="bcSensor">' (si può escludere dall'elaborazione il caso in cui sia correntemente elaborato il tag '<Data name="boundingBox">')
						if(!boundingBoxFound)
						{
							//se il secondo carattere di actualTag è una 'D' e il terzo è una 'a', allora ho trovato un tag del tipo '<Data name=...>' 
							//e devo verificare che sia un tag '<Data name="bcSensor">'
							if((actualTag[1] == bcSensorStart[1])&&(actualTag[2] == bcSensorStart[2]))
							{
								boolean foundBcSstart = true; 
								//se actualTag contiene '<Data name="bcSensor">', il booleano foundbBcSstart rimane a true
								for(int i = 0; i < bcSensorStart.length; i++)
									if(actualTag[i] != bcSensorStart[i])
										foundBcSstart = false;
									
								//se actualTag contiene '<Data name="boundingBox">', allora sono in presenza di informazioni utili e imposto la variabile booleana
								//boundingBoxFound a true, che rimarrà così fino a quando non verrà rilevato il corrispondete tag di chiusura
								if(foundBcSstart)
								{
									//Log.d("Map", "copyStreamBytePerByte()--> open tag: " +new String(actualTag, 0, actualTagIndex+1));
									bcSensorFound = true;
									counterBcSStart++; //incremento il numero di tag '<Data name="bcSensor">' aperti
								}	
							}
							//se il tag '<Data name="bcSensor">' risulta aperto, il terzo carattere di actualTag è uguale a 'D', verifico che actualTag contenga il tag
							//di chiusura '</Data>'
							else if((bcSensorFound)&&(actualTag[2] == boundingBoxEnd[2]))
							{
								boolean foundBcSend = true;
								for(int i = 0; i < boundingBoxEnd.length; i++)
									if(actualTag[i] != boundingBoxEnd[i])
										foundBcSend = false;
									
								//se actualTag contiene il tag di chiusura '</Data>', allora ho raccolto informazioni utili racchiuse nella coppia di tags 
								//'<Data name="boundingBox">...</Data>'
								if(foundBcSend)
								{
									//Log.d("Map", "copyStreamBytePerByte()--> close tag: " +new String(actualTag, 0, actualTagIndex+1));
									bcSensorFound = false;
									counterBcSEnd++;
									
									//elaboro il contenuto del tag fin qui rilevato ( è il livello di black carbon)
									String bcLevelStr = new String(tagContent, 0, tagContentIndex);
									
									//Log.d("Map", "copyStreamBytePerByte()--> tag content: " +bcLevelStr);
									
									if(bcLevelStr.equalsIgnoreCase("NaN"))
										bcLevel = 0;
									else
									{
										try
										{
											bcLevel = Double.valueOf(bcLevelStr);
										}
										catch(Exception e)
										{
											e.printStackTrace();
										}
									}
																		
									tagContentIndex = 0; //resetto indice sul buffer tagContent
									
									//se ho rilevato lo stesso numero per i due tipi di tag, OK e aggiungo alla lista
									if(counterBcSEnd == counterBbEnd)
										mapClusters.add(new MapCluster(bcLevel, minLat, minLon, maxLat, maxLon));
									//altrimenti errore!
									else
										Log.d("Map", "copyStreamBytePerByte()--> ERROR IN TAGS COUNTER! " +counterBbEnd+ " " +counterBcSEnd);
								}									
							}
						}
						 
						//il tag chiuso è stato elaborato (riconosciuto come interessante oppure no); quindi resetto a 0 l'indice sul buffer actualTag per
						//riutilizzare il buffer
						actualTagIndex = 0; 	
						
					} /*************** FINE IF TAG RILEVATO INTERAMENTE *************/				
				}
				else
				{
					//se il tag <Data name="boundingBox"> è attualmente riconosciuto, ne copio il contenuto
					if(boundingBoxFound)
					{									
						tagContent[tagContentIndex] = buffer[0]; //copio il carattere attuale anche nel buffer che conterrà il contenuto del tag
						tagContentIndex++;			
					}			
					
					//se il tag <Data name="bcSensor"> è attualmente riconosciuto, ne copio il contenuto
					if(bcSensorFound)
					{									
						tagContent[tagContentIndex] = buffer[0]; //copio il carattere attuale anche nel buffer che conterrà il contenuto del tag
						tagContentIndex++;			
					}								
				}
		
			}
			Log.d("Map", "copyStreamBytePerByte()--> # di tag <Data name=\"boundingBox\">: " +counterBbStart+ " e # di chiusure: " +counterBbEnd);
			Log.d("Map", "copyStreamBytePerByte()--> # di tag <Data name=\"bcSensor\">: " +counterBcSStart+ " e # di chiusure: " +counterBcSEnd);
		}
		catch(Exception e)
		{
			e.printStackTrace();			
		} 

		Log.d("Map", "copyStreamBytePerByte()--> # di map clusters: "+mapClusters.size());
		
		return mapClusters;
    }
}
